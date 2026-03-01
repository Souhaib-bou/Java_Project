package Services;

import Models.User;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import Utils.CustomLocalReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GoogleAuthService {

<<<<<<< HEAD
=======

>>>>>>> e83af0e702d3bb2c83b5340e20de94cbf3d1e24c
    private static final List<String> SCOPES = Arrays.asList("openid", "email", "profile");

    private final UserService userService = new UserService();

    /**
     * Runs the Google OAuth 2.0 loopback flow.
     * Opens the user's browser, waits for the callback, verifies the ID token,
     * and returns the matching (or newly created) User, or null on cancellation.
     */
    public User authenticateWithGoogle() throws Exception {
        NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
        GsonFactory      jsonFactory = GsonFactory.getDefaultInstance();

        // Build client secrets directly from hardcoded credentials
        GoogleClientSecrets.Details details = new GoogleClientSecrets.Details()
                .setClientId(CLIENT_ID)
                .setClientSecret(CLIENT_SECRET);
        GoogleClientSecrets clientSecrets = new GoogleClientSecrets().setInstalled(details);

        // Build the authorization flow with a loopback receiver
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                transport, jsonFactory, clientSecrets, SCOPES)
                .setDataStoreFactory(new MemoryDataStoreFactory())
                .setAccessType("online")
                .build();

        CustomLocalReceiver receiver = new CustomLocalReceiver();

        // This blocks the calling thread until the user completes auth in browser
        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver)
                .authorize("user");

        if (credential == null || credential.getAccessToken() == null) {
            return null;
        }

        // Call Google's UserInfo endpoint to get profile data
        HttpResponse response = transport
                .createRequestFactory(credential)
                .buildGetRequest(new GenericUrl("https://www.googleapis.com/oauth2/v3/userinfo"))
                .execute();

        @SuppressWarnings("unchecked")
        Map<String, Object> userInfo = jsonFactory.createJsonParser(response.parseAsString())
                .parseAndClose(HashMap.class);

        String googleId  = (String) userInfo.get("sub");
        String email     = (String) userInfo.get("email");
        String firstName = userInfo.getOrDefault("given_name", "").toString();
        String lastName  = userInfo.getOrDefault("family_name", "").toString();

        return findOrCreateUser(googleId, email, firstName, lastName);
    }

    private User findOrCreateUser(String googleId, String email,
                                  String firstName, String lastName) throws SQLException {
        // 1. Look up by Google ID
        User existing = userService.findByGoogleId(googleId);
        if (existing != null) return existing;

        // 2. Look up by email — link the Google ID if account already exists
        User byEmail = userService.findByEmail(email);
        if (byEmail != null) {
            // Link google_id to existing account
            userService.saveFaceData(byEmail.getUserId(), byEmail.getFaceData()); // no-op placeholder
            // Direct update for google_id linkage:
            linkGoogleId(byEmail.getUserId(), googleId);
            byEmail.setGoogleId(googleId);
            return byEmail;
        }

        // 3. Create new account
        int newId = userService.addGoogleUser(googleId, email, firstName, lastName);
        User u = new User(newId, firstName, lastName, email, "", null, "active");
        u.setGoogleId(googleId);
        return u;
    }

    private void linkGoogleId(int userId, String googleId) throws SQLException {
        java.sql.Connection cnx = Utils.MyDB.getInstance().getConnection();
        java.sql.PreparedStatement ps = cnx.prepareStatement(
                "UPDATE users SET google_id = ? WHERE user_id = ?");
        ps.setString(1, googleId);
        ps.setInt(2, userId);
        ps.executeUpdate();
    }
}
