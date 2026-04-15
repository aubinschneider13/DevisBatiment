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
        Stage ancienStage;
        ancienStage = (Stage) this.vue.getScene().getWindow();
        ancienStage.close();

        Stage nouveauStage = new Stage();
        DashBoardView dashBoardView = new DashBoardView();
        Scene nouveauScene = new Scene(dashBoardView);
        nouveauStage.setTitle("InsaBuilder - Tableau de bord");
        nouveauStage.setScene(nouveauScene);
        nouveauStage.setMaximized(true);
        nouveauStage.show();
    }
}
