package Services;

import Models.User;

import java.sql.SQLException;

public class AuthService {

    private final UserService userService = new UserService();

    public User login(String email, String password) throws SQLException {

        String em = (email == null) ? "" : email.trim();
        String pw = (password == null) ? "" : password; // keep as-is first

        if (em.isEmpty() || pw.isEmpty()) {
            return null;
        }

        User u = userService.findByEmail(em);

        // 1) email not found
        if (u == null) {
            System.out.println("❌ LOGIN FAIL: email not found -> [" + em + "]");
            return null;
        }

        // 2) inactive
        String status = (u.getStatus() == null) ? "active" : u.getStatus().trim();
        if (status.equalsIgnoreCase("inactive")) {
            System.out.println("❌ LOGIN FAIL: account inactive -> user_id=" + u.getUserId());
            return null;
        }

        // 3) password mismatch
        String dbPass = (u.getPassword() == null) ? "" : u.getPassword();

        // try exact match first
        if (pw.equals(dbPass)) {
            System.out.println("✅ LOGIN OK (exact) -> user_id=" + u.getUserId());
            return u;
        }

        // fallback: trimmed compare (helps if you accidentally stored trailing spaces)
        if (pw.trim().equals(dbPass.trim())) {
            System.out.println("✅ LOGIN OK (trim fallback) -> user_id=" + u.getUserId());
            return u;
        }

        System.out.println("❌ LOGIN FAIL: password mismatch -> user_id=" + u.getUserId()
                + " | dbLen=" + dbPass.length() + " inputLen=" + pw.length());
        return null;
    }
}
