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

    public static final int ETAT_RIEN    = 0;
    public static final int ETAT_MUR     = 30;
    public static final int ETAT_PORTE   = 40;
    public static final int ETAT_FENETRE = 50;
    public static final int ETAT_PIECE   = 60;

    // =========================================================================
    // ATTRIBUTS
    // =========================================================================

    private final PieceView            vue;
    private final Stage                stage;
    private final GestionnaireSauvegarde gestionnaire;

    private int etat = ETAT_RIEN;

    // Appartement courant et son contour
    private Appartement   appartement          = null;
    private List<Point>   polygoneAppartement  = null;

    // Pièces créées dans cet appartement
    private final List<Piece>                  pieces        = new ArrayList<>();
    private final Map<TreeItem<String>, Piece> mapItemPiece  = new HashMap<>();

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

    /**
     * Enregistre le callback appelé à chaque création de pièce.
     * Permet à AppControleur de peupler le NavigateurView sans couplage direct.
     *
     * @param callback fonction recevant la Pièce créée et renvoyant le TreeItem associé
     */
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
        if (visible) {
            vue.getEchelleVue().getGroupeEchelle()
               .selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
                   if (newVal != null) {
                       vue.getCanvas().setGridSize(
                               vue.getEchelleVue().getEchelleSelectionnee());
                   }
               });
        }
    }

    // =========================================================================
    // CLICS DANS LA ZONE DE DESSIN
    // =========================================================================

    public void clicDansZoneDeDessin(MouseEvent event) {
        Point pClic = posInModel(event.getX(), event.getY());

        if (!estDansAppartement(pClic.getX(), pClic.getY())) return;

        switch (etat) {
            case ETAT_MUR     -> gererClicMur(pClic);
            case ETAT_PIECE   -> gererClicPiece(pClic);
            case ETAT_PORTE   -> gererClicOuverture(pClic, true);
            case ETAT_FENETRE -> gererClicOuverture(pClic, false);
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

    /** Annule le mur ou le rectangle en cours de construction. */
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

        // Refus si une pièce occupe déjà ce point
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

        // Détection de la zone fermée via l'algorithme de cycle minimal
        List<SegmentSource> sources = collecterSegmentsSources();
        List<SegmentSource> cycle   = GeometrieUtils.trouverCycleMinimal(px, py, sources);

        if (cycle == null || cycle.size() < 3) {
            vue.setInstructions(
                    "Aucune zone fermée ici — vérifiez que les murs se rejoignent bien.");
            return;
        }

        // Construire et ordonner les murs délimiteurs de la pièce
        List<Mur> mursPiece = new ArrayList<>();
        for (SegmentSource ss : cycle) mursPiece.add(ss.mur);
        mursPiece = GeometrieUtils.ordonnerMurs(mursPiece);

        // Créer la pièce via l'appartement et l'ajouter au canvas
        Piece piece = appartement.ajouterPiece(mursPiece);
        pieces.add(piece);

        if (onPieceCree != null) {
            TreeItem<String> itemPiece = onPieceCree.apply(piece);
            if (itemPiece != null) mapItemPiece.put(itemPiece, piece);
        }

        vue.getCanvas().ajouterElement(creerDessinPiece(piece));
        vue.setInstructions("Pièce créée — cliquez dans une autre zone pour en ajouter une.");
        vue.redrawAll();
    }

    /** Crée le {@link Dessin} d'une pièce (fond coloré + label centré). */
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
        double t       = Math.max(marge, Math.min(1.0 - marge,
                cible.calculerPositionSurMur(pClic)));

        cible.ajouterOuverture(estPorte ? new Porte(t) : new Fenetre(t));
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

    /**
     * Initialise le canvas avec le contour de l'appartement et classifie
     * ses murs (extérieur / intérieur) selon leur appartenance au bâtiment.
     */
    public void initialiserAvecContourAppartement(List<Point> polygone,
                                                  List<Mur> mursDelimiteurs,
                                                  AireImmeuble aire,
                                                  Appartement appartement) {
        this.appartement        = appartement;
        this.polygoneAppartement = polygone;
        if (polygone == null || polygone.size() < 3) return;

        // Fond bleuté de l'appartement
        vue.getCanvas().getElements().add(0, creerFondAppartement(polygone));

        // Calculer les côtés du bâtiment pour classifier les murs
        List<double[]> cotesBatiment = extraireCotesBatiment(aire);

        for (Mur mur : mursDelimiteurs) {
            boolean exterieur = cotesBatiment.stream().anyMatch(c -> murInclsDansCote(mur, c));
            mur.setTypeMur(exterieur ? Mur.TypeMur.EXTERIEUR : Mur.TypeMur.NORMAL);
            vue.getCanvas().getElements().add(mur);
        }

        vue.getCanvas().redrawAll();
        
    }

    /** Crée le fond bleuté semi-transparent de l'appartement. */
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

    /** Extrait les 4 côtés du bâtiment sous forme de tableaux [x1,y1,x2,y2]. */
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

    // =========================================================================
    // COLLECTE DES SEGMENTS (délègue la subdivision à GeometrieUtils)
    // =========================================================================

    /**
     * Collecte tous les segments pertinents de l'appartement (contour +
     * murs intérieurs), les subdivise aux intersections et les déduplique
     * via {@link GeometrieUtils#subdiviserEtDeduplicer(List)}.
     */
    private List<SegmentSource> collecterSegmentsSources() {
        List<SegmentSource> bruts = new ArrayList<>();

        // Contour de l'appartement
        if (polygoneAppartement != null && polygoneAppartement.size() >= 3) {
            int n = polygoneAppartement.size();
            for (int i = 0; i < n; i++) {
                Point a = polygoneAppartement.get(i);
                Point b = polygoneAppartement.get((i + 1) % n);
                bruts.add(new SegmentSource(a, b, new Mur(a, b)));
            }
        }

        // Murs intérieurs dessinés sur le canvas
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

    /** Convertit les coordonnées canvas en coordonnées modèle (grille snappée). */
    public Point posInModel(double x, double y) {
        Point2D p = vue.getCanvas().snapToGrid(x, y);
        return new Point(p.getX(), p.getY());
    }

    /** Vérifie si un point est à l'intérieur de l'appartement ou sur son contour. */
    private boolean estDansAppartement(double px, double py) {
        if (polygoneAppartement == null) return true;
        return GeometrieUtils.estDansZone(px, py, polygoneAppartement);
    }

    /**
     * Calcule le point sur la perpendiculaire à [p1Rect→p2Rect] passant par
     * {@code centre}, projeté depuis {@code cible}.
     */
    public Point calculerPointOrthogonal(Point centre, Point cible) {
        double dx    = p2Rect.getX() - p1Rect.getX();
        double dy    = p2Rect.getY() - p1Rect.getY();
        double perpX = -dy, perpY = dx;
        double ux    = cible.getX() - centre.getX();
        double uy    = cible.getY() - centre.getY();
        double s     = (ux * perpX + uy * perpY) / (perpX * perpX + perpY * perpY);
        return new Point(centre.getX() + s * perpX, centre.getY() + s * perpY);
    }

    /** Projette {@code p} sur le mur et le clamp dans les marges de l'ouverture. */
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

    /** Retourne le mur le plus proche de {@code p} dans un rayon de 0.3 unité. */
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

    /** Vérifie si les deux extrémités du mur sont incluses dans le côté du bâtiment. */
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
}