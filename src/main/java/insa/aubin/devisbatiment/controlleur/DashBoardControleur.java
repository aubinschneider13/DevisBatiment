package insa.aubin.devisbatiment.controlleur;

import insa.aubin.devisbatiment.view.DashBoardView;
import insa.aubin.devisbatiment.view.PieceView;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class DashBoardControleur {
    private DashBoardView dashBoardView;
    private Stage stage;

    public DashBoardControleur(DashBoardView dashBoardView, Stage stage) {
        this.dashBoardView = dashBoardView;
        this.stage = stage;
        creerPiece();
    }

    public void creerPiece(){
        this.dashBoardView.getPieceButton().setOnAction(e -> {
            ouvrirPiece();
        });
    }

    public void ouvrirPiece(){
        PieceView pieceView = new PieceView();
        Scene pieceScene = new Scene(pieceView);

        stage.setScene(pieceScene);
        stage.setTitle("InsaBuilder - Nouveau devis pour une pièce");
        stage.show();
        stage.setMaximized(false); //On force pour que la fenêtre occupe toute la page
        stage.setMaximized(true);
    }
}
