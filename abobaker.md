# Hirely — Project Documentation
### Everything you need to set up, run, and explain to your teacher

---

## Table of Contents

1. [What is this project?](#1-what-is-this-project)
2. [How to set up and run it](#2-how-to-set-up-and-run-it)
3. [Project architecture](#3-project-architecture)
4. [Feature 1 — Forgot Password with Email OTP](#4-feature-1--forgot-password-with-email-otp)
5. [Feature 2 — Google OAuth Login](#5-feature-2--google-oauth-login)
6. [Feature 3 — Face Recognition Login](#6-feature-3--face-recognition-login)
7. [Feature 4 — QR Code in Profile](#7-feature-4--qr-code-in-profile)
8. [Database changes we made](#8-database-changes-we-made)
9. [Libraries used and why](#9-libraries-used-and-why)

---

## 1. What is this project?

**Hirely** is a desktop JavaFX application for managing employee onboarding. It has user accounts, roles, onboarding plans, and tasks.

We added **four new features** on top of the existing project:

| Feature | What it does |
|---|---|
| Forgot Password | User enters email → receives a 6-digit code → resets password |
| Google OAuth Login | User clicks "Sign in with Google" → browser opens → logs in with Google account |
| Face Recognition | User sets up their face in profile → can log in by looking at the webcam |
| QR Code in Profile | Profile page shows a scannable QR code with the user's contact info |

---

## 2. How to set up and run it


```sql
ALTER TABLE users ADD COLUMN IF NOT EXISTS google_id VARCHAR(255) NULL DEFAULT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_google_id ON users (google_id);
ALTER TABLE users ADD COLUMN IF NOT EXISTS face_data TEXT NULL DEFAULT NULL;

CREATE TABLE IF NOT EXISTS password_reset_otp (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT        NOT NULL,
    otp_code   CHAR(6)    NOT NULL,
    created_at DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME   NOT NULL,
    used       TINYINT(1) NOT NULL DEFAULT 0,
    CONSTRAINT fk_otp_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### Step 2 — Check the database connection

Open `src/main/java/Utils/MyDB.java`. The default config is:
- Host: `localhost:3306`
- Database: `hirely`
- Username: `root`
- Password: *(empty)*

If your MySQL has a password, change line 22:
```java
private static final String PASSWORD = "your_password_here";
```

### Step 3 — Run the project

Open a terminal in the project folder and run:
```
mvn javafx:run
```

**Important:** The first time you run this, Maven will download about 600 MB of OpenCV (face recognition) libraries. This is normal and only happens once.

---

## 3. Project architecture

The project follows the **MVC pattern** (Model-View-Controller):

```
src/main/java/
├── Models/         ← Data classes (User, Role, Plan, Task)
├── Services/       ← Business logic + database queries
├── Controllers/    ← JavaFX event handlers (one per screen)
└── Utils/          ← Helpers (DB connection, session, QR code, etc.)

src/main/resources/
└── *.fxml          ← UI layout files (one per screen)
```

### How navigation works

The app uses a **single Stage** (window) with one Scene. To navigate between screens, we replace the root node of the Scene:

```java
Parent newScreen = FXMLLoader.load(getClass().getResource("/SomeView.fxml"));
Stage stage = (Stage) anyNode.getScene().getWindow();
stage.getScene().setRoot(newScreen);
```

This is cleaner than opening new windows — only the Face Setup dialog opens as a separate window (because it needs a webcam preview).

### How the database connection works

`Utils/MyDB.java` is a **Singleton** — only one database connection is created for the whole app:

```java
public static MyDB getInstance() {
    if (instance == null) instance = new MyDB();
    return instance;
}
```

Every service gets the connection like this:
```java
private final Connection cnx = MyDB.getInstance().getConnection();
```

### How the session works

`Utils/UserSession.java` is also a Singleton that stores the currently logged-in user in memory. After login:
```java
UserSession.getInstance().setCurrentUser(user);
```

Any screen can get the current user:
```java
User u = UserSession.getInstance().getCurrentUser();
```

---

## 4. Feature 1 — Forgot Password with Email OTP

### How it works (user perspective)
1. User clicks "Forgot password?" on the login screen
2. Enters their email address → clicks "Send Reset Code"
3. They receive an email with a 6-digit code (valid for 10 minutes)
4. They enter the code → click "Verify"
5. They enter a new password → click "Reset Password"
6. They are redirected back to the login screen

### Files created

#### `Services/EmailService.java`
Sends emails using Gmail's SMTP server.

```java
public void sendOtpEmail(String toAddress, String otpCode) throws MessagingException {
    Properties props = new Properties();
    props.put("mail.smtp.auth",            "true");
    props.put("mail.smtp.starttls.enable", "true");   // uses TLS encryption
    props.put("mail.smtp.ssl.trust",       SMTP_HOST); // trust Gmail's certificate
    props.put("mail.smtp.host",            SMTP_HOST); // smtp.gmail.com
    props.put("mail.smtp.port",            "587");     // Gmail SMTP port

    Session session = Session.getInstance(props, new Authenticator() {
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(FROM_EMAIL, APP_PASSWORD);
        }
    });

    Message message = new MimeMessage(session);
    message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toAddress));
    message.setSubject("Hirely — Password Reset Code");
    message.setText("Your password reset code is: " + otpCode + "\n\nExpires in 10 minutes.");
    Transport.send(message);
}
```

**Key detail:** We use a **Gmail App Password** (not the real Gmail password). This is a 16-character code generated in Google Account → Security → 2-Step Verification → App Passwords. It allows the app to send emails without storing the real password.

**Why `mail.smtp.ssl.trust`?** Java's default SSL truststore sometimes doesn't include Gmail's certificate. This property tells the mail library to trust Gmail's SMTP server explicitly, avoiding `SSLHandshakeException`.

---

#### `Services/PasswordResetService.java`
Handles the 3-step reset logic.

**`generateAndSendOtp(email)`** — Step 1:
```java
public boolean generateAndSendOtp(String email) throws SQLException, MessagingException {
    User user = userService.findByEmail(email);
    if (user == null) return false;  // no account with this email

    // Rate-limit: prevent spam — don't send another code if one is still valid
    String checkSql = "SELECT id FROM password_reset_otp " +
                      "WHERE user_id = ? AND used = 0 AND expires_at > NOW() LIMIT 1";
    PreparedStatement check = cnx.prepareStatement(checkSql);
    check.setInt(1, user.getUserId());
    if (check.executeQuery().next()) {
        throw new IllegalStateException("A reset code was already sent.");
    }

    // Generate a random 6-digit code
    String otp = String.format("%06d", new Random().nextInt(1_000_000));

    // IMPORTANT: send email FIRST, then save to DB
    // If the email fails, we don't save a code that the user never received
    emailService.sendOtpEmail(email, otp);

    String insertSql = "INSERT INTO password_reset_otp (user_id, otp_code, expires_at) " +
                       "VALUES (?, ?, DATE_ADD(NOW(), INTERVAL 10 MINUTE))";
    PreparedStatement ps = cnx.prepareStatement(insertSql);
    ps.setInt(1, user.getUserId());
    ps.setString(2, otp);
    ps.executeUpdate();
    return true;
}
```

**`verifyOtp(email, otp)`** — Step 2:
```java
public boolean verifyOtp(String email, String otpCode) throws SQLException {
    User user = userService.findByEmail(email);
    String sql = "SELECT id FROM password_reset_otp " +
                 "WHERE user_id = ? AND otp_code = ? AND used = 0 AND expires_at > NOW() LIMIT 1";
    PreparedStatement ps = cnx.prepareStatement(sql);
    ps.setInt(1, user.getUserId());
    ps.setString(2, otpCode.trim());
    return ps.executeQuery().next(); // true if a valid unexpired matching code exists
}
```

**`resetPassword(email, otp, newPassword)`** — Step 3:
```java
public boolean resetPassword(String email, String otpCode, String newPassword) throws SQLException {
    if (!verifyOtp(email, otpCode)) return false;

    // Update the user's password
    PreparedStatement update = cnx.prepareStatement("UPDATE users SET password = ? WHERE user_id = ?");
    update.setString(1, newPassword);
    update.setInt(2, user.getUserId());
    update.executeUpdate();

    // Mark the OTP as used so it cannot be reused
    PreparedStatement mark = cnx.prepareStatement(
        "UPDATE password_reset_otp SET used = 1 WHERE user_id = ? AND otp_code = ? AND used = 0");
    mark.setInt(1, user.getUserId());
    mark.setString(2, otpCode.trim());
    mark.executeUpdate();
    return true;
}
```

---

#### `Controllers/ForgotPasswordController.java`
Controls the 3-step form. Uses a clever UI trick: all 3 steps exist in the FXML at the same time, but only one is visible at a time. We toggle them using `setVisible()` and `setManaged()`:

```java
private void showPane(VBox target) {
    // Hide all 3 panes
    for (VBox p : new VBox[]{paneEmail, paneOtp, paneNewPassword}) {
        p.setVisible(false);
        p.setManaged(false);  // setManaged(false) removes it from layout (no empty space)
    }
    // Show only the target pane
    target.setVisible(true);
    target.setManaged(true);
}
```

The controller calls `showPane(paneOtp)` after sending the code, and `showPane(paneNewPassword)` after verifying it.

---

#### `resources/ForgotPasswordView.fxml`
Three `VBox` containers stacked. Two are hidden initially (`visible="false" managed="false"`). Each has its own fields and button.

---

## 5. Feature 2 — Google OAuth Login

### How it works (user perspective)
1. User clicks "Sign in with Google" on the login screen
2. Their browser opens and shows Google's sign-in page
3. They sign in with their Google account
4. Browser shows "Login successful! You can close this tab."
5. The desktop app receives their profile info and logs them in automatically

### How OAuth 2.0 works (for the teacher)

OAuth 2.0 is an **authorization protocol**. Instead of asking the user for a password, we redirect them to Google who authenticates them and gives us a temporary token. The flow:

```
App                    Google                   User's Browser
 |                        |                           |
 |---(1) Open browser---->|                           |
 |         with auth URL  |<--(2) User signs in-------|
 |                        |---(3) Redirect to our---->|
 |                        |    localhost callback URL  |
 |<--(4) We receive the authorization code------------|
 |---(5) Exchange code for access token-------------->|
 |<--(6) Google returns access token------------------|
 |---(7) Use token to call Google's API to get profile|
 |<--(8) Google returns name, email, Google ID--------|
 |
 |--> Find or create user in our DB
 |--> Log in the user
```

### Files created

#### `Services/GoogleAuthService.java`
The main OAuth service.

```java
public User authenticateWithGoogle() throws Exception {
    NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
    GsonFactory jsonFactory = GsonFactory.getDefaultInstance();

    // Our app's credentials from Google Cloud Console
    GoogleClientSecrets.Details details = new GoogleClientSecrets.Details()
            .setClientId(CLIENT_ID)
            .setClientSecret(CLIENT_SECRET);
    GoogleClientSecrets clientSecrets = new GoogleClientSecrets().setInstalled(details);

    // Build the OAuth flow requesting email and profile access
    GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
            transport, jsonFactory, clientSecrets, Arrays.asList("openid", "email", "profile"))
            .setDataStoreFactory(new MemoryDataStoreFactory())
            .setAccessType("online")
            .build();

    // Our custom receiver that starts a local HTTP server
    CustomLocalReceiver receiver = new CustomLocalReceiver();

    // This opens the browser and BLOCKS until Google redirects back
    Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

    // Call Google's UserInfo API to get profile data
    HttpResponse response = transport
            .createRequestFactory(credential)
            .buildGetRequest(new GenericUrl("https://www.googleapis.com/oauth2/v3/userinfo"))
            .execute();

    Map<String, Object> userInfo = jsonFactory.createJsonParser(response.parseAsString())
            .parseAndClose(HashMap.class);

    String googleId  = (String) userInfo.get("sub");    // unique Google user ID
    String email     = (String) userInfo.get("email");
    String firstName = userInfo.getOrDefault("given_name",  "").toString();
    String lastName  = userInfo.getOrDefault("family_name", "").toString();

    return findOrCreateUser(googleId, email, firstName, lastName);
}
```

`findOrCreateUser()` handles 3 cases:
1. User already logged in with Google before → find by `google_id`, return them
2. User has an account with same email but never used Google → link their `google_id` to the existing account
3. Brand new user → create a new account in the database

---

#### `Utils/CustomLocalReceiver.java`
This is the most technically interesting file. When Google redirects the browser after login, it redirects to `http://localhost:PORT/Callback`. We need a local HTTP server to receive that redirect and extract the authorization code from the URL.

We built this from scratch using Java's built-in `HttpServer`:

```java
public class CustomLocalReceiver implements VerificationCodeReceiver {

    private HttpServer server;
    private String code;
    private int port;
    private final Semaphore signal = new Semaphore(0); // used to block until callback arrives

    @Override
    public String getRedirectUri() throws IOException {
        // Let the OS pick a free port (port 0 = OS chooses)
        try (ServerSocket s = new ServerSocket(0)) {
            port = s.getLocalPort();
        }

        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/Callback", exchange -> {
            // Extract the "code" parameter from the URL query string
            String query = exchange.getRequestURI().getQuery();
            for (String param : query.split("&")) {
                String[] kv = param.split("=", 2);
                if ("code".equals(kv[0])) code = kv[1];
            }

            // Send a nice HTML page to the browser
            String html = "<h2>Login successful!</h2><p>You can close this tab.</p>";
            exchange.sendResponseHeaders(200, html.getBytes().length);
            exchange.getResponseBody().write(html.getBytes());
            exchange.getResponseBody().close();

            signal.release(); // unblock waitForCode()
        });
        server.start();
        return "http://localhost:" + port + "/Callback";
    }

    @Override
    public String waitForCode() throws IOException {
        signal.acquireUninterruptibly(); // BLOCKS here until Google calls our /Callback endpoint
        return code;
    }
}
```

**Why a Semaphore?** The OAuth flow happens on a background thread. `waitForCode()` needs to block that thread until Google's browser redirect arrives. A `Semaphore` with 0 permits does exactly this: `acquire()` blocks, and when the HTTP callback arrives it calls `release()` which unblocks it.

**Why port 0?** Using a fixed port like 8080 causes "Address already in use" errors if anything else is using that port. Port 0 tells the OS to pick any free port automatically.

---

#### `Controllers/LoginController.java` — handleGoogleLogin()
OAuth must run on a **background thread** because it blocks waiting for the user to sign in. If we ran it on the JavaFX Application Thread, the UI would freeze.

```java
@FXML
private void handleGoogleLogin() {
    lblMsg.setText("Opening browser for Google sign-in…");

    // Start a NEW background thread
    new Thread(() -> {
        try {
            User u = googleAuth.authenticateWithGoogle(); // blocks here until login done
            if (u == null) {
                Platform.runLater(() -> lblMsg.setText("Sign-in cancelled."));
                return;
            }
            UserSession.getInstance().setCurrentUser(u);
            Platform.runLater(this::loadMainShell); // MUST use Platform.runLater to update UI from background thread
        } catch (Exception e) {
            Platform.runLater(() -> lblMsg.setText("Error: " + e.getMessage()));
        }
    }, "google-auth-thread").start();
}
```

**Key rule in JavaFX:** UI updates must happen on the **JavaFX Application Thread**. `Platform.runLater()` schedules code to run on that thread from a background thread.

---

## 6. Feature 3 — Face Recognition Login

### How it works (user perspective)
**Setup (done once in profile):**
1. User goes to their profile
2. Clicks "Setup Face ID"
3. A webcam preview window opens
4. User clicks "Capture Face"
5. Their face is saved to the database

**Login:**
1. User clicks "Login with Face" on the login screen
2. Webcam screen opens showing live video
3. App scans for a face automatically every 500ms
4. When the face matches a registered user, they are logged in

### How face recognition works (for the teacher)

We use the **LBPH algorithm** (Local Binary Pattern Histogram). Here's how it works:

1. **Preprocessing**: Convert the image to grayscale, then apply histogram equalization to normalize lighting
2. **Face detection**: Use a Haar Cascade Classifier (a pre-trained XML file with 160,000 negative images) to find where the face is in the image
3. **Normalization**: Resize all detected faces to the same size (100×100 pixels)
4. **Encoding during enrollment**: Convert the face to a Base64 JPEG string and save it in the database
5. **Recognition during login**:
   - Load all enrolled faces from the database
   - Train the LBPH recognizer with those faces as "known" identities
   - Capture a new face from the webcam
   - Ask the recognizer "which known identity does this face most resemble?"
   - If the confidence score is below 80.0 (lower = more certain), the match is accepted

### Files created

#### `Services/FaceRecognitionService.java`
The core face recognition logic.

**Loading the face detector:**
```java
private CascadeClassifier loadCascade() {
    // The Haar Cascade XML is bundled in our project resources
    String[] candidates = {
        "/haarcascade_frontalface_default.xml",
        "/org/bytedeco/opencv/linux-x86_64/share/opencv4/haarcascades/haarcascade_frontalface_default.xml"
    };
    for (String path : candidates) {
        try (var stream = getClass().getResourceAsStream(path)) {
            if (stream == null) continue;
            // Extract to a temp file because OpenCV needs a real file path
            Path tmp = Files.createTempFile("haarcascade", ".xml");
            Files.copy(stream, tmp, StandardCopyOption.REPLACE_EXISTING);
            CascadeClassifier cc = new CascadeClassifier(tmp.toAbsolutePath().toString());
            if (!cc.empty()) return cc; // successfully loaded
        }
    }
    return new CascadeClassifier(); // empty fallback
}
```

**Capturing a face from the webcam:**
```java
public Mat captureFaceFromWebcam() throws Exception {
    OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0); // 0 = default webcam
    grabber.start();
    OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();

    for (int attempt = 0; attempt < 60; attempt++) {
        Frame frame = grabber.grab();
        Mat colorMat = converter.convert(frame);

        // Convert to grayscale (face recognition works on grayscale)
        Mat grayMat = new Mat();
        opencv_imgproc.cvtColor(colorMat, grayMat, opencv_imgproc.COLOR_BGR2GRAY);
        opencv_imgproc.equalizeHist(grayMat, grayMat); // normalize lighting

        // Detect faces
        RectVector faces = new RectVector();
        faceDetector.detectMultiScale(grayMat, faces);

        if (faces.size() > 0) {
            Rect faceRect = faces.get(0); // take the first detected face
            Mat faceRegion = new Mat(grayMat, faceRect); // crop to face region
            Mat resized = new Mat();
            opencv_imgproc.resize(faceRegion, resized, new Size(100, 100)); // normalize size
            grabber.stop();
            return resized;
        }
        Thread.sleep(100); // wait 100ms before trying again
    }
    return null; // no face detected after 60 attempts
}
```

**Enrolling (saving) a face:**
```java
public void enroll(int userId, Mat faceMat) throws Exception {
    String b64 = matToBase64(faceMat); // convert Mat image to Base64 string
    userService.saveFaceData(userId, b64); // save to users.face_data column in DB
}
```

**Recognizing a face during login:**
```java
public User loginWithFace() throws Exception {
    List<User> enrolled = userService.getAllUsersWithFaceData(); // get all users with saved faces
    if (enrolled.isEmpty()) return null;

    Mat captured = captureFaceFromWebcam(); // capture current face
    if (captured == null) return null;

    // Build the LBPH recognizer and train it with all enrolled faces
    LBPHFaceRecognizer recognizer = LBPHFaceRecognizer.create();
    MatVector images = new MatVector(enrolled.size());
    Mat labels = new Mat(enrolled.size(), 1, opencv_core.CV_32SC1);

    for (int i = 0; i < enrolled.size(); i++) {
        Mat face = base64ToMat(enrolled.get(i).getFaceData());
        images.put(i, face);
        labels.ptr(i, 0).putInt(i); // label = index in the list
    }
    recognizer.train(images, labels);

    // Predict: find the best matching label and confidence score
    int[] predictedLabel = {-1};
    double[] predictedConfidence = {Double.MAX_VALUE};
    recognizer.predict(captured, predictedLabel, predictedConfidence);

    // Accept the match only if confidence is below threshold (lower = more certain)
    if (predictedLabel[0] >= 0 && predictedConfidence[0] < 80.0) {
        return enrolled.get(predictedLabel[0]); // return the matched user
    }
    return null; // no match confident enough
}
```

---

#### `Controllers/FaceSetupController.java`
Shows the webcam live preview. The camera runs in a background thread so the UI stays responsive:

```java
private void startCamera() {
    grabber = new OpenCVFrameGrabber(0);
    running = true;
    grabThread = new Thread(() -> {
        grabber.start();
        while (running) {
            Frame frame = grabber.grab();
            BufferedImage img = frameConverter.convert(frame);

            // Convert to JavaFX image and update the ImageView
            WritableImage fxImage = bufferedToWritable(img);
            Platform.runLater(() -> webcamView.setImage(fxImage)); // update UI on FX thread

            Thread.sleep(33); // ~30 fps
        }
        grabber.stop();
    }, "webcam-grab-thread");
    grabThread.setDaemon(true); // daemon = thread dies when app closes
    grabThread.start();
}
```

When the user clicks "Capture Face":
```java
private void handleCapture() {
    new Thread(() -> {
        Mat face = faceService.captureFaceFromWebcam(); // detect face
        faceService.enroll(userId, face);               // save to DB
        Platform.runLater(() -> lblStatus.setText("Face ID saved!"));
    }).start();
}
```

---

#### `Controllers/FaceLoginController.java`
Similar to FaceSetupController but also tries to match the face every 500ms:

```java
while (running && !recognized) {
    // show live preview
    Platform.runLater(() -> webcamView.setImage(currentFrame));

    // try to recognize
    User matched = faceService.loginWithFace();
    if (matched != null) {
        recognized = true;
        UserSession.getInstance().setCurrentUser(matched);
        Platform.runLater(() -> {
            lblStatus.setText("Welcome, " + matched.getFirstName() + "!");
            loadMainShell(); // navigate to main app
        });
        return;
    }
    Thread.sleep(500); // try again every 500ms
}
```

---

#### `Utils/OpenCVLoader.java`
OpenCV native libraries (written in C++) must be loaded before any OpenCV code runs. We do this once at app startup:

```java
public class OpenCVLoader {
    public static void load() {
        try {
            Loader.load(opencv_core.class);
            System.out.println("OpenCV loaded.");
        } catch (Exception e) {
            System.err.println("OpenCV load failed: " + e.getMessage());
        }
    }
}
```

Called from `Test/MainFX.java`:
```java
public void start(Stage stage) {
    OpenCVLoader.load(); // must be first
    // ... rest of startup
}
```

---

## 7. Feature 4 — QR Code in Profile

### How it works
When the profile page loads, a QR code is generated automatically. The code contains the user's contact information in **vCard 3.0 format**, which is a standard format that all phone QR scanners recognize as a contact card.

### Files created/modified

#### `Utils/QRCodeGenerator.java`
Uses the ZXing library to generate a QR code as a JavaFX `Image`:

```java
public static Image generate(String content, int size) throws WriterException {
    QRCodeWriter writer = new QRCodeWriter();
    Map<EncodeHintType, Object> hints = Map.of(
        EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M, // medium error correction
        EncodeHintType.MARGIN, 2
    );

    // Encode the content as a QR code bit matrix
    BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints);

    // Convert the bit matrix to a JavaFX WritableImage
    WritableImage image = new WritableImage(size, size);
    PixelWriter pw = image.getPixelWriter();
    for (int y = 0; y < size; y++) {
        for (int x = 0; x < size; x++) {
            pw.setArgb(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF); // black or white pixel
        }
    }
    return image;
}
```

#### `Controllers/UserProfileController.java` — generateQRCode()
Builds a vCard string with the user's info and passes it to the generator:

```java
private void generateQRCode(User u) {
    try {
        String vCard =
            "BEGIN:VCARD\r\n" +
            "VERSION:3.0\r\n" +
            "FN:" + u.getFullName() + "\r\n" +          // full name
            "N:" + u.getLastName() + ";" + u.getFirstName() + ";;;\r\n" + // structured name
            "EMAIL:" + u.getEmail() + "\r\n" +
            "TITLE:" + u.getRoleName() + "\r\n" +        // job title
            "NOTE:Status: " + u.getStatus() + "\r\n" +
            "END:VCARD";

        qrCodeView.setImage(QRCodeGenerator.generate(vCard, 300));
    } catch (Exception e) {
        e.printStackTrace();
    }
}
```

**Why vCard format?** Plain text in a QR code shows up as raw text on most phones. vCard is an internationally recognized standard (RFC 6350) that tells the phone's QR scanner "this is a contact card." The phone will offer to save it as a contact directly.

---

## 8. Database changes we made

We added these to the existing `users` table and created one new table:

```sql
-- Added to users table:
-- google_id: stores the unique ID from Google ("sub" claim) for OAuth login
-- face_data: stores the face image as a Base64-encoded JPEG string
ALTER TABLE users ADD COLUMN google_id VARCHAR(255) NULL DEFAULT NULL;
ALTER TABLE users ADD COLUMN face_data TEXT NULL DEFAULT NULL;

-- New table for password reset OTP codes
CREATE TABLE password_reset_otp (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT        NOT NULL,                      -- which user requested the reset
    otp_code   CHAR(6)    NOT NULL,                      -- the 6-digit code
    created_at DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME   NOT NULL,                      -- 10 minutes after created_at
    used       TINYINT(1) NOT NULL DEFAULT 0,            -- 1 = already used, cannot reuse
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);
```

---

## 9. Libraries used and why

| Library | Version | What it does |
|---|---|---|
| `google-oauth-client` | 1.36.0 | Google OAuth 2.0 flow — handles building auth URL, exchanging codes for tokens |
| `google-api-client` | 2.7.0 | Google API client — makes HTTP calls to Google's UserInfo endpoint |
| `google-http-client-jackson2` | 1.45.0 | JSON parsing for Google API responses |
| `javacv-platform` | 1.5.10 | JavaCV = Java wrapper around OpenCV. Includes webcam access, face detection, LBPH face recognition, image processing |
| `jakarta.mail` | 2.0.1 | Java Mail API — sends emails via SMTP (used for OTP emails via Gmail) |
| `zxing core` | 3.5.2 | ZXing ("Zebra Crossing") — generates QR codes from text content |
| `zxing javase` | 3.5.2 | ZXing JavaSE extension — needed for file I/O utilities with ZXing |

All dependencies are declared in `pom.xml` and downloaded automatically by Maven.

---

*Documentation written for the Hirely Onboarding Coordination project.*
