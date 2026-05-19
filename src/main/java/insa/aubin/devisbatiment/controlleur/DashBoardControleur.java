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
import insa.aubin.devisbatiment.modele.Immeuble;
import java.util.List;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
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
        chargerDevisRecents();
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
    
    private void chargerDevisRecents() {
        if (!gestionnaire.isSauvegardeActive()) return;

        List<Immeuble> immeubles = gestionnaire.chargerTousBatiments();
        HBox boite = dashBoardView.getDevisRecentsBox();

        for (Immeuble immeuble : immeubles) {
            Button btn = creerBoutonDevis(immeuble);
            boite.getChildren().add(btn);
        }
    }

    private Button creerBoutonDevis(Immeuble immeuble) {
        Image icone = new Image(
            getClass().getResource("/images/immeuble_icone.png").toExternalForm()
        );
        ImageView img = new ImageView(icone);
        img.setFitWidth(50);
        img.setFitHeight(50);
        img.setPreserveRatio(true);

        Button btn = new Button(immeuble.getNomBatiment());
        btn.setGraphic(img);
        btn.setContentDisplay(javafx.scene.control.ContentDisplay.TOP);
        btn.setStyle(
            "-fx-cursor: hand; -fx-font-family: 'Arial'; " +
            "-fx-font-weight: bold; -fx-text-fill: #34495e;"
        );
        btn.setPrefSize(120, 100);

        btn.setOnAction(e -> ouvrirImmeuble(immeuble));
        return btn;
    }

    private void ouvrirImmeuble(Immeuble immeuble) {
        AppView appView = new AppView();
        Scene scene = new Scene(appView);
        stage.setScene(scene);
        stage.setTitle("InsaBuilder - " + immeuble.getNomBatiment());
        new AppControleur(appView, stage, gestionnaire, immeuble); // ← surcharge avec immeuble existant
        mettreFenetrePleinEcran();
    }
}
