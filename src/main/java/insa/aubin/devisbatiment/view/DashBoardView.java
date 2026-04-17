package insa.aubin.devisbatiment.view;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import javafx.scene.control.Label;

import javafx.scene.control.Separator;
import javafx.geometry.Orientation;
import javafx.geometry.Insets;

public class DashBoardView extends BorderPane {
    String cheminImmeuble = "/images/appartement_icone.png";
    String cheminMaison = "/images/maison_icone.png";
    String cheminPiece = "/images/piece_icone.png";

    private Button immeubleButton;
    private Button appartementButton;
    private Button maisonButton;
    private Button pieceButton;



    public DashBoardView(){
        //Les composants sont au top
        VBox topVBox = new VBox();
        topVBox.setAlignment(Pos.TOP_CENTER);

        Label messageBienvenue = new Label("Bienvenue dans InsaBuilder");
        messageBienvenue.setStyle("-fx-font-family: 'Bauhaus 93'; -fx-font-size: 40px; -fx-text-fill: #2c3e50;");
        messageBienvenue.setPadding(new Insets(5,0,0,0));

        Label labelInstruction = new Label("A vous de jouer : commencer un nouveau devis ou continuer les !");
        labelInstruction.setStyle("-fx-font-family: 'Arial Black'; -fx-font-size: 18px");

        Separator separator = new Separator();
        separator.setOrientation(Orientation.HORIZONTAL);
        separator.setStyle("-fx-padding: 0 50 0 50;");

        topVBox.getChildren().addAll(messageBienvenue,labelInstruction, separator);
        this.setTop(topVBox);

        //Les composants au centre
        VBox centerVBox = new VBox();
        centerVBox.setAlignment(Pos.TOP_CENTER);
        centerVBox.setPadding(new Insets(20,20,20,20));
        centerVBox.setSpacing(20);

            //1re HBox
        HBox firstHBox = new HBox();
        firstHBox.setAlignment(Pos.TOP_LEFT);
        firstHBox.setPadding(new Insets(10,10,10,10));
        firstHBox.setSpacing(20);

        Label nouveauLabel = new Label("Réalisez un nouveau devis : ");
        nouveauLabel.setStyle("-fx-font-family: Arial Black; -fx-font-size: 14px");

                //Configuration du bouton Immeuble
        Image iconeImmeuble = new Image(getClass().getResource(cheminImmeuble).toExternalForm());
        ImageView imgImmeuble = new ImageView(iconeImmeuble);
        imgImmeuble.setFitWidth(50);
        imgImmeuble.setFitHeight(50);
        imgImmeuble.setPreserveRatio(true);

        immeubleButton = new Button("Immeuble");
        immeubleButton.setGraphic(imgImmeuble);
        immeubleButton.setContentDisplay(ContentDisplay.TOP); //On place l'icône au-dessus du txt
        immeubleButton.setStyle("-fx-cursor: hand;" + "-fx-font-family: 'Arial';" + "fx-font-size: 13px;" + "-fx-font-weight: bold;" + "-fx-text-fill: #34495e;");
        immeubleButton.setPrefSize(120, 100);

                //Configuration du bouton Appartement
        Image iconeAppartement = new Image(getClass().getResource(cheminImmeuble).toExternalForm());
        ImageView imgAppartement = new ImageView(iconeAppartement);
        imgAppartement.setFitWidth(50);
        imgAppartement.setFitHeight(50);
        imgAppartement.setPreserveRatio(true);

        appartementButton = new Button("Appartement");
        appartementButton.setGraphic(imgAppartement);
        appartementButton.setContentDisplay(ContentDisplay.TOP);
        appartementButton.setStyle("-fx-cursor: hand;" + "-fx-font-family: 'Arial';" + "fx-font-size: 13px;" + "-fx-font-weight: bold;" + "-fx-text-fill: #34495e;");
        appartementButton.setPrefSize(120, 100);

                //Configuration du bouton Maison
        Image iconeMaison = new Image(getClass().getResource(cheminMaison).toExternalForm());
        ImageView imgMaison = new ImageView(iconeMaison);
        imgMaison.setFitWidth(50);
        imgMaison.setFitHeight(50);
        imgMaison.setPreserveRatio(true);

        maisonButton = new Button("Maison");
        maisonButton.setGraphic(imgMaison);
        maisonButton.setContentDisplay(ContentDisplay.TOP);
        maisonButton.setStyle("-fx-cursor: hand;" + "-fx-font-family: 'Arial';" + "fx-font-size: 13px;" + "-fx-font-weight: bold;" + "-fx-text-fill: #34495e;");
        maisonButton.setPrefSize(120, 100);

                //Configuration du bouton Pièce
        Image iconePiece = new Image(getClass().getResource(cheminPiece).toExternalForm());
        ImageView imgPiece = new ImageView(iconePiece);
        imgPiece.setFitWidth(50);
        imgPiece.setFitHeight(50);
        imgPiece.setPreserveRatio(true);

        pieceButton = new Button("Piece");
        pieceButton.setGraphic(imgPiece);
        pieceButton.setContentDisplay(ContentDisplay.TOP);
        pieceButton.setStyle("-fx-cursor: hand;" + "-fx-font-family: 'Arial';" + "fx-font-size: 13px;" + "-fx-font-weight: bold;" + "-fx-text-fill: #34495e;");
        pieceButton.setPrefSize(120, 100);

        firstHBox.getChildren().addAll(nouveauLabel,immeubleButton, appartementButton, maisonButton,pieceButton);

            //2ème HBox
        HBox secondHBox = new HBox();
        secondHBox.setAlignment(Pos.TOP_LEFT);
        secondHBox.setSpacing(10);
        secondHBox.setPadding(new Insets(5,5,5,5));

        Label recentLabel = new Label("Devis récents : ");
        recentLabel.setStyle("-fx-font-family: Arial Black; -fx-font-size: 14px");

        secondHBox.getChildren().add(recentLabel);

        centerVBox.getChildren().addAll(firstHBox, secondHBox);
        this.setCenter(centerVBox);
    }

    public Button getImmeubleButton() {
        return immeubleButton;
    }

    public Button getAppartementButton() {
        return appartementButton;
    }

    public Button getMaisonButton() {
        return maisonButton;
    }

    public Button getPieceButton() {
        return pieceButton;
    }
}
