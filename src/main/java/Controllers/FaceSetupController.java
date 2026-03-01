package Controllers;

import Services.FaceRecognitionService;
import javafx.application.Platform;
import javafx.fxml.FXML;
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
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.bytedeco.opencv.global.opencv_imgproc;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class FaceSetupController {

    @FXML private ImageView webcamView;
    @FXML private Label     lblStatus;

    private int userId;

    private final FaceRecognitionService faceService = new FaceRecognitionService();

    private OpenCVFrameGrabber          grabber;
    private OpenCVFrameConverter.ToMat  matConverter;
    private Java2DFrameConverter        frameConverter;
    private Thread                      grabThread;
    private volatile boolean            running = false;
    private volatile Mat                lastGrayFace = null;

    @FXML
    private void initialize() {
        matConverter   = new OpenCVFrameConverter.ToMat();
        frameConverter = new Java2DFrameConverter();
        startCamera();
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    // ── Camera loop ──────────────────────────────────────────────────────────

    private void startCamera() {
        grabber   = new OpenCVFrameGrabber(0);
        running   = true;
        grabThread = new Thread(() -> {
            try {
                grabber.start();
                Platform.runLater(() -> lblStatus.setText("Camera ready. Click 'Capture Face' when ready."));

                while (running) {
                    Frame frame = grabber.grab();
                    if (frame == null || frame.image == null) continue;

                    Mat colorMat = matConverter.convert(frame);

                    // Update preview on FX thread
                    BufferedImage img = frameConverter.convert(frame);
                    if (img != null) {
                        WritableImage fxImage = bufferedToWritable(img);
                        Platform.runLater(() -> webcamView.setImage(fxImage));
                    }

                    // Detect face continuously and cache it
                    Mat gray = new Mat();
                    opencv_imgproc.cvtColor(colorMat, gray, opencv_imgproc.COLOR_BGR2GRAY);
                    opencv_imgproc.equalizeHist(gray, gray);
                    RectVector faces = new RectVector();
                    // Note: faceDetector is internal to FaceRecognitionService;
                    // we rely on enroll() which re-captures via the service.
                    lastGrayFace = gray;

                    Thread.sleep(33); // ~30 fps
                }
                grabber.stop();
            } catch (Exception e) {
                Platform.runLater(() -> lblStatus.setText("Camera error: " + e.getMessage()));
            }
        }, "webcam-grab-thread");
        grabThread.setDaemon(true);
        grabThread.start();
    }

    // ── Capture button ───────────────────────────────────────────────────────

    @FXML
    private void handleCapture() {
        lblStatus.setText("Capturing… please hold still.");
        new Thread(() -> {
            try {
                // Use service to capture a detected face
                Mat face = faceService.captureFaceFromWebcam();
                if (face == null) {
                    Platform.runLater(() ->
                            lblStatus.setText("No face detected. Make sure your face is visible."));
                    return;
                }
                faceService.enroll(userId, face);
                Platform.runLater(() -> {
                    lblStatus.setText("Face ID saved successfully!");
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> lblStatus.setText("Error: " + e.getMessage()));
            }
        }, "face-capture-thread").start();
    }

    // ── Close ────────────────────────────────────────────────────────────────

    @FXML
    private void handleClose() {
        running = false;
        if (grabThread != null) grabThread.interrupt();
        Stage stage = (Stage) webcamView.getScene().getWindow();
        stage.close();
    }

    // ── Helper ───────────────────────────────────────────────────────────────

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
