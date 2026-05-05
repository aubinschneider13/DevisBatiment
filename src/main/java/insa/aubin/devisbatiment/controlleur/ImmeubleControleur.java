package insa.aubin.devisbatiment.controlleur;

import insa.aubin.devisbatiment.modele.GestionnaireSauvegarde;
import insa.aubin.devisbatiment.view.DashBoardView;
import insa.aubin.devisbatiment.view.ImmeubleView;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class ImmeubleControleur {
    private ImmeubleView vue;
    private Stage stage;
    private GestionnaireSauvegarde gestionnaire;

    public ImmeubleControleur(ImmeubleView vue, Stage stage, GestionnaireSauvegarde gestionnaire) {
        this.vue = vue;
        this.stage = stage;
        this.gestionnaire = gestionnaire;
    }

    public void btnNavigation(ActionEvent t) {
        // Active le mode déplacement sur le canvas
        this.vue.getCanvas().setPanActif(true);
    }

    public void retourDashboard() {
        DashBoardView dashBoardView = new DashBoardView();
        Scene dashScene = new Scene(dashBoardView);
        stage.setScene(dashScene);
        stage.setTitle("InsaBuilder - Tableau de bord");
        
        // Initialise le contrôleur du dashboard (à adapter selon votre constructeur)
        new DashBoardControleur(dashBoardView, stage, gestionnaire);
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
    
    public void btnAjouterNiveau(ActionEvent t) {
        // Pour l'instant, ne fait rien comme demandé
        System.out.println("Ajout de niveau demandé");
    }
}