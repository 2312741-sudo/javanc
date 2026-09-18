package vn.edu.dlu.dhopm.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

/**
 * Lop khoi tao ung dung JavaFX.
 */
public class DHOPMApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        URL fxmlUrl = getClass().getResource("/vn/edu/dlu/dhopm/view/main.fxml");
        if (fxmlUrl == null) {
            throw new IllegalStateException("Khong tim thay file main.fxml trong resources!");
        }

        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Parent root = loader.load();

        Scene scene = new Scene(root, 1200, 800);

        URL cssUrl = getClass().getResource("/vn/edu/dlu/dhopm/css/style.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        primaryStage.setTitle("DHOPM Stream Visualizer - Khai Pha Mau Do Chiem Dung Suy Giam Theo Thoi Gian Thuc");
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(700);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
