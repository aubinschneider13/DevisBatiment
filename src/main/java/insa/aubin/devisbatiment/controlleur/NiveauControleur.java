package insa.aubin.devisbatiment.controlleur;

import insa.aubin.devisbatiment.modele.*;
import insa.aubin.devisbatiment.view.NiveauView;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.TreeItem;
import javafx.scene.paint.Color;

import java.util.*;

public class NiveauControleur {

    private final NiveauView vue;
    private final AireImmeuble aireImmeuble;
    private final TreeItem<String> itemNiveau;
    private final Niveau niveau;

    private final Map<TreeItem<String>, Appartement> mapItemAppartement = new HashMap<>();
    private final List<Appartement> appartements = new ArrayList<>();
    private int compteurAppartements = 0;

    private String mode = "AUCUN";
    private Mur mur1EnCours = null;
    private Mur mur2EnCours = null;
    private Point p1Rect = null;
    private Point p2Rect = null;
    private int etapeRectangle = 0;

    // ✅ TOL augmentée pour absorber les erreurs de virgule flottante
    private static final double TOL = 1e-6;

    public NiveauControleur(NiveauView vue, AireImmeuble aireImmeuble,
                            TreeItem<String> itemNiveau, Niveau niveau) {
        this.vue          = vue;
        this.aireImmeuble = aireImmeuble;
        this.itemNiveau   = itemNiveau;
        this.niveau       = niveau;

        if (aireImmeuble != null && aireImmeuble.isComplete()) {
            vue.getCanvas().ajouterElement(creerContourAire());
        }

        for (Mur m : niveau.getMursDelimiteurs()) {
            vue.getCanvas().ajouterElement(m);
        }

        brancherListeners();
    }

    // =========================================================================
    // LISTENERS SOURIS
    // =========================================================================

    private void brancherListeners() {
        vue.getCanvas().setOnMouseClicked(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY
                    && e.isStillSincePress()) {
                clicCanvas(e);
            }
        });
        vue.getCanvas().setOnMouseMoved(e -> mouvementCanvas(e));

        // ← Échap sur le canvas directement (il reçoit le focus au clic)
        vue.getCanvas().setFocusTraversable(true);
        vue.getCanvas().setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                annulerMurEnCours();
            }
        });

        // Donner le focus au canvas dès qu'on clique dessus
        vue.getCanvas().setOnMousePressed(e -> vue.getCanvas().requestFocus());
    }

    private void clicCanvas(javafx.scene.input.MouseEvent e) {
        Point2D snap = vue.getCanvas().snapToGrid(e.getX(), e.getY());
        switch (mode) {
            case "MUR":         gererClicMur(snap);         break;
            case "APPARTEMENT": gererClicAppartement(snap); break;
        }
    }

    private void mouvementCanvas(javafx.scene.input.MouseEvent e) {
        if (!mode.equals("MUR")) return;
        Point2D snap = vue.getCanvas().snapToGrid(e.getX(), e.getY());
        boolean modRect = vue.getOptionsMurVue().estRectangulaire();

        if (modRect) {
            if (etapeRectangle == 1 && mur1EnCours != null) {
                mur1EnCours.setPoint2(new Point(snap.getX(), snap.getY()));
            } else if (etapeRectangle == 2 && mur2EnCours != null) {
                Point p3 = calculerPointOrthogonal(p2Rect,
                        new Point(snap.getX(), snap.getY()));
                mur2EnCours.setPoint2(p3);
            }
        } else {
            if (mur1EnCours != null) {
                mur1EnCours.setPoint2(new Point(snap.getX(), snap.getY()));
            }
        }
        vue.getCanvas().redrawAll();
    }

    // =========================================================================
    // GESTION DES MURS
    // =========================================================================

    private void gererClicMur(Point2D snap) {
        boolean modRect = vue.getOptionsMurVue().estRectangulaire();
        Point pClic = new Point(snap.getX(), snap.getY());

        if (modRect) {
            switch (etapeRectangle) {
                case 0:
                    p1Rect = pClic;
                    mur1EnCours = new Mur(p1Rect, p1Rect);
                    vue.getCanvas().ajouterElement(mur1EnCours);
                    vue.setInstructions("Premier coin posé — cliquez pour la longueur");
                    etapeRectangle = 1;
                    break;
                case 1:
                    p2Rect = pClic;
                    mur1EnCours.setPoint2(p2Rect);
                    mur2EnCours = new Mur(p2Rect, p2Rect);
                    vue.getCanvas().ajouterElement(mur2EnCours);
                    vue.setInstructions("Longueur définie — cliquez pour la largeur");
                    etapeRectangle = 2;
                    break;
                case 2:
                    Point p3 = calculerPointOrthogonal(p2Rect, pClic);
                    mur2EnCours.setPoint2(p3);
                    Point p4 = new Point(
                        p1Rect.getX() + (p3.getX() - p2Rect.getX()),
                        p1Rect.getY() + (p3.getY() - p2Rect.getY())
                    );
                    vue.getCanvas().ajouterElement(new Mur(p3, p4));
                    vue.getCanvas().ajouterElement(new Mur(p4, p1Rect));
                    vue.setInstructions("Rectangle créé — cliquez pour un nouveau ou changez d'outil");
                    etapeRectangle = 0;
                    mur1EnCours = null;
                    mur2EnCours = null;
                    p1Rect = null;
                    p2Rect = null;
                    break;
            }
        } else {
            // Mode libre — comportement existant
            if (mur1EnCours == null) {
                mur1EnCours = new Mur(pClic, pClic);
                vue.getCanvas().ajouterElement(mur1EnCours);
                vue.setInstructions("Cliquez pour poser l'extrémité — Échap pour annuler");
            } else {
                mur1EnCours.setPoint2(pClic);
                mur1EnCours = null;
                vue.setInstructions("Cliquez pour démarrer un nouveau mur");
            }
        }
        vue.getCanvas().redrawAll();
    }

    public void annulerMurEnCours() {
        if (mur1EnCours != null) {
            vue.getCanvas().getElements().remove(mur1EnCours);
            mur1EnCours = null;
        }
        if (mur2EnCours != null) {
            vue.getCanvas().getElements().remove(mur2EnCours);
            mur2EnCours = null;
        }
        etapeRectangle = 0;
        p1Rect = null;
        p2Rect = null;
        vue.getCanvas().redrawAll();
    }

    // =========================================================================
    // GESTION DES APPARTEMENTS
    // =========================================================================

    private void gererClicAppartement(Point2D snap) {
        double px = snap.getX(), py = snap.getY();

        // Vérifie qu'aucun appartement n'occupe déjà cette zone
        for (Appartement a : appartements) {
            if (pointDansPolygone(px, py, a.getPolygone())) {
                vue.setInstructions("Un appartement existe déjà dans cette zone.");
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
        List<Mur> mursDelimiteurs = new ArrayList<>();
        for (SegmentSource ss : cycle) {
            mursDelimiteurs.add(ss.mur);
        }
        mursDelimiteurs = ordonnerMurs(mursDelimiteurs);

        // Créer et afficher l'appartement
        compteurAppartements++;
        Appartement appart = new Appartement(mursDelimiteurs, 2.5);
        appartements.add(appart);
        vue.getCanvas().ajouterElement(appart);

        // Mettre à jour le TreeView
        TreeItem<String> itemAppart = new TreeItem<>(appart.toString());
        itemNiveau.getChildren().add(itemAppart);
        itemNiveau.setExpanded(true);
        mapItemAppartement.put(itemAppart, appart);

        vue.setInstructions(
                "« " + appart + " » créé — cliquez dans une autre zone pour en ajouter un.");
        vue.getCanvas().redrawAll();
    }

    /**
     * Ordonne les murs pour former une chaîne cohérente :
     * point2(mur_i) ≈ point1(mur_i+1).
     */
    private List<Mur> ordonnerMurs(List<Mur> murs) {
        if (murs.size() <= 1) return murs;

        List<Mur> ordonne  = new ArrayList<>();
        List<Mur> restants = new ArrayList<>(murs);

        Mur courant = restants.remove(0);
        ordonne.add(courant);

        while (!restants.isEmpty()) {
            Point dernierPoint = courant.getPoint2();
            boolean trouve = false;

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
                    // Sens inversé
                    Mur inverse = new Mur(
                            candidat.getPoint2(),
                            candidat.getPoint1()
                    );
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

    private boolean correspondA(Point p1, Point p2) {
        return Math.abs(p1.getX() - p2.getX()) < TOL
                && Math.abs(p1.getY() - p2.getY()) < TOL;
    }

    // =========================================================================
    // COLLECTE DES SEGMENTS — AVEC DÉDUPLICATION ✅
    // =========================================================================

    private List<SegmentSource> collecterSegmentsSources() {
        List<SegmentSource> sources = new ArrayList<>();

        // 1. Collecter tous les segments bruts (frontières + murs intérieurs)
        List<SegmentSource> bruts = new ArrayList<>();

        if (aireImmeuble != null && aireImmeuble.isComplete()) {
            Point p1 = aireImmeuble.getP1(), p2 = aireImmeuble.getP2();
            Point p3 = aireImmeuble.getP3(), p4 = aireImmeuble.getP4();
            bruts.add(new SegmentSource(p1, p2, new Mur(p1, p2)));
            bruts.add(new SegmentSource(p2, p3, new Mur(p2, p3)));
            bruts.add(new SegmentSource(p3, p4, new Mur(p3, p4)));
            bruts.add(new SegmentSource(p4, p1, new Mur(p4, p1)));
        }

        List<Mur> mursDelimiteurs = niveau.getMursDelimiteurs();
        for (Object el : vue.getCanvas().getElements()) {
            if (el instanceof Mur) {
                Mur m = (Mur) el;
                if (mursDelimiteurs.contains(m)) continue;
                bruts.add(new SegmentSource(m.getPoint1(), m.getPoint2(), m));
            }
        }

        // 2. Collecter tous les points de tous les segments
        List<double[]> tousLesPoints = new ArrayList<>();
        for (SegmentSource ss : bruts) {
            ajouterNoeudSiAbsent(tousLesPoints, ss.x1, ss.y1);
            ajouterNoeudSiAbsent(tousLesPoints, ss.x2, ss.y2);
        }

        // 3. Pour chaque segment, trouver les points intermédiaires qui tombent
        //    dessus et le subdiviser
        for (SegmentSource ss : bruts) {
            List<double[]> pointsSurSegment = new ArrayList<>();
            pointsSurSegment.add(new double[]{ss.x1, ss.y1});
            pointsSurSegment.add(new double[]{ss.x2, ss.y2});

            for (double[] pt : tousLesPoints) {
                if (pointSurSegment(pt[0], pt[1], ss.x1, ss.y1, ss.x2, ss.y2)) {
                    // Pas déjà une extrémité
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

            // Trier les points le long du segment (par paramètre t ∈ [0,1])
            double dx = ss.x2 - ss.x1, dy = ss.y2 - ss.y1;
            double len2 = dx * dx + dy * dy;
            pointsSurSegment.sort((a, b) -> {
                double ta = ((a[0] - ss.x1) * dx + (a[1] - ss.y1) * dy) / len2;
                double tb = ((b[0] - ss.x1) * dx + (b[1] - ss.y1) * dy) / len2;
                return Double.compare(ta, tb);
            });

            // Créer un sous-segment par paire consécutive
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

    /**
     * Vérifie si le point (px, py) est strictement sur le segment (x1,y1)→(x2,y2),
     * c'est-à-dire pas aux extrémités et colinéaire avec t ∈ (0, 1).
     */
    private boolean pointSurSegment(double px, double py,
                                    double x1, double y1,
                                    double x2, double y2) {
        // Produit vectoriel nul = colinéaire
        double cross = (px - x1) * (y2 - y1) - (py - y1) * (x2 - x1);
        if (Math.abs(cross) > TOL) return false;

        // Paramètre t le long du segment
        double dx = x2 - x1, dy = y2 - y1;
        double len2 = dx * dx + dy * dy;
        if (len2 < TOL) return false;

        double t = ((px - x1) * dx + (py - y1) * dy) / len2;
        return t > TOL && t < 1.0 - TOL; // strictement entre les deux extrémités
    }

    /**
     * ✅ Ajoute le segment seulement s'il n'existe pas déjà
     * (dans un sens ou dans l'autre).
     */
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

    // =========================================================================
    // ALGORITHME : CYCLE MINIMAL CONTENANT UN POINT
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

        // Liste d'adjacence : pour chaque nœud, liste de [voisin, idxSeg]
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

        // ✅ Trier les voisins de chaque nœud par angle (sens antihoraire)
        // C'est la clé de l'algorithme des faces planaires
        for (int i = 0; i < N; i++) {
            final double[] noeud = noeuds.get(i);
            adj.get(i).sort((a, b) -> {
            double ax = noeuds.get(a[0])[0] - noeud[0];
            double ay = noeuds.get(a[0])[1] - noeud[1];
            double bx = noeuds.get(b[0])[0] - noeud[0];
            double by = noeuds.get(b[0])[1] - noeud[1];
            double angleA = Math.atan2(-ay, ax);  // ← Y inversé = canvas JavaFX
            double angleB = Math.atan2(-by, bx);
            return Double.compare(angleA, angleB);
        });
        }

        // ✅ Énumérer toutes les faces par "Next Half-Edge"
        // Pour chaque demi-arête (i→j), la prochaine demi-arête de la face
        // est le prédécesseur de (j→i) dans la liste triée de j
        List<List<Integer>> faces = new ArrayList<>();
        Set<String> demiAretesVisitees = new HashSet<>();

        for (int i = 0; i < N; i++) {
            for (int[] arete : adj.get(i)) {
                int j = arete[0];
                String cle = i + "->" + j;
                if (demiAretesVisitees.contains(cle)) continue;

                // Parcourir la face en tournant toujours à gauche
                List<Integer> face = new ArrayList<>();
                int courant = i;
                int suivant = j;

                int maxIter = N + 2;
                while (maxIter-- > 0) {
                    String cleEtape = courant + "->" + suivant;
                    if (demiAretesVisitees.contains(cleEtape)) break;
                    demiAretesVisitees.add(cleEtape);
                    face.add(courant);

                    // Trouver le prédécesseur de (suivant→courant) dans adj[suivant]
                    List<int[]> voisinsSuivant = adj.get(suivant);
                    int idxCourantDansSuivant = -1;
                    for (int k = 0; k < voisinsSuivant.size(); k++) {
                        if (voisinsSuivant.get(k)[0] == courant) {
                            idxCourantDansSuivant = k;
                            break;
                        }
                    }
                    if (idxCourantDansSuivant == -1) break;

                    // Prédécesseur dans la liste triée (= tourner à droite depuis suivant)
                    int idxPred = (idxCourantDansSuivant + 1) % voisinsSuivant.size();
                    int prochainNoeud = voisinsSuivant.get(idxPred)[0];

                    courant = suivant;
                    suivant = prochainNoeud;

                    if (suivant == i && courant == j) {
                        // On a bouclé — face terminée mais déjà ajoutée
                        break;
                    }
                    if (courant == i) {
                        // Retour au départ — face complète
                        faces.add(new ArrayList<>(face));
                        break;
                    }
                }
            }
        }

        // ✅ Parmi toutes les faces, trouver la plus petite contenant (px,py)
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

                // Reconstruire les SegmentSource du cycle
                meilleur = new ArrayList<>();
                for (int k = 0; k < face.size(); k++) {
                    int nA = face.get(k);
                    int nB = face.get((k + 1) % face.size());
                    // Trouver le segment correspondant
                    for (int[] ar : adj.get(nA)) {
                        if (ar[0] == nB) {
                            meilleur.add(sources.get(ar[1]));
                            break;
                        }
                    }
                }
            }
        }

        return meilleur;
    }

    private void dfsCycles(int depart, int courant, int parentNoeud,
                           List<Integer> chemin, List<Integer> cheminSeg,
                           List<List<int[]>> adj,
                           List<List<Integer>> tousChemins,
                           List<List<Integer>> tousCheminSegs,
                           int N) {

        if (chemin.size() > N) return;

        for (int[] arete : adj.get(courant)) {
            int voisin = arete[0];
            int idxSeg = arete[1];

            // ✅ Bloquer uniquement le nœud immédiatement précédent
            // (pas le segment, qui peut avoir des index ambigus)
            if (voisin == parentNoeud) continue;

            if (voisin == depart && chemin.size() >= 3) {
                tousChemins.add(new ArrayList<>(chemin));
                tousCheminSegs.add(new ArrayList<>(cheminSeg));
                continue;
            }

            if (!chemin.contains(voisin) && voisin > depart) {
                chemin.add(voisin);
                cheminSeg.add(idxSeg);
                dfsCycles(depart, voisin, courant, // ✅ passer courant comme parentNoeud
                        chemin, cheminSeg,
                        adj, tousChemins, tousCheminSegs, N);
                chemin.remove(chemin.size() - 1);
                cheminSeg.remove(cheminSeg.size() - 1);
            }
        }
    }

    // =========================================================================
    // UTILITAIRES GÉOMÉTRIQUES
    // =========================================================================

    private void ajouterNoeudSiAbsent(List<double[]> noeuds,
                                      double x, double y) {
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
            double ax = a.getX(), ay = a.getY(),
                    bx = b.getX(), by = b.getY();
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
    
    private Point calculerPointOrthogonal(Point centre, Point cible) {
        double dx = p2Rect.getX() - p1Rect.getX();
        double dy = p2Rect.getY() - p1Rect.getY();
        double perpX = -dy, perpY = dx;
        double ux = cible.getX() - centre.getX();
        double uy = cible.getY() - centre.getY();
        double s = (ux*perpX + uy*perpY) / (perpX*perpX + perpY*perpY);
        return new Point(centre.getX() + s*perpX, centre.getY() + s*perpY);
    }

    // =========================================================================
    // CONTOUR DE L'AIRE (lecture seule)
    // =========================================================================

    private Dessin creerContourAire() {
        return new Dessin() {
            @Override
            public void dessiner(GraphicsContext gc) {
                Point p1 = aireImmeuble.getP1(), p2 = aireImmeuble.getP2();
                Point p3 = aireImmeuble.getP3(), p4 = aireImmeuble.getP4();

                double[] xs = {p1.getX(), p2.getX(), p3.getX(), p4.getX()};
                double[] ys = {p1.getY(), p2.getY(), p3.getY(), p4.getY()};

                gc.setFill(Color.web("#4a90d9", 0.05));
                gc.fillPolygon(xs, ys, 4);

                gc.setStroke(Color.web("#999999", 0.7));
                gc.setLineWidth(0.05);
                gc.setLineDashes(0.2, 0.15);
                gc.strokePolygon(xs, ys, 4);
                gc.setLineDashes();
            }

            @Override public Color getColor() { return Color.GRAY; }
            @Override public void setColor(Color c) { }
        };
    }

    // =========================================================================
    // CONTRÔLE DU MODE
    // =========================================================================

    public void activerModeMur() {
        annulerMurEnCours();
        mode = "MUR";
        vue.getCanvas().setPanActif(false);
        vue.getOptionsMurVue().setVisible(true);   // ← ajout
        vue.setInstructions("Cliquez pour poser le premier point du mur");
    }

    public void activerModeAppartement() {
        annulerMurEnCours();
        mode = "APPARTEMENT";
        vue.getCanvas().setPanActif(false);
        vue.getOptionsMurVue().setVisible(false);  // ← ajout
        vue.setInstructions("Cliquez à l'intérieur d'une zone fermée pour créer un appartement");
    }

    public void activerModeNavigation() {
        annulerMurEnCours();
        mode = "AUCUN";
        vue.getCanvas().setPanActif(true);
        vue.getOptionsMurVue().setVisible(false);  // ← ajout
        vue.setInstructions("Navigation — molette pour zoomer, clic droit pour déplacer");
    }

    // =========================================================================
    // GETTERS
    // =========================================================================

    public NiveauView getVue()  { return vue; }
    public Niveau getNiveau()   { return niveau; }
    public Map<TreeItem<String>, Appartement> getMapItemAppartement() {
        return mapItemAppartement;
    }

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
    
    
}