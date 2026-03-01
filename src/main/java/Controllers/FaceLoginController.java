package Controllers;

import Models.User;
import Services.FaceRecognitionService;
import Utils.UserSession;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.bytedeco.opencv.global.opencv_imgproc;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class FaceLoginController {

    @FXML private ImageView webcamView;
    @FXML private Label     lblStatus;

    private final FaceRecognitionService faceService = new FaceRecognitionService();

    private OpenCVFrameGrabber         grabber;
    private OpenCVFrameConverter.ToMat matConverter;
    private Java2DFrameConverter       frameConverter;
    private Thread                     grabThread;
    private volatile boolean           running     = false;
    private volatile boolean           recognized  = false;

    @FXML
    private void initialize() {
        matConverter   = new OpenCVFrameConverter.ToMat();
        frameConverter = new Java2DFrameConverter();
        startRecognitionLoop();
    }

    private void startRecognitionLoop() {
        grabber   = new OpenCVFrameGrabber(0);
        running   = true;
        grabThread = new Thread(() -> {
            try {
                grabber.start();
                Platform.runLater(() -> lblStatus.setText("Scanning… look at the camera."));

                while (running && !recognized) {
                    Frame frame = grabber.grab();
                    if (frame == null || frame.image == null) continue;

                    // Show live preview
                    BufferedImage img = frameConverter.convert(frame);
                    if (img != null) {
                        WritableImage fxImg = bufferedToWritable(img);
                        Platform.runLater(() -> webcamView.setImage(fxImg));
                    }

                    // Try face recognition on every 10th frame to avoid overloading
                    Mat colorMat = matConverter.convert(frame);
                    Mat gray = new Mat();
                    opencv_imgproc.cvtColor(colorMat, gray, opencv_imgproc.COLOR_BGR2GRAY);
                    opencv_imgproc.equalizeHist(gray, gray);

                    try {
                        User matched = faceService.loginWithFace();
                        if (matched != null && !recognized) {
                            recognized = true;
                            running    = false;
                            grabber.stop();
                            UserSession.getInstance().setCurrentUser(matched);
                            Platform.runLater(() -> {
                                lblStatus.setText("Welcome, " + matched.getFirstName() + "!");
                                loadMainShell();
                            });
                            return;
                        }
                    } catch (Exception ignored) {}

                    Thread.sleep(500);
                }

                try { grabber.stop(); } catch (Exception ignored) {}

            } catch (Exception e) {
                Platform.runLater(() -> lblStatus.setText("Camera error: " + e.getMessage()));
            }
        }, "face-login-thread");
        grabThread.setDaemon(true);
        grabThread.start();
    }

    @FXML
    private void handleCancel() {
        stopCamera();
        try {
            Parent loginRoot = FXMLLoader.load(getClass().getResource("/LoginView.fxml"));
            Stage stage = (Stage) webcamView.getScene().getWindow();
            stage.getScene().setRoot(loginRoot);
            stage.setTitle("Hirely — Login");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadMainShell() {
        stopCamera();
        try {
            Parent shellRoot = FXMLLoader.load(getClass().getResource("/MainShell.fxml"));
            Stage stage = (Stage) webcamView.getScene().getWindow();
            stage.getScene().setRoot(shellRoot);
            stage.setTitle("Hirely — Onboarding Plans");
        } catch (Exception e) {
            e.printStackTrace();
            lblStatus.setText("Login OK but cannot open shell: " + e.getMessage());
        }
    }

    private void stopCamera() {
        running = false;
        if (grabThread != null) grabThread.interrupt();
        try { grabber.stop(); } catch (Exception ignored) {}
    }

    private WritableImage bufferedToWritable(BufferedImage src) {
        BufferedImage converted = new BufferedImage(
                src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        converted.getGraphics().drawImage(src, 0, 0, null);
        WritableImage fxImage = new WritableImage(converted.getWidth(), converted.getHeight());
        PixelWriter pw = fxImage.getPixelWriter();
        int[] pixels = ((DataBufferInt) converted.getRaster().getDataBuffer()).getData();
        pw.setPixels(0, 0, converted.getWidth(), converted.getHeight(),
                javafx.scene.image.PixelFormat.getIntArgbInstance(), pixels, 0, converted.getWidth());
        return fxImage;
    }
}
