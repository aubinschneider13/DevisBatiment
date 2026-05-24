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
    public static final int ETAT_EDITION   = 80;

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
    private Niveau niveauSauvegarde = null;
    private Batiment batimentSauvegarde = null;
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

    // État de l'inversion et suivi de la porte fantôme
    private Porte porteEnCours = null;
    private Point dernierPointSouris = new Point(0, 0);

    // ✅ Ajouté : Variables de votre mode sélection pour les revêtements
    private final List<SurfaceAvecRevetement> surfacesSelectionnees = new ArrayList<>();

    private Runnable onModification = null;

    public void setOnModification(Runnable onModification) {
        this.onModification = onModification;
    }

    public void setContexteSauvegarde(Appartement appartement, Niveau niveau, Batiment batiment) {
        if (appartement != null) {
            this.appartement = appartement;
            this.pieces.clear();
            this.pieces.addAll(appartement.getPieces());
        }
        this.niveauSauvegarde = niveau;
        this.batimentSauvegarde = batiment;
    }

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
            porteEnCours = null;
            vue.getCanvas().setFantome(null, 0, 0, 0, false);
        }

        // Reset selection highlight when changing states
        vue.getCanvas().setElementSelectionne(null);

        this.etat = nouvelEtat;
        vue.getCanvas().setPanActif(nouvelEtat == ETAT_RIEN);
        vue.getOptionsMurVue().setVisible(nouvelEtat == ETAT_MUR);

        if (nouvelEtat == ETAT_PORTE || nouvelEtat == ETAT_FENETRE || nouvelEtat == ETAT_MUR || nouvelEtat == ETAT_EDITION) {
            javafx.application.Platform.runLater(() -> vue.getCanvas().requestFocus());
        }

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
                    "Mode Matériaux — Cliquez sur un mur ou un sol pour le sélectionner");
            case ETAT_EDITION   -> vue.setInstructions(
                    "Outil Sélection / Édition — Clic gauche pour sélectionner, Clic droit sur porte pour inverser, Suppr pour supprimer");
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
        Point pClic = (etat == ETAT_SELECTION || etat == ETAT_EDITION)
                ? posInModelUnsnapped(event.getX(), event.getY())
                : posInModel(event.getX(), event.getY());

        if (!estDansAppartement(pClic.getX(), pClic.getY())) return;

        switch (etat) {
            case ETAT_MUR       -> { if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY) gererClicMur(pClic); }
            case ETAT_PIECE     -> { if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY) gererClicPiece(pClic); }
            case ETAT_PORTE     -> { if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY) gererClicOuverture(pClic, true); }
            case ETAT_FENETRE   -> { if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY) gererClicOuverture(pClic, false); }
            case ETAT_SELECTION -> gererClicSelection(pClic, event);
            case ETAT_EDITION   -> gererClicEdition(pClic, event);
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
        if (event.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
            if (inverserPorteProche(pClic, 0.35)) {
                vue.redrawAll();
                vue.setInstructions("Orientation de la porte inversée avec succès !");
            }
            return;
        }

        // Clic Gauche : Sélection du mur
        double distMinMur = 0.15;

        for (Dessin d : vue.getCanvas().getElements()) {
            if (d instanceof Mur m) {
                if (m.distanceA(pClic) < distMinMur) {

                    double x1 = m.getPoint1().getX(), y1 = m.getPoint1().getY();
                    double x2 = m.getPoint2().getX(), y2 = m.getPoint2().getY();
                    double px = pClic.getX(), py = pClic.getY();

                    // Calcul du produit vectoriel par rapport au sens du mur affiché (m)
                    double cross = (x2 - x1) * (py - y1) - (y2 - y1) * (px - x1);

                    // Sélection directe sur le mur d'AFFICHAGE (pas l'original).
                    // Cela garantit que les coordonnées de surbrillance restent
                    // dans les limites du mur tel qu'il est dessiné sur le canevas.
                    CoteMur coteSel = (cross > 0) ? m.getCoteGauche() : m.getCoteDroit();

                    if (event.isShiftDown()) {
                        basculerSelection(m.getCoteGauche());
                        basculerSelection(m.getCoteDroit());
                        vue.setInstructions("Sélection biface : Côté Gauche ET Droit synchronisés.");
                    } else {
                        basculerSelection(coteSel);
                        vue.setInstructions("Face sélectionnée : " + coteSel);
                    }
                    return;
                }
            }
        }

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

    /**
     * Vérifie si deux segments de mur partagent les mêmes extrémités géométriques,
     * peu importe le sens (A->B ou B->A).
     */
    private boolean sontMursIdentiques(Mur m1, Mur m2) {
        return GeometrieUtils.mursIdentiques(m1, m2);
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
        porteEnCours = null;
        vue.getCanvas().setFantome(null, 0, 0, 0, false);
        vue.redrawAll();
    }

    public void gererToucheClavier(javafx.scene.input.KeyEvent e) {
        if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
            annulerConstruction();
        } else if (e.getCode() == javafx.scene.input.KeyCode.R) {
            if (etat == ETAT_PORTE && porteEnCours != null) {
                // Inverser le sens d'ouverture de la porte
                porteEnCours.setOuvertureInversee(!porteEnCours.isOuvertureInversee());

                // Mettre à jour immédiatement l'affichage du fantôme
                if (murSurvole != null) {
                    boolean compatible = murSurvole.getTypeMur() != Mur.TypeMur.EXTERIEUR;
                    double angle = Math.toDegrees(Math.atan2(
                            murSurvole.getPoint2().getY() - murSurvole.getPoint1().getY(),
                            murSurvole.getPoint2().getX() - murSurvole.getPoint1().getX()));
                    Point posAccrochage = projeterSurMur(dernierPointSouris, murSurvole, true);
                    vue.getCanvas().setFantome(porteEnCours, posAccrochage.getX(), posAccrochage.getY(), angle, compatible);
                } else {
                    vue.getCanvas().setFantome(porteEnCours, dernierPointSouris.getX(), dernierPointSouris.getY(), 0, false);
                }
                vue.redrawAll();
            }
        } else if (e.getCode() == javafx.scene.input.KeyCode.DELETE || e.getCode() == javafx.scene.input.KeyCode.BACK_SPACE) {
            if (this.etat == ETAT_EDITION) {
                Object sel = vue.getCanvas().getElementSelectionne();
                if (sel != null) {
                    supprimerElement(sel);
                }
            }
        }
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
        Piece piece = appartement.ajouterPiece(mursPiece);
        pieces.add(piece);

        if (onPieceCree != null) {
            TreeItem<String> itemPiece = onPieceCree.apply(piece);
            if (itemPiece != null) mapItemPiece.put(itemPiece, piece);
        }


        vue.getCanvas().ajouterElement(creerDessinPiece(piece));
        sauvegarderDetailsAppartement();
        vue.setInstructions("Pièce créée — cliquez dans une autre zone pour en ajouter une.");
        vue.redrawAll();
    }

    public static class DessinPiece implements Dessin {
        private final Piece piece;
        public DessinPiece(Piece piece) {
            this.piece = piece;
        }
        public Piece getPiece() { return piece; }
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
    }

    private Dessin creerDessinPiece(Piece piece) {
        return new DessinPiece(piece);
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

        boolean inversee = false;
        if (estPorte && porteEnCours != null) {
            inversee = porteEnCours.isOuvertureInversee();
        }

        Ouverture ouverture = estPorte ? new Porte(t) : new Fenetre(t);
        if (estPorte) ((Porte)ouverture).setOuvertureInversee(inversee);

        cible.ajouterOuverture(ouverture);

        // Propager aussi sur le mur de l'appartement géométriquement parent
        if (appartement != null) {
            for (Mur murAppart : appartement.getMurs()) {
                if (!murAppart.equals(cible) && GeometrieUtils.mursSuperposes(murAppart, cible)) {
                    Point posAbsolue = cible.getPointSurMur(t);
                    OuvertureUtils.ajouterCopieSiAbsente(murAppart, ouverture, cible);
                }
            }
        }

    sauvegarderDetailsAppartement();
    vue.redrawAll();
    if (onModification != null) {
        onModification.run();
    }
}      

    private void gererMouvementOuverture(Point pSouris) {
        this.dernierPointSouris = pSouris;
        murSurvole = trouverMurProche(pSouris);
        boolean estPorte = (etat == ETAT_PORTE);

        if (estPorte) {
            if (porteEnCours == null) {
                porteEnCours = new Porte();
            }
        }

        Fantome fantome  = estPorte ? porteEnCours : new Fenetre();

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

        List<double[]> cotesBatiment = extraireCotesBatiment(aire);
        this.cotesBatimentAffichage = cotesBatiment;

        for (Mur murOriginal : mursDelimiteurs) {
            murOriginal.setEstDelimiteur(true);
            appliquerTypeMurAffichage(murOriginal, murOriginal, cotesBatiment);
        }

        vue.getCanvas().getElements().add(0, creerFondAppartement(this.polygoneAppartement));
        vue.getCanvas().getElements().addAll(mursDelimiteurs);

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

    public void rechargerVueAppartementDepuisDisque() {
        if (appartement == null) return;
        gestionnaire.rechargerOuverturesAppartement(appartement, niveauSauvegarde, batimentSauvegarde);
        vue.getCanvas().getElements().clear();
        if (polygoneAppartement != null && polygoneAppartement.size() >= 3) {
            vue.getCanvas().getElements().add(creerFondAppartement(polygoneAppartement));
        }
        for (Mur mur : appartement.getMurs()) {
            mur.setTypeMur(Mur.TypeMur.NORMAL);
            boolean estDelimiteur = appartement.getMursDelimiteurs().contains(mur);
            mur.setEstDelimiteur(estDelimiteur);
            appliquerTypeMurAffichage(mur, mur, cotesBatimentAffichage);
            vue.getCanvas().getElements().add(mur);
        }
        for (Piece piece : appartement.getPieces()) {
            vue.getCanvas().getElements().add(creerDessinPiece(piece));
        }
        rafraichirTypesMursAffichage();
        vue.redrawAll();
    }

    public void rechargerVuePieceDepuisDisque(Piece piece) {
        if (piece == null) return;
        if (appartement != null)
            gestionnaire.rechargerOuverturesAppartement(appartement, niveauSauvegarde, batimentSauvegarde);

        List<Mur> sources = OuvertureUtils.collecterSourcesMurs(appartement, vue.getCanvas().getElements());
        this.polygoneAppartement = new ArrayList<>(piece.getPoints());
        vue.getCanvas().getElements().clear();
        vue.getCanvas().getElements().add(creerFondAppartement(this.polygoneAppartement));

        for (Mur murPiece : piece.construireMursAffichage()) {
            murPiece.setTypeMur(Mur.TypeMur.NORMAL);
            appliquerTypeMurAffichage(murPiece, murPiece.getOriginal(), cotesBatimentAffichage);
            OuvertureUtils.propagerOuverturesSurMur(murPiece, sources);
            vue.getCanvas().getElements().add(murPiece);
        }

        vue.getCanvas().getElements().add(creerDessinPiece(piece));
        rafraichirTypesMursAffichage();
        vue.redrawAll();
    }

    private void sauvegarderDetailsAppartement() {
        if (gestionnaire != null && appartement != null
                && niveauSauvegarde != null && batimentSauvegarde != null) {
            gestionnaire.sauvegarderDetailsAppartement(appartement, niveauSauvegarde, batimentSauvegarde);
        }
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
        rafraichirTypesMursAffichage();
    }

    public void rechargerPieces(List<Piece> piecesChargees) {
        for (Piece piece : piecesChargees) {
            pieces.add(piece);

            // Ajouter les murs de la pièce au canvas
            for (Mur mur : piece.getMurs()) {
                if (!contientMurIdentiqueCanvas(mur)) {
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

    public Point posInModelUnsnapped(double x, double y) {
        Point2D p = vue.getCanvas().getModelPosUnsnapped(x, y);
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
            double px = pts[i], py = pts[i + 1];
            double cross = (px - ax) * dy - (py - ay) * dx;
            // Vérifier colinéarité
            if (Math.abs(cross) / Math.sqrt(len2) > 0.05) return false;
            // Vérifier que le point est DANS le segment du bâtiment (pas juste colinéaire)
            double t = ((px - ax) * dx + (py - ay) * dy) / len2;
            if (t < -0.05 || t > 1 + 0.05) return false;
        }
        return true;
    }

    private boolean inverserPorteProche(Point pClic, double distanceMax) {
        Porte porteLaPlusProche = null;
        Mur murParentDeLaPorte = null;
        double distMinPorte = distanceMax;

        for (Dessin d : vue.getCanvas().getElements()) {
            if (d instanceof Mur m) {
                Mur ref = m;
                for (Ouverture o : ref.getListeOuvertures()) {
                    if (o instanceof Porte p) {
                        Point posPorte = ref.getPointSurMur(p.getPositionSurMur());
                        double dist = Math.hypot(posPorte.getX() - pClic.getX(), posPorte.getY() - pClic.getY());
                        if (dist < distMinPorte) {
                            distMinPorte = dist;
                            porteLaPlusProche = p;
                            murParentDeLaPorte = ref;
                        }
                    }
                }
            }
        }

        if (porteLaPlusProche == null) return false;
        boolean nouvelleInversion = !porteLaPlusProche.isOuvertureInversee();
        Point posAbsolue = murParentDeLaPorte.getPointSurMur(porteLaPlusProche.getPositionSurMur());
        appliquerOrientationPorte(vue.getCanvas().getElements(), posAbsolue, nouvelleInversion);
        if (appartement != null) appliquerOrientationPorte(appartement.getMurs(), posAbsolue, nouvelleInversion);
        for (Piece piece : pieces) appliquerOrientationPorte(piece.getMurs(), posAbsolue, nouvelleInversion);
        sauvegarderDetailsAppartement();
        return true;
    }

    private void appliquerOrientationPorte(Collection<?> elements, Point posAbsolue, boolean nouvelleInversion) {
        for (Object element : elements) {
            if (element instanceof Mur m) {
                for (Ouverture o : m.getListeOuvertures()) {
                    if (o instanceof Porte p) {
                        Point posP = m.getPointSurMur(p.getPositionSurMur());
                        if (Math.hypot(posP.getX() - posAbsolue.getX(), posP.getY() - posAbsolue.getY()) < 0.15) {
                            p.setOuvertureInversee(nouvelleInversion);
                        }
                    }
                }
            }
        }
    }
    // =========================================================================
    // GETTERS
    // =========================================================================

    public Map<TreeItem<String>, Piece> getMapItemPiece() { return mapItemPiece; }

    public void rafraichirNavigateur() { }

    // =========================================================================
    // UTILITAIRES DE MURS
    // =========================================================================

    private boolean sontMursSuperposes(Mur murParent, Mur sousMur) {
        return GeometrieUtils.mursSuperposes(murParent, sousMur);
    }

    /** Remonte les portes/fenêtres des pièces vers la grande vue Appartement */
    private boolean contientMurIdentiqueCanvas(Mur mur) {
        for (Dessin d : vue.getCanvas().getElements()) {
            if (d instanceof Mur existant && sontMursIdentiques(existant, mur)) {
                return true;
            }
        }
        return false;
    }

    public void gererClicEdition(Point pClic, MouseEvent event) {
        if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
            // Clic gauche : Sélection
            Object selection = null;

            // 1. Chercher d'abord une Ouverture (Porte ou Fenetre) - Sensibilité 0.40m
            double distMinOuverture = 0.40;
            Ouverture ouvertureElue = null;
            for (Dessin d : vue.getCanvas().getElements()) {
                if (d instanceof Mur m) {
                    for (Ouverture o : m.getListeOuvertures()) {
                        Point posAbs = m.getPointSurMur(o.getPositionSurMur());
                        double dist = Math.hypot(posAbs.getX() - pClic.getX(), posAbs.getY() - pClic.getY());
                        if (dist < distMinOuverture) {
                            distMinOuverture = dist;
                            ouvertureElue = o;
                        }
                    }
                }
            }
            if (ouvertureElue != null) {
                selection = ouvertureElue;
            }

            // 2. Si aucune ouverture n'est proche, chercher un Mur - Sensibilité 0.25m
            if (selection == null) {
                double distMinMur = 0.25;
                Mur murElu = null;
                for (Dessin d : vue.getCanvas().getElements()) {
                    if (d instanceof Mur m) {
                        double dist = m.distanceA(pClic);
                        if (dist < distMinMur) {
                            distMinMur = dist;
                            murElu = m;
                        }
                    }
                }
                if (murElu != null) {
                    selection = murElu;
                }
            }

            // 3. Si aucun mur n'est proche, chercher une Pièce (pointDansPolygone)
            if (selection == null) {
                for (Piece piece : pieces) {
                    if (GeometrieUtils.pointDansPolygone(pClic.getX(), pClic.getY(), piece.getPoints())) {
                        selection = piece;
                        break;
                    }
                }
            }

            // Assigner l'élément sélectionné
            vue.getCanvas().setElementSelectionne(selection);
            if (selection != null) {
                if (selection instanceof Ouverture) {
                    vue.setInstructions("Ouverture sélectionnée — Cliquer droit sur porte pour inverser, Suppr pour supprimer.");
                } else if (selection instanceof Mur) {
                    vue.setInstructions("Cloison sélectionnée — Suppr pour supprimer.");
                } else if (selection instanceof Piece) {
                    vue.setInstructions("Pièce sélectionnée — Suppr pour supprimer.");
                }
            } else {
                vue.setInstructions("Aucun élément proche. Clic gauche pour sélectionner, Clic droit sur porte pour inverser.");
            }
        } else if (event.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
            if (inverserPorteProche(pClic, 0.40)) {
                vue.redrawAll();
                vue.setInstructions("Orientation de la porte inversée avec succès !");
                if (onModification != null) {
                    onModification.run();
                }
            }
        }
    }
    public void supprimerElement(Object sel) {
        if (sel == null) return;

        if (sel instanceof Ouverture) {
            Ouverture ouv = (Ouverture) sel;
            Point posAbs = null;
            // 1. Trouver le mur parent et la position absolue
            for (Dessin d : vue.getCanvas().getElements()) {
                if (d instanceof Mur m) {
                    if (m.getListeOuvertures().contains(ouv)) {
                        posAbs = m.getPointSurMur(ouv.getPositionSurMur());
                        break;
                    }
                }
            }

            if (posAbs != null) {
                final Point finalPosAbs = posAbs;
                // A. Supprimer du canevas graphique
                for (Dessin d : vue.getCanvas().getElements()) {
                    if (d instanceof Mur m) {
                        m.getListeOuvertures().removeIf(o -> {
                            Point p = m.getPointSurMur(o.getPositionSurMur());
                            return Math.hypot(p.getX() - finalPosAbs.getX(), p.getY() - finalPosAbs.getY()) < 0.15;
                        });
                    }
                }

                // B. Supprimer des murs de l'appartement en mémoire
                if (appartement != null) {
                    for (Mur m : appartement.getMurs()) {
                        m.getListeOuvertures().removeIf(o -> {
                            Point p = m.getPointSurMur(o.getPositionSurMur());
                            return Math.hypot(p.getX() - finalPosAbs.getX(), p.getY() - finalPosAbs.getY()) < 0.15;
                        });
                    }
                }

                // C. Supprimer des murs de toutes les pièces
                for (Piece piece : pieces) {
                    for (Mur m : piece.getMurs()) {
                        m.getListeOuvertures().removeIf(o -> {
                            Point p = m.getPointSurMur(o.getPositionSurMur());
                            return Math.hypot(p.getX() - finalPosAbs.getX(), p.getY() - finalPosAbs.getY()) < 0.15;
                        });
                    }
                }

            }
        }
        else if (sel instanceof Mur) {
            Mur deletedWall = (Mur) sel;

            // 1. Supprimer toutes les pièces dépendantes de ce mur
            List<Piece> piecesToDelete = new ArrayList<>();
            for (Piece piece : pieces) {
                if (piece.getMurs().stream().anyMatch(m ->
                        sontMursIdentiques(m, deletedWall)
                                || sontMursSuperposes(deletedWall, m)
                                || sontMursSuperposes(m, deletedWall))) {
                    piecesToDelete.add(piece);
                }
            }

            for (Piece p : piecesToDelete) {
                // A. Retirer du TreeView et de la mapItemPiece
                TreeItem<String> itemPiece = null;
                for (Map.Entry<TreeItem<String>, Piece> entry : mapItemPiece.entrySet()) {
                    if (entry.getValue() == p) {
                        itemPiece = entry.getKey();
                        break;
                    }
                }
                if (itemPiece != null) {
                    TreeItem<String> parent = itemPiece.getParent();
                    if (parent != null) {
                        parent.getChildren().remove(itemPiece);
                    }
                    mapItemPiece.remove(itemPiece);
                }

                // B. Retirer des listes locales et du modèle
                pieces.remove(p);
                if (appartement != null) {
                    appartement.supprimerPiece(p);
                }

                // C. Retirer le dessin de la pièce sur le canevas
                vue.getCanvas().getElements().removeIf(d -> d instanceof DessinPiece dp && dp.getPiece() == p);
            }

            // 2. Retirer le mur lui-même (et tous ses clones correspondants)
            vue.getCanvas().getElements().removeIf(d -> d instanceof Mur m && sontMursIdentiques(m, deletedWall));

            if (appartement != null) {
                appartement.getMursDelimiteurs().removeIf(m -> sontMursIdentiques(m, deletedWall));
            }

        }
        else if (sel instanceof Piece) {
            Piece p = (Piece) sel;

            // A. Retirer du TreeView et de la mapItemPiece
            TreeItem<String> itemPiece = null;
            for (Map.Entry<TreeItem<String>, Piece> entry : mapItemPiece.entrySet()) {
                if (entry.getValue() == p) {
                    itemPiece = entry.getKey();
                    break;
                }
            }
            if (itemPiece != null) {
                TreeItem<String> parent = itemPiece.getParent();
                if (parent != null) {
                    parent.getChildren().remove(itemPiece);
                }
                mapItemPiece.remove(itemPiece);
            }

            // B. Retirer des listes locales et du modèle
            pieces.remove(p);
            if (appartement != null) {
                appartement.supprimerPiece(p);
            }

            // C. Retirer le dessin de la pièce sur le canevas
            vue.getCanvas().getElements().removeIf(d -> d instanceof DessinPiece dp && dp.getPiece() == p);
        }

        // Nettoyer la sélection et redessiner
        vue.getCanvas().setElementSelectionne(null);
        vue.redrawAll();
        if (onModification != null) {
            onModification.run();
        }
    }

    public void setNiveauControleur(NiveauControleur ctrl) {
        this.niveauControleur = ctrl;
        rafraichirTypesMursAffichage();
    }

    public void rafraichirTypesMursAffichage() {
        for (Dessin d : vue.getCanvas().getElements()) {
            if (d instanceof Mur mur) {
                appliquerTypeMurAffichage(mur, mur, cotesBatimentAffichage);
            }
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
