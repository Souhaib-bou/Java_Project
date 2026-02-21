package util;

/**
 * In-memory session context for the current desktop user.
 * This is intentionally simple and supports local/dev role switching.
 */
public class Session {

    // App-level role gates used by JavaFX controllers to show/hide admin features.
    public enum Role {
        ADMIN, USER
    }

    // Enables local role-switch controls in UI for development/testing.
    public static final boolean DEV_MODE = true; // set to false for production
    public static boolean LIGHT_MODE = false;
    private static long currentUserId = 1;
    private static Role currentRole = Role.USER;

    public static long getCurrentUserId() {
        return currentUserId;
    }

    public static Role getCurrentRole() {
        return currentRole;
    }

    public static boolean isAdmin() {
        return currentRole == Role.ADMIN;
    }

    public static void set(long userId, Role role) {
        // Central session mutation used when switching dev users or escalating role.
        currentUserId = userId;
        currentRole = role;
    }

    public static String getThemeStylesheetPath() {
        return LIGHT_MODE ? "/styles/hirely-light.css" : "/styles/hirely.css";
    }
}
