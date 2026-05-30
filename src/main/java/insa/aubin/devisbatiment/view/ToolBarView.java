package insa.aubin.devisbatiment.view;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.util.List;

/**
 * Barre d'outils commune à tous les contextes de l'application.
 *
 * Architecture "slots" : les boutons qui s'excluent mutuellement sont
 * superposés dans un StackPane (slot).
 *
 * Règles de visibilité par slot :
 *
 * Slot 1 — validerAire / ajouterNiveau
 *   "validerAire"    → validerAire visible (grisé par défaut, vert quand aire complète),
 *                       ajouterNiveau caché
 *   "ajouterNiveau"  → ajouterNiveau visible, validerAire caché
 *   aucun des deux   → slot entier caché
 *
 * Slot 2 — appartement / piece
 *   "appartement"    → appartement visible, piece caché
 *   "piece"          → piece visible, appartement caché
 *   aucun des deux   → slot entier caché
 *
 * Slot 3 — ouvertures (paire porte+fenetre  OU  escalier+ascenseur)
 *   "porte"/"fenetre"       → paire PF visible (chacun actif/grisé selon liste),
 *                             paire EA cachée
 *   "escalier"/"ascenseur"  → paire EA visible, paire PF cachée
 *   aucun                   → slot entier caché
 *
 * setBtnValiderAireActif(true) est appelé par AppControleur quand les 3 points
 * de l'aire sont posés pour rendre le bouton cliquable.
 */
public class ToolBarView extends HBox {

    // -------------------------------------------------------------------------
    // Chemins des icônes
    // -------------------------------------------------------------------------
    private static final String CHEMIN_MAIN        = "/images/main_icone.png";
    private static final String CHEMIN_SELECTION   = "/images/selection_icone.png";
    private static final String CHEMIN_ECHELLE     = "/images/echelle_icone.png";
    private static final String CHEMIN_MUR         = "/images/mur_icone.png";
    private static final String CHEMIN_PIECE       = "/images/piece_icone.png";
    private static final String CHEMIN_APPARTEMENT = "/images/appartement_icone.png";
    private static final String CHEMIN_PORTE       = "/images/porte_icone.png";
    private static final String CHEMIN_FENETRE     = "/images/fenetre_icone.png";
    private static final String CHEMIN_ASCENSEUR   = "/images/ascenseur_icone.png";
    private static final String CHEMIN_ESCALIER    = "/images/escalier_icone.png";
    private static final String CHEMIN_RETOUR      = "/images/fleche_retour_icone.png";

    // -------------------------------------------------------------------------
    // Styles
    // -------------------------------------------------------------------------
    private static final String STYLE_INACTIF =
            "-fx-cursor: default; -fx-font-family: 'Arial';" +
            "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #34495e;" +
            "-fx-opacity: 0.35;";

    private static final String STYLE_ACTIF =
            "-fx-cursor: hand; -fx-font-family: 'Arial';" +
            "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #34495e;";

    private static final String STYLE_VALIDER_ACTIF =
            "-fx-cursor: hand; -fx-font-weight: bold; -fx-text-alignment: center;" +
            "-fx-text-fill: white; -fx-background-color: #27ae60;";

    private static final String STYLE_VALIDER_INACTIF =
            "-fx-cursor: default; -fx-font-weight: bold; -fx-text-alignment: center;" +
            "-fx-text-fill: white; -fx-background-color: #27ae60; -fx-opacity: 0.35;";

    // -------------------------------------------------------------------------
    // Boutons
    // -------------------------------------------------------------------------
    private final Button btnNavigation;
    private final Button btnSelection;
    private final Button btnEchelle;
    private final Button btnMur;

    private final Button btnValiderAire;
    private final Button btnAjouterNiveau;

    private final Button btnAppartement;
    private final Button btnPiece;

    private final Button btnPorte;
    private final Button btnFenetre;
    private final Button btnEscalier;
    private final Button btnAscenseur;

    private final Button btnRetour;

    // -------------------------------------------------------------------------
    // Slots (StackPanes)
    // -------------------------------------------------------------------------
    private final StackPane slotValiderAjout;
    private final StackPane slotAppartPiece;
    private final StackPane slotOuvertures;

    // -------------------------------------------------------------------------
    // Constructeur
    // -------------------------------------------------------------------------
    public ToolBarView() {
        setSpacing(10);
        setPadding(new Insets(10));
        setAlignment(Pos.CENTER_LEFT);

        // Boutons hors-slot
        btnNavigation = creerBouton("Naviguer",  CHEMIN_MAIN,      80);
        btnSelection  = creerBouton("Sélection", CHEMIN_SELECTION, 80);
        btnEchelle    = creerBouton("Échelle",   CHEMIN_ECHELLE,   70);
        btnMur        = creerBouton("Mur",       CHEMIN_MUR,       60);

        // Slot 1
        btnValiderAire   = creerBoutonValiderAire();
        btnAjouterNiveau = creerBoutonAjouterNiveau();
        slotValiderAjout = creerSlotSuperpose(btnValiderAire, btnAjouterNiveau);

        // Slot 2
        btnAppartement  = creerBouton("Appartement", CHEMIN_APPARTEMENT, 100);
        btnPiece        = creerBouton("Piece",        CHEMIN_PIECE,        60);
        slotAppartPiece = creerSlotSuperpose(btnAppartement, btnPiece);

        // Slot 3
        btnPorte     = creerBouton("Porte",     CHEMIN_PORTE,     60);
        btnFenetre   = creerBouton("Fenêtre",   CHEMIN_FENETRE,   70);
        btnEscalier  = creerBouton("Escalier",  CHEMIN_ESCALIER,  80);
        btnAscenseur = creerBouton("Ascenseur", CHEMIN_ASCENSEUR, 90);
        slotOuvertures = creerSlotOuvertures();

        // Retour
        btnRetour = creerBouton("Retour", CHEMIN_RETOUR, 80);

        // Séparateurs
        Separator sep1 = new Separator(Orientation.VERTICAL);
        Separator sep2 = new Separator(Orientation.VERTICAL);
        Separator sep3 = new Separator(Orientation.VERTICAL);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        getChildren().addAll(
            btnNavigation, btnSelection, btnEchelle,
            sep1,
            slotValiderAjout,
            sep2,
            btnMur, slotAppartPiece,
            sep3,
            slotOuvertures,
            spacer,
            btnRetour
        );

        // État initial : AppControleur appellera mettreAJourBoutons() immédiatement.
        // On cache les slots pour éviter tout affichage parasite.
        setCacherSlot(slotValiderAjout, true);
        setCacherSlot(slotAppartPiece,  true);
        setCacherSlot(slotOuvertures,   true);
        appliquerEtatInactif(btnSelection);
        appliquerEtatInactif(btnMur);
    }

    // =========================================================================
    // API PUBLIQUE
    // =========================================================================

    /**
     * Met à jour la toolbar selon la liste d'identifiants fournie par le contexte.
     *
     * Identifiants gérés :
     *   "navigation", "selection", "echelle", "mur"
     *   "validerAire", "ajouterNiveau"
     *   "appartement", "piece"
     *   "porte", "fenetre", "escalier", "ascenseur"
     */
    public void mettreAJourBoutons(List<String> actifs) {

        // --- Boutons simples ---
        setActif(btnNavigation, actifs.contains("navigation"));
        setActif(btnSelection,  actifs.contains("selection"));
        setActif(btnEchelle,    actifs.contains("echelle"));

        // mur : caché complètement si absent de la liste, sinon actif
        boolean avecMur = actifs.contains("mur");
        btnMur.setVisible(avecMur);
        btnMur.setManaged(avecMur);
        if (avecMur) setActif(btnMur, true);

        // --- Slot 1 : validerAire / ajouterNiveau ---
        boolean avecValider = actifs.contains("validerAire");
        boolean avecAjout   = actifs.contains("ajouterNiveau");

        if (avecValider) {
            // Seul validerAire visible dans le slot (grisé jusqu'à setBtnValiderAireActif)
            setCacherSlot(slotValiderAjout, false);
            setCacherBouton(btnValiderAire,   false);
            setCacherBouton(btnAjouterNiveau, true);
            btnValiderAire.toFront();
            // NB : l'activation/désactivation du vert est gérée par setBtnValiderAireActif()
            // On s'assure qu'il est bien grisé au départ si non encore activé
            if (btnValiderAire.isDisabled()) {
                btnValiderAire.setStyle(STYLE_VALIDER_INACTIF);
            }
        } else if (avecAjout) {
            setCacherSlot(slotValiderAjout, false);
            setCacherBouton(btnAjouterNiveau, false);
            setCacherBouton(btnValiderAire,   true);
            btnAjouterNiveau.toFront();
            setActif(btnAjouterNiveau, true);
        } else {
            // Ni l'un ni l'autre → slot entièrement caché
            setCacherSlot(slotValiderAjout, true);
        }

        // --- Slot 2 : appartement / piece ---
        boolean avecAppart = actifs.contains("appartement");
        boolean avecPiece  = actifs.contains("piece");

        if (avecAppart) {
            setCacherSlot(slotAppartPiece, false);
            setCacherBouton(btnAppartement, false);
            setCacherBouton(btnPiece,       true);
            btnAppartement.toFront();
            setActif(btnAppartement, true);
        } else if (avecPiece) {
            setCacherSlot(slotAppartPiece, false);
            setCacherBouton(btnPiece,       false);
            setCacherBouton(btnAppartement, true);
            btnPiece.toFront();
            setActif(btnPiece, true);
        } else {
            setCacherSlot(slotAppartPiece, true);
        }

        // --- Slot 3 : ouvertures ---
        boolean avecPorte     = actifs.contains("porte");
        boolean avecFenetre   = actifs.contains("fenetre");
        boolean avecEscalier  = actifs.contains("escalier");
        boolean avecAscenseur = actifs.contains("ascenseur");

        HBox pairePF = (HBox) slotOuvertures.getChildren().get(0);
        HBox paireEA = (HBox) slotOuvertures.getChildren().get(1);

        if (avecPorte || avecFenetre) {
            setCacherSlot(slotOuvertures, false);
            pairePF.setVisible(true);  pairePF.setManaged(true);
            paireEA.setVisible(false); paireEA.setManaged(false);
            setActif(btnPorte,   avecPorte);
            setActif(btnFenetre, avecFenetre);
        } else if (avecEscalier || avecAscenseur) {
            setCacherSlot(slotOuvertures, false);
            paireEA.setVisible(true);  paireEA.setManaged(true);
            pairePF.setVisible(false); pairePF.setManaged(false);
            setActif(btnEscalier,  avecEscalier);
            setActif(btnAscenseur, avecAscenseur);
        } else {
            // Aucune ouverture pertinente → slot caché
            setCacherSlot(slotOuvertures, true);
        }
    }

    /**
     * Active le bouton "Valider l'aire" (style vert cliquable) une fois que
     * les 3 points de l'aire ont été posés. Appelé par AppControleur.
     *
     * @param actif true = vert + cliquable ; false = vert grisé
     */
    public void setBtnValiderAireActif(boolean actif) {
        btnValiderAire.setDisable(!actif);
        btnValiderAire.setStyle(actif ? STYLE_VALIDER_ACTIF : STYLE_VALIDER_INACTIF);
    }

    // =========================================================================
    // GETTERS
    // =========================================================================

    public Button getBtnNavigation()    { return btnNavigation;    }
    public Button getBtnSelection()     { return btnSelection;     }
    public Button getBtnEchelle()       { return btnEchelle;       }
    public Button getBtnMur()           { return btnMur;           }
    public Button getBtnAppartement()   { return btnAppartement;   }
    public Button getBtnEscalier()      { return btnEscalier;      }
    public Button getBtnAscenseur()     { return btnAscenseur;     }
    public Button getBtnPiece()         { return btnPiece;         }
    public Button getBtnPorte()         { return btnPorte;         }
    public Button getBtnFenetre()       { return btnFenetre;       }
    public Button getBtnValiderAire()   { return btnValiderAire;   }
    public Button getBtnAjouterNiveau() { return btnAjouterNiveau; }
    public Button getBtnRetour()        { return btnRetour;        }

    // =========================================================================
    // HELPERS PRIVÉS
    // =========================================================================

    /** Active ou désactive visuellement un bouton (style + disable). */
    private void setActif(Button btn, boolean actif) {
        btn.setDisable(!actif);
        if (btn == btnValiderAire) {
            btn.setStyle(actif ? STYLE_VALIDER_ACTIF : STYLE_VALIDER_INACTIF);
        } else {
            btn.setStyle(actif ? STYLE_ACTIF : STYLE_INACTIF);
        }
    }

    private void appliquerEtatInactif(Button btn) {
        setActif(btn, false);
    }

    /**
     * Cache ou affiche un slot entier (visible + managed ensemble pour ne pas
     * laisser d'espace vide dans la HBox).
     */
    private void setCacherSlot(javafx.scene.Node slot, boolean cacher) {
        slot.setVisible(!cacher);
        slot.setManaged(!cacher);
    }

    /**
     * Cache ou affiche un bouton individuel à l'intérieur d'un slot superposé.
     */
    private void setCacherBouton(Button btn, boolean cacher) {
        btn.setVisible(!cacher);
        btn.setManaged(!cacher);
    }

    /**
     * Crée un StackPane avec deux boutons superposés.
     * La largeur du slot est celle du plus large des deux boutons.
     */
    private StackPane creerSlotSuperpose(Button b1, Button b2) {
        StackPane slot = new StackPane(b1, b2);
        slot.setAlignment(Pos.CENTER);
        double w = Math.max(b1.getPrefWidth(), b2.getPrefWidth());
        slot.setPrefWidth(w);
        slot.setMaxWidth(w);
        return slot;
    }

    /**
     * Crée le slot ouvertures : deux HBox superposées dans un StackPane.
     *   index 0 : pairePF (porte + fenetre)
     *   index 1 : paireEA (escalier + ascenseur)
     */
    private StackPane creerSlotOuvertures() {
        HBox pairePF = new HBox(6, btnPorte, btnFenetre);
        pairePF.setAlignment(Pos.CENTER);

        HBox paireEA = new HBox(6, btnEscalier, btnAscenseur);
        paireEA.setAlignment(Pos.CENTER);

        StackPane slot = new StackPane(pairePF, paireEA);
        slot.setAlignment(Pos.CENTER);
        double w = Math.max(
            btnPorte.getPrefWidth()    + btnFenetre.getPrefWidth()   + 6,
            btnEscalier.getPrefWidth() + btnAscenseur.getPrefWidth() + 6
        );
        slot.setPrefWidth(w);
        slot.setMaxWidth(w);
        return slot;
    }

    private Button creerBoutonValiderAire() {
        Button btn = new Button("Valider\nl'aire");
        btn.setStyle(STYLE_VALIDER_INACTIF);
        btn.setPrefSize(80, 60);
        btn.setDisable(true);
        return btn;
    }

    private Button creerBoutonAjouterNiveau() {
        Button btn = new Button("Ajouter Niveau");
        btn.setStyle(STYLE_ACTIF);
        btn.setPrefSize(110, 60);
        Label labelPlus = new Label("+");
        labelPlus.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #34495e;");
        btn.setGraphic(labelPlus);
        btn.setContentDisplay(ContentDisplay.TOP);
        return btn;
    }

    private Button creerBouton(String libelle, String cheminIcone, double largeur) {
        Button btn = new Button(libelle);
        btn.setStyle(STYLE_ACTIF);
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
            System.err.println("ToolBarView : icône introuvable → " + cheminIcone);
        }

        return btn;
    }
}