package com.gl.analyticsservice.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendWelcomeEmail(String toEmail, String patientName, String patientId, String accountId, String billingAmount, String diagnosis, String country) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Welcome to Patient Management System!");
            helper.setText(buildEmailBody(patientName, patientId, toEmail, accountId, billingAmount, diagnosis, country), true);

            mailSender.send(message);
            log.info("Welcome email sent to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send email to: {} error: {}", toEmail, e.getMessage());
        }
    }

    private String buildEmailBody(String name, String patientId, String email, String accountId, String billingAmount, String diagnosis, String country) {
        return """
                <html>
                <body style="font-family: Arial, sans-serif; padding: 20px;">
                    <h2 style="color: #2E86AB;">Welcome to Patient Management System!</h2>
                    <p>Dear <strong>%s</strong>,</p>
                    <p>Your patient account has been successfully created.</p>
                    
                    <h3 style="color: #444;">Patient Details:</h3>
                    <table style="border-collapse: collapse; width: 100%%;">
                        <tr style="background-color: #f2f2f2;">
                            <td style="padding: 8px; border: 1px solid #ddd;"><strong>Patient ID</strong></td>
                            <td style="padding: 8px; border: 1px solid #ddd;">%s</td>
                        </tr>
                        <tr>
                            <td style="padding: 8px; border: 1px solid #ddd;"><strong>Email</strong></td>
                            <td style="padding: 8px; border: 1px solid #ddd;">%s</td>
                        </tr>
                        <tr style="background-color: #f2f2f2;">
                            <td style="padding: 8px; border: 1px solid #ddd;"><strong>Diagnosis</strong></td>
                            <td style="padding: 8px; border: 1px solid #ddd;">%s</td>
                        </tr>
                        <tr>
                            <td style="padding: 8px; border: 1px solid #ddd;"><strong>Country</strong></td>
                            <td style="padding: 8px; border: 1px solid #ddd;">%s</td>
                        </tr>
                    </table>
                    
                    <h3 style="color: #444;">Billing Details:</h3>
                    <table style="border-collapse: collapse; width: 100%%;">
                        <tr style="background-color: #f2f2f2;">
                            <td style="padding: 8px; border: 1px solid #ddd;"><strong>Account ID</strong></td>
                            <td style="padding: 8px; border: 1px solid #ddd;">%s</td>
                        </tr>
                        <tr>
                            <td style="padding: 8px; border: 1px solid #ddd;"><strong>Billing Amount</strong></td>
                            <td style="padding: 8px; border: 1px solid #ddd;">$%s</td>
                        </tr>
                        <tr style="background-color: #f2f2f2;">
                            <td style="padding: 8px; border: 1px solid #ddd;"><strong>Status</strong></td>
                            <td style="padding: 8px; border: 1px solid #ddd; color: green;"><strong>ACTIVE</strong></td>
                        </tr>
                    </table>
                    
                    <br/>
                    <p>Welcome aboard!</p>
                    <p style="color: #888;">Patient Management Team</p>
                </body>
                </html>
                """.formatted(name, patientId, email, diagnosis, country, accountId, billingAmount);
    }
}