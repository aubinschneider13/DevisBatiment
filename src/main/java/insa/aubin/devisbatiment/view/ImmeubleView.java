package insa.aubin.devisbatiment.view;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/**
 * Vue de l'aire de l'immeuble.
 *
 * ✅ SIMPLIFIÉE dans la nouvelle architecture (pattern State + contextes) :
 *   - Ne contient plus de toolbar, de TreeView, ni de SplitPane.
 *   - Ne connaît plus ImmeubleControleur — c'est AppControleur qui l'instancie
 *     et le relie au canvas via AppView.
 *   - Se réduit à un StackPane : canvas de l'aire + voile cadenas + EchelleVue
 *     + label d'instructions.
 *
 * Cette vue est insérée dans la zone centrale d'AppView par
 * AppView.afficherCanvasAire(). Sa taille est liée à celle de la zone centrale
 * par AppView (bind sur widthProperty / heightProperty).
 *
 * ✅ NOTE ARCHITECTURE : l'ancienne ImmeubleView (BorderPane complet avec
 * toolbar et navigateur) est conservée à titre de référence dans le dépôt,
 * mais n'est plus instanciée dans la nouvelle architecture. AppView la remplace
 * dans son rôle de fenêtre racine.
 */
public class ImmeubleView extends StackPane {

    // --- Composants visuels ---
    private final DessinCanvas canvasAire;
    private final StackPane voileValidation;
    private final EchelleVue echelleVue;
    private final Label labelInstructions;

    /** Mémorise si le voile a été activé pour le restaurer si on revient sur cette vue. */
    private boolean voileActif = false;

    public ImmeubleView() {

        // --- Canvas de l'aire ---
        canvasAire = new DessinCanvas();
        // La taille sera bindée par AppView lors de l'insertion dans zoneDessin

        // --- Voile cadenas (affiché une fois l'aire validée) ---
        voileValidation = creerVoileValidation();
        voileValidation.setVisible(false);

        // --- EchelleVue (overlay haut-gauche) ---
        echelleVue = new EchelleVue();
        echelleVue.setVisible(false); // masqué jusqu'au clic sur "Échelle"
        StackPane.setAlignment(echelleVue, Pos.TOP_LEFT);
        StackPane.setMargin(echelleVue, new Insets(10));

        // --- Label d'instructions (bas de la vue) ---
        labelInstructions = new Label("Cliquez pour définir le premier coin de l'immeuble");
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

        // --- Assemblage ---
        this.getChildren().addAll(canvasAire, voileValidation, echelleVue, labelInstructions);
        this.setStyle("-fx-background-color: #fffefe;");
    }

    // =========================================================================
    // VOILE CADENAS
    // =========================================================================

    /**
     * Active le voile cadenas sur le canvas de l'aire et mémorise cet état
     * pour le restaurer si l'utilisateur revient sur l'aire depuis un niveau.
     * Appelé par AppControleur.onAireValidee().
     */
    public void activerVoile() {
        voileActif = true;
        voileValidation.setVisible(true);
    }

    /**
     * Restaure la visibilité du voile selon l'état mémorisé.
     * Appelé par AppView.afficherCanvasAire() lors d'un retour sur cette vue.
     */


    /**
     * Crée le voile gris semi-transparent avec un cadenas centré.
     * Purement visuel — ne capte pas les clics (mouseTransparent).
     */
    private StackPane creerVoileValidation() {
        Rectangle voile = new Rectangle();
        voile.setFill(Color.web("#808080", 0.35));
        // Le bind sur la taille sera effectué par AppView après insertion
        voile.widthProperty().bind(this.widthProperty());
        voile.heightProperty().bind(this.heightProperty());

        Text cadenas = new Text("🔒");
        cadenas.setFont(Font.font(48));
        cadenas.setFill(Color.web("#2c3e50", 0.8));

        StackPane voilePane = new StackPane(voile, cadenas);
        StackPane.setAlignment(cadenas, Pos.CENTER);
        voilePane.setMouseTransparent(true);
        return voilePane;
    }

    // =========================================================================
    // API PUBLIQUE
    // =========================================================================

    /**
     * Met à jour le texte du label d'instructions.
     * Appelé par ImmeubleControleur selon l'étape de saisie.
     *
     * @param texte message à afficher
     */
    public void setInstructions(String texte) {
        labelInstructions.setText(texte);
    }

    // =========================================================================
    // GETTERS
    // =========================================================================

    public DessinCanvas getCanvasAire()     { return canvasAire;        }
    public EchelleVue getEchelleVue()       { return echelleVue;        }
    public boolean isVoileActif()           { return voileActif;        }
}