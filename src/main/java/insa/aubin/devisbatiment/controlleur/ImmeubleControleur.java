package insa.aubin.devisbatiment.controlleur;

import insa.aubin.devisbatiment.modele.*;
import insa.aubin.devisbatiment.view.*;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Contrôleur CAO dédié à la capture, au tracé et à l'ajustement géométrique de l'emprise au sol du bâtiment.
 * <p>
 * Cette classe orchestre l'interaction événementielle de la phase initiale de conception au sein du
 * motif <b>MVC (Modèle-Vue-Contrôleur)</b>. Conformément au <b>Pattern State (Contexte)</b>, elle n'instancie plus
 * la vue racine et ne gère plus les transitions globales. Sa responsabilité unique est de piloter le dessin
 * interactif de l'{@link AireImmeuble} sur le {@link DessinCanvas} qui lui est fourni par le contrôleur maître.
 * </p>
 * <p>
 * <b>Cinématique de saisie (3 points contraints) :</b>
 * <ol>
 * <li><b>Clic 1 :</b> Ancrage de l'origine géométrique ({@code Point p1}).</li>
 * <li><b>Clic 2 :</b> Définition du vecteur directeur du premier côté ({@code Point p2}).</li>
 * <li><b>Clic 3 :</b> Projection orthogonale contrainte par rapport au segment [p1, p2] pour fixer la largeur et engendrer le rectangle initial ({@code Point p3}).</li>
 * <li><b>Phase d'ajustement :</b> Support du drag-and-drop sur les arêtes pour redimensionner les façades avant validation définitive.</li>
 * </ol>
 * </p>
 * * @see AireImmeuble
 * @see DessinCanvas
 * @see AppView
 */
public class ImmeubleControleur {

    /** Le canevas de rendu vectoriel sur lequel s'effectuent les opérations de tracé et d'aimantage. */
    private final DessinCanvas canvas;

    /** La vue racine principale, sollicitée exclusivement pour diffuser les consignes et rafraîchir les boutons. */
    private final AppView appView;

    /** Le service de persistance pour l'accès aux flux de sauvegarde et d'écriture de configuration. */
    private final GestionnaireSauvegarde gestionnaire;

    /** Écouteur de propriété mémorisant le changement de granulométrie de la grille de snapping (aimantage). */
    private javafx.beans.value.ChangeListener<javafx.scene.control.Toggle> listenerEchelle = null;

    /** L'entité métier représentant le polygone contraint de l'emprise au sol. */
    private AireImmeuble aireImmeuble;

    /** Indicateur d'étape du chaînage géométrique (valeurs de 0 à 3). */
    private int etapeAire = 0;

    /** Flag de verrouillage indiquant si l'emprise au sol est définitive et non-modifiable. */
    private boolean aireValidee = false;

    /** Index de l'arête en cours de déplacement par l'utilisateur (vaut -1 si aucune arête n'est saisie). */
    private int coteEnCoursDeDeplacement = -1;

    /**
     * <b>Constructeur unique du contrôleur géométrique.</b>
     * <p>
     * Initialise les liaisons vers la vue racine, le canvas et la persistance, puis procède à la capture
     * et à l'accrochage des écouteurs d'événements physiques (clics, déplacements, glissements de souris).
     * </p>
     *
     * @param canvas       Le canevas vectoriel extrait de la zone centrale d'{@code AppView}.
     * @param appView      La fenêtre racine de l'application gérant les bandeaux d'affichage.
     * @param gestionnaire Le gestionnaire de sauvegarde du projet.
     */
    public ImmeubleControleur(DessinCanvas canvas,
                              AppView appView,
                              GestionnaireSauvegarde gestionnaire) {
        this.canvas       = canvas;
        this.appView      = appView;
        this.gestionnaire = gestionnaire;

        // Configuration des gestionnaires d'événements de la souris sur le canvas de CAO
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
    // BOUTONS TOOLBAR — Invoqués par le ContexteActif (Pattern State)
    // =========================================================================

    /**
     * Déclenche l'activation du mode de translation et de zoom (pan/zoom) sur la surface du canevas.
     *
     * @param t L'événement d'action JavaFX associé.
     */
    public void btnNavigation(javafx.event.ActionEvent t) {
        canvas.setPanActif(true);
    }

    /**
     * Alerte le sous-panneau d'échelle graphique afin d'ajuster le pas de la grille vectorielle.
     * <p>
     * Le patron de conception garantit ici qu'un unique écouteur de propriété (listener) est branché
     * sur le groupe de boutons afin de prévenir les fuites de mémoire mémoire lors des clics répétés.
     * </p>
     *
     * @param t L'événement d'action JavaFX associé.
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
     * Valide définitivement l'emprise géométrique brute du bâtiment.
     * <p>
     * <b>Mécanique de rupture :</b>
     * Cette méthode applique le statut verrouillé au modèle, puis procède à l'éradication complète (purge)
     * de tous les écouteurs de souris du canvas, figeant ainsi l'aire. La bascule vers le contexte supérieur
     * (étages) est alors notifiée au contrôleur maître.
     * </p>
     *
     * @param t L'événement d'action JavaFX associé.
     */
    public void btnValiderAire(javafx.event.ActionEvent t) {
        if (aireImmeuble == null || etapeAire != 3) return;

        aireImmeuble.valider();
        aireValidee = true;

        // Désactivation des interactions physiques pour sanctuariser l'aire géométrique
        canvas.setOnMouseClicked(null);
        canvas.setOnMouseMoved(null);
        canvas.setOnMousePressed(null);
        canvas.setOnMouseDragged(null);
        canvas.setOnMouseReleased(null);

        appView.setInstructions(
                "Aire validée — ajoutez des niveaux via le bouton ou le navigateur"
        );
    }

    /**
     * Annule les opérations de tracé en cours et réinitialise intégralement l'état du contrôleur.
     * <p>
     * Efface l'élément du canevas et désactive les boutons de validation au sein de la toolbar.
     * Invoqué notamment lors de la capture de la touche d'échappement (Échap).
     * </p>
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
    // GESTION DES ÉVÉNEMENTS SOURIS (INTERACTION CANVAS)
    // =========================================================================

    /**
     * Intercepte les clics sur le canevas pour implanter séquentiellement les jalons du polygone de l'aire.
     * @param e L'événement de souris capté.
     */
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
                appView.getToolBarView().setBtnValiderAireActif(true);
                break;
        }
        canvas.redrawAll();
    }

    /**
     * Calcule l'aperçu dynamique ("rubber-banding") des lignes de l'emprise en suivant le déplacement du curseur.
     * @param e L'événement de déplacement de la souris.
     */
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

    /**
     * Identifie si l'utilisateur presse la souris à proximité immédiate d'une arête pour initier un glissement.
     * @param e L'événement de pression de la souris.
     */
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

    /**
     * Répercute en temps réel la translation appliquée à une arête sélectionnée lors du glissement.
     * @param e L'événement de déplacement avec bouton enfoncé.
     */
    private void glisserAire(javafx.scene.input.MouseEvent e) {
        if (coteEnCoursDeDeplacement == -1 || aireImmeuble == null) return;

        Point2D snap = canvas.snapToGrid(e.getX(), e.getY());
        aireImmeuble.deplacerCote(
                coteEnCoursDeDeplacement, snap.getX(), snap.getY());
        canvas.redrawAll();
    }

    /**
     * Clôture l'opération de translation de l'arête et réinitialise les index de sélection.
     * @param e L'événement de relâchement du clic.
     */
    private void relacherAire(javafx.scene.input.MouseEvent e) {
        if (coteEnCoursDeDeplacement != -1) {
            coteEnCoursDeDeplacement = -1;
            aireImmeuble.setCoteGlisse(-1);
            canvas.redrawAll();
        }
    }

    // =========================================================================
    // UTILITAIRE GÉOMÉTRIQUE VECTORIEL
    // =========================================================================

    /**
     * Calcule le point orthogonal à la direction de référence (refP1 → refP2) passant par un centre et le plus proche d'une cible.
     * <p>
     * <b>Algorithme de projection :</b>
     * Cette méthode mathématique contraint le troisième sommet de l'emprise à former un angle
     * rigoureusement droit (90°) avec le premier côté dessiné, garantissant la parfaite rectangularité
     * des fondations de l'immeuble.
     * </p>
     *
     * @param centre Le point pivot d'articulation de l'angle droit (généralement p2).
     * @param cible  La coordonnée courante de la souris à projeter.
     * @param refP1  L'origine du segment de base.
     * @param refP2  L'extrémité du segment de base dictant le vecteur directeur.
     * @return L'instance de {@link Point} contrainte calculée par projection orthogonale.
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
    // GETTERS & SETTERS (INTERFAÇAGE APPCONTROLEUR)
    // =========================================================================

    /**
     * Retourne l'entité géométrique de l'emprise au sol.
     * @return L'instance courante d'{@link AireImmeuble}.
     */
    public AireImmeuble getAireImmeuble() { return aireImmeuble; }

    /**
     * Indique si l'emprise au sol du terrain a été validée et verrouillée.
     * @return {@code true} si l'aire est scellée, {@code false} sinon.
     */
    public boolean isAireValidee() { return aireValidee; }

    /**
     * Retourne le canevas de dessin vectoriel associé.
     * @return L'instance de {@link DessinCanvas}.
     */
    public DessinCanvas getCanvas() { return canvas; }

    /**
     * Injecte une emprise géométrique préexistante (chargement de fichier) et configure instantanément le contrôleur à l'état validé.
     * <p>
     * Cette méthode neutralise les écouteurs de souris pour empêcher toute altération ultérieure d'un plan chargé
     * et rafraîchit le calque graphique.
     * </p>
     *
     * @param aire L'entité {@link AireImmeuble} restaurée depuis la persistance.
     */
    public void setAireImmeuble(AireImmeuble aire) {
        this.aireImmeuble = aire;
        this.etapeAire    = 3;
        this.aireValidee  = true;

        canvas.setOnMouseClicked(null);
        canvas.setOnMouseMoved(null);
        canvas.setOnMousePressed(null);
        canvas.setOnMouseDragged(null);
        canvas.setOnMouseReleased(null);

        canvas.getElements().clear();
        canvas.ajouterElement(aire);
        canvas.redrawAll();
    }
}