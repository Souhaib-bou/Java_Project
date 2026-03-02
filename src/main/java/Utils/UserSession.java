package Utils;

import Models.User;

public class UserSession {

    private static UserSession instance;

    private User currentUser;

    private UserSession() {}

<<<<<<< HEAD
=======
<<<<<<< HEAD
    /**
     * Returns the instance value.
     */
=======
>>>>>>> e83af0e702d3bb2c83b5340e20de94cbf3d1e24c
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
    public static UserSession getInstance() {
        if (instance == null) instance = new UserSession();
        return instance;
    }

<<<<<<< HEAD
=======
<<<<<<< HEAD
    /**
     * Returns the currentuser value.
     */
=======
>>>>>>> e83af0e702d3bb2c83b5340e20de94cbf3d1e24c
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
    public User getCurrentUser() {
        return currentUser;
    }

<<<<<<< HEAD
=======
<<<<<<< HEAD
    /**
     * Sets the currentuser value.
     */
=======
>>>>>>> e83af0e702d3bb2c83b5340e20de94cbf3d1e24c
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

<<<<<<< HEAD
=======
<<<<<<< HEAD
    /**
     * Checks whether loggedin is enabled.
     */
=======
>>>>>>> e83af0e702d3bb2c83b5340e20de94cbf3d1e24c
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
    public boolean isLoggedIn() {
        return currentUser != null;
    }

<<<<<<< HEAD
=======
<<<<<<< HEAD
    /**
     * Executes this operation.
     */
=======
>>>>>>> e83af0e702d3bb2c83b5340e20de94cbf3d1e24c
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
    public void clear() {
        currentUser = null;
    }
}
