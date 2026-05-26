package insa.aubin.devisbatiment.view;

import insa.aubin.devisbatiment.modele.Piece;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class OptionsPieceVue extends VBox {

    private final Label titre;
    private final TextField champUsage;
    private Piece pieceCourante;
    private Consumer<String> onUsageModifie;
    private boolean miseAJourInterne;

    public OptionsPieceVue() {
        this.setSpacing(10);
        this.setPadding(new Insets(15));
        this.setStyle("-fx-background-color: rgba(240, 240, 240, 0.9);"
                + "-fx-border-color: #bdc3c7;"
                + "-fx-border-radius: 5;"
                + "-fx-background-radius: 5;"
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 0);");
        this.setMaxSize(220, Region.USE_PREF_SIZE);

        titre = new Label("Définissez l'usage de la pièce");
        titre.setStyle("-fx-font-weight: bold;");

        Label labelUsage = new Label("Usage");
        champUsage = new TextField();
        champUsage.setPromptText("Ex : chambre");

        champUsage.setOnAction(e -> notifierUsageModifie());
        champUsage.focusedProperty().addListener((obs, ancien, nouveau) -> {
            if (!nouveau) {
                notifierUsageModifie();
            }
        });

        this.getChildren().addAll(titre, new Separator(), labelUsage, champUsage);
        this.setVisible(false);
    }

    public void setPieceCourante(Piece piece) {
        pieceCourante = piece;
        miseAJourInterne = true;
        champUsage.setText(piece != null ? piece.getUsage() : "");
        miseAJourInterne = false;
        champUsage.setDisable(piece == null);
    }

    public Piece getPieceCourante() {
        return pieceCourante;
    }

    public void setOnUsageModifie(Consumer<String> onUsageModifie) {
        this.onUsageModifie = onUsageModifie;
    }

    private void notifierUsageModifie() {
        if (miseAJourInterne || pieceCourante == null || onUsageModifie == null) return;
        onUsageModifie.accept(champUsage.getText());
    }
}
