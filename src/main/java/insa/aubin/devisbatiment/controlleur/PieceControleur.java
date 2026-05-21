package insa.aubin.devisbatiment.controlleur;

import insa.aubin.devisbatiment.modele.*;
import insa.aubin.devisbatiment.modele.GeometrieUtils.SegmentSource;
import insa.aubin.devisbatiment.view.DashBoardView;
import insa.aubin.devisbatiment.view.PieceView;

import java.util.*;
import java.util.function.Function;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.TreeItem;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * Contrôleur de la vue d'un appartement.
 *
 * Responsabilité : gérer le dessin interactif sur le DessinCanvas de l'appartement
 * (murs intérieurs libres ou rectangulaires, pièces par détection de cycle minimal,
 * portes et fenêtres sur les murs).
 *
 * La géométrie (détection de point dans polygone, cycle minimal, subdivision
 * des segments, etc.) est entièrement déléguée à {@link GeometrieUtils}.
 */
public class PieceControleur {

    // =========================================================================
    // CONSTANTES D'ÉTAT
    // =========================================================================

    public static final int ETAT_RIEN      = 0;
    public static final int ETAT_MUR       = 30;
    public static final int ETAT_PORTE     = 40;
    public static final int ETAT_FENETRE   = 50;
    public static final int ETAT_PIECE     = 60;
    public static final int ETAT_SELECTION = 70; // ✅ Ajouté de vos modifications

    // =========================================================================
    // ATTRIBUTS
    // =========================================================================

    private final PieceView            vue;
    private final Stage                stage;
    private final GestionnaireSauvegarde gestionnaire;
    private NiveauControleur niveauControleur = null;
    private int etat = ETAT_RIEN;

    // Appartement courant et son contour
    private Appartement   appartement          = null;
    private List<Point>   polygoneAppartement  = null;
    private javafx.beans.value.ChangeListener<javafx.scene.control.Toggle> listenerEchelle = null;

    // Pièces créées dans cet appartement
    private final List<Piece>                  pieces        = new ArrayList<>();
    private final Map<TreeItem<String>, Piece> mapItemPiece  = new HashMap<>();
    // ✅ Ajouté : Pour relier les clones graphiques translatés aux vrais murs du modèle
    private final Map<Mur, Mur> mapCopieVersOriginal = new HashMap<>();
    private boolean afficherAdjacenceCouloir = true;
    private List<double[]> cotesBatimentAffichage = new ArrayList<>();

    // Callback notifiant AppControleur lors de la création d'une pièce
    private Function<Piece, TreeItem<String>> onPieceCree = null;

    // État du dessin de mur libre
    private Mur mur1EnCours = null;

    // État du dessin de mur rectangulaire
    private Mur   mur1Rect       = null;
    private Mur   mur2Rect       = null;
    private Point p1Rect         = null;
    private Point p2Rect         = null;
    private int   etapeRectangle = 0;

    // Mur actuellement survolé (pour portes / fenêtres)
    private Mur murSurvole = null;

    // ✅ Ajouté : Variables de votre mode sélection pour les revêtements
    private final List<SurfaceAvecRevetement> surfacesSelectionnees = new ArrayList<>();

    // =========================================================================
    // CONSTRUCTEUR
    // =========================================================================

    public PieceControleur(PieceView vue, Stage stage,
                           GestionnaireSauvegarde gestionnaire) {
        this.vue          = vue;
        this.stage        = stage;
        this.gestionnaire = gestionnaire;
    }

    // =========================================================================
    // CALLBACK — enregistrement par AppControleur après construction
    // =========================================================================

    public void setOnPieceCree(Function<Piece, TreeItem<String>> callback) {
        this.onPieceCree = callback;
    }

    // =========================================================================
    // NAVIGATION
    // =========================================================================

    public void mettreFenetrePleinEcran() {
        Platform.runLater(() -> {
            Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
            stage.setResizable(true);
            stage.setX(bounds.getMinX());
            stage.setY(bounds.getMinY());
            stage.setWidth(bounds.getWidth());
            stage.setHeight(bounds.getHeight());
        });
    }

    public void retourDashboard() {
        DashBoardView dashBoardView = new DashBoardView();
        Scene dashScene = new Scene(dashBoardView);
        stage.setScene(dashScene);
        stage.setTitle("InsaBuilder - Tableau de bord");
        new DashBoardControleur(dashBoardView, stage, gestionnaire);
        mettreFenetrePleinEcran();
    }

    // =========================================================================
    // GESTION DES ÉTATS
    // =========================================================================

    public void changerEtat(int nouvelEtat) {
        // ✅ Provenant de origin/master : Nettoyer le fantôme d'ouverture en quittant le mode
        if (this.etat == ETAT_PORTE || this.etat == ETAT_FENETRE) {
            murSurvole = null;
            vue.getCanvas().setFantome(null, 0, 0, 0, false);
        }
        this.etat = nouvelEtat;
        vue.getCanvas().setPanActif(nouvelEtat == ETAT_RIEN);
        vue.getOptionsMurVue().setVisible(nouvelEtat == ETAT_MUR);

        switch (nouvelEtat) {
            case ETAT_RIEN    -> vue.setInstructions(
                    "Navigation active — Clic droit ou molette pour déplacer la vue");
            case ETAT_MUR     -> mettreAJourInstructionsMur();
            case ETAT_PIECE   -> vue.setInstructions(
                    "Cliquez dans une zone fermée pour créer une pièce");
            case ETAT_PORTE   -> vue.setInstructions(
                    "Survolez un mur puis cliquez pour insérer une porte — Échap pour annuler");
            case ETAT_FENETRE -> vue.setInstructions(
                    "Survolez un mur puis cliquez pour insérer une fenêtre — Échap pour annuler");
            case ETAT_SELECTION -> vue.setInstructions(
                    "Mode Matériaux — Cliquez sur un mur ou un sol pour le sélectionner"); // ✅
        }
    }

    private void mettreAJourInstructionsMur() {
        if (vue.getOptionsMurVue().estRectangulaire()) {
            vue.setInstructions("Mode rectangle — Cliquez pour définir le premier coin");
        } else {
            vue.setInstructions("Mode libre — Cliquez pour définir le début du mur");
        }
    }

    // =========================================================================
    // BOUTONS TOOLBAR
    // =========================================================================

    public void btnNavigation(ActionEvent t) {
        changerEtat(ETAT_RIEN);
    }

    public void btnMur(ActionEvent t) {
        changerEtat(ETAT_MUR);
    }

    public void activerModePiece() {
        changerEtat(ETAT_PIECE);
    }

    public void btnEchelle(ActionEvent t) {
        boolean visible = !vue.getEchelleVue().isVisible();
        vue.getEchelleVue().setVisible(visible);
        if (visible && listenerEchelle == null) {
            listenerEchelle = (obs, oldVal, newVal) -> {
                if (newVal != null) {
                    vue.getCanvas().setGridSize(
                            vue.getEchelleVue().getEchelleSelectionnee());
                }
            };
            vue.getEchelleVue().getGroupeEchelle()
                    .selectedToggleProperty().addListener(listenerEchelle);
        }
    }

    // =========================================================================
    // CLICS DANS LA ZONE DE DESSIN
    // =========================================================================

    public void clicDansZoneDeDessin(MouseEvent event) {
        Point pClic = posInModel(event.getX(), event.getY());

        if (!estDansAppartement(pClic.getX(), pClic.getY())) return;

        switch (etat) {
            case ETAT_MUR       -> gererClicMur(pClic);
            case ETAT_PIECE     -> gererClicPiece(pClic);
            case ETAT_PORTE     -> gererClicOuverture(pClic, true);
            case ETAT_FENETRE   -> gererClicOuverture(pClic, false);
            case ETAT_SELECTION -> gererClicSelection(pClic, event); // ✅
        }
    }

    // =========================================================================
    // MOUVEMENTS SOURIS
    // =========================================================================

    public void mouseMovedDansZoneDessin(MouseEvent event) {
        Point pSouris = posInModel(event.getX(), event.getY());

        if (etat == ETAT_MUR) {
            gererMouvementMur(pSouris);
        } else if (etat == ETAT_PORTE || etat == ETAT_FENETRE) {
            gererMouvementOuverture(pSouris);
        }
    }

    // =========================================================================
    // GESTION DU MODE SÉLECTION (Revêtements) - MISE À JOUR BIFACE
    // =========================================================================

    private void gererClicSelection(Point pClic, MouseEvent event) {
        // Sensibilité ajustée à 0.15 (15 cm) pour mieux distinguer le "pile" du "face"
        Mur cibleCanvas = trouverMurProcheAjuste(pClic, 0.15);

        if (cibleCanvas != null) {
            Mur vraiMurModele = retrouverVraiMurModele(cibleCanvas);
            Mur ref = vraiMurModele != null ? vraiMurModele : cibleCanvas;

            // Raccourci de secours : Si SHIFT est enfoncé, on sélectionne les DEUX côtés d'un coup
            if (event.isShiftDown()) {
                // On s'assure d'ajouter (ou d'inverser) les deux cloisons
                basculerSelection(ref.getCoteGauche());
                basculerSelection(ref.getCoteDroit());
                vue.setInstructions("Sélection biface : Côté Gauche ET Droit synchronisés.");
            } else {
                // Calcul vectoriel de précision par rapport à l'axe du mur
                double x1 = ref.getPoint1().getX();
                double y1 = ref.getPoint1().getY();
                double x2 = ref.getPoint2().getX();
                double y2 = ref.getPoint2().getY();
                double px = pClic.getX();
                double py = pClic.getY();

                double cross = (x2 - x1) * (py - y1) - (y2 - y1) * (px - x1);

                // On s'assure d'extraire l'original du mur pour avoir les bonnes références d'affichage
                Mur originalRef = ref.getOriginal();

                if (event.isShiftDown()) {
                    basculerSelection(originalRef.getCoteGauche());
                    basculerSelection(originalRef.getCoteDroit());
                } else {
                    if (cross > 0) {
                        basculerSelection(originalRef.getCoteGauche());
                    } else {
                        basculerSelection(originalRef.getCoteDroit());
                    }
                }
            }
        } else {
            // Clic au centre de la pièce (Sol + Plafond)
            for (Piece piece : pieces) {
                if (GeometrieUtils.pointDansPolygone(pClic.getX(), pClic.getY(), piece.getPoints())) {
                    basculerSelection(piece.getSol());
                    basculerSelection(piece.getPlafond());
                    vue.setInstructions("Centre de pièce détecté : Sol et Plafond sélectionnés.");
                    break;
                }
            }
        }
    }

    /**
     * Compare les coordonnées du mur graphique avec les murs métier en mémoire.
     * Donne la priorité à l'appartement global pour capturer TOUTES les faces,
     * même celles qui font face à un couloir ou à l'extérieur.
     */
    private Mur retrouverVraiMurModele(Mur murCanvas) {
        // 1. Si on est dans la vue d'une pièce isolée (murs copiés/translatés)
        if (mapCopieVersOriginal != null && mapCopieVersOriginal.containsKey(murCanvas)) {
            return mapCopieVersOriginal.get(murCanvas);
        }

        // 2. PRIORITÉ ABSOLUE : On cherche dans la liste complète de l'appartement
        // Grâce à la nouvelle méthode, cela inclut les cloisons intérieures !
        if (appartement != null) {
            for (Mur m : appartement.getMurs()) {
                if (sontMursIdentiques(murCanvas, m)) {
                    return m; // Retourne le mur d'origine qui possède ses deux côtés bien initialisés
                }
            }
        }

        // 3. SECOURS : Si l'appartement n'est pas défini, on cherche dans les pièces
        if (pieces != null) {
            for (Piece p : pieces) {
                for (Mur m : p.getMurs()) {
                    if (sontMursIdentiques(murCanvas, m)) {
                        return m;
                    }
                }
            }
        }

        return murCanvas;
    }

    /**
     * Vérifie si deux segments de mur partagent les mêmes extrémités géométriques,
     * peu importe le sens (A->B ou B->A).
     */
    private boolean sontMursIdentiques(Mur m1, Mur m2) {
        double tol = 1e-4; // Tolérance pour les calculs de flottants

        // Sens normal : P1==P1 et P2==P2
        boolean sensNormal =
                Math.abs(m1.getPoint1().getX() - m2.getPoint1().getX()) < tol &&
                        Math.abs(m1.getPoint1().getY() - m2.getPoint1().getY()) < tol &&
                        Math.abs(m1.getPoint2().getX() - m2.getPoint2().getX()) < tol &&
                        Math.abs(m1.getPoint2().getY() - m2.getPoint2().getY()) < tol;

        // Sens inverse : P1==P2 et P2==P1
        boolean sensInverse =
                Math.abs(m1.getPoint1().getX() - m2.getPoint2().getX()) < tol &&
                        Math.abs(m1.getPoint1().getY() - m2.getPoint2().getY()) < tol &&
                        Math.abs(m1.getPoint2().getX() - m2.getPoint1().getX()) < tol &&
                        Math.abs(m1.getPoint2().getY() - m2.getPoint1().getY()) < tol;

        return sensNormal || sensInverse;
    }

    private void basculerSelection(SurfaceAvecRevetement surface) {
        if (surfacesSelectionnees.contains(surface)) {
            surfacesSelectionnees.remove(surface);
        } else {
            surfacesSelectionnees.add(surface);
        }
        vue.getCanvas().setSelection(surfacesSelectionnees);
        vue.redrawAll();
    }

    public List<SurfaceAvecRevetement> getSurfacesSelectionnees() {
        return surfacesSelectionnees;
    }

    public void viderSelection() {
        surfacesSelectionnees.clear();
        vue.getCanvas().setSelection(surfacesSelectionnees);
        vue.redrawAll();
    }

    // =========================================================================
    // GESTION DES MURS
    // =========================================================================

    private void gererClicMur(Point pClic) {
        if (vue.getOptionsMurVue().estRectangulaire()) {
            gererClicMurRectangulaire(pClic);
        } else {
            gererClicMurLibre(pClic);
        }
    }

    private void gererClicMurLibre(Point pClic) {
        if (mur1EnCours == null) {
            mur1EnCours = new Mur(pClic, pClic);
            mur1EnCours.setTypeMur(Mur.TypeMur.NORMAL);
            vue.getCanvas().ajouterElement(mur1EnCours);
            vue.setInstructions("Début du mur posé — Cliquez pour définir la fin");
        } else {
            mur1EnCours.setPoint2(pClic);
            mur1EnCours = null;
            vue.setInstructions("Mur créé — Cliquez pour un nouveau mur ou changez d'outil");
        }
    }

    private void gererClicMurRectangulaire(Point pClic) {
        switch (etapeRectangle) {
            case 0 -> {
                p1Rect   = pClic;
                mur1Rect = new Mur(p1Rect, p1Rect);
                mur1Rect.setTypeMur(Mur.TypeMur.NORMAL);
                vue.getCanvas().ajouterElement(mur1Rect);
                vue.setInstructions("Premier coin posé — Cliquez pour définir la longueur");
                etapeRectangle = 1;
            }
            case 1 -> {
                p2Rect   = pClic;
                mur1Rect.setPoint2(p2Rect);
                mur2Rect = new Mur(p2Rect, p2Rect);
                mur2Rect.setTypeMur(Mur.TypeMur.NORMAL);
                vue.getCanvas().ajouterElement(mur2Rect);
                vue.setInstructions("Longueur définie — Cliquez pour définir la largeur");
                etapeRectangle = 2;
            }
            case 2 -> {
                Point p3 = calculerPointOrthogonal(p2Rect, pClic);
                mur2Rect.setPoint2(p3);
                Point p4 = new Point(
                        p1Rect.getX() + (p3.getX() - p2Rect.getX()),
                        p1Rect.getY() + (p3.getY() - p2Rect.getY()));
                Mur mur3 = new Mur(p3, p4); mur3.setTypeMur(Mur.TypeMur.NORMAL);
                Mur mur4 = new Mur(p4, p1Rect); mur4.setTypeMur(Mur.TypeMur.NORMAL);
                vue.getCanvas().ajouterElement(mur3);
                vue.getCanvas().ajouterElement(mur4);
                vue.setInstructions("Rectangle créé — Cliquez pour un nouveau mur ou changez d'outil");
                reinitialiserRectangle();
            }
        }
    }

    private void gererMouvementMur(Point pSouris) {
        if (polygoneAppartement != null
                && !estDansAppartement(pSouris.getX(), pSouris.getY())) return;

        if (vue.getOptionsMurVue().estRectangulaire()) {
            if (etapeRectangle == 1 && mur1Rect != null) {
                mur1Rect.setPoint2(pSouris);
            } else if (etapeRectangle == 2 && mur2Rect != null) {
                mur2Rect.setPoint2(calculerPointOrthogonal(p2Rect, pSouris));
            }
        } else {
            if (mur1EnCours != null) mur1EnCours.setPoint2(pSouris);
        }
        vue.redrawAll();
    }

    public void annulerConstruction() {
        if (mur1EnCours != null) {
            vue.getCanvas().getElements().remove(mur1EnCours);
            mur1EnCours = null;
        }
        if (mur1Rect != null) vue.getCanvas().getElements().remove(mur1Rect);
        if (mur2Rect != null) vue.getCanvas().getElements().remove(mur2Rect);
        reinitialiserRectangle();
        vue.redrawAll();
    }

    private void reinitialiserRectangle() {
        mur1Rect       = null;
        mur2Rect       = null;
        p1Rect         = null;
        p2Rect         = null;
        etapeRectangle = 0;
    }

    // =========================================================================
    // GESTION DES PIÈCES
    // =========================================================================

    private void gererClicPiece(Point pClic) {
        double px = pClic.getX(), py = pClic.getY();

        for (Piece piece : pieces) {
            if (GeometrieUtils.pointDansPolygone(px, py, piece.getPoints())) {
                vue.setInstructions("Une pièce existe déjà dans cette zone.");
                return;
            }
        }

        if (appartement == null) {
            vue.setInstructions("Aucun appartement associé — impossible de créer la pièce.");
            return;
        }

        List<SegmentSource> sources = collecterSegmentsSources();
        List<SegmentSource> cycle   = GeometrieUtils.trouverCycleMinimal(px, py, sources);

        if (cycle == null || cycle.size() < 3) {
            vue.setInstructions(
                    "Aucune zone fermée ici — vérifiez que les murs se rejoignent bien.");
            return;
        }

        List<Mur> mursPiece = new ArrayList<>();
        for (SegmentSource ss : cycle) mursPiece.add(ss.mur);
        mursPiece = GeometrieUtils.ordonnerMurs(mursPiece);

        Piece piece = appartement.ajouterPiece(mursPiece);
        pieces.add(piece);

        if (onPieceCree != null) {
            TreeItem<String> itemPiece = onPieceCree.apply(piece);
            if (itemPiece != null) mapItemPiece.put(itemPiece, piece);
        }

        // --- NOUVEAU : Absorber les ouvertures existantes sur le canevas ---
        for (Dessin d : vue.getCanvas().getElements()) {
            if (d instanceof Mur murCanvas && !murCanvas.getListeOuvertures().isEmpty()) {
                for (Mur murPiece : piece.getMurs()) {
                    if (sontMursSuperposes(murCanvas, murPiece)) {

                        murPiece.setTypeMur(murCanvas.getTypeMur()); // On copie le type

                        for (Ouverture ouv : murCanvas.getListeOuvertures()) {
                            // On retrouve les vraies coordonnées XY de la porte/fenêtre
                            Point posAbsolue = murCanvas.getPointSurMur(ouv.getPositionSurMur());
                            double tPiece = murPiece.calculerPositionSurMur(posAbsolue);

                            if (tPiece >= -0.05 && tPiece <= 1.05) {
                                double margePiece = ouv.getLargeur() / (2 * murPiece.calculerLongueur());
                                tPiece = Math.max(margePiece, Math.min(1.0 - margePiece, tPiece));
                                murPiece.ajouterOuverture(ouv instanceof Porte ? new Porte(tPiece) : new Fenetre(tPiece));
                            }
                        }
                    }
                }
            }
        }
        // -------------------------------------------------------------------

        vue.getCanvas().ajouterElement(creerDessinPiece(piece));
        vue.setInstructions("Pièce créée — cliquez dans une autre zone pour en ajouter une.");
        vue.redrawAll();
    }

    private Dessin creerDessinPiece(Piece piece) {
        return new Dessin() {
            @Override
            public void dessiner(GraphicsContext gc) {
                List<Point> pts = piece.getPoints();
                if (pts == null || pts.size() < 3) return;

                double[] xs = pts.stream().mapToDouble(Point::getX).toArray();
                double[] ys = pts.stream().mapToDouble(Point::getY).toArray();

                gc.setFill(PaletteVisuelle.fondPiece(piece.getNumero()));
                gc.fillPolygon(xs, ys, pts.size());

                double cx = pts.stream().mapToDouble(Point::getX).average().orElse(0);
                double cy = pts.stream().mapToDouble(Point::getY).average().orElse(0);
                gc.save();
                gc.scale(1, -1);
                gc.setFill(PaletteVisuelle.contourPiece(piece.getNumero()));
                gc.setFont(javafx.scene.text.Font.font(
                        "Arial", javafx.scene.text.FontWeight.BOLD, 0.25));
                gc.fillText(piece.toString(), cx - 0.3, -cy + 0.1);
                gc.restore();
            }

            @Override public Color getColor()        { return Color.ORANGE; }
            @Override public void  setColor(Color c) { }
        };
    }

    // =========================================================================
    // GESTION DES OUVERTURES (portes et fenêtres)
    // =========================================================================

    private void gererClicOuverture(Point pClic, boolean estPorte) {
        Mur cible = trouverMurProche(pClic);
        if (cible == null) return;

        double largeur = estPorte ? Porte.LARGEUR_PORTE : Fenetre.COTE_FENETRE;
        double marge   = largeur / (2 * cible.calculerLongueur());
        double t       = Math.max(marge, Math.min(1.0 - marge, cible.calculerPositionSurMur(pClic)));

        // 1. Ajout visuel sur le canvas
        cible.ajouterOuverture(estPorte ? new Porte(t) : new Fenetre(t));

        // 2. Si on est dans la vue d'une pièce, on sécurise l'ajout dans le vrai mur en mémoire
        Mur vraiMur = retrouverVraiMurModele(cible);
        if (vraiMur != null && vraiMur != cible) {
            vraiMur.ajouterOuverture(estPorte ? new Porte(t) : new Fenetre(t));
        }

        // 3. Si on est dans la vue Appartement, on propage la découpe aux sous-murs des pièces
        for (Piece piece : pieces) {
            for (Mur murPiece : piece.getMurs()) {
                if (sontMursSuperposes(cible, murPiece)) {
                    murPiece.setTypeMur(cible.getTypeMur());
                    double tPiece = murPiece.calculerPositionSurMur(pClic);
                    if (tPiece >= -0.05 && tPiece <= 1.05) {
                        double margePiece = largeur / (2 * murPiece.calculerLongueur());
                        tPiece = Math.max(margePiece, Math.min(1.0 - margePiece, tPiece));
                        murPiece.ajouterOuverture(estPorte ? new Porte(tPiece) : new Fenetre(tPiece));
                    }
                }
            }
        }
    }

    private void gererMouvementOuverture(Point pSouris) {
        murSurvole = trouverMurProche(pSouris);
        boolean estPorte = (etat == ETAT_PORTE);
        Fantome fantome  = estPorte ? new Porte() : new Fenetre();

        if (murSurvole != null) {
            boolean compatible = estPorte
                    ? murSurvole.getTypeMur() != Mur.TypeMur.EXTERIEUR
                    : murSurvole.getTypeMur() == Mur.TypeMur.EXTERIEUR;
            double angle = Math.toDegrees(Math.atan2(
                    murSurvole.getPoint2().getY() - murSurvole.getPoint1().getY(),
                    murSurvole.getPoint2().getX() - murSurvole.getPoint1().getX()));
            Point posAccrochage = projeterSurMur(pSouris, murSurvole, estPorte);
            vue.getCanvas().setFantome(fantome, posAccrochage.getX(),
                    posAccrochage.getY(), angle, compatible);
        } else {
            vue.getCanvas().setFantome(fantome, pSouris.getX(), pSouris.getY(), 0, false);
        }
        vue.redrawAll();
    }

    // =========================================================================
    // INITIALISATION AVEC LE CONTOUR DE L'APPARTEMENT
    // =========================================================================

    public void initialiserAvecContourAppartement(List<Point> polygone,
                                                  List<Mur> mursDelimiteurs,
                                                  AireImmeuble aire,
                                                  Appartement appartement) {
        this.appartement = appartement;
        if (polygone == null || polygone.size() < 3) return;

        // On conserve la logique de non-translation directe ou de polygone de origin/master
        this.polygoneAppartement = new ArrayList<>(polygone);

        List<Mur> mursAffichage = new ArrayList<>();
        List<double[]> cotesBatiment = extraireCotesBatiment(aire);
        this.cotesBatimentAffichage = cotesBatiment;

        for (Mur murOriginal : mursDelimiteurs) {
            Mur copie = new Mur(
                    new Point(murOriginal.getPoint1().getX(), murOriginal.getPoint1().getY()),
                    new Point(murOriginal.getPoint2().getX(), murOriginal.getPoint2().getY())
            );

            // Définir le type AVANT d'ajouter les ouvertures (pour passer la sécurité de Mur.java)
            appliquerTypeMurAffichage(copie, murOriginal, cotesBatiment);

            // --- FIX 1 : COPIER LES OUVERTURES SUR LE CLONE ---
            for (Ouverture ouv : murOriginal.getListeOuvertures()) {
                if (ouv instanceof Porte) {
                    copie.ajouterOuverture(new Porte(ouv.getPositionSurMur()));
                } else if (ouv instanceof Fenetre) {
                    copie.ajouterOuverture(new Fenetre(ouv.getPositionSurMur()));
                }
            }

            // --- FIX 2 : COPIER LES REVÊTEMENTS SUR LE CLONE (biface de master) ---
            if (murOriginal.getCoteGauche().getRevetements() != null) {
                for (Revetement r : murOriginal.getCoteGauche().getRevetements()) {
                    copie.getCoteGauche().ajouterRevetement(r);
                }
            }
            if (murOriginal.getCoteDroit().getRevetements() != null) {
                for (Revetement r : murOriginal.getCoteDroit().getRevetements()) {
                    copie.getCoteDroit().ajouterRevetement(r);
                }
            }

            // --- FIX 3 : GARDER LE LIEN VERS L'ORIGINAL ---
            mapCopieVersOriginal.put(copie, murOriginal);
            mursAffichage.add(copie);
        }

        vue.getCanvas().getElements().add(0, creerFondAppartement(this.polygoneAppartement));
        for (Mur copie : mursAffichage) {
            vue.getCanvas().getElements().add(copie);
        }

        vue.getCanvas().redrawAll();
    }

    private Dessin creerFondAppartement(List<Point> polygone) {
        return new Dessin() {
            @Override
            public void dessiner(GraphicsContext gc) {
                double[] xs = polygone.stream().mapToDouble(Point::getX).toArray();
                double[] ys = polygone.stream().mapToDouble(Point::getY).toArray();
                gc.setFill(Color.web("#4a90d9", 0.05));
                gc.fillPolygon(xs, ys, polygone.size());
            }
            @Override public Color getColor()        { return Color.TRANSPARENT; }
            @Override public void  setColor(Color c) { }
        };
    }

    private List<double[]> extraireCotesBatiment(AireImmeuble aire) {
        List<double[]> cotes = new ArrayList<>();
        if (aire == null || !aire.isComplete()) return cotes;

        Point q1 = aire.getP1(), q2 = aire.getP2();
        Point q3 = aire.getP3(), q4 = aire.getP4();
        cotes.add(new double[]{q1.getX(), q1.getY(), q2.getX(), q2.getY()});
        cotes.add(new double[]{q2.getX(), q2.getY(), q3.getX(), q3.getY()});
        cotes.add(new double[]{q3.getX(), q3.getY(), q4.getX(), q4.getY()});
        cotes.add(new double[]{q4.getX(), q4.getY(), q1.getX(), q1.getY()});
        return cotes;
    }

    public void setAfficherAdjacenceCouloir(boolean afficherAdjacenceCouloir) {
        this.afficherAdjacenceCouloir = afficherAdjacenceCouloir;
    }

    public void rechargerPieces(List<Piece> piecesChargees) {
        for (Piece piece : piecesChargees) {
            pieces.add(piece);

            // Ajouter les murs de la pièce au canvas
            for (Mur mur : piece.getMurs()) {
                if (!vue.getCanvas().getElements().contains(mur)) {
                    vue.getCanvas().getElements().add(mur);
                }
            }

            // Ajouter le dessin coloré de la pièce
            vue.getCanvas().getElements().add(creerDessinPiece(piece));
        }
        vue.getCanvas().redrawAll();
    }

    // =========================================================================
    // COLLECTE DES SEGMENTS (délègue la subdivision à GeometrieUtils)
    // =========================================================================

    private List<SegmentSource> collecterSegmentsSources() {
        List<SegmentSource> bruts = new ArrayList<>();

        if (polygoneAppartement != null && polygoneAppartement.size() >= 3) {
            int n = polygoneAppartement.size();
            for (int i = 0; i < n; i++) {
                Point a = polygoneAppartement.get(i);
                Point b = polygoneAppartement.get((i + 1) % n);
                bruts.add(new SegmentSource(a, b, new Mur(a, b)));
            }
        }

        for (Dessin d : vue.getCanvas().getElements()) {
            if (d instanceof Mur m) {
                bruts.add(new SegmentSource(m.getPoint1(), m.getPoint2(), m));
            }
        }

        return GeometrieUtils.subdiviserEtDeduplicer(bruts);
    }

    // =========================================================================
    // UTILITAIRES GÉOMÉTRIQUES LOCAUX
    // =========================================================================

    public Point posInModel(double x, double y) {
        Point2D p = vue.getCanvas().snapToGrid(x, y);
        return new Point(p.getX(), p.getY());
    }

    private boolean estDansAppartement(double px, double py) {
        if (polygoneAppartement == null) return true;
        return GeometrieUtils.estDansZone(px, py, polygoneAppartement);
    }

    public Point calculerPointOrthogonal(Point centre, Point cible) {
        double dx    = p2Rect.getX() - p1Rect.getX();
        double dy    = p2Rect.getY() - p1Rect.getY();
        double perpX = -dy, perpY = dx;
        double ux    = cible.getX() - centre.getX();
        double uy    = cible.getY() - centre.getY();
        double s     = (ux * perpX + uy * perpY) / (perpX * perpX + perpY * perpY);
        return new Point(centre.getX() + s * perpX, centre.getY() + s * perpY);
    }

    private Point projeterSurMur(Point p, Mur mur, boolean estPorte) {
        double x1 = mur.getPoint1().getX(), y1 = mur.getPoint1().getY();
        double x2 = mur.getPoint2().getX(), y2 = mur.getPoint2().getY();
        double dx = x2 - x1, dy = y2 - y1;
        double l2 = dx * dx + dy * dy;
        if (l2 == 0) return mur.getPoint1();

        double largeur = estPorte ? Porte.LARGEUR_PORTE : Fenetre.COTE_FENETRE;
        double marge   = largeur / (2 * mur.calculerLongueur());
        double t       = ((p.getX() - x1) * dx + (p.getY() - y1) * dy) / l2;
        t = Math.max(marge, Math.min(1.0 - marge, t));
        return new Point(x1 + t * dx, y1 + t * dy);
    }

    private Mur trouverMurProche(Point p) {
        Mur    plusProche = null;
        double distMin    = 0.3;
        for (Dessin d : vue.getCanvas().getElements()) {
            if (d instanceof Mur m) {
                double dist = m.distanceA(p);
                if (dist < distMin) { distMin = dist; plusProche = m; }
            }
        }
        return plusProche;
    }

    private Mur trouverMurProcheAjuste(Point p, double sensibiliteMax) {
        Mur plusProche = null;
        double distMin = sensibiliteMax;
        for (Dessin d : vue.getCanvas().getElements()) {
            if (d instanceof Mur m) {
                double dist = m.distanceA(p);
                if (dist < distMin) {
                    distMin = dist;
                    plusProche = m;
                }
            }
        }
        return plusProche;
    }

    private boolean murInclsDansCote(Mur mur, double[] cote) {
        double ax = cote[0], ay = cote[1], bx = cote[2], by = cote[3];
        double dx = bx - ax, dy = by - ay;
        double len2 = dx * dx + dy * dy;
        if (len2 < GeometrieUtils.TOL) return false;

        double[] pts = {
                mur.getPoint1().getX(), mur.getPoint1().getY(),
                mur.getPoint2().getX(), mur.getPoint2().getY()
        };
        for (int i = 0; i < 4; i += 2) {
            double px    = pts[i], py = pts[i + 1];
            double cross = (px - ax) * dy - (py - ay) * dx;
            if (Math.abs(cross) / Math.sqrt(len2) > GeometrieUtils.TOL) return false;
            double t = ((px - ax) * dx + (py - ay) * dy) / len2;
            if (t < -GeometrieUtils.TOL || t > 1 + GeometrieUtils.TOL) return false;
        }
        return true;
    }

    // =========================================================================
    // GETTERS
    // =========================================================================

    public Map<TreeItem<String>, Piece> getMapItemPiece() { return mapItemPiece; }

    public void rafraichirNavigateur() { }

    // =========================================================================
    // SYNCHRONISATION DES OUVERTURES (Murs Graphiques <-> Murs Métier)
    // =========================================================================

    private boolean sontMursSuperposes(Mur murParent, Mur sousMur) {
        return estPointSurSegment(sousMur.getPoint1(), murParent) &&
                estPointSurSegment(sousMur.getPoint2(), murParent);
    }

    private boolean estPointSurSegment(Point p, Mur m) {
        double x1 = m.getPoint1().getX(), y1 = m.getPoint1().getY();
        double x2 = m.getPoint2().getX(), y2 = m.getPoint2().getY();
        double px = p.getX(), py = p.getY();

        double dx = x2 - x1;
        double dy = y2 - y1;
        double len2 = dx * dx + dy * dy;
        if (len2 < 1e-6) return false;

        // VRAIE DISTANCE : on divise par la longueur du mur pour la tolérance (5 cm)
        double cross = (px - x1) * dy - (py - y1) * dx;
        double dist = Math.abs(cross) / Math.sqrt(len2);
        if (dist > 0.05) return false;

        // On vérifie que le point est bien coincé entre les extrémités
        double dot = (px - x1) * dx + (py - y1) * dy;
        if (dot < -0.05 || dot > len2 + 0.05) return false;

        return true;
    }

    /** Remonte les portes/fenêtres des pièces vers la grande vue Appartement */
    public void synchroniserOuverturesVersAppartement() {
        if (appartement == null) return;

        for (Dessin d : vue.getCanvas().getElements()) {
            if (d instanceof Mur murCanvas) {
                murCanvas.getListeOuvertures().clear(); // Nettoyage pour éviter les doublons

                for (Piece piece : appartement.getPieces()) {
                    for (Mur murPiece : piece.getMurs()) {
                        if (sontMursSuperposes(murCanvas, murPiece)) {
                            for (Ouverture ouv : murPiece.getListeOuvertures()) {
                                Point posAbsolue = murPiece.getPointSurMur(ouv.getPositionSurMur());
                                double tCanvas = murCanvas.calculerPositionSurMur(posAbsolue);

                                if (tCanvas >= -0.05 && tCanvas <= 1.05) {
                                    double marge = ouv.getLargeur() / (2 * murCanvas.calculerLongueur());
                                    tCanvas = Math.max(marge, Math.min(1.0 - marge, tCanvas));

                                    // Anti-doublons (si le mur est partagé par deux pièces)
                                    boolean existe = false;
                                    for(Ouverture oExist : murCanvas.getListeOuvertures()) {
                                        if (Math.abs(oExist.getPositionSurMur() - tCanvas) < 0.01) {
                                            existe = true; break;
                                        }
                                    }

                                    if (!existe) {
                                        murCanvas.ajouterOuverture(ouv instanceof Porte ? new Porte(tCanvas) : new Fenetre(tCanvas));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        vue.redrawAll();
    }

    /** Descend les portes/fenêtres de l'Appartement vers les clones de la vue Pièce */
    public void synchroniserOuverturesVersPiece() {
        if (mapCopieVersOriginal.isEmpty()) return;

        for (Map.Entry<Mur, Mur> entry : mapCopieVersOriginal.entrySet()) {
            Mur clone = entry.getKey();
            Mur original = entry.getValue();

            clone.getListeOuvertures().clear();
            for (Ouverture ouv : original.getListeOuvertures()) {
                clone.ajouterOuverture(ouv instanceof Porte ? new Porte(ouv.getPositionSurMur()) : new Fenetre(ouv.getPositionSurMur()));
            }
        }
        vue.redrawAll();
    }
    
    public void setNiveauControleur(NiveauControleur ctrl) {
        this.niveauControleur = ctrl;
        rafraichirTypesMursAffichage();
    }

    private void rafraichirTypesMursAffichage() {
        if (mapCopieVersOriginal.isEmpty()) return;

        for (Map.Entry<Mur, Mur> entry : mapCopieVersOriginal.entrySet()) {
            appliquerTypeMurAffichage(entry.getKey(), entry.getValue(), cotesBatimentAffichage);
        }
        vue.redrawAll();
    }

    private void appliquerTypeMurAffichage(Mur copie, Mur murOriginal, List<double[]> cotesBatiment) {
        boolean exterieur = cotesBatiment.stream().anyMatch(c -> murInclsDansCote(murOriginal, c))
                || murOriginal.getTypeMur() == Mur.TypeMur.EXTERIEUR;

        if (exterieur) {
            copie.setTypeMur(Mur.TypeMur.EXTERIEUR);
        } else if (afficherAdjacenceCouloir && estMurAdjacentCouloir(murOriginal)) {
            copie.setTypeMur(Mur.TypeMur.ADJ_COULOIR);
        } else {
            copie.setTypeMur(Mur.TypeMur.NORMAL);
        }
    }

    private boolean estMurAdjacentCouloir(Mur mur) {
        return mur.getTypeMur() == Mur.TypeMur.ADJ_COULOIR
                || (niveauControleur != null && niveauControleur.estAdjacentsAuCouloir(mur));
    }
}
