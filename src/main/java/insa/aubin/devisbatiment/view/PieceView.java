package insa.aubin.devisbatiment.view;

import insa.aubin.devisbatiment.controlleur.PieceControleur;
import insa.aubin.devisbatiment.modele.Appartement;
import insa.aubin.devisbatiment.modele.GestionnaireSauvegarde;
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

    private final PieceControleur controleur;
    private final DessinCanvas canvas;
    private final EchelleVue echelleVue;
    private final OptionsMurVue optionsMurVue;
    private final Label labelInstructions;

    /**
     * Constructeur de base — crée la vue sans contour d'appartement.
     * Utilisé en dehors du contexte immeuble (tests, usage standalone).
     *
     * @param stage       fenêtre principale (transmis à PieceControleur)
     * @param gestionnaire service de sauvegarde
     */
    public PieceView(Stage stage, GestionnaireSauvegarde gestionnaire) {
        this(stage, gestionnaire, null);
    }

    /**
     * Constructeur principal — crée la vue avec le contour de l'appartement
     * dessiné en fond du canvas.
     *
     * @param stage        fenêtre principale (transmis à PieceControleur)
     * @param gestionnaire service de sauvegarde
     * @param appartement  appartement dont on aménage l'intérieur (peut être null)
     */
    public PieceView(Stage stage, GestionnaireSauvegarde gestionnaire,
                     Appartement appartement) {

        // --- Canvas de dessin ---
        canvas = new DessinCanvas();
        canvas.widthProperty().bind(this.widthProperty());
        canvas.heightProperty().bind(this.heightProperty());

        // --- Options mur (rectangle / libre) — alignées en haut à droite ---
        optionsMurVue = new OptionsMurVue();
        optionsMurVue.setVisible(false);
        optionsMurVue.setDefaultLibre(); 
        StackPane.setAlignment(optionsMurVue, Pos.TOP_RIGHT);
        StackPane.setMargin(optionsMurVue, new Insets(10));

        // --- EchelleVue — alignée en haut à gauche ---
        echelleVue = new EchelleVue();
        echelleVue.setVisible(false); // masqué jusqu'au clic sur "Échelle"
        StackPane.setAlignment(echelleVue, Pos.TOP_LEFT);
        StackPane.setMargin(echelleVue, new Insets(10));

        // --- Label d'instructions — centré en bas ---
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

        // --- Assemblage du StackPane ---
        this.getChildren().addAll(canvas, optionsMurVue, echelleVue, labelInstructions);
        this.setStyle("-fx-background-color: #fffefe;");

        // --- Contrôleur ---
        // ✅ PieceControleur reçoit this (PieceView) mais plus le Stage :
        //    il ne gère plus la navigation entre scènes (c'est AppControleur).
        this.controleur = new PieceControleur(this, stage, gestionnaire);

        // --- Listeners souris sur le canvas ---
        canvas.setOnMouseClicked(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                this.controleur.clicDansZoneDeDessin(e);
            }
        });
        canvas.setOnMouseMoved(e -> this.controleur.mouseMovedDansZoneDessin(e));

        // --- Raccourci Échap (annule un mur en cours) ---
        this.setFocusTraversable(true);
        this.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                this.controleur.annulerConstruction();
            }
        });

        // --- Contour de l'appartement en fond (si fourni) ---
        if (appartement != null && appartement.getPolygone() != null) {
            this.controleur.initialiserAvecContourAppartement(
                appartement.getPolygone()
            );
        }
    }

    // =========================================================================
    // API PUBLIQUE
    // =========================================================================

    /**
     * Met à jour le texte du label d'instructions.
     * Appelé par PieceControleur selon l'outil actif.
     *
     * @param texte message à afficher
     */
    public void setInstructions(String texte) {
        labelInstructions.setText(texte);
    }

    /** Redessine le canvas (délègue à DessinCanvas). */
    public void redrawAll() {
        canvas.redrawAll();
    }

    // =========================================================================
    // GETTERS
    // =========================================================================

    /**
     * Expose le contrôleur pour que ContextePiece puisse lui déléguer
     * les événements de la toolbar commune (btnMur, btnPorte, etc.).
     *
     * @return le PieceControleur associé à cette vue
     */
    public PieceControleur getControleur() { return controleur; }

    public DessinCanvas getCanvas()          { return canvas;          }
    public OptionsMurVue getOptionsMurVue()  { return optionsMurVue;   }
    public EchelleVue getEchelleVue()        { return echelleVue;      }
}