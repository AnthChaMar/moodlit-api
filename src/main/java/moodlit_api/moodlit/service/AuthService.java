package moodlit_api.moodlit.service;


import moodlit_api.moodlit.dto.AuthRequests;
import moodlit_api.moodlit.dto.AuthResponses;
import moodlit_api.moodlit.model.User;
import moodlit_api.moodlit.model.User.AuthProvider;
import moodlit_api.moodlit.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ─── Sign Up (email/password) ───

    public AuthResponses.Auth signUp(AuthRequests.SignUp request) {
        if (userRepository.existsByEmail(request.getEmail().toLowerCase())) {
            throw new RuntimeException("An account with this email already exists");
        }

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(request.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .authProvider(AuthProvider.EMAIL)
                .build();

        user = userRepository.save(user);
        return buildAuthResponse(user);
    }

    // ─── Log In (email/password) ───

    public AuthResponses.Auth login(AuthRequests.Login request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        // Prevent email login on Apple accounts
        if (user.getAuthProvider() == AuthProvider.APPLE) {
            throw new RuntimeException("This account uses Apple Sign In");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        return buildAuthResponse(user);
    }

    // ─── Apple Sign In ───
    // First login: creates account
    // Subsequent logins: finds existing account by appleUserId

    public AuthResponses.Auth appleLogin(AuthRequests.AppleLogin request) {
        // 1. Decode Apple's identity token to get the unique user ID
        String appleUserId = decodeAppleToken(request.getIdentityToken());

        // 2. Check if this Apple user already exists
        var existingUser = userRepository.findByAppleUserId(appleUserId);

        if (existingUser.isPresent()) {
            return buildAuthResponse(existingUser.get());
        }

        // 3. First time — create account
        // Apple only sends name/email on the very first sign in
        String email = request.getEmail();
        String name = request.getFullName();

        if (email == null || email.isBlank()) {
            email = appleUserId + "@privaterelay.appleid.com";
        }
        if (name == null || name.isBlank()) {
            name = "MoodLit Reader";
        }

        // Check if email already exists (user previously signed up with email)
        var emailUser = userRepository.findByEmail(email.toLowerCase());
        if (emailUser.isPresent()) {
            // Link Apple ID to existing email account
            User user = emailUser.get();
            user.setAppleUserId(appleUserId);
            user.setAuthProvider(AuthProvider.APPLE);
            user = userRepository.save(user);
            return buildAuthResponse(user);
        }

        // Brand new user
        User user = User.builder()
                .fullName(name.trim())
                .email(email.toLowerCase().trim())
                .appleUserId(appleUserId)
                .authProvider(AuthProvider.APPLE)
                .build();

        user = userRepository.save(user);
        return buildAuthResponse(user);
    }

    // ─── Forgot Password ───

    public AuthResponses.Message forgotPassword(AuthRequests.ForgotPassword request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new RuntimeException("No account found with this email"));

        // Apple users don't have passwords
        if (user.getAuthProvider() == AuthProvider.APPLE) {
            throw new RuntimeException("This account uses Apple Sign In. No password to reset.");
        }

        String code = generateCode();
        user.setResetCode(code);
        user.setResetCodeExpiry(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        emailService.sendResetCode(user.getEmail(), code);

        return new AuthResponses.Message("Reset code sent to your email");
    }

    // ─── Verify Code ───

    public AuthResponses.Message verifyCode(AuthRequests.VerifyCode request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new RuntimeException("No account found with this email"));

        validateResetCode(user, request.getCode());

        return new AuthResponses.Message("Code verified successfully");
    }

    // ─── Reset Password ───

    public AuthResponses.Message resetPassword(AuthRequests.ResetPassword request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new RuntimeException("No account found with this email"));

        validateResetCode(user, request.getCode());

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetCode(null);
        user.setResetCodeExpiry(null);
        userRepository.save(user);

        return new AuthResponses.Message("Password reset successfully");
    }

    // ─── Helpers ───

    private AuthResponses.Auth buildAuthResponse(User user) {
        String token = jwtService.generateToken(user.getId(), user.getEmail());
        AuthResponses.UserInfo userInfo = new AuthResponses.UserInfo(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getAuthProvider().name()
        );
        return new AuthResponses.Auth(token, userInfo);
    }

    private void validateResetCode(User user, String code) {
        if (user.getResetCode() == null) {
            throw new RuntimeException("No reset code was requested");
        }
        if (user.getResetCodeExpiry().isBefore(LocalDateTime.now())) {
            user.setResetCode(null);
            user.setResetCodeExpiry(null);
            userRepository.save(user);
            throw new RuntimeException("Reset code has expired. Please request a new one");
        }
        if (!user.getResetCode().equals(code)) {
            throw new RuntimeException("Invalid reset code");
        }
    }

    private String generateCode() {
        return String.valueOf(100000 + new SecureRandom().nextInt(900000));
    }

    // Decodes Apple's identity token JWT to extract the 'sub' (user ID)
    // Apple tokens are signed by Apple — in production you should verify
    // the signature against Apple's public keys
    private String decodeAppleToken(String identityToken) {
        try {
            // Apple's identity token is a JWT with 3 parts: header.payload.signature
            String[] parts = identityToken.split("\\.");
            if (parts.length < 2) {
                throw new RuntimeException("Invalid Apple identity token");
            }

            // Decode the payload (base64)
            String payload = new String(
                    java.util.Base64.getUrlDecoder().decode(parts[1])
            );

            // Extract "sub" field — this is the Apple user ID
            // Using simple string parsing to avoid extra dependencies
            int subStart = payload.indexOf("\"sub\"");
            if (subStart == -1) {
                throw new RuntimeException("Apple token missing user ID");
            }
            String afterSub = payload.substring(subStart);
            int valueStart = afterSub.indexOf(":") + 1;
            String valueStr = afterSub.substring(valueStart).trim();

            // Remove quotes
            if (valueStr.startsWith("\"")) {
                valueStr = valueStr.substring(1);
                int endQuote = valueStr.indexOf("\"");
                return valueStr.substring(0, endQuote);
            }

            throw new RuntimeException("Could not parse Apple user ID");
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode Apple identity token");
        }
    }
}