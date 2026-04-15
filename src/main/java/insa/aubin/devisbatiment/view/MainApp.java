package insa.aubin.devisbatiment.view;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        // On instancie ta vue principale (que l'on crée juste après)
        LoginView root = new LoginView();
        
        Scene scene = new Scene(root, 400, 350);
        primaryStage.setTitle("InsaBuilder");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false); //On empêche la redimension de la fenêtre
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}