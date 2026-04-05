package Services;

import Models.User;
import Utils.MyDB;
import jakarta.mail.MessagingException;

import java.sql.*;
import java.util.Random;

public class PasswordResetService {

    private final Connection cnx;
    private final UserService userService;
    private final EmailService emailService;

    public PasswordResetService() {
        cnx          = MyDB.getInstance().getConnection();
        userService  = new UserService();
        emailService = new EmailService();
    }

    /**
     * Generates a 6-digit OTP, stores it in password_reset_otp (expires in 10 min),
     * and sends it to the given email. Returns false if the email doesn't exist.
     * Throws if an unexpired OTP already exists (rate-limit guard).
     */
    public boolean generateAndSendOtp(String email) throws SQLException, MessagingException {
        User user = userService.findByEmail(email);
        if (user == null) return false;

        // Rate-limit: reject if an unexpired, unused OTP already exists
        String checkSql =
                "SELECT id FROM password_reset_otp " +
                "WHERE user_id = ? AND used = 0 AND expires_at > NOW() LIMIT 1";
        PreparedStatement check = cnx.prepareStatement(checkSql);
        check.setInt(1, user.getUserId());
        ResultSet checkRs = check.executeQuery();
        if (checkRs.next()) {
            throw new IllegalStateException("A reset code was already sent. Please wait before requesting another.");
        }

        String otp = String.format("%06d", new Random().nextInt(1_000_000));

        // Send email FIRST — only persist if send succeeds
        emailService.sendOtpEmail(email, otp);

        String insertSql =
                "INSERT INTO password_reset_otp (user_id, otp_code, expires_at) " +
                "VALUES (?, ?, DATE_ADD(NOW(), INTERVAL 10 MINUTE))";
        PreparedStatement ps = cnx.prepareStatement(insertSql);
        ps.setInt(1, user.getUserId());
        ps.setString(2, otp);
        ps.executeUpdate();

        return true;
    }

    /**
     * Returns true if the OTP is valid (correct, unexpired, unused) for the given email.
     */
    public boolean verifyOtp(String email, String otpCode) throws SQLException {
        User user = userService.findByEmail(email);
        if (user == null) return false;

        String sql =
                "SELECT id FROM password_reset_otp " +
                "WHERE user_id = ? AND otp_code = ? AND used = 0 AND expires_at > NOW() LIMIT 1";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, user.getUserId());
        ps.setString(2, otpCode.trim());
        ResultSet rs = ps.executeQuery();
        return rs.next();
    }

    /**
     * Resets the password after verifying the OTP. Marks the OTP as used.
     * Returns true on success.
     */
    public boolean resetPassword(String email, String otpCode, String newPassword) throws SQLException {
        if (!verifyOtp(email, otpCode)) return false;

        User user = userService.findByEmail(email);
        if (user == null) return false;

        // Update password
        String updateSql = "UPDATE users SET password = ? WHERE user_id = ?";
        PreparedStatement update = cnx.prepareStatement(updateSql);
        update.setString(1, newPassword);
        update.setInt(2, user.getUserId());
        update.executeUpdate();

        // Mark OTP as used
        String usedSql =
                "UPDATE password_reset_otp SET used = 1 " +
                "WHERE user_id = ? AND otp_code = ? AND used = 0";
        PreparedStatement usedPs = cnx.prepareStatement(usedSql);
        usedPs.setInt(1, user.getUserId());
        usedPs.setString(2, otpCode.trim());
        usedPs.executeUpdate();

        return true;
    }
}
