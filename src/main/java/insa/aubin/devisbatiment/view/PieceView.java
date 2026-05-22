package insa.aubin.devisbatiment.view;

import insa.aubin.devisbatiment.controlleur.PieceControleur;
import insa.aubin.devisbatiment.modele.AireImmeuble;
import insa.aubin.devisbatiment.modele.Appartement;
import insa.aubin.devisbatiment.modele.Couloir;
import insa.aubin.devisbatiment.modele.GestionnaireSauvegarde;
import insa.aubin.devisbatiment.modele.Mur;
import insa.aubin.devisbatiment.modele.Piece;
import insa.aubin.devisbatiment.modele.Point;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

/**
 * Vue de la pièce (aménagement intérieur d'un appartement).
 *
 * ✅ SIMPLIFIÉE par rapport à l'ancienne version :
 *   - Ne contient plus sa propre toolbar (TabPane + boutons) — c'est désormais
 *     la ToolBarView commune d'AppView qui pilote PieceControleur via ContextePiece.
 *   - Ne contient plus son propre TreeView — c'est NavigateurView d'AppView.
 *   - Ne connaît plus le Stage directement pour changer de scène — c'est
 *     AppControleur.retourDashboard() qui s'en charge.
 *   - Se réduit à un StackPane : canvas de dessin + OptionsMurVue + EchelleVue
 *     + label d'instructions.
 *
 * ✅ AJOUT :
 *   - Expose getControleur() pour que ContextePiece puisse déléguer les
 *     événements toolbar à PieceControleur.
 *
 * La PieceView est insérée dans la zone centrale d'AppView par
 * AppView.afficherPiece(). Sa taille est liée à celle de la zone centrale
 * par AppView (bind sur widthProperty/heightProperty).
 */
public class PieceView extends StackPane {

    private PieceControleur controleur;
    private DessinCanvas canvas;
    private EchelleVue echelleVue;
    private OptionsMurVue optionsMurVue;
    private Label labelInstructions;

    // Constructeur de base
    public PieceView(Stage stage, GestionnaireSauvegarde gestionnaire) {
        this(stage, gestionnaire, (Appartement) null, (AireImmeuble) null);
    }

    // Constructeur avec appartement sans aire
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
    //Constructeur Couloir
    public PieceView(Stage stage, GestionnaireSauvegarde gestionnaire,
                    Couloir couloir, AireImmeuble aire) {
       this(stage, gestionnaire, (Appartement) null, (AireImmeuble) null);

       if (couloir != null && !couloir.getPolygones().isEmpty()) {
           // On initialise avec chaque zone du couloir
           for (List<Mur> zone : couloir.getZonesDelimiteurs()) {
               List<Point> polygone = new ArrayList<>();
               for (Mur m : zone) polygone.add(m.getPoint1());
               this.controleur.initialiserAvecContourAppartement(
                   polygone,
                   zone,
                   aire,
                   null
               );
           }
       }
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

        this.getChildren().addAll(canvas, optionsMurVue, echelleVue, labelInstructions);
        this.setStyle("-fx-background-color: #fffefe;");

        // ✅ Contrôleur initialisé AVANT initialiserAvecContourAppartement
        this.controleur = new PieceControleur(this, stage, gestionnaire);

        canvas.setOnMouseClicked(e -> {
            this.controleur.clicDansZoneDeDessin(e);
        });
        canvas.setOnMouseMoved(e -> this.controleur.mouseMovedDansZoneDessin(e));

        canvas.setFocusTraversable(true);

        // ✅ Appelé EN DERNIER, après initialisation du contrôleur
        if (appartement != null && appartement.getPolygone() != null) {
            this.controleur.initialiserAvecContourAppartement(
                appartement.getPolygone(),
                appartement.getMursDelimiteurs(),
                aire,appartement );
        }
    }

    public void setInstructions(String texte) { labelInstructions.setText(texte); }
    public void redrawAll() { canvas.redrawAll(); }
    public PieceControleur getControleur() { return controleur; }
    public DessinCanvas getCanvas() { return canvas; }
    public OptionsMurVue getOptionsMurVue() { return optionsMurVue; }
    public EchelleVue getEchelleVue() { return echelleVue; }

    public Label getLabelInstructions() {
        return labelInstructions;
    }
}