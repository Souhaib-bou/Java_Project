package Services;

import Models.User;
import Utils.UserSession;
import Utils.api.ApiClient;

public class AuthService {

    private final UserService userService = new UserService();

    public User login(String email, String password) throws Exception {

        String em = (email == null) ? "" : email.trim();
        String pw = (password == null) ? "" : password;

        if (em.isEmpty() || pw.isEmpty()) return null;

        String body = "{"
                + "\"email\":\"" + escape(em) + "\","
                + "\"password\":\"" + escape(pw) + "\""
                + "}";

        // 1) Call backend login
        String json = ApiClient.post("/api/auth/login", body);

        // 2) Extract token + ids (simple parsing, no extra libs)
        String token = extractString(json, "token");
        int userId = extractInt(json, "userId");
        int roleId = extractInt(json, "roleId");

        if (token == null || token.isBlank() || userId <= 0) {
            return null;
        }

        // 3) Load user details (from DB for now)
        User u = userService.findById(userId); // If you don't have findById, tell me and I’ll adjust
        if (u == null) {
            // fallback: at least build a minimal User object
            u = new User();
            u.setUserId(userId);
            u.setRoleId(roleId);
            u.setEmail(em);
        } else {
            u.setRoleId(roleId); // ensure role matches backend
        }

        // 4) Save session
        UserSession.getInstance().setCurrentUser(u);
        UserSession.getInstance().setToken(token);

        return u;
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String extractString(String json, String key) {
        String needle = "\"" + key + "\":\"";
        int i = json.indexOf(needle);
        if (i < 0) return null;
        int start = i + needle.length();
        int end = json.indexOf("\"", start);
        if (end < 0) return null;
        return json.substring(start, end);
    }

    private static int extractInt(String json, String key) {
        String needle = "\"" + key + "\":";
        int i = json.indexOf(needle);
        if (i < 0) return 0;
        int start = i + needle.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
        return Integer.parseInt(json.substring(start, end));
    }
}