package insa.aubin.devisbatiment.view;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Barre d'outils commune à tous les contextes de l'application.
 *
 * Contient tous les boutons possibles (navigation, échelle, mur, appartement,
 * porte, fenêtre, validerAire, ajouterNiveau, retour). Chaque contexte
 * contrôle quels boutons sont visibles via mettreAJourBoutons(List<String>).
 *
 * Les actions des boutons sont branchées par AppControleur après construction,
 * via les getters exposés en bas de classe. ToolBarView ne connaît donc ni
 * AppControleur ni aucun autre contrôleur — elle est purement déclarative.
 *
 * Identifiants reconnus par mettreAJourBoutons() :
 *   "navigation", "echelle", "mur", "appartement",
 *   "porte", "fenetre", "validerAire", "ajouterNiveau"
 * Le bouton "retour" est toujours visible et ne fait pas partie de cette liste.
 */
public class ToolBarView extends HBox {

    // Chemins des icônes (identiques à ceux utilisés dans ImmeubleView / PieceView)
    private static final String CHEMIN_MAIN       = "/images/main_icone.png";
    private static final String CHEMIN_ECHELLE    = "/images/echelle_icone.png";
    private static final String CHEMIN_MUR        = "/images/mur_icone.png";
    private static final String CHEMIN_APPARTEMENT= "/images/appartement_icone.png";
    private static final String CHEMIN_PORTE      = "/images/porte_icone.png";
    private static final String CHEMIN_FENETRE    = "/images/fenetre_icone.png";
    private static final String CHEMIN_RETOUR     = "/images/fleche_retour_icone.png";

    // --- Boutons ---
    private final Button btnNavigation;
    private final Button btnEchelle;
    private final Button btnMur;
    private final Button btnAppartement;
    private final Button btnPorte;
    private final Button btnFenetre;
    private final Button btnValiderAire;
    private final Button btnAjouterNiveau;
    private final Button btnRetour;

    /**
     * Map id → bouton pour que mettreAJourBoutons() puisse itérer proprement
     * sans une longue suite de if/else.
     */
    private final Map<String, Button> boutonsParId = new HashMap<>();

    public ToolBarView() {
        setSpacing(10);
        setPadding(new Insets(10));
        setAlignment(Pos.CENTER_LEFT);

        // --- Construction des boutons ---
        btnNavigation    = creerBouton("Naviguer",       CHEMIN_MAIN,        80);
        btnEchelle       = creerBouton("Échelle",        CHEMIN_ECHELLE,     70);
        btnMur           = creerBouton("Mur",            CHEMIN_MUR,         60);
        btnAppartement   = creerBouton("Appartement",    CHEMIN_APPARTEMENT, 100);
        btnPorte         = creerBouton("Porte",          CHEMIN_PORTE,       60);
        btnFenetre       = creerBouton("Fenêtre",        CHEMIN_FENETRE,     70);
        btnRetour        = creerBouton("Retour",         CHEMIN_RETOUR,      80);

        // Bouton "Valider l'aire" — style vert distinct, pas d'icône image
        btnValiderAire = new Button("Valider\nl'aire");
        btnValiderAire.setStyle(
            "-fx-cursor: hand; -fx-font-weight: bold; -fx-text-alignment: center;" +
            "-fx-text-fill: white; -fx-background-color: #27ae60;"
        );
        btnValiderAire.setPrefSize(80, 60);
        btnValiderAire.setDisable(true); // activé quand l'aire est complète (3 points posés)

        // Bouton "Ajouter Niveau" — icône "+" généré en Label
        btnAjouterNiveau = new Button("Ajouter Niveau");
        btnAjouterNiveau.setStyle(
            "-fx-cursor: hand; -fx-font-weight: bold; -fx-text-fill: #34495e;"
        );
        btnAjouterNiveau.setPrefSize(110, 60);
        Label labelPlus = new Label("+");
        labelPlus.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #34495e;");
        btnAjouterNiveau.setGraphic(labelPlus);
        btnAjouterNiveau.setContentDisplay(ContentDisplay.TOP);

        // --- Remplissage de la map id → bouton ---
        boutonsParId.put("navigation",    btnNavigation);
        boutonsParId.put("echelle",       btnEchelle);
        boutonsParId.put("mur",           btnMur);
        boutonsParId.put("appartement",   btnAppartement);
        boutonsParId.put("porte",         btnPorte);
        boutonsParId.put("fenetre",       btnFenetre);
        boutonsParId.put("validerAire",   btnValiderAire);
        boutonsParId.put("ajouterNiveau", btnAjouterNiveau);
        // "retour" est géré séparément — toujours visible

        // --- Séparateurs ---
        Separator sep1 = new Separator(Orientation.VERTICAL);
        Separator sep2 = new Separator(Orientation.VERTICAL);

        // Espaceur poussant le bouton Retour à droite
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // --- Assemblage (tous les boutons présents ; la visibilité est gérée dynamiquement) ---
        getChildren().addAll(
            btnNavigation, btnEchelle,
            sep1,
            btnValiderAire, btnAjouterNiveau,
            btnMur, btnAppartement,
            sep2,
            btnPorte, btnFenetre,
            spacer,
            btnRetour
        );

        // État initial : tout masqué sauf retour (AppControleur appellera mettreAJourBoutons)
        boutonsParId.values().forEach(b -> b.setVisible(false));
    }

    // =========================================================================
    // API PUBLIQUE — appelée par AppControleur
    // =========================================================================

    /**
     * Met à jour la visibilité des boutons selon le contexte actif.
     * Seuls les boutons dont l'identifiant figure dans {@code boutonsVisibles}
     * sont affichés ; tous les autres sont masqués.
     *
     * @param boutonsVisibles liste des identifiants à afficher (cf. Javadoc classe)
     */
    public void mettreAJourBoutons(List<String> boutonsVisibles) {
        boutonsParId.forEach((id, btn) ->
            btn.setVisible(boutonsVisibles.contains(id))
        );
    }

    /**
     * Active ou désactive le bouton "Valider l'aire".
     * Appelé par AppControleur quand l'aire passe de 2 à 3 points posés.
     *
     * @param actif true = bouton cliquable
     */
    public void setBtnValiderAireActif(boolean actif) {
        btnValiderAire.setDisable(!actif);
    }

    // =========================================================================
    // UTILITAIRE PRIVÉ — création d'un bouton avec icône
    // =========================================================================

    /**
     * Crée un bouton standardisé avec une icône en haut et le libellé en bas.
     *
     * @param libelle  texte affiché sous l'icône
     * @param cheminIcone chemin ressource de l'image (ex. "/images/mur_icone.png")
     * @param largeur  largeur préférée du bouton en pixels
     * @return bouton configuré
     */
    private Button creerBouton(String libelle, String cheminIcone, double largeur) {
        Button btn = new Button(libelle);
        btn.setStyle(
            "-fx-cursor: hand; -fx-font-family: 'Arial';" +
            "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #34495e;"
        );
        btn.setPrefSize(largeur, 60);

        try {
            Image img = new Image(getClass().getResource(cheminIcone).toExternalForm());
            ImageView icone = new ImageView(img);
            icone.setFitHeight(30);
            icone.setFitWidth(30);
            icone.setPreserveRatio(true);
            btn.setGraphic(icone);
            btn.setContentDisplay(ContentDisplay.TOP);
        } catch (Exception e) {
            // Icône introuvable : le bouton reste fonctionnel sans image
            System.err.println("ToolBarView : icône introuvable → " + cheminIcone);
        }

        return btn;
    }

    // =========================================================================
    // GETTERS — pour que AppControleur branche les actions
    // =========================================================================

    public Button getBtnNavigation()    { return btnNavigation;    }
    public Button getBtnEchelle()       { return btnEchelle;       }
    public Button getBtnMur()           { return btnMur;           }
    public Button getBtnAppartement()   { return btnAppartement;   }
    public Button getBtnPorte()         { return btnPorte;         }
    public Button getBtnFenetre()       { return btnFenetre;       }
    public Button getBtnValiderAire()   { return btnValiderAire;   }
    public Button getBtnAjouterNiveau() { return btnAjouterNiveau; }
    public Button getBtnRetour()        { return btnRetour;        }
}