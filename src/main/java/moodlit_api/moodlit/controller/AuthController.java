package moodlit_api.moodlit.controller;

import moodlit_api.moodlit.dto.AuthRequests;
import moodlit_api.moodlit.dto.AuthResponses;
import moodlit_api.moodlit.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    // POST /api/auth/signup
    @PostMapping("/signup")
    public ResponseEntity<AuthResponses.Auth> signUp(
            @Valid @RequestBody AuthRequests.SignUp request) {
        return ResponseEntity.ok(authService.signUp(request));
    }

    // POST /api/auth/login
    @PostMapping("/login")
    public ResponseEntity<AuthResponses.Auth> login(
            @Valid @RequestBody AuthRequests.Login request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // POST /api/auth/apple
    @PostMapping("/apple")
    public ResponseEntity<AuthResponses.Auth> appleLogin(
            @Valid @RequestBody AuthRequests.AppleLogin request) {
        return ResponseEntity.ok(authService.appleLogin(request));
    }

    // POST /api/auth/forgot-password
    @PostMapping("/forgot-password")
    public ResponseEntity<AuthResponses.Message> forgotPassword(
            @Valid @RequestBody AuthRequests.ForgotPassword request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    // POST /api/auth/verify-code
    @PostMapping("/verify-code")
    public ResponseEntity<AuthResponses.Message> verifyCode(
            @Valid @RequestBody AuthRequests.VerifyCode request) {
        return ResponseEntity.ok(authService.verifyCode(request));
    }

    // POST /api/auth/reset-password
    @PostMapping("/reset-password")
    public ResponseEntity<AuthResponses.Message> resetPassword(
            @Valid @RequestBody AuthRequests.ResetPassword request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }
}
