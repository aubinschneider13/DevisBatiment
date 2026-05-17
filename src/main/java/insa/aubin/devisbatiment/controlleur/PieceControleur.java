package insa.aubin.devisbatiment.controlleur;

import insa.aubin.devisbatiment.modele.*;
import insa.aubin.devisbatiment.view.DashBoardView;
import insa.aubin.devisbatiment.view.PieceView;
import java.util.*;
import javafx.event.ActionEvent;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import insa.aubin.devisbatiment.modele.SurfaceAvecRevetement;
import insa.aubin.devisbatiment.modele.Sol;
import insa.aubin.devisbatiment.modele.Mur;

public class PieceControleur {

    // =========================================================================
    // CONSTANTES D'ÉTAT
    // =========================================================================
    public static final int ETAT_RIEN    = 0;
    public static final int ETAT_MUR     = 30;
    public static final int ETAT_PORTE   = 40;
    public static final int ETAT_FENETRE = 50;
    public static final int ETAT_PIECE   = 60;
    public static final int ETAT_SELECTION = 70;

    // =========================================================================
    // ATTRIBUTS
    // =========================================================================
    private PieceView vue;
    private Stage stage;
    private GestionnaireSauvegarde gestionnaire;
    private int etat = ETAT_RIEN;
    private Point p1, p2;
    private Mur mur1, mur2;
    private int etapeRectangle;
    private Mur murSurvolé;
    private List<Point> polygoneAppartement = null;
    private final List<Piece> pieces = new ArrayList<>();
    private Appartement appartement;
    private java.util.function.Function<Piece, TreeItem<String>> onPieceCree = null;
    private static final double TOL = 1e-6;
    private final Map<TreeItem<String>, Piece> mapItemPiece = new HashMap<>();
    public Map<TreeItem<String>, Piece> getMapItemPiece() { return mapItemPiece; }
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
        this.vue.getCanvas().setPanActif(this.etat == ETAT_RIEN);
        this.vue.getOptionsMurVue().setVisible(this.etat == ETAT_MUR);

        switch (nouvelEtat) {
            case ETAT_RIEN ->
                    this.vue.setInstructions(
                            "Navigation active — Clic droit ou molette pour déplacer la vue");
            case ETAT_MUR -> {
                if (this.vue.getOptionsMurVue().estRectangulaire()) {
                    this.vue.setInstructions(
                            "Mode rectangle — Cliquez pour définir le premier coin");
                } else {
                    this.vue.setInstructions(
                            "Mode libre — Cliquez pour définir le début du mur");
                }
            }
            case ETAT_PIECE ->
                    this.vue.setInstructions(
                            "Cliquez dans une zone fermée pour créer une pièce");
            case ETAT_PORTE ->
                    this.vue.setInstructions(
                            "Survolez un mur puis cliquez pour insérer une porte — Échap pour annuler");
            case ETAT_FENETRE ->
                    this.vue.setInstructions(
                            "Survolez un mur puis cliquez pour insérer une fenêtre — Échap pour annuler");
            case ETAT_SELECTION ->
                    this.vue.setInstructions("Mode Matériaux — Cliquez sur un mur ou un sol pour le sélectionner");
        }
    }

    public Point posInModel(double x, double y) {
        Point2D p = this.vue.getCanvas().snapToGrid(x, y);
        return new Point(p.getX(), p.getY());
    }

    public void btnMur(ActionEvent t) {
        this.changerEtat(ETAT_MUR);
        this.vue.getOptionsMurVue().setVisible(true);
    }

    // =========================================================================
    // CLICS ET MOUVEMENTS SOURIS
    // =========================================================================
    public void clicDansZoneDeDessin(MouseEvent event) {
        Point pClic = this.posInModel(event.getX(), event.getY());

        if (!estDansAppartement(pClic.getX(), pClic.getY())) return;

        if (this.etat == ETAT_MUR) {
            genererDessinMur(pClic);
        } else if (this.etat == ETAT_PIECE) {
            gererClicPiece(pClic);
        } else if (this.etat == ETAT_PORTE) {
            Mur cible = trouverMurProche(pClic);
            if (cible != null) {
                double t     = cible.calculerPositionSurMur(pClic);
                double marge = Porte.LARGEUR_PORTE / (2 * cible.calculerLongueur());
                t = Math.max(marge, Math.min(1.0 - marge, t));
                cible.ajouterOuverture(new Porte(t));
            }
        } else if (this.etat == ETAT_FENETRE) {
            Mur cible = trouverMurProche(pClic);
            if (cible != null) {
                double t     = cible.calculerPositionSurMur(pClic);
                double marge = Fenetre.COTE_FENETRE / (2 * cible.calculerLongueur());
                t = Math.max(marge, Math.min(1.0 - marge, t));
                cible.ajouterOuverture(new Fenetre(t));
            }
        }
        else if (this.etat == ETAT_SELECTION) {
            Mur cible = trouverMurProche(pClic);

            // Si on a cliqué proche d'un mur, on le sélectionne
            if (cible != null && cible.distanceA(pClic) < 0.5) { // 0.5 est la tolérance de clic
                basculerSelection(cible);
            } else {
                // Sinon, on vérifie si on a cliqué au milieu d'une pièce pour sélectionner son Sol
                for (Piece piece : pieces) {
                    if (pointDansPolygone(pClic.getX(), pClic.getY(), piece.getPoints())) {
                        basculerSelection(piece.getSol());
                        break;
                    }
                }
            }
        }
    }

    // =========================================================================
    // GESTION DES PIÈCES
    // =========================================================================
    private void gererClicPiece(Point pClic) {
        double px = pClic.getX(), py = pClic.getY();

        // Vérifier qu'aucune pièce n'occupe déjà cette zone
        for (Piece piece : pieces) {
            if (pointDansPolygone(px, py, piece.getPoints())) {
                vue.setInstructions("Une pièce existe déjà dans cette zone.");
                return;
            }
        }

        List<SegmentSource> sources = collecterSegmentsSources();
        List<SegmentSource> cycle   = trouverCycleMinimal(px, py, sources);

        if (cycle == null || cycle.size() < 3) {
            vue.setInstructions(
                    "Aucune zone fermée ici — vérifiez que les murs se rejoignent bien.");
            return;
        }

        // Extraire et ordonner les murs
        List<Mur> mursPiece = new ArrayList<>();
        for (SegmentSource ss : cycle) {
            mursPiece.add(ss.mur);
        }
        mursPiece = ordonnerMurs(mursPiece);

        // Créer la pièce via l'appartement
        if (appartement == null) {
            vue.setInstructions("Aucun appartement associé — impossible de créer la pièce.");
            return;
        }
        Piece piece = appartement.ajouterPiece(mursPiece);
        pieces.add(piece);
        if (onPieceCree != null) {
            TreeItem<String> itemPiece = onPieceCree.apply(piece);
            if (itemPiece != null) {
                mapItemPiece.put(itemPiece, piece);
            }
        }
        List<Mur> mursFinaux = mursPiece;
        Dessin dessinPiece = new Dessin() {
            @Override
            public void dessiner(GraphicsContext gc) {
                List<Point> pts = piece.getPoints();
                if (pts == null || pts.size() < 3) return;
                double[] xs = pts.stream().mapToDouble(Point::getX).toArray();
                double[] ys = pts.stream().mapToDouble(Point::getY).toArray();

                // Fond coloré
                gc.setFill(PaletteVisuelle.fondPiece(piece.getNumero()));
                gc.fillPolygon(xs, ys, pts.size());

                // Label au centre
                double cx = pts.stream().mapToDouble(Point::getX).average().orElse(0);
                double cy = pts.stream().mapToDouble(Point::getY).average().orElse(0);
                gc.save();
                gc.scale(1, -1);
                gc.setFill(PaletteVisuelle.contourPiece(piece.getNumero()));
                gc.setFont(javafx.scene.text.Font.font("Arial",
                    javafx.scene.text.FontWeight.BOLD, 0.25));
                gc.fillText(piece.toString(), cx - 0.3, -cy + 0.1);
                gc.restore();
            }
            @Override public Color getColor() { return Color.ORANGE; }
            @Override public void setColor(Color c) { }
        };

        vue.getCanvas().ajouterElement(dessinPiece);
        vue.setInstructions("Pièce créée — cliquez dans une autre zone pour en ajouter une.");
        vue.redrawAll();
    }

    // =========================================================================
    // COLLECTE DES SEGMENTS
    // =========================================================================

    /**
     * Collecte les segments disponibles pour la détection de pièces :
     * — contour de l'appartement (polygoneAppartement)
     * — murs intérieurs dessinés sur le canvas
     */
    private List<SegmentSource> collecterSegmentsSources() {
        List<SegmentSource> sources = new ArrayList<>();
        List<SegmentSource> bruts = new ArrayList<>();

        // 1. Contour de l'appartement
        if (polygoneAppartement != null && polygoneAppartement.size() >= 3) {
            int n = polygoneAppartement.size();
            for (int i = 0; i < n; i++) {
                Point a = polygoneAppartement.get(i);
                Point b = polygoneAppartement.get((i + 1) % n);
                bruts.add(new SegmentSource(a, b, new Mur(a, b)));
            }
        }

        // 2. Murs intérieurs dessinés sur le canvas
        for (Dessin d : vue.getCanvas().getElements()) {
            if (d instanceof Mur) {
                Mur m = (Mur) d;
                bruts.add(new SegmentSource(m.getPoint1(), m.getPoint2(), m));
            }
        }

        // 3. Collecter tous les nœuds
        List<double[]> tousLesPoints = new ArrayList<>();
        for (SegmentSource ss : bruts) {
            ajouterNoeudSiAbsent(tousLesPoints, ss.x1, ss.y1);
            ajouterNoeudSiAbsent(tousLesPoints, ss.x2, ss.y2);
        }

        // 4. Subdiviser chaque segment aux intersections
        for (SegmentSource ss : bruts) {
            List<double[]> pointsSurSegment = new ArrayList<>();
            pointsSurSegment.add(new double[]{ss.x1, ss.y1});
            pointsSurSegment.add(new double[]{ss.x2, ss.y2});

            for (double[] pt : tousLesPoints) {
                if (pointSurSegment(pt[0], pt[1], ss.x1, ss.y1, ss.x2, ss.y2)) {
                    boolean dejaDedans = false;
                    for (double[] existing : pointsSurSegment) {
                        if (Math.abs(existing[0] - pt[0]) < TOL
                                && Math.abs(existing[1] - pt[1]) < TOL) {
                            dejaDedans = true;
                            break;
                        }
                    }
                    if (!dejaDedans) pointsSurSegment.add(pt);
                }
            }

            // Trier par paramètre t
            double dx = ss.x2 - ss.x1, dy = ss.y2 - ss.y1;
            double len2 = dx*dx + dy*dy;
            pointsSurSegment.sort((a, b) -> {
                double ta = ((a[0]-ss.x1)*dx + (a[1]-ss.y1)*dy) / len2;
                double tb = ((b[0]-ss.x1)*dx + (b[1]-ss.y1)*dy) / len2;
                return Double.compare(ta, tb);
            });

            // Créer sous-segments
            for (int i = 0; i < pointsSurSegment.size() - 1; i++) {
                double[] a = pointsSurSegment.get(i);
                double[] b = pointsSurSegment.get(i + 1);
                Point pa = new Point(a[0], a[1]);
                Point pb = new Point(b[0], b[1]);
                ajouterSegmentSiAbsent(sources, pa, pb, new Mur(pa, pb));
            }
        }

        return sources;
    }

    private boolean pointSurSegment(double px, double py,
                                    double x1, double y1,
                                    double x2, double y2) {
        double cross = (px - x1)*(y2 - y1) - (py - y1)*(x2 - x1);
        if (Math.abs(cross) > TOL) return false;
        double dx = x2-x1, dy = y2-y1;
        double len2 = dx*dx + dy*dy;
        if (len2 < TOL) return false;
        double t = ((px-x1)*dx + (py-y1)*dy) / len2;
        return t >= -TOL && t <= 1.0 + TOL;
    }

    private void ajouterSegmentSiAbsent(List<SegmentSource> sources,
                                        Point a, Point b, Mur mur) {
        for (SegmentSource ss : sources) {
            Point ssA = new Point(ss.x1, ss.y1);
            Point ssB = new Point(ss.x2, ss.y2);
            boolean sensNormal  = correspondA(ssA, a) && correspondA(ssB, b);
            boolean sensInverse = correspondA(ssA, b) && correspondA(ssB, a);
            if (sensNormal || sensInverse) return;
        }
        sources.add(new SegmentSource(a, b, mur));
    }

    private boolean correspondA(Point p1, Point p2) {
        return Math.abs(p1.getX() - p2.getX()) < TOL
                && Math.abs(p1.getY() - p2.getY()) < TOL;
    }

    // =========================================================================
    // ALGORITHME : CYCLE MINIMAL (faces planaires) — identique à NiveauControleur
    // =========================================================================
    private List<SegmentSource> trouverCycleMinimal(double px, double py,
                                                    List<SegmentSource> sources) {
        List<double[]> noeuds = new ArrayList<>();
        for (SegmentSource ss : sources) {
            ajouterNoeudSiAbsent(noeuds, ss.x1, ss.y1);
            ajouterNoeudSiAbsent(noeuds, ss.x2, ss.y2);
        }
        int N = noeuds.size();
        if (N < 3) return null;

        // Liste d'adjacence : [voisin, idxSeg]
        List<List<int[]>> adj = new ArrayList<>();
        for (int i = 0; i < N; i++) adj.add(new ArrayList<>());

        for (int s = 0; s < sources.size(); s++) {
            SegmentSource ss = sources.get(s);
            int i = indexNoeud(noeuds, ss.x1, ss.y1);
            int j = indexNoeud(noeuds, ss.x2, ss.y2);
            if (i == -1 || j == -1 || i == j) continue;
            adj.get(i).add(new int[]{j, s});
            adj.get(j).add(new int[]{i, s});
        }

        // Trier les voisins par angle (algorithme des faces planaires)
        for (int i = 0; i < N; i++) {
            final double[] noeud = noeuds.get(i);
            adj.get(i).sort((a, b) -> {
                double ax = noeuds.get(a[0])[0] - noeud[0];
                double ay = noeuds.get(a[0])[1] - noeud[1];
                double bx = noeuds.get(b[0])[0] - noeud[0];
                double by = noeuds.get(b[0])[1] - noeud[1];
                return Double.compare(Math.atan2(-ay, ax), Math.atan2(-by, bx));
            });
        }

        // Énumérer les faces via Next Half-Edge
        List<List<Integer>> faces        = new ArrayList<>();
        Set<String>         visitees     = new HashSet<>();

        for (int i = 0; i < N; i++) {
            for (int[] arete : adj.get(i)) {
                int j   = arete[0];
                String cle = i + "->" + j;
                if (visitees.contains(cle)) continue;

                List<Integer> face = new ArrayList<>();
                int courant = i, suivant = j;
                int maxIter = N + 2;

                while (maxIter-- > 0) {
                    String cleEtape = courant + "->" + suivant;
                    if (visitees.contains(cleEtape)) break;
                    visitees.add(cleEtape);
                    face.add(courant);

                    List<int[]> voisins = adj.get(suivant);
                    int idxCourant = -1;
                    for (int k = 0; k < voisins.size(); k++) {
                        if (voisins.get(k)[0] == courant) { idxCourant = k; break; }
                    }
                    if (idxCourant == -1) break;

                    int idxPred      = (idxCourant + 1) % voisins.size();
                    int prochainNoeud = voisins.get(idxPred)[0];

                    courant = suivant;
                    suivant = prochainNoeud;

                    if (suivant == i && courant == j) break;
                    if (courant == i) { faces.add(new ArrayList<>(face)); break; }
                }
            }
        }

        // Trouver la face minimale contenant (px, py)
        List<SegmentSource> meilleur      = null;
        double              meilleureAire = Double.MAX_VALUE;

        for (List<Integer> face : faces) {
            if (face.size() < 3) continue;

            List<Point> polygone = new ArrayList<>();
            for (int idx : face) {
                polygone.add(new Point(noeuds.get(idx)[0], noeuds.get(idx)[1]));
            }

            if (!pointDansPolygone(px, py, polygone)) continue;

            double aire = Math.abs(calculerAire(polygone));
            if (aire < meilleureAire && aire > TOL) {
                meilleureAire = aire;
                meilleur      = new ArrayList<>();
                for (int k = 0; k < face.size(); k++) {
                    int nA = face.get(k);
                    int nB = face.get((k + 1) % face.size());
                    for (int[] ar : adj.get(nA)) {
                        if (ar[0] == nB) { meilleur.add(sources.get(ar[1])); break; }
                    }
                }
            }
        }
        return meilleur;
    }

    // =========================================================================
    // ORDONNANCEMENT DES MURS
    // =========================================================================
    private List<Mur> ordonnerMurs(List<Mur> murs) {
        if (murs.size() <= 1) return murs;

        List<Mur> ordonne  = new ArrayList<>();
        List<Mur> restants = new ArrayList<>(murs);

        Mur courant = restants.remove(0);
        ordonne.add(courant);

        while (!restants.isEmpty()) {
            Point dernierPoint = courant.getPoint2();
            boolean trouve     = false;

            for (int i = 0; i < restants.size(); i++) {
                Mur candidat = restants.get(i);
                if (correspondA(candidat.getPoint1(), dernierPoint)) {
                    courant = candidat;
                    ordonne.add(courant);
                    restants.remove(i);
                    trouve = true;
                    break;
                }
                if (correspondA(candidat.getPoint2(), dernierPoint)) {
                    Mur inverse = new Mur(candidat.getPoint2(), candidat.getPoint1());
                    courant = inverse;
                    ordonne.add(courant);
                    restants.remove(i);
                    trouve = true;
                    break;
                }
            }
            if (!trouve) break;
        }
        return ordonne;
    }

    // =========================================================================
    // UTILITAIRES GÉOMÉTRIQUES
    // =========================================================================
    private void ajouterNoeudSiAbsent(List<double[]> noeuds, double x, double y) {
        for (double[] n : noeuds) {
            if (Math.abs(n[0] - x) < TOL && Math.abs(n[1] - y) < TOL) return;
        }
        noeuds.add(new double[]{x, y});
    }

    private int indexNoeud(List<double[]> noeuds, double x, double y) {
        for (int i = 0; i < noeuds.size(); i++) {
            double[] n = noeuds.get(i);
            if (Math.abs(n[0] - x) < TOL && Math.abs(n[1] - y) < TOL) return i;
        }
        return -1;
    }

    private boolean pointDansPolygone(double px, double py, List<Point> poly) {
        if (poly == null || poly.size() < 3) return false;
        int n = poly.size(), intersections = 0;
        for (int i = 0; i < n; i++) {
            Point a = poly.get(i);
            Point b = poly.get((i + 1) % n);
            double ax = a.getX(), ay = a.getY(), bx = b.getX(), by = b.getY();
            if ((ay <= py && by > py) || (by <= py && ay > py)) {
                double xInter = ax + (py - ay) / (by - ay) * (bx - ax);
                if (px < xInter) intersections++;
            }
        }
        return (intersections % 2) == 1;
    }

    private double calculerAire(List<Point> poly) {
        double aire = 0;
        int n = poly.size();
        for (int i = 0; i < n; i++) {
            Point a = poly.get(i);
            Point b = poly.get((i + 1) % n);
            aire += a.getX() * b.getY() - b.getX() * a.getY();
        }
        return aire / 2.0;
    }

    // =========================================================================
    // DESSIN DES MURS
    // =========================================================================
    public void genererDessinMur(Point pClic) {
        boolean modRect = this.vue.getOptionsMurVue().estRectangulaire();
        if (modRect) {
            switch (etapeRectangle) {
                case 0:
                    this.p1   = pClic;
                    this.mur1 = new Mur(p1, p1);
                    this.mur1.setTypeMur(Mur.TypeMur.NORMAL);
                    this.vue.getCanvas().ajouterElement(this.mur1);
                    this.vue.setInstructions(
                            "Premier coin posé — Cliquez pour définir la longueur");
                    this.etapeRectangle = 1;
                    break;
                case 1:
                    this.p2   = pClic;
                    this.mur1.setPoint2(p2);
                    this.mur2 = new Mur(p2, p2);
                    this.mur2.setTypeMur(Mur.TypeMur.NORMAL);
                    this.vue.getCanvas().ajouterElement(this.mur2);
                    this.vue.setInstructions(
                            "Longueur définie — Cliquez pour définir la largeur");
                    this.etapeRectangle = 2;
                    break;
                case 2:
                    Point p3 = calculerPointOrthogonal(this.p2, pClic);
                    this.mur2.setPoint2(p3);
                    Point p4 = new Point(
                            p1.getX() + (p3.getX() - p2.getX()),
                            p1.getY() + (p3.getY() - p2.getY())
                    );
                    Mur mur3 = new Mur(p3, p4);
                    mur3.setTypeMur(Mur.TypeMur.NORMAL);
                    this.vue.getCanvas().ajouterElement(mur3);
                    Mur mur4 = new Mur(p4, p1);
                    mur4.setTypeMur(Mur.TypeMur.NORMAL);
                    this.vue.getCanvas().ajouterElement(mur4);
                    this.vue.setInstructions(
                            "Rectangle créé — Cliquez pour un nouveau mur ou changez d'outil");
                    this.etapeRectangle = 0;
                    this.mur1 = null;
                    this.mur2 = null;
                    break;
            }
        } else {
            if (this.mur1 == null) {
                this.mur1 = new Mur(pClic, pClic);
                this.vue.getCanvas().ajouterElement(this.mur1);
                this.vue.setInstructions(
                        "Début du mur posé — Cliquez pour définir la fin");
            } else {
                this.mur1.setPoint2(pClic);
                this.mur1 = null;
                this.vue.setInstructions(
                        "Mur créé — Cliquez pour un nouveau mur ou changez d'outil");
            }
        }
    }

    // =========================================================================
    // MOUVEMENTS SOURIS
    // =========================================================================
    public void mouseMovedDansZoneDessin(MouseEvent t) {
        Point pSouris = this.posInModel(t.getX(), t.getY());

        if (this.etat == ETAT_MUR) {
            if (polygoneAppartement != null
                    && !estDansAppartement(pSouris.getX(), pSouris.getY())) return;

            boolean modRect = this.vue.getOptionsMurVue().estRectangulaire();
            if (modRect) {
                if (etapeRectangle == 1 && mur1 != null) {
                    this.mur1.setPoint2(pSouris);
                } else if (etapeRectangle == 2 && mur2 != null) {
                    this.mur2.setPoint2(calculerPointOrthogonal(this.p2, pSouris));
                }
            } else {
                if (this.mur1 != null) this.mur1.setPoint2(pSouris);
            }
            this.vue.redrawAll();

        } else if (this.etat == ETAT_PORTE || this.etat == ETAT_FENETRE) {
            this.murSurvolé = trouverMurProche(pSouris);
            Fantome fantome = (this.etat == ETAT_PORTE) ? new Porte() : new Fenetre();

            if (this.murSurvolé != null) {
                boolean compatible = (this.etat == ETAT_FENETRE)
                        ? this.murSurvolé.getTypeMur() == Mur.TypeMur.EXTERIEUR
                        : this.murSurvolé.getTypeMur() != Mur.TypeMur.EXTERIEUR;
                double angle = Math.toDegrees(Math.atan2(
                        murSurvolé.getPoint2().getY() - murSurvolé.getPoint1().getY(),
                        murSurvolé.getPoint2().getX() - murSurvolé.getPoint1().getX()));
                Point posAccrochage = projeterSurMur(pSouris, murSurvolé);
                this.vue.getCanvas().setFantome(
                        fantome, posAccrochage.getX(), posAccrochage.getY(),
                        angle, compatible);
            } else {
                this.vue.getCanvas().setFantome(
                        fantome, pSouris.getX(), pSouris.getY(), 0, false);
            }
            this.vue.redrawAll();
        }
    }

    // =========================================================================
    // INITIALISATION AVEC CONTOUR APPARTEMENT
    // =========================================================================
    public void initialiserAvecContourAppartement(List<Point> polygone,
                                                  List<Mur> mursDelimiteurs,
                                                  AireImmeuble aire,
                                                  Appartement appartement) {
        this.appartement         = appartement;
        this.polygoneAppartement = polygone;
        if (polygone == null || polygone.size() < 3) return;

        // Fond bleuté
        Dessin fond = new Dessin() {
            @Override
            public void dessiner(GraphicsContext gc) {
                double[] xs = polygone.stream().mapToDouble(Point::getX).toArray();
                double[] ys = polygone.stream().mapToDouble(Point::getY).toArray();
                gc.setFill(Color.web("#4a90d9", 0.05));
                gc.fillPolygon(xs, ys, polygone.size());
            }
            @Override public Color getColor() { return Color.TRANSPARENT; }
            @Override public void setColor(Color c) { }
        };
        this.vue.getCanvas().getElements().add(0, fond);

        // Classifier les murs (extérieur / intérieur) et les ajouter au canvas
        List<double[]> cotesBatiment = new ArrayList<>();
        if (aire != null && aire.isComplete()) {
            Point q1 = aire.getP1(), q2 = aire.getP2();
            Point q3 = aire.getP3(), q4 = aire.getP4();
            cotesBatiment.add(new double[]{q1.getX(), q1.getY(), q2.getX(), q2.getY()});
            cotesBatiment.add(new double[]{q2.getX(), q2.getY(), q3.getX(), q3.getY()});
            cotesBatiment.add(new double[]{q3.getX(), q3.getY(), q4.getX(), q4.getY()});
            cotesBatiment.add(new double[]{q4.getX(), q4.getY(), q1.getX(), q1.getY()});
        }

        for (Mur mur : mursDelimiteurs) {
            boolean exterieur = false;
            for (double[] cote : cotesBatiment) {
                if (murInclsDansCote(mur, cote)) { exterieur = true; break; }
            }
            mur.setTypeMur(exterieur ? Mur.TypeMur.EXTERIEUR : Mur.TypeMur.NORMAL);
            this.vue.getCanvas().getElements().add(mur);
        }

        this.vue.getCanvas().redrawAll();
    }

    // =========================================================================
    // MÉTHODES UTILITAIRES
    // =========================================================================
    private boolean murInclsDansCote(Mur mur, double[] cote) {
        double ax = cote[0], ay = cote[1], bx = cote[2], by = cote[3];
        double dx = bx - ax, dy = by - ay;
        double len2 = dx*dx + dy*dy;
        if (len2 < TOL) return false;

        double[] pts = {
                mur.getPoint1().getX(), mur.getPoint1().getY(),
                mur.getPoint2().getX(), mur.getPoint2().getY()
        };
        for (int i = 0; i < 4; i += 2) {
            double px = pts[i], py = pts[i+1];
            double cross = (px - ax) * dy - (py - ay) * dx;
            if (Math.abs(cross) / Math.sqrt(len2) > TOL) return false;
            double t = ((px - ax)*dx + (py - ay)*dy) / len2;
            if (t < -TOL || t > 1 + TOL) return false;
        }
        return true;
    }

    private boolean estDansAppartement(double px, double py) {
        if (polygoneAppartement == null) return true;
        return pointDansPolygone(px, py) || pointSurContour(px, py);
    }

    private boolean pointDansPolygone(double px, double py) {
        int n = polygoneAppartement.size(), intersections = 0;
        for (int i = 0; i < n; i++) {
            Point a = polygoneAppartement.get(i);
            Point b = polygoneAppartement.get((i + 1) % n);
            if ((a.getY() <= py && b.getY() > py) || (b.getY() <= py && a.getY() > py)) {
                double xInter = a.getX() + (py - a.getY()) / (b.getY() - a.getY())
                        * (b.getX() - a.getX());
                if (px < xInter) intersections++;
            }
        }
        return (intersections % 2) == 1;
    }

    private boolean pointSurContour(double px, double py) {
        if (polygoneAppartement == null) return false;
        int n = polygoneAppartement.size();
        for (int i = 0; i < n; i++) {
            Point a = polygoneAppartement.get(i);
            Point b = polygoneAppartement.get((i + 1) % n);
            double dx = b.getX() - a.getX(), dy = b.getY() - a.getY();
            double len2 = dx*dx + dy*dy;
            if (len2 < 1e-10) continue;
            double t = ((px - a.getX())*dx + (py - a.getY())*dy) / len2;
            if (t < -TOL || t > 1 + TOL) continue;
            double cx = a.getX() + t*dx, cy = a.getY() + t*dy;
            if (Math.hypot(px - cx, py - cy) < TOL) return true;
        }
        return false;
    }

    private Point projeterSurMur(Point p, Mur mur) {
        double x1 = mur.getPoint1().getX(), y1 = mur.getPoint1().getY();
        double x2 = mur.getPoint2().getX(), y2 = mur.getPoint2().getY();
        double dx = x2 - x1, dy = y2 - y1;
        double l2 = dx*dx + dy*dy;
        if (l2 == 0) return mur.getPoint1();
        double largeur = (this.etat == ETAT_PORTE) ? Porte.LARGEUR_PORTE : Fenetre.COTE_FENETRE;
        double t = ((p.getX() - x1)*dx + (p.getY() - y1)*dy) / l2;
        double marge = largeur / (2 * mur.calculerLongueur());
        t = Math.max(marge, Math.min(1.0 - marge, t));
        return new Point(x1 + t*dx, y1 + t*dy);
    }

    private Mur trouverMurProche(Point p) {
        Mur plusProche = null;
        double distMin = 0.3;
        for (Dessin d : vue.getCanvas().getElements()) {
            if (d instanceof Mur) {
                Mur m = (Mur) d;
                double dist = m.distanceA(p);
                if (dist < distMin) { distMin = dist; plusProche = m; }
            }
        }
        return plusProche;
    }

    public Point calculerPointOrthogonal(Point centre, Point cible) {
        double dx = p2.getX() - p1.getX(), dy = p2.getY() - p1.getY();
        double perpX = -dy, perpY = dx;
        double ux = cible.getX() - centre.getX(), uy = cible.getY() - centre.getY();
        double s = (ux*perpX + uy*perpY) / (perpX*perpX + perpY*perpY);
        return new Point(centre.getX() + s*perpX, centre.getY() + s*perpY);
    }

    private void basculerSelection(SurfaceAvecRevetement surface) {
        if (surfacesSelectionnees.contains(surface)) {
            surfacesSelectionnees.remove(surface);
        } else {
            surfacesSelectionnees.add(surface);
        }
        // On transmet la liste au canvas pour qu'il la dessine, puis on rafraîchit
        vue.getCanvas().setSelection(surfacesSelectionnees);
        vue.redrawAll();
    }

    // =========================================================================
    // ANNULATION
    // =========================================================================
    public void annulerConstruction() {
        if (this.mur1 != null) this.vue.getCanvas().getElements().remove(this.mur1);
        if (this.mur2 != null) this.vue.getCanvas().getElements().remove(this.mur2);
        this.etapeRectangle = 0;
        this.mur1 = null;
        this.mur2 = null;
        this.vue.redrawAll();
    }

    // =========================================================================
    // BOUTONS TOOLBAR
    // =========================================================================
    public void btnNavigation(ActionEvent t) {
        this.changerEtat(ETAT_RIEN);
        this.vue.getCanvas().setPanActif(true);
        this.vue.getOptionsMurVue().setVisible(false);
    }

    public void btnEchelle(ActionEvent t) {
        boolean visible = !this.vue.getEchelleVue().isVisible();
        this.vue.getEchelleVue().setVisible(visible);
        if (visible) {
            this.vue.getEchelleVue().getGroupeEchelle()
                    .selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
                        if (newVal != null) {
                            this.vue.getCanvas().setGridSize(
                                    this.vue.getEchelleVue().getEchelleSelectionnee());
                        }
                    });
        }
    }
    
    public void activerModePiece() {
        changerEtat(ETAT_PIECE);
        this.vue.setInstructions(
            "Cliquez à l'intérieur d'une zone fermée pour créer une pièce"
        );
    }
    
    public void rafraichirNavigateur() { }

    // =========================================================================
    // CLASSE INTERNE
    // =========================================================================
    private static class SegmentSource {
        final double x1, y1, x2, y2;
        final Mur mur;

        SegmentSource(Point a, Point b, Mur mur) {
            this.x1 = a.getX(); this.y1 = a.getY();
            this.x2 = b.getX(); this.y2 = b.getY();
            this.mur = mur;
        }
    }
    
    public void setOnPieceCree(java.util.function.Function<Piece, TreeItem<String>> callback) {
        this.onPieceCree = callback;
    }

    public List<SurfaceAvecRevetement> getSurfacesSelectionnees() {
        return surfacesSelectionnees;
    }

    public void viderSelection() {
        surfacesSelectionnees.clear();
        vue.getCanvas().setSelection(surfacesSelectionnees);
        vue.redrawAll();
    }
}