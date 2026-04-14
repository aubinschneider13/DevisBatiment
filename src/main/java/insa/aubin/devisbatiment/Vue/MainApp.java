package insa.aubin.devisbatiment.Vue;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        // On instancie ta vue principale (que l'on crée juste après)
        VuePrincipale root = new VuePrincipale();
        
        Scene scene = new Scene(root, 1000, 700);
        primaryStage.setTitle("Devis Bâtiment INSA - Vue 2D");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}