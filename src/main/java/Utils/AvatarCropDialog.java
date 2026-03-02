package Utils;

import java.io.File;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.transform.Scale;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javax.imageio.ImageIO;

public class AvatarCropDialog {

    /**
     * Opens a modal cropper. Returns true if user saved, false if canceled.
     * Saves PNG to outputFile.
     */
    /**
     * Executes this operation.
     */
    public static boolean showAndSave(Window owner, File inputFile, File outputFile, int outSize) throws Exception {

        Image img = new Image(inputFile.toURI().toString(), false);
        if (img.isError()) throw new IllegalArgumentException("Cannot read image.");

        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Adjust Profile Picture");

        // --- viewport size shown to user ---
        double viewSize = 260; // UI circle area

        ImageView iv = new ImageView(img);
        iv.setPreserveRatio(true);

        // Put ImageView inside a pane so we can drag it around
        Pane pan = new Pane(iv);
        pan.setPrefSize(viewSize, viewSize);
        pan.setMinSize(viewSize, viewSize);
        pan.setMaxSize(viewSize, viewSize);

        // Circle clip on the pane (not the image), so it behaves like Instagram crop circle
        Circle clip = new Circle(viewSize / 2, viewSize / 2, viewSize / 2);
        pan.setClip(clip);

        // Border ring overlay
        Circle ring = new Circle(viewSize / 2, viewSize / 2, viewSize / 2);
        ring.setFill(Color.TRANSPARENT);
        ring.setStroke(Color.rgb(255, 255, 255, 0.65));
        ring.setStrokeWidth(2);

        StackPane cropArea = new StackPane(pan, ring);
        cropArea.setPadding(new Insets(10));
        cropArea.setStyle("-fx-background-color: rgba(0,0,0,0.25); -fx-background-radius: 18;");
        cropArea.setMaxWidth(viewSize + 20);

        // Zoom slider
        Slider zoom = new Slider(1.0, 3.0, 1.2);
        zoom.setMaxWidth(320);

        // Apply scaling transform
        Scale scale = new Scale(zoom.getValue(), zoom.getValue(), 0, 0);
        iv.getTransforms().add(scale);
        zoom.valueProperty().addListener((o, a, b) -> {
            scale.setX(b.doubleValue());
            scale.setY(b.doubleValue());
        });

        // Initial sizing: fit image reasonably
        // We’ll set a base fit so that smallest side covers crop circle
        double iw = img.getWidth();
        double ih = img.getHeight();
        if (iw <= 0 || ih <= 0) throw new IllegalArgumentException("Invalid image size.");

        // base scale so the image covers the crop circle
        double coverScale = Math.max(viewSize / iw, viewSize / ih);
        iv.setFitWidth(iw * coverScale);
        iv.setFitHeight(ih * coverScale);

        // Center image initially
        iv.relocate((viewSize - iv.getFitWidth()) / 2, (viewSize - iv.getFitHeight()) / 2);

        // Drag behavior
        final double[] drag = new double[2];
        iv.setOnMousePressed(e -> {
            drag[0] = e.getSceneX() - iv.getLayoutX();
            drag[1] = e.getSceneY() - iv.getLayoutY();
        });
        iv.setOnMouseDragged(e -> {
            iv.setLayoutX(e.getSceneX() - drag[0]);
            iv.setLayoutY(e.getSceneY() - drag[1]);
        });

        // Buttons
        Button btnCancel = new Button("Cancel");
        Button btnSave = new Button("Save");

        HBox actions = new HBox(10, btnCancel, btnSave);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Label hint = new Label("Drag to position • Use slider to zoom");
        hint.setStyle("-fx-text-fill: rgba(255,255,255,0.85);");

        VBox root = new VBox(14, hint, cropArea, zoom, actions);
        root.setPadding(new Insets(18));
        root.setStyle("-fx-background-color: #2b2b2b;");

        final boolean[] saved = {false};

        btnCancel.setOnAction(e -> dialog.close());

        btnSave.setOnAction(e -> {
            try {
                // Snapshot ONLY the clipped pane (square), then resize to outSize
                SnapshotParameters sp = new SnapshotParameters();
                sp.setFill(Color.TRANSPARENT);

                WritableImage snapped = pan.snapshot(sp, null);

                // Scale snapshot to outSize x outSize
                ImageView tmp = new ImageView(snapped);
                tmp.setFitWidth(outSize);
                tmp.setFitHeight(outSize);
                tmp.setPreserveRatio(false);

                WritableImage out = new WritableImage(outSize, outSize);
                SnapshotParameters sp2 = new SnapshotParameters();
                sp2.setFill(Color.TRANSPARENT);
                tmp.snapshot(sp2, out);

                ImageIO.write(SwingFXUtils.fromFXImage(out, null), "png", outputFile);

                saved[0] = true;
                dialog.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        dialog.setScene(new Scene(root));
        dialog.setResizable(false);
        dialog.showAndWait();

        return saved[0];
    }
}
