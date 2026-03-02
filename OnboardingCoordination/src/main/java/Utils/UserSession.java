package Utils;

public class UserSession {

    private static UserSession instance;

    private User currentUser;

    private UserSession() {}

    /**
     * Returns the instance value.
     */
    public static UserSession getInstance() {
        if (instance == null) instance = new UserSession();
        return instance;
    }

    /**
     * Returns the currentuser value.
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Sets the currentuser value.
     */
    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    /**
     * Checks whether loggedin is enabled.
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Executes this operation.
     */
    public void clear() {
        currentUser = null;
    }
}
