package Services;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

public class EmailService {

    private static final String SMTP_HOST     = "smtp.gmail.com";
    private static final int    SMTP_PORT     = 587;
    private static final String FROM_EMAIL    = "xabobaker@gmail.com";   // <-- replace
    private static final String APP_PASSWORD  = "arhe idpx mxix bcaj";       // <-- replace (16-char Gmail App Password)

    public void sendOtpEmail(String toAddress, String otpCode) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.trust",       SMTP_HOST);
        props.put("mail.smtp.host",            SMTP_HOST);
        props.put("mail.smtp.port",            String.valueOf(SMTP_PORT));

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, APP_PASSWORD);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(FROM_EMAIL));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toAddress));
        message.setSubject("Hirely — Password Reset Code");
        message.setText(
                "Hello,\n\n" +
                "Your password reset code is: " + otpCode + "\n\n" +
                "This code expires in 10 minutes.\n\n" +
                "If you did not request a password reset, please ignore this email.\n\n" +
                "— The Hirely Team"
        );

        Transport.send(message);
    }
}
