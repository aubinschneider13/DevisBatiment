package insa.aubin.devisbatiment.controlleur;

import insa.aubin.devisbatiment.modele.*;
import insa.aubin.devisbatiment.view.*;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Contrôleur de l'aire de l'immeuble.
 *
 * ✅ ALLÉGÉ par rapport à l'ancienne version :
 *   - Ne connaît plus le Stage (c'est AppControleur qui gère la navigation).
 *   - Ne connaît plus le TreeView (c'est NavigateurView + AppControleur).
 *   - Ne gère plus les niveaux ni les appartements.
 *   - Ne dialogue plus avec NiveauControleur.
 *   - N'instancie plus ImmeubleView : il reçoit le canvas depuis AppView
 *     via AppControleur, conformément au pattern State.
 *
 * Responsabilité unique : piloter le dessin interactif de l'AireImmeuble
 * sur le DessinCanvas qui lui est fourni, et exposer l'état de cette aire
 * à AppControleur via des getters.
 *
 * Cycle de vie :
 *   1. AppControleur instancie ImmeubleControleur en lui passant le canvasAire
 *      extrait d'AppView.
 *   2. L'utilisateur clique pour poser les 3 points de l'aire.
 *   3. ContexteAire appelle btnValiderAire() puis AppControleur.onAireValidee()
 *      pour basculer de contexte.
 */
public class ImmeubleControleur {

    // --- Vue canvas (reçu d'AppView, pas possédé) ---
    private final DessinCanvas canvas;

    // --- AppView : uniquement pour setInstructions() et getToolBarView() ---
    // ✅ On conserve AppView plutôt que Stage : ImmeubleControleur n'a pas
    //    besoin de changer de scène, il met juste à jour le label et la toolbar.
    private final AppView appView;

    // --- Service de sauvegarde (transmis pour usage futur) ---
    private final GestionnaireSauvegarde gestionnaire;

    // ✅ Listener d'échelle — conservé pour btnEchelle(), branché une seule fois
    private javafx.beans.value.ChangeListener<javafx.scene.control.Toggle>
            listenerEchelle = null;

    // --- Aire de l'immeuble ---
    private AireImmeuble aireImmeuble;
    private int etapeAire = 0;
    private boolean aireValidee = false;
    private int coteEnCoursDeDeplacement = -1;

    /**
     * Unique constructeur valide dans la nouvelle architecture.
     *
     * ✅ L'ancien constructeur (Stage, ImmeubleView, GestionnaireSauvegarde)
     * est supprimé : ImmeubleControleur ne connaît plus ni le Stage ni
     * ImmeubleView. C'est AppControleur qui possède le Stage et AppView.
     *
     * @param canvas       canvas de l'aire fourni par AppView
     * @param appView      vue racine (pour setInstructions et getToolBarView)
     * @param gestionnaire service de sauvegarde
     */
    public ImmeubleControleur(DessinCanvas canvas,
                               AppView appView,
                               GestionnaireSauvegarde gestionnaire) {
        this.canvas       = canvas;
        this.appView      = appView;
        this.gestionnaire = gestionnaire;

        // Branchement des listeners souris sur le canvas de l'aire.
        // ✅ Identiques à l'ancienne version — la logique de dessin est inchangée.
        this.canvas.setOnMouseClicked(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY
                    && e.isStillSincePress()) clicAire(e);
        });
        this.canvas.setOnMouseMoved(e   -> mouvementAire(e));
        this.canvas.setOnMousePressed(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) pressionAire(e);
        });
        this.canvas.setOnMouseDragged(e -> glisserAire(e));
        this.canvas.setOnMouseReleased(e -> relacherAire(e));
    }

    // =========================================================================
    // BOUTONS TOOLBAR — appelés par ContexteAire
    // =========================================================================

    /**
     * Active le mode navigation (pan/zoom) sur le canvas de l'aire.
     * Appelé par ContexteAire.onBtnNavigation().
     *
     * @param t événement ActionEvent (peut être null si appelé programmatiquement)
     */
    public void btnNavigation(javafx.event.ActionEvent t) {
        canvas.setPanActif(true);
    }

    /**
     * Bascule la visibilité du panneau EchelleVue et branche son listener
     * la première fois.
     * Appelé par ContexteAire.onBtnEchelle().
     *
     * ✅ Le listener n'est branché qu'une seule fois (guard listenerEchelle != null)
     * pour éviter les doublons si l'utilisateur clique plusieurs fois sur "Échelle".
     *
     * @param t événement ActionEvent (peut être null si appelé programmatiquement)
     */
    public void btnEchelle(javafx.event.ActionEvent t) {
        EchelleVue echelleVue = appView.getEchelleVue();
        boolean visible = !echelleVue.isVisible();
        echelleVue.setVisible(visible);

        if (visible && listenerEchelle == null) {
            listenerEchelle = (obs, oldVal, newVal) -> {
                if (newVal != null) {
                    canvas.setGridSize(echelleVue.getEchelleSelectionnee());
                }
            };
            echelleVue.getGroupeEchelle()
                      .selectedToggleProperty()
                      .addListener(listenerEchelle);
        }
    }

    /**
     * Valide l'emprise de l'immeuble :
     *   - marque l'aire comme validée dans le modèle
     *   - débranche les listeners souris du canvas (l'aire ne sera plus modifiable)
     *   - met à jour le label d'instructions
     *
     * ✅ L'instanciation du modèle Immeuble et la bascule de contexte sont
     * gérées par AppControleur.onAireValidee(), appelé par ContexteAire
     * juste après cette méthode. Séparation des responsabilités respectée.
     *
     * @param t événement ActionEvent (peut être null si appelé programmatiquement)
     */
    public void btnValiderAire(javafx.event.ActionEvent t) {
        if (aireImmeuble == null || etapeAire != 3) return;

        aireImmeuble.valider();
        aireValidee = true;

        // Débrancher les listeners souris : l'aire ne peut plus être modifiée
        canvas.setOnMouseClicked(null);
        canvas.setOnMouseMoved(null);
        canvas.setOnMousePressed(null);
        canvas.setOnMouseDragged(null);
        canvas.setOnMouseReleased(null);

        appView.setInstructions(
            "Aire validée — ajoutez des niveaux via le bouton ou le navigateur"
        );

        // Le voile cadenas et la bascule de contexte sont gérés par AppControleur
    }

    /**
     * Annule le dessin de l'aire en cours et remet l'état à zéro.
     * Appelé par AppControleur quand l'utilisateur appuie sur Échap.
     */
    public void annulerAire() {
        if (aireValidee) return;
        if (aireImmeuble != null) {
            canvas.getElements().remove(aireImmeuble);
        }
        aireImmeuble = null;
        etapeAire = 0;
        coteEnCoursDeDeplacement = -1;
        appView.getToolBarView().setBtnValiderAireActif(false);
        appView.setInstructions(
            "Cliquez pour définir le premier coin de l'immeuble"
        );
        canvas.redrawAll();
    }

    // =========================================================================
    // GESTION DES CLICS SUR LE CANVAS DE L'AIRE
    // =========================================================================
    // ✅ Logique identique à l'ancienne version — aucun changement fonctionnel.

    private void clicAire(javafx.scene.input.MouseEvent e) {
        if (aireValidee) return;

        Point2D snap = canvas.snapToGrid(e.getX(), e.getY());
        Point pClic  = new Point(snap.getX(), snap.getY());

        switch (etapeAire) {
            case 0:
                aireImmeuble = new AireImmeuble(pClic);
                canvas.ajouterElement(aireImmeuble);
                etapeAire = 1;
                appView.setInstructions(
                    "Cliquez pour définir le deuxième coin (côté 1)");
                break;
            case 1:
                aireImmeuble.setP2(pClic);
                etapeAire = 2;
                appView.setInstructions(
                    "Cliquez pour définir la largeur (côté 2)");
                break;
            case 2:
                Point p3Contraint = calculerPointOrthogonal(
                    aireImmeuble.getP2(), pClic,
                    aireImmeuble.getP1(), aireImmeuble.getP2()
                );
                aireImmeuble.setP3(p3Contraint);
                etapeAire = 3;
                appView.setInstructions(
                    "Glissez les côtés pour ajuster, puis cliquez sur « Valider »");
                // ✅ Active le bouton "Valider l'aire" dans la toolbar commune
                appView.getToolBarView().setBtnValiderAireActif(true);
                break;
        }
        canvas.redrawAll();
    }

    private void mouvementAire(javafx.scene.input.MouseEvent e) {
        if (aireValidee || aireImmeuble == null) return;

        Point2D snap  = canvas.snapToGrid(e.getX(), e.getY());
        Point pSouris = new Point(snap.getX(), snap.getY());

        if (etapeAire == 1) {
            aireImmeuble.setP2(pSouris);
        } else if (etapeAire == 2) {
            Point p3Contraint = calculerPointOrthogonal(
                aireImmeuble.getP2(), pSouris,
                aireImmeuble.getP1(), aireImmeuble.getP2()
            );
            aireImmeuble.setP3(p3Contraint);
        } else if (etapeAire == 3) {
            int coteSurvole = aireImmeuble.detecterCote(snap.getX(), snap.getY(), 0.3);
            aireImmeuble.setCoteGlisse(coteSurvole);
        }
        canvas.redrawAll();
    }

    private void pressionAire(javafx.scene.input.MouseEvent e) {
        if (aireValidee || aireImmeuble == null || etapeAire != 3) return;

        Point2D snap = canvas.snapToGrid(e.getX(), e.getY());
        coteEnCoursDeDeplacement = aireImmeuble.detecterCote(
            snap.getX(), snap.getY(), 0.3);
        if (coteEnCoursDeDeplacement != -1) {
            aireImmeuble.setCoteGlisse(coteEnCoursDeDeplacement);
            canvas.redrawAll();
        }
    }

    private void glisserAire(javafx.scene.input.MouseEvent e) {
        if (coteEnCoursDeDeplacement == -1 || aireImmeuble == null) return;

        Point2D snap = canvas.snapToGrid(e.getX(), e.getY());
        aireImmeuble.deplacerCote(
            coteEnCoursDeDeplacement, snap.getX(), snap.getY());
        canvas.redrawAll();
    }

    private void relacherAire(javafx.scene.input.MouseEvent e) {
        if (coteEnCoursDeDeplacement != -1) {
            coteEnCoursDeDeplacement = -1;
            aireImmeuble.setCoteGlisse(-1);
            canvas.redrawAll();
        }
    }

    // =========================================================================
    // UTILITAIRE GÉOMÉTRIQUE
    // =========================================================================

    /**
     * Calcule le point orthogonal à la direction (refP1→refP2) passant par
     * {@code centre} et le plus proche de {@code cible}.
     * Utilisé pour contraindre le troisième coin de l'aire à être
     * perpendiculaire au premier côté.
     */
    private Point calculerPointOrthogonal(Point centre, Point cible,
                                          Point refP1, Point refP2) {
        double dx    = refP2.getX() - refP1.getX();
        double dy    = refP2.getY() - refP1.getY();
        double perpX = -dy;
        double perpY = dx;
        double ux    = cible.getX() - centre.getX();
        double uy    = cible.getY() - centre.getY();
        double s     = (ux * perpX + uy * perpY) / (perpX * perpX + perpY * perpY);
        return new Point(
            centre.getX() + s * perpX,
            centre.getY() + s * perpY
        );
    }

    // =========================================================================
    // GETTERS — exposés à AppControleur
    // =========================================================================

    /**
     * Retourne l'aire de l'immeuble telle que dessinée par l'utilisateur.
     * Utilisé par AppControleur pour instancier le modèle Immeuble et par
     * NiveauControleur pour afficher le contour en fond de canvas.
     *
     * @return AireImmeuble courante, ou null si aucun point n'a encore été posé
     */
    public AireImmeuble getAireImmeuble() { return aireImmeuble; }

    /**
     * Indique si l'aire a été validée par l'utilisateur.
     * Utilisé par AppControleur pour conditionner l'ajout de niveaux.
     *
     * @return true si btnValiderAire() a été appelé avec succès
     */
    public boolean isAireValidee() { return aireValidee; }

    /**
     * Expose le canvas de l'aire pour que NiveauControleur puisse y lire
     * la taille de grille courante si nécessaire.
     *
     * @return le DessinCanvas de l'aire
     */
    public DessinCanvas getCanvas() { return canvas; }
}