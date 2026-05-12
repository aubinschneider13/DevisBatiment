package insa.aubin.devisbatiment.controlleur;

import insa.aubin.devisbatiment.modele.*;
import insa.aubin.devisbatiment.view.NiveauView;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.TreeItem;
import javafx.scene.paint.Color;

import java.util.*;

/**
 * Contrôleur d'un niveau (RDC, Niveau 1, …).
 * Gère le dessin de murs, la détection d'appartements par graphe de cycles,
 * et la mise à jour du TreeItem correspondant dans le navigateur.
 *
 * Modes : "AUCUN", "MUR", "APPARTEMENT".
 */
public class NiveauControleur {

    private final NiveauView vue;
    private final AireImmeuble aireImmeuble;
    private final TreeItem<String> itemNiveau;
    private final Niveau niveau; // Modèle métier de ce niveau
    
    // Association TreeItem ↔ Appartement (ajout du collègue conservé)
    private final Map<TreeItem<String>, Appartement> mapItemAppartement = new HashMap<>();

    private String mode = "AUCUN";
    private Mur murEnCours = null;

    // Tolérance zéro : le snap garantit des coordonnées exactes sur la grille
    private static final double TOL = 1e-9;

    public NiveauControleur(NiveauView vue, AireImmeuble aireImmeuble,
                             TreeItem<String> itemNiveau, Niveau niveau) {
        this.vue          = vue;
        this.aireImmeuble = aireImmeuble;
        this.itemNiveau   = itemNiveau;
        this.niveau       = niveau;

        // Affiche les limites de l'aire en fond (lecture seule, non interactif)
        if (aireImmeuble != null && aireImmeuble.isComplete()) {
            vue.getCanvas().ajouterElement(creerContourAire());
        }

        // Ajoute les 4 murs délimiteurs du niveau dans le canvas
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

        // Échap : annule le mur en cours
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
        // Aperçu du mur en cours (suit la souris avant le 2e clic)
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
            // 1er clic : pose le point de départ
            murEnCours = new Mur(pClic, pClic);
            vue.getCanvas().ajouterElement(murEnCours);
            vue.setInstructions("Cliquez pour poser l'extrémité du mur — Échap pour annuler");
        } else {
            // 2e clic : finalise le mur
            murEnCours.setPoint2(pClic);
            murEnCours = null;
            vue.setInstructions("Cliquez pour démarrer un nouveau mur");
        }
        vue.getCanvas().redrawAll();
    }

    /** Annule le mur en cours de tracé (Échap ou changement de mode). */
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

        // Vérifie qu'aucun appartement n'occupe déjà cette zone
        for (Appartement a : niveau.getAppartements()) {
            if (pointDansPolygone(px, py, a.getPolygone())) {
                vue.setInstructions("Un appartement existe déjà dans cette zone.");
                return;
            }
        }

        // Collecte tous les segments avec leur Mur source
        List<SegmentSource> segmentsSources = collecterSegmentsSources();

        // Cherche le cycle fermé minimal contenant le point cliqué
        List<SegmentSource> cycleSources = trouverCycleMinimal(px, py, segmentsSources);

        if (cycleSources == null || cycleSources.size() < 3) {
            vue.setInstructions(
                "Aucune zone fermée ici — vérifiez que les murs se rejoignent bien.");
            return;
        }

        // Reconstruit la liste de Mur ordonnés pour l'appartement
        List<Mur> mursDelimiteurs = new ArrayList<>();
        for (SegmentSource ss : cycleSources) {
            mursDelimiteurs.add(ss.mur);
        }

        // Crée l'appartement via le modèle Niveau (qui gère la liste interne)
        Appartement appart = niveau.ajouterAppartement(mursDelimiteurs);
        vue.getCanvas().ajouterElement(appart);

        // Nœud enfant dans le TreeView
        TreeItem<String> itemAppart = new TreeItem<>(appart.toString());
        itemNiveau.getChildren().add(itemAppart);
        itemNiveau.setExpanded(true);

        // Conservation de la map TreeItem ↔ Appartement (ajout du collègue)
        mapItemAppartement.put(itemAppart, appart);

        vue.setInstructions(
            "« " + appart + " » créé — cliquez dans une autre zone pour en ajouter un.");
    }

    // =========================================================================
    // COLLECTE DES SEGMENTS AVEC SOURCE
    // =========================================================================

    /**
     * Collecte tous les segments disponibles en associant chacun à son Mur source.
     * — Côtés de l'aire → Mur synthétiques (non ajoutés au canvas)
     * — Murs du canvas → objets Mur réels
     */
    private List<SegmentSource> collecterSegmentsSources() {
        List<SegmentSource> sources = new ArrayList<>();

        // Frontières de l'aire (murs synthétiques — servent de limites extérieures)
        if (aireImmeuble != null && aireImmeuble.isComplete()) {
            Point p1 = aireImmeuble.getP1(), p2 = aireImmeuble.getP2();
            Point p3 = aireImmeuble.getP3(), p4 = aireImmeuble.getP4();
            sources.add(new SegmentSource(p1, p2, new Mur(p1, p2)));
            sources.add(new SegmentSource(p2, p3, new Mur(p2, p3)));
            sources.add(new SegmentSource(p3, p4, new Mur(p3, p4)));
            sources.add(new SegmentSource(p4, p1, new Mur(p4, p1)));
        }

        // Murs dessinés dans le canvas (murs intérieurs + murs délimiteurs du niveau)
        for (Object el : vue.getCanvas().getElements()) {
            if (el instanceof Mur) {
                Mur m = (Mur) el;
                sources.add(new SegmentSource(m.getPoint1(), m.getPoint2(), m));
            }
        }
        return sources;
    }

    // =========================================================================
    // ALGORITHME : CYCLE MINIMAL CONTENANT UN POINT
    //
    // 1. Construire un graphe nœuds/arêtes depuis les segments
    // 2. DFS depuis chaque nœud pour énumérer tous les cycles simples,
    //    en traçant en parallèle les segments parcourus
    // 3. Sélectionner le cycle de surface minimale contenant (px,py)
    // =========================================================================

    private List<SegmentSource> trouverCycleMinimal(double px, double py,
                                                     List<SegmentSource> sources) {
        // --- 1. Nœuds uniques ---
        List<double[]> noeuds = new ArrayList<>();
        for (SegmentSource ss : sources) {
            ajouterNoeudSiAbsent(noeuds, ss.x1, ss.y1);
            ajouterNoeudSiAbsent(noeuds, ss.x2, ss.y2);
        }
        int N = noeuds.size();
        if (N < 3) return null;

        // --- 2. Liste d'adjacence : index nœud → liste de [index voisin, index segment] ---
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

        // --- 3. DFS depuis chaque nœud ---
        List<List<Integer>> tousChemins    = new ArrayList<>();
        List<List<Integer>> tousCheminSegs = new ArrayList<>();

        for (int depart = 0; depart < N; depart++) {
            List<Integer> chemin    = new ArrayList<>();
            List<Integer> cheminSeg = new ArrayList<>();
            chemin.add(depart);
            dfsCycles(depart, depart, -1, chemin, cheminSeg,
                      adj, tousChemins, tousCheminSegs, N);
        }

        // --- 4. Sélection du cycle minimal contenant (px,py) ---
        List<SegmentSource> meilleur = null;
        double meilleureAire = Double.MAX_VALUE;

        for (int c = 0; c < tousChemins.size(); c++) {
            List<Integer> chemin    = tousChemins.get(c);
            List<Integer> cheminSeg = tousCheminSegs.get(c);
            if (chemin.size() < 3) continue;

            List<Point> polygone = new ArrayList<>();
            for (int idx : chemin) {
                polygone.add(new Point(noeuds.get(idx)[0], noeuds.get(idx)[1]));
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

    /**
     * DFS récursif pour énumérer les cycles simples.
     * Trace en parallèle le chemin de nœuds ET les index de segments,
     * ce qui permet de retrouver les SegmentSource exacts du cycle.
     *
     * Déduplication : on n'explore que les voisins d'index > depart,
     * ce qui évite d'énumérer le même cycle dans les deux sens.
     */
    private void dfsCycles(int depart, int courant, int parentSeg,
                            List<Integer> chemin, List<Integer> cheminSeg,
                            List<List<int[]>> adj,
                            List<List<Integer>> tousChemins,
                            List<List<Integer>> tousCheminSegs,
                            int N) {

        if (chemin.size() > N) return; // borne de sécurité anti-explosion

        for (int[] arete : adj.get(courant)) {
            int voisin = arete[0];
            int idxSeg = arete[1];

            // Ne pas rebrousser immédiatement sur le segment qu'on vient de parcourir
            if (idxSeg == parentSeg) continue;

            if (voisin == depart && chemin.size() >= 3) {
                // Cycle fermé valide — on l'enregistre
                tousChemins.add(new ArrayList<>(chemin));
                tousCheminSegs.add(new ArrayList<>(cheminSeg));
                continue;
            }

            // N'explore que les nœuds non encore dans le chemin et d'index > depart
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

    /** Ray casting : true si (px,py) est strictement à l'intérieur du polygone. */
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

    /** Formule du lacet (shoelace) pour l'aire signée d'un polygone. */
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
    // CONTOUR DE L'AIRE (lecture seule dans le canvas du niveau)
    // =========================================================================

    /**
     * Crée un Dessin anonyme affichant les limites de l'AireImmeuble
     * en pointillés grisés — non interactif, juste une référence visuelle.
     */
    private Dessin creerContourAire() {
        return new Dessin() {
            @Override
            public void dessiner(GraphicsContext gc) {
                Point p1 = aireImmeuble.getP1(), p2 = aireImmeuble.getP2();
                Point p3 = aireImmeuble.getP3(), p4 = aireImmeuble.getP4();

                double[] xs = {p1.getX(), p2.getX(), p3.getX(), p4.getX()};
                double[] ys = {p1.getY(), p2.getY(), p3.getY(), p4.getY()};

                // Fond très légèrement bleuté pour rappeler l'aire de l'immeuble
                gc.setFill(Color.web("#4a90d9", 0.05));
                gc.fillPolygon(xs, ys, 4);

                // Contour en pointillés gris — non interactif
                gc.setStroke(Color.web("#999999", 0.7));
                gc.setLineWidth(0.05);
                gc.setLineDashes(0.2, 0.15);
                gc.strokePolygon(xs, ys, 4);

                // Remet le trait plein pour les éléments suivants
                gc.setLineDashes();
            }

            @Override public Color getColor() { return Color.GRAY; }
            @Override public void setColor(Color c) { /* lecture seule */ }
        };
    }

    // =========================================================================
    // CONTRÔLE DU MODE (appelé depuis ImmeubleControleur)
    // =========================================================================

    /** Active le mode MUR. */
    public void activerModeMur() {
        annulerMurEnCours();
        mode = "MUR";
        vue.getCanvas().setPanActif(false);
        vue.setInstructions("Cliquez pour poser le premier point du mur");
    }

    /** Active le mode APPARTEMENT. */
    public void activerModeAppartement() {
        annulerMurEnCours();
        mode = "APPARTEMENT";
        vue.getCanvas().setPanActif(false);
        vue.setInstructions("Cliquez à l'intérieur d'une zone fermée pour créer un appartement");
    }

    /** Active le mode navigation (pan uniquement, aucun dessin). */
    public void activerModeNavigation() {
        annulerMurEnCours();
        mode = "AUCUN";
        vue.getCanvas().setPanActif(true);
        vue.setInstructions("Navigation — molette pour zoomer, clic droit pour déplacer");
    }

    public NiveauView getVue()                                          { return vue; }
    public Niveau getNiveau()                                           { return niveau; }
    public Map<TreeItem<String>, Appartement> getMapItemAppartement()   { return mapItemAppartement; }

    // =========================================================================
    // CLASSE INTERNE : association segment ↔ Mur source
    // =========================================================================

    /**
     * Associe un segment géométrique [x1,y1]-[x2,y2] à son Mur source.
     * Pour les côtés de l'aire, le Mur est synthétique (créé à la volée,
     * non présent dans le canvas).
     */
    private static class SegmentSource {
        final double x1, y1, x2, y2;
        final Mur mur;

        SegmentSource(Point a, Point b, Mur mur) {
            this.x1  = a.getX(); this.y1 = a.getY();
            this.x2  = b.getX(); this.y2 = b.getY();
            this.mur = mur;
        }
    }
} 