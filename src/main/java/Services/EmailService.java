package Services;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class EmailService {

private static String API_KEY = System.getenv("SENDGRID_API_KEY");

    private static final String FROM_EMAIL = "boualleguisouhaib@gmail.com"; // Must be verified in SendGrid

    public static void sendApplicationNotification(
            String toEmail,
            String recruiterName,
            String candidateName,
            String jobTitle,
            int appId
    ) {
        new Thread(() -> {
            try {
                URL url = new URL("https://api.sendgrid.com/v3/mail/send");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                String htmlBody = "<div style='font-family:Arial,sans-serif;padding:20px;'>"
                        + "<h2 style='color:#2c3e50;'>New Job Application Received</h2>"
                        + "<p>Hello <strong>" + recruiterName + "</strong>,</p>"
                        + "<p>A new application has been submitted for: <strong>" + jobTitle + "</strong></p>"
                        + "<table style='border-collapse:collapse;width:100%'>"
                        + "<tr><td style='padding:8px;border:1px solid #ddd'><strong>Candidate</strong></td>"
                        + "<td style='padding:8px;border:1px solid #ddd'>" + candidateName + "</td></tr>"
                        + "<tr><td style='padding:8px;border:1px solid #ddd'><strong>Application ID</strong></td>"
                        + "<td style='padding:8px;border:1px solid #ddd'>#" + appId + "</td></tr>"
                        + "</table>"
                        + "<p>Please log in to the HR system to review this application.</p>"
                        + "</div>";

                String safeHtml = htmlBody.replace("\"", "\\\"");
                String safeSubject = ("New Application Received - " + jobTitle).replace("\"", "\\\"");
                String safeRecruiterName = recruiterName.replace("\"", "\\\"");

                String json = "{"
                        + "\"personalizations\":[{\"to\":[{\"email\":\"" + toEmail + "\"}]}],"
                        + "\"from\":{\"email\":\"" + FROM_EMAIL + "\",\"name\":\"HR Application System\"},"
                        + "\"subject\":\"" + safeSubject + "\","
                        + "\"content\":[{\"type\":\"text/html\",\"value\":\"" + safeHtml + "\"}]"
                        + "}";

                byte[] input = json.getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(input);
                }

                int status = conn.getResponseCode();
                if (status == 202) {
                    System.out.println("✅ Email sent successfully to " + toEmail);
                } else {
                    System.err.println("❌ SendGrid error. Status: " + status);
                }

                conn.disconnect();

            } catch (Exception e) {
                System.err.println("❌ Failed to send email: " + e.getMessage());
            }
        }).start();
    }


    public static void sendStatusUpdateNotification(
            String toEmail,
            String candidateName,
            String jobTitle,
            String newStatus
    ) {
        new Thread(() -> {
            try {
                String statusColor = newStatus.equalsIgnoreCase("Accepted") ? "#16a34a" : "#dc2626";
                String statusEmoji = newStatus.equalsIgnoreCase("Accepted") ? "🎉" : "📋";

                String htmlBody = "<div style='font-family:Arial,sans-serif;padding:20px;'>"
                        + "<h2 style='color:#0f172a;'>Application Status Update</h2>"
                        + "<p>Hello <strong>" + candidateName + "</strong>,</p>"
                        + "<p>Your application for <strong>" + jobTitle + "</strong> has been reviewed.</p>"
                        + "<p>Your application status is now:</p>"
                        + "<div style='display:inline-block;padding:10px 24px;background-color:" + statusColor + ";"
                        + "color:white;border-radius:8px;font-size:18px;font-weight:bold;margin:10px 0;'>"
                        + statusEmoji + " " + newStatus
                        + "</div>"
                        + (newStatus.equalsIgnoreCase("Accepted")
                        ? "<p>Congratulations! The recruiter will be in touch with next steps.</p>"
                        : "<p>Thank you for your interest. We encourage you to apply for future opportunities.</p>")
                        + "<p style='color:#64748b;font-size:12px;'>This is an automated message. Please do not reply.</p>"
                        + "</div>";

                String safeHtml = htmlBody.replace("\"", "\\\"");
                String safeSubject = ("Your Application Status: " + newStatus + " – " + jobTitle).replace("\"", "\\\"");
                String safeName = candidateName.replace("\"", "\\\"");

                String json = "{"
                        + "\"personalizations\":[{\"to\":[{\"email\":\"" + toEmail + "\"}]}],"
                        + "\"from\":{\"email\":\"" + FROM_EMAIL + "\",\"name\":\"HR Application System\"},"
                        + "\"subject\":\"" + safeSubject + "\","
                        + "\"content\":[{\"type\":\"text/html\",\"value\":\"" + safeHtml + "\"}]"
                        + "}";

                URL url = new URL("https://api.sendgrid.com/v3/mail/send");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }

                int status = conn.getResponseCode();
                if (status == 202) {
                    System.out.println("✅ Status email sent to " + toEmail);
                } else {
                    System.err.println("❌ SendGrid error: " + status);
                }
                conn.disconnect();

            } catch (Exception e) {
                System.err.println("❌ Failed to send status email: " + e.getMessage());
            }
        }).start();
    }
}