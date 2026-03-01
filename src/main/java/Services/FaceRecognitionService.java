package Services;

import Models.User;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.SQLException;
import java.util.Base64;
import java.util.List;

public class FaceRecognitionService {

    private static final double CONFIDENCE_THRESHOLD = 80.0;

    private final UserService userService = new UserService();
    private final CascadeClassifier faceDetector;

    public FaceRecognitionService() {
        faceDetector = loadCascade();
    }

    private CascadeClassifier loadCascade() {
        // JavaCV embeds cascade data files at /org/bytedeco/opencv/data/ inside its jar
        String[] candidates = {
            // Bundled directly in project resources (most reliable)
            "/haarcascade_frontalface_default.xml",
            // Paths inside javacv-platform jars
            "/org/bytedeco/opencv/linux-x86_64/share/opencv4/haarcascades/haarcascade_frontalface_default.xml",
            "/org/bytedeco/opencv/windows-x86_64/share/opencv4/haarcascades/haarcascade_frontalface_default.xml"
        };
        for (String path : candidates) {
            try (var stream = getClass().getResourceAsStream(path)) {
                if (stream == null) continue;
                java.nio.file.Path tmp = java.nio.file.Files.createTempFile("haarcascade", ".xml");
                tmp.toFile().deleteOnExit();
                java.nio.file.Files.copy(stream, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                CascadeClassifier cc = new CascadeClassifier(tmp.toAbsolutePath().toString());
                if (!cc.empty()) {
                    System.out.println("Cascade loaded from: " + path);
                    return cc;
                }
            } catch (Exception e) {
                System.err.println("Tried " + path + ": " + e.getMessage());
            }
        }
        System.err.println("FaceRecognitionService: could not load cascade classifier.");
        return new CascadeClassifier();
    }

    /**
     * Captures a face image from the default webcam.
     * Returns a grayscale face Mat, or null if no face detected.
     * Caller must call grabber.stop() after use.
     */
    public Mat captureFaceFromWebcam() throws Exception {
        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
        try {
            grabber.start();
            OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();

            for (int attempt = 0; attempt < 60; attempt++) {
                Frame frame = grabber.grab();
                if (frame == null || frame.image == null) continue;

                Mat colorMat = converter.convert(frame);
                Mat grayMat  = new Mat();
                opencv_imgproc.cvtColor(colorMat, grayMat, opencv_imgproc.COLOR_BGR2GRAY);
                opencv_imgproc.equalizeHist(grayMat, grayMat);

                RectVector faces = new RectVector();
                faceDetector.detectMultiScale(grayMat, faces);

                if (faces.size() > 0) {
                    Rect faceRect = faces.get(0);
                    Mat faceRegion = new Mat(grayMat, faceRect);
                    Mat resized = new Mat();
                    opencv_imgproc.resize(faceRegion, resized, new Size(100, 100));
                    grabber.stop();
                    return resized;
                }
                Thread.sleep(100);
            }
        } finally {
            try { grabber.stop(); } catch (Exception ignored) {}
        }
        return null;
    }

    /**
     * Enrolls a face for the given user: encodes the Mat as a base64 JPEG and saves it.
     */
    public void enroll(int userId, Mat faceMat) throws Exception {
        String b64 = matToBase64(faceMat);
        userService.saveFaceData(userId, b64);
    }

    /**
     * Captures a face from the webcam and attempts to match it against enrolled users.
     * Returns the matching User or null.
     */
    public User loginWithFace() throws Exception {
        List<User> enrolled;
        try {
            enrolled = userService.getAllUsersWithFaceData();
        } catch (SQLException e) {
            throw new RuntimeException("DB error loading face data: " + e.getMessage(), e);
        }
        if (enrolled.isEmpty()) return null;

        Mat captured = captureFaceFromWebcam();
        if (captured == null) return null;

        // Build recognizer from enrolled faces
        LBPHFaceRecognizer recognizer = LBPHFaceRecognizer.create();
        MatVector images = new MatVector(enrolled.size());
        Mat labels = new Mat(enrolled.size(), 1, opencv_core.CV_32SC1);

        for (int i = 0; i < enrolled.size(); i++) {
            Mat face = base64ToMat(enrolled.get(i).getFaceData());
            images.put(i, face);
            labels.ptr(i, 0).putInt(i);
        }
        recognizer.train(images, labels);

        int[]    predictedLabel      = {-1};
        double[] predictedConfidence = {Double.MAX_VALUE};
        recognizer.predict(captured, predictedLabel, predictedConfidence);

        if (predictedLabel[0] >= 0 && predictedConfidence[0] < CONFIDENCE_THRESHOLD) {
            return enrolled.get(predictedLabel[0]);
        }
        return null;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    public static String matToBase64(Mat mat) throws Exception {
        Java2DFrameConverter frameConverter = new Java2DFrameConverter();
        OpenCVFrameConverter.ToMat matConverter = new OpenCVFrameConverter.ToMat();
        Frame frame = matConverter.convert(mat);
        BufferedImage img = frameConverter.convert(frame);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    public static Mat base64ToMat(String b64) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(b64);
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
        Java2DFrameConverter frameConverter = new Java2DFrameConverter();
        OpenCVFrameConverter.ToMat matConverter = new OpenCVFrameConverter.ToMat();
        Frame frame = frameConverter.convert(img);
        return matConverter.convert(frame);
    }
}
