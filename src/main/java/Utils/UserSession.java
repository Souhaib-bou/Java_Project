package Utils;

import Models.User;

public class UserSession {

    private static UserSession instance;

    private User currentUser;

    private UserSession() {}

<<<<<<< HEAD
    /**
     * Returns the instance value.
     */
=======
>>>>>>> e83af0e702d3bb2c83b5340e20de94cbf3d1e24c
    public static UserSession getInstance() {
        if (instance == null) instance = new UserSession();
        return instance;
    }

<<<<<<< HEAD
    /**
     * Returns the currentuser value.
     */
=======
>>>>>>> e83af0e702d3bb2c83b5340e20de94cbf3d1e24c
    public User getCurrentUser() {
        return currentUser;
    }

<<<<<<< HEAD
    /**
     * Sets the currentuser value.
     */
=======
>>>>>>> e83af0e702d3bb2c83b5340e20de94cbf3d1e24c
    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

<<<<<<< HEAD
    /**
     * Checks whether loggedin is enabled.
     */
=======
>>>>>>> e83af0e702d3bb2c83b5340e20de94cbf3d1e24c
    public boolean isLoggedIn() {
        return currentUser != null;
    }

<<<<<<< HEAD
    /**
     * Executes this operation.
     */
=======
>>>>>>> e83af0e702d3bb2c83b5340e20de94cbf3d1e24c
    public void clear() {
        currentUser = null;
    }
}
