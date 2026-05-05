package insa.aubin.devisbatiment.controlleur;

import insa.aubin.devisbatiment.view.DashBoardView;
import insa.aubin.devisbatiment.view.PieceView;
import javafx.scene.Scene;
import javafx.stage.Stage;
import insa.aubin.devisbatiment.modele.GestionnaireSauvegarde;
import insa.aubin.devisbatiment.view.SettingsView;
import insa.aubin.devisbatiment.controlleur.SettingsControleur;
import javafx.stage.Modality;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.application.Platform;

public class DashBoardControleur {
    private DashBoardView dashBoardView;
    private Stage stage;
    private GestionnaireSauvegarde gestionnaire;

    public DashBoardControleur(DashBoardView dashBoardView, Stage stage, GestionnaireSauvegarde gestionnaire) {
        this.dashBoardView = dashBoardView;
        this.stage = stage;
        this.gestionnaire = gestionnaire;
        creerPiece();
        configurerSettings();
    }
    
    private void mettreFenetrePleinEcran() {
    Platform.runLater(() -> {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();

        stage.setResizable(true);
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
    });
}

    public void creerPiece(){
        this.dashBoardView.getPieceButton().setOnAction(e -> {
            ouvrirPiece();
        });
    }

    public void ouvrirPiece(){
        PieceView pieceView = new PieceView(stage,gestionnaire);
        Scene pieceScene = new Scene(pieceView);

        stage.setScene(pieceScene);
        stage.setTitle("InsaBuilder - Nouveau devis pour une pièce");
        mettreFenetrePleinEcran();
    }
    

    private void configurerSettings() {
        dashBoardView.getSettingsButton().setOnAction(e -> ouvrirSettings());
    }

    private void ouvrirSettings() {
        Stage settingsStage = new Stage();
        settingsStage.initModality(Modality.APPLICATION_MODAL); // bloque le dashboard derrière
        settingsStage.initOwner(stage);
        settingsStage.setTitle("Paramètres");
        settingsStage.setResizable(false);

        SettingsView settingsView = new SettingsView();
        new SettingsControleur(settingsView, gestionnaire);

        Scene settingsScene = new Scene(settingsView);
        settingsStage.setScene(settingsScene);
        settingsStage.show();
        settingsStage.sizeToScene();
    }
}
