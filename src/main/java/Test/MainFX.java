package Test;

<<<<<<< HEAD
=======
import Utils.OpenCVLoader;
>>>>>>> e83af0e702d3bb2c83b5340e20de94cbf3d1e24c
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainFX extends Application {

    @Override
<<<<<<< HEAD
    /**
     * Executes this operation.
     */
    public void start(Stage primaryStage) {
=======
    public void start(Stage primaryStage) {
        OpenCVLoader.load();
>>>>>>> e83af0e702d3bb2c83b5340e20de94cbf3d1e24c
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/LoginView.fxml"));

            Scene scene = new Scene(root, 1100, 750);
            scene.getStylesheets().add(getClass().getResource("/styles/hirely.css").toExternalForm());

            primaryStage.setTitle("Hirely — Login");
            primaryStage.setScene(scene);

            // allow resize + maximize
            primaryStage.setResizable(true);
            primaryStage.setMinWidth(980);
            primaryStage.setMinHeight(650);

            primaryStage.show();

        } catch (Exception e) {
            System.err.println("Error loading JavaFX application: " + e.getMessage());
            e.printStackTrace();
        }
    }

<<<<<<< HEAD
    /**
     * Executes this operation.
     */
=======
>>>>>>> e83af0e702d3bb2c83b5340e20de94cbf3d1e24c
    public static void main(String[] args) {
        launch(args);
    }
}
