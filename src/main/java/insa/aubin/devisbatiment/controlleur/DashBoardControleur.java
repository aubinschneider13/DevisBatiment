package insa.aubin.devisbatiment.controlleur;

import insa.aubin.devisbatiment.view.DashBoardView;
import insa.aubin.devisbatiment.view.ImmeubleView;
import insa.aubin.devisbatiment.view.PieceView;
import insa.aubin.devisbatiment.view.AppView;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import insa.aubin.devisbatiment.modele.GestionnaireSauvegarde;
import insa.aubin.devisbatiment.view.SettingsView;
import insa.aubin.devisbatiment.controlleur.SettingsControleur;
import javafx.stage.Modality;

public class DashBoardControleur {
    private DashBoardView dashBoardView;
    private Stage stage;
    private GestionnaireSauvegarde gestionnaire;

    public DashBoardControleur(DashBoardView dashBoardView, Stage stage, GestionnaireSauvegarde gestionnaire) {
        this.dashBoardView = dashBoardView;
        this.stage = stage;
        this.gestionnaire = gestionnaire;
        creerImmeuble();
        configurerSettings();
    }

    public void creerImmeuble(){
        this.dashBoardView.getImmeubleButton().setOnAction(e -> {
            ouvrirImmeuble();
        });
    }

    public void ouvrirImmeuble(){
        AppView appView = new AppView();
        Scene immeubleScene = new Scene(appView);
        stage.setScene(immeubleScene);
        stage.setTitle("InsaBuilder - Nouveau devis pour un Immeuble");
        new AppControleur(appView, stage, gestionnaire);
        mettreFenetrePleinEcran();
    }

    public void mettreFenetrePleinEcran() {
        Platform.runLater(() -> {
            Rectangle2D bounds = Screen.getPrimary().getVisualBounds();

            stage.setResizable(true);
            stage.setX(bounds.getMinX());
            stage.setY(bounds.getMinY());
            stage.setWidth(bounds.getWidth());
            stage.setHeight(bounds.getHeight());
        });
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
