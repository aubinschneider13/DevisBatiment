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
    private Mur murEnCours = null;

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

        vue.setFocusTraversable(true);
        vue.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                annulerMurEnCours();
            }
        });
    }

    private void clicCanvas(javafx.scene.input.MouseEvent e) {
        Point2D snap = vue.getCanvas().snapToGrid(e.getX(), e.getY());
        switch (mode) {
            case "MUR":         gererClicMur(snap);         break;
            case "APPARTEMENT": gererClicAppartement(snap); break;
        }
    }

    private void mouvementCanvas(javafx.scene.input.MouseEvent e) {
        if (mode.equals("MUR") && murEnCours != null) {
            Point2D snap = vue.getCanvas().snapToGrid(e.getX(), e.getY());
            murEnCours.setPoint2(new Point(snap.getX(), snap.getY()));
            vue.getCanvas().redrawAll();
        }
    }

    // =========================================================================
    // GESTION DES MURS
    // =========================================================================

    private void gererClicMur(Point2D snap) {
        Point pClic = new Point(snap.getX(), snap.getY());
        if (murEnCours == null) {
            murEnCours = new Mur(pClic, pClic);
            vue.getCanvas().ajouterElement(murEnCours);
            vue.setInstructions(
                    "Cliquez pour poser l'extrémité du mur — Échap pour annuler");
        } else {
            murEnCours.setPoint2(pClic);
            murEnCours = null;
            vue.setInstructions("Cliquez pour démarrer un nouveau mur");
        }
        vue.getCanvas().redrawAll();
    }

    public void annulerMurEnCours() {
        if (murEnCours != null) {
            vue.getCanvas().getElements().remove(murEnCours);
            murEnCours = null;
            vue.getCanvas().redrawAll();
        }
    }

    // =========================================================================
    // GESTION DES APPARTEMENTS
    // =========================================================================

    private void gererClicAppartement(Point2D snap) {
        double px = snap.getX(), py = snap.getY();

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

        // Extraire les Mur depuis les SegmentSource
        List<Mur> mursDelimiteurs = new ArrayList<>();
        for (SegmentSource ss : cycle) {
            mursDelimiteurs.add(ss.mur);
        }

        // ✅ Ordonner pour que point2(i) ≈ point1(i+1)
        mursDelimiteurs = ordonnerMurs(mursDelimiteurs);

        compteurAppartements++;
        Appartement appart = new Appartement(mursDelimiteurs, 2.5);
        appartements.add(appart);
        vue.getCanvas().ajouterElement(appart);

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

        // Frontières de l'aire
        if (aireImmeuble != null && aireImmeuble.isComplete()) {
            Point p1 = aireImmeuble.getP1(), p2 = aireImmeuble.getP2();
            Point p3 = aireImmeuble.getP3(), p4 = aireImmeuble.getP4();
            ajouterSegmentSiAbsent(sources, p1, p2, new Mur(p1, p2));
            ajouterSegmentSiAbsent(sources, p2, p3, new Mur(p2, p3));
            ajouterSegmentSiAbsent(sources, p3, p4, new Mur(p3, p4));
            ajouterSegmentSiAbsent(sources, p4, p1, new Mur(p4, p1));
        }

        // Murs du canvas (murs intérieurs + murs délimiteurs du niveau)
        for (Object el : vue.getCanvas().getElements()) {
            if (el instanceof Mur) {
                Mur m = (Mur) el;
                ajouterSegmentSiAbsent(
                        sources, m.getPoint1(), m.getPoint2(), m);
            }
        }
        return sources;
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

        List<List<Integer>> tousChemins    = new ArrayList<>();
        List<List<Integer>> tousCheminSegs = new ArrayList<>();

        for (int depart = 0; depart < N; depart++) {
            List<Integer> chemin    = new ArrayList<>();
            List<Integer> cheminSeg = new ArrayList<>();
            chemin.add(depart);
            dfsCycles(depart, depart, -1, chemin, cheminSeg,
                    adj, tousChemins, tousCheminSegs, N);
        }

        List<SegmentSource> meilleur      = null;
        double              meilleureAire = Double.MAX_VALUE;

        for (int c = 0; c < tousChemins.size(); c++) {
            List<Integer> chemin    = tousChemins.get(c);
            List<Integer> cheminSeg = tousCheminSegs.get(c);
            if (chemin.size() < 3) continue;

            List<Point> polygone = new ArrayList<>();
            for (int idx : chemin) {
                polygone.add(
                        new Point(noeuds.get(idx)[0], noeuds.get(idx)[1]));
            }

            if (!pointDansPolygone(px, py, polygone)) continue;

            double aire = Math.abs(calculerAire(polygone));
            if (aire < meilleureAire) {
                meilleureAire = aire;
                meilleur = new ArrayList<>();
                for (int si : cheminSeg) meilleur.add(sources.get(si));
            }
        }

        return meilleur;
    }

    private void dfsCycles(int depart, int courant, int parentSeg,
                           List<Integer> chemin, List<Integer> cheminSeg,
                           List<List<int[]>> adj,
                           List<List<Integer>> tousChemins,
                           List<List<Integer>> tousCheminSegs,
                           int N) {

        if (chemin.size() > N) return;

        for (int[] arete : adj.get(courant)) {
            int voisin = arete[0];
            int idxSeg = arete[1];

            if (idxSeg == parentSeg) continue;

            if (voisin == depart && chemin.size() >= 3) {
                tousChemins.add(new ArrayList<>(chemin));
                tousCheminSegs.add(new ArrayList<>(cheminSeg));
                continue;
            }

            if (!chemin.contains(voisin) && voisin > depart) {
                chemin.add(voisin);
                cheminSeg.add(idxSeg);
                dfsCycles(depart, voisin, idxSeg, chemin, cheminSeg,
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
        vue.setInstructions("Cliquez pour poser le premier point du mur");
    }

    public void activerModeAppartement() {
        annulerMurEnCours();
        mode = "APPARTEMENT";
        vue.getCanvas().setPanActif(false);
        vue.setInstructions(
                "Cliquez à l'intérieur d'une zone fermée pour créer un appartement");
    }

    public void activerModeNavigation() {
        annulerMurEnCours();
        mode = "AUCUN";
        vue.getCanvas().setPanActif(true);
        vue.setInstructions(
                "Navigation — molette pour zoomer, clic droit pour déplacer");
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