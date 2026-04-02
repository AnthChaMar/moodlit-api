package moodlit_api.moodlit.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendResetCode(String toEmail, String code) {
        System.out.println("📧 Sending code: " + code + " to: " + toEmail);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("achangdevapp@gmail.com");
        message.setTo(toEmail);
        message.setSubject("MoodLit - Password Reset Code");
        message.setText(
                "Hi there,\n\n" +
                        "Your password reset code is: " + code + "\n\n" +
                        "This code expires in 10 minutes.\n\n" +
                        "If you didn't request this, you can safely ignore this email.\n\n" +
                        "- MoodLit Team"
        );
        mailSender.send(message);
    }
}