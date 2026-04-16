package insa.aubin.devisbatiment.controlleur;

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
        this.vue.getBtnValider().setOnAction(e -> {
            String motDePasse = this.vue.getPassword();
            if(motDePasse.equals("Hayk")){
                ouvrirTableauDeBord();
            }
            else{
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erreur");
                alert.setHeaderText("Erreur");
                alert.setContentText("Erreur");
                alert.showAndWait();

                this.vue.nettoyerMotDePasse();
            }
        });
    }

    public void ouvrirTableauDeBord(){
        // 1. On prépare  la nouvelle fenêtre SANS fermer l'ancien tout de suite
        Stage nouveauStage = new Stage();
        DashBoardView dashBoardView = new DashBoardView();
        Scene nouveauScene = new Scene(dashBoardView);

        nouveauStage.setTitle("InsaBuilder - Tableau de bord");
        nouveauStage.setScene(nouveauScene);
        nouveauStage.setMaximized(true);

        // 2. On affiche la nouvelle fenêtre
        nouveauStage.show();

        // 3. SEULEMENT MAINTENANT, on récupère et on ferme l'ancienne
        // On utilise le "Window" de la vue actuelle (LoginView)
        Stage ancienStage = (Stage) this.vue.getScene().getWindow();
        if (ancienStage != null) {
            ancienStage.close();
        }
    }
}
