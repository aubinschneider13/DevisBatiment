package insa.aubin.devisbatiment.view;

import insa.aubin.devisbatiment.controlleur.PieceControleur;
import insa.aubin.devisbatiment.modele.AireImmeuble;
import insa.aubin.devisbatiment.modele.Appartement;
import insa.aubin.devisbatiment.modele.Couloir;
import insa.aubin.devisbatiment.modele.GestionnaireSauvegarde;
import insa.aubin.devisbatiment.modele.Mur;
import insa.aubin.devisbatiment.modele.Piece;
import insa.aubin.devisbatiment.modele.Point;
import insa.aubin.devisbatiment.modele.Tremie;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Vue de la pièce (aménagement intérieur d'un appartement).
 *
 * Simplifiée par rapport à l'ancienne version :
 * - Ne contient plus sa propre toolbar : la ToolBarView commune d'AppView pilote
 *   PieceControleur via ContexteAppartement ou ContextePiece.
 * - Ne contient plus son propre TreeView : c'est NavigateurView d'AppView.
 * - Ne connaît plus le Stage directement pour changer de scène :
 *   AppControleur.retourDashboard() s'en charge.
 * - Se réduit à un StackPane : canvas de dessin + OptionsMurVue + EchelleVue
 *   + label d'instructions.
 *
 * Expose getControleur() pour que les contextes puissent déléguer les événements
 * toolbar à PieceControleur.
 */
public class PieceView extends StackPane {

    private PieceControleur controleur;
    private DessinCanvas canvas;
    private EchelleVue echelleVue;
    private OptionsMurVue optionsMurVue;
    private OptionsPieceVue optionsPieceVue;
    private Label labelInstructions;

    // Constructeur de base
    public PieceView(Stage stage, GestionnaireSauvegarde gestionnaire) {
        this(stage, gestionnaire, (Appartement) null, (AireImmeuble) null);
    }

    // Constructeur avec pièce
    public PieceView(Stage stage, GestionnaireSauvegarde gestionnaire,
                     Piece piece, AireImmeuble aire) {
        this(stage, gestionnaire, (Appartement) null, (AireImmeuble) null);

        if (piece != null && piece.getPoints() != null) {
            this.controleur.initialiserAvecContourAppartement(
                    piece.getPoints(),
                    piece.getMurs(),
                    aire,
                    null
            );
        }
    }

    // Constructeur avec couloir
    public PieceView(Stage stage, GestionnaireSauvegarde gestionnaire,
                    Couloir couloir, AireImmeuble aire) {
       this(stage, gestionnaire, couloir, aire, List.of());
   }

    public PieceView(Stage stage, GestionnaireSauvegarde gestionnaire,
                    Couloir couloir, AireImmeuble aire, List<Tremie> tremies) {
       this(stage, gestionnaire, (Appartement) null, (AireImmeuble) null);
       this.controleur.setAfficherAdjacenceCouloir(false);

       if (couloir != null && !couloir.getZonesDelimiteurs().isEmpty()) {
           // On prend tous les murs de toutes les zones
           List<Mur> tousMurs = new ArrayList<>();
           List<Point> tousPoints = new ArrayList<>();
           for (List<Mur> zone : couloir.getZonesDelimiteurs()) {
               for (Mur mur : zone) {
                   tousMurs.add(mur);
                   tousPoints.add(mur.getPoint1());
               }
           }
           // Un seul appel avec tous les points et murs fusionnés
           this.controleur.initialiserAvecContourAppartement(
                   tousPoints,
                   tousMurs,
                   aire,
                   null
           );
       }
       afficherTremies(tremies);
   }

    private void afficherTremies(List<Tremie> tremies) {
        if (tremies == null) return;
        for (Tremie tremie : tremies) {
            if (tremie != null && !canvas.getElements().contains(tremie)) {
                canvas.ajouterElement(tremie);
            }
        }
        canvas.redrawAll();
    }
    // Constructeur principal
    public PieceView(Stage stage, GestionnaireSauvegarde gestionnaire,
                     Appartement appartement, AireImmeuble aire) {

        canvas = new DessinCanvas();
        canvas.widthProperty().bind(this.widthProperty());
        canvas.heightProperty().bind(this.heightProperty());

        optionsMurVue = new OptionsMurVue();
        optionsMurVue.setVisible(false);
        optionsMurVue.setDefaultLibre();
        StackPane.setAlignment(optionsMurVue, Pos.TOP_RIGHT);
        StackPane.setMargin(optionsMurVue, new Insets(10));

        echelleVue = new EchelleVue();
        echelleVue.setVisible(false);
        StackPane.setAlignment(echelleVue, Pos.TOP_LEFT);
        StackPane.setMargin(echelleVue, new Insets(10));

        optionsPieceVue = new OptionsPieceVue();
        StackPane.setAlignment(optionsPieceVue, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(optionsPieceVue, new Insets(0, 10, 15, 0));

        labelInstructions = new Label("Sélectionnez un outil pour commencer");
        labelInstructions.setStyle(
                "-fx-background-color: rgba(240,240,240,0.9);" +
                "-fx-padding: 6 12 6 12;" +
                "-fx-border-radius: 4;" +
                "-fx-background-radius: 4;" +
                "-fx-text-fill: #2c3e50;" +
                "-fx-font-size: 13px;"
        );
        StackPane.setAlignment(labelInstructions, Pos.BOTTOM_CENTER);
        StackPane.setMargin(labelInstructions, new Insets(0, 0, 15, 0));

        this.getChildren().addAll(canvas, optionsMurVue, echelleVue, optionsPieceVue, labelInstructions);
        this.setStyle("-fx-background-color: #fffefe;");

        // Contrôleur initialisé avant initialiserAvecContourAppartement.
        this.controleur = new PieceControleur(this, stage, gestionnaire);

        canvas.setOnMouseClicked(e -> this.controleur.clicDansZoneDeDessin(e));
        canvas.setOnMouseMoved(e -> this.controleur.mouseMovedDansZoneDessin(e));

        canvas.setFocusTraversable(true);
        canvas.setOnKeyPressed(e -> this.controleur.gererToucheClavier(e));

        this.setFocusTraversable(true);
        this.setOnKeyPressed(e -> this.controleur.gererToucheClavier(e));

        // Appelé en dernier, après initialisation du contrôleur.
        if (appartement != null && appartement.getPolygone() != null) {
            this.controleur.initialiserAvecContourAppartement(
                    appartement.getPolygone(),
                    appartement.getMursDelimiteurs(),
                    aire,
                    appartement
            );
        }
    }

    public void setInstructions(String texte) {
        labelInstructions.setText(texte);
    }

    public void redrawAll() {
        canvas.redrawAll();
    }

    public PieceControleur getControleur() {
        return controleur;
    }

    public DessinCanvas getCanvas() {
        return canvas;
    }

    public OptionsMurVue getOptionsMurVue() {
        return optionsMurVue;
    }

    public OptionsPieceVue getOptionsPieceVue() {
        return optionsPieceVue;
    }

    public EchelleVue getEchelleVue() {
        return echelleVue;
    }
}
