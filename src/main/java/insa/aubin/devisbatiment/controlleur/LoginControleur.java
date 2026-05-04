package insa.aubin.devisbatiment.controlleur;

import insa.aubin.devisbatiment.modele.GestionnaireSauvegarde;
import insa.aubin.devisbatiment.view.LoginView;
import insa.aubin.devisbatiment.view.DashBoardView;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class LoginControleur {
    private final LoginView vue;

    public LoginControleur(LoginView vue) {
        this.vue = vue;
        valider();
    }

    public void valider(){
        this.vue.getBtnValider().setDefaultButton(true); //mettre le bouton en default permet de déclencher automatiquement l'action de ce bouton en pressant sur la touche entrée
        this.vue.getBtnValider().setOnAction(e -> {
            String motDePasse = this.vue.getPassword();
            if(motDePasse.equals("Hayk")){
                ouvrirTableauDeBord();
            }
            else{
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erreur");
                alert.setHeaderText("Échec de l'authentification");
                alert.setContentText("Le mot de passe saisi est incorrect. Veuillez réessayer.");
                alert.showAndWait();
                this.vue.nettoyerMotDePasse();
            }
        });
    }

    public void ouvrirTableauDeBord(){
        // 1. On prépare la nouvelle fenêtre SANS fermer l'ancien tout de suite
        Stage nouveauStage = new Stage();
        DashBoardView dashBoardView = new DashBoardView();
        Scene nouveauScene = new Scene(dashBoardView);
        nouveauStage.setTitle("InsaBuilder - Tableau de bord");
        nouveauStage.setScene(nouveauScene);
        nouveauStage.setMaximized(true);

        // 2. On affiche la nouvelle fenêtre
        nouveauStage.show();

        // On instancie le gestionnaire une seule fois ici et on le passe au controleur
        GestionnaireSauvegarde gestionnaire = new GestionnaireSauvegarde();
        gestionnaire.chargerConfig();
        DashBoardControleur dashBoardControleur = new DashBoardControleur(dashBoardView, nouveauStage, gestionnaire);

        // 3. SEULEMENT MAINTENANT, on récupère et on ferme l'ancienne
        // On utilise le "Window" de la vue actuelle (LoginView)
        Stage ancienStage = (Stage) this.vue.getScene().getWindow();
        if (ancienStage != null) {
            ancienStage.close();
        }
    }
}