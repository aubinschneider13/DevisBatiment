package insa.aubin.devisbatiment.controlleur;

import insa.aubin.devisbatiment.modele.GestionnaireSauvegarde;
import insa.aubin.devisbatiment.view.DashBoardView;
import insa.aubin.devisbatiment.view.ImmeubleView;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;

import java.util.Optional;

public class ImmeubleControleur {
    private ImmeubleView vue;
    private Stage stage;
    private GestionnaireSauvegarde gestionnaire;

    public ImmeubleControleur(ImmeubleView vue, Stage stage, GestionnaireSauvegarde gestionnaire) {
        this.vue = vue;
        this.stage = stage;
        this.gestionnaire = gestionnaire;

        // Lance la demande de nom dès que l'interface est affichée
        Platform.runLater(this::demanderNomImmeuble);
    }

    private void demanderNomImmeuble() {
        TextInputDialog dialog = new TextInputDialog("Nouvel Immeuble");
        dialog.setTitle("Nom de l'immeuble");
        dialog.setHeaderText("Initialisation du projet");
        dialog.setContentText("Veuillez entrer le nom de l'immeuble :");
        dialog.setGraphic(null);

        // Récupération du bouton OK pour forcer la validation
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (dialog.getEditor().getText().trim().isEmpty()) {
                event.consume(); // Empêche la fermeture si vide
                dialog.getEditor().setStyle("-fx-border-color: red;");
            }
        });

        Optional<String> result = dialog.showAndWait();

        if (result.isPresent() && !result.get().trim().isEmpty()) {
            // Met à jour le TreeItem racine dans la vue
            this.vue.getRootItem().setValue("Immeuble : " + result.get().trim());
        } else {
            // Si l'utilisateur annule ou ferme via la croix, retour forcé au dashboard
            retourDashboard();
        }
    }

    public void btnNavigation(ActionEvent t) {
        this.vue.getCanvas().setPanActif(true);
    }

    public void retourDashboard() {
        DashBoardView dashBoardView = new DashBoardView();
        Scene dashScene = new Scene(dashBoardView);
        stage.setScene(dashScene);
        stage.setTitle("InsaBuilder - Tableau de bord");
        new DashBoardControleur(dashBoardView, stage, gestionnaire);
    }

    public void btnAjouterNiveau(ActionEvent t) {
        // Logique future
        System.out.println("Ajout de niveau");
    }
}