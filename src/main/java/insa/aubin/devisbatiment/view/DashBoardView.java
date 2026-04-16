package insa.aubin.devisbatiment.view;

import javafx.geometry.Pos;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import javafx.scene.control.Label;

import javafx.scene.control.Separator;
import javafx.geometry.Orientation;
import javafx.geometry.Insets;

public class DashBoardView extends BorderPane {
    public DashBoardView(){
        VBox topVBox = new VBox();
        topVBox.setAlignment(Pos.TOP_CENTER);

        Label messageBienvenue = new Label("Bienvenue dans InsaBuilder");
        messageBienvenue.setStyle("-fx-font-family: 'Bauhaus 93'; -fx-font-size: 30px; -fx-text-fill: #2c3e50;");
        messageBienvenue.setPadding(new Insets(5,0,0,0));

        Label labelInstruction = new Label("A vous de jouer : commencer un nouveau devis ou continuer les !");
        labelInstruction.setStyle("-fx-font-family: 'Arial Black'; -fx-font-size: 14px");

        Separator separator = new Separator();
        separator.setOrientation(Orientation.HORIZONTAL);
        separator.setStyle("-fx-padding: 0 50 0 50;");

        topVBox.getChildren().addAll(messageBienvenue,labelInstruction, separator);
        this.setTop(topVBox);

    }
}
