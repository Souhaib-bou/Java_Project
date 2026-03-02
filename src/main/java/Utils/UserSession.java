package Utils;

import Models.User;

public class UserSession {

    private static UserSession instance;

    private User currentUser;

    // NEW: JWT token returned by Spring Boot login
    private String token;

    private UserSession() {}

    public static UserSession getInstance() {
        if (instance == null) instance = new UserSession();
        return instance;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    // NEW
    public String getToken() {
        return token;
    }

    // NEW
    public void setToken(String token) {
        this.token = token;
    }

    public boolean isLoggedIn() {
        return currentUser != null && token != null && !token.isBlank();
    }

    public void clear() {
        currentUser = null;
        token = null;
    }
}