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

    private String mode = "AUCUN";
    private Mur murEnCours = null;

    private final List<Appartement> appartements = new ArrayList<>();
    private int compteurAppartements = 0;

    // Tolérance zéro : le snap garantit des coordonnées exactes sur la grille
    private static final double TOL = 1e-9;

    public NiveauControleur(NiveauView vue, AireImmeuble aireImmeuble,
                             TreeItem<String> itemNiveau) {
        this.vue = vue;
        this.aireImmeuble = aireImmeuble;
        this.itemNiveau = itemNiveau;

        // Affiche les limites de l'aire en fond (lecture seule, non interactif)
        if (aireImmeuble != null && aireImmeuble.isComplete()) {
            vue.getCanvas().ajouterElement(creerContourAire());
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
        for (Appartement a : appartements) {
            if (pointDansPolygone(px, py, a.getPolygone())) {
                vue.setInstructions("Un appartement existe déjà dans cette zone.");
                return;
            }
        }

        // Collecte tous les segments (aire + murs du canvas)
        List<double[]> segments = collecterSegments();

        // Cherche le cycle fermé minimal contenant le point cliqué
        List<Point> polygone = trouverCycleMinimal(px, py, segments);

        if (polygone == null || polygone.size() < 3) {
            vue.setInstructions(
                "Aucune zone fermée ici — vérifiez que les murs se rejoignent bien.");
            return;
        }

        // Crée l'appartement avec son polygone visuel
        compteurAppartements++;
        Appartement appart = new Appartement(0, 2.5);
        appart.setPolygone(polygone);
        appartements.add(appart);
        vue.getCanvas().ajouterElement(appart);

        // Nœud enfant dans le TreeView
        TreeItem<String> itemAppart = new TreeItem<>(appart.toString());
        itemNiveau.getChildren().add(itemAppart);
        itemNiveau.setExpanded(true);

        vue.setInstructions(
            "« " + appart + " » créé — cliquez dans une autre zone pour en ajouter un.");
    }

    // =========================================================================
    // COLLECTE DES SEGMENTS
    // =========================================================================

    /**
     * Collecte tous les segments disponibles :
     * — les 4 côtés de l'AireImmeuble (frontières extérieures)
     * — tous les Mur du canvas (frontières intérieures)
     * Retourne une liste de [x1, y1, x2, y2].
     */
    private List<double[]> collecterSegments() {
        List<double[]> segments = new ArrayList<>();

        if (aireImmeuble != null && aireImmeuble.isComplete()) {
            Point p1 = aireImmeuble.getP1(), p2 = aireImmeuble.getP2();
            Point p3 = aireImmeuble.getP3(), p4 = aireImmeuble.getP4();
            segments.add(new double[]{p1.getX(), p1.getY(), p2.getX(), p2.getY()});
            segments.add(new double[]{p2.getX(), p2.getY(), p3.getX(), p3.getY()});
            segments.add(new double[]{p3.getX(), p3.getY(), p4.getX(), p4.getY()});
            segments.add(new double[]{p4.getX(), p4.getY(), p1.getX(), p1.getY()});
        }

        for (Object el : vue.getCanvas().getElements()) {
            if (el instanceof Mur) {
                Mur m = (Mur) el;
                segments.add(new double[]{
                    m.getPoint1().getX(), m.getPoint1().getY(),
                    m.getPoint2().getX(), m.getPoint2().getY()
                });
            }
        }
        return segments;
    }

    // =========================================================================
    // ALGORITHME : CYCLE MINIMAL CONTENANT UN POINT
    //
    // Étapes :
    //   1. Construire un graphe de nœuds (extrémités uniques) + arêtes (segments)
    //   2. Pour chaque nœud du graphe, DFS pour trouver tous les cycles simples
    //   3. Parmi les cycles dont l'intérieur contient (px,py), garder le plus
    //      petit en surface (= zone minimale encadrante)
    // =========================================================================

    private List<Point> trouverCycleMinimal(double px, double py,
                                             List<double[]> segments) {
        // --- 1. Nœuds uniques ---
        List<double[]> noeuds = new ArrayList<>();
        for (double[] s : segments) {
            ajouterNoeudSiAbsent(noeuds, s[0], s[1]);
            ajouterNoeudSiAbsent(noeuds, s[2], s[3]);
        }
        int N = noeuds.size();
        if (N < 3) return null;

        // --- 2. Liste d'adjacence ---
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < N; i++) adj.add(new ArrayList<>());

        for (double[] s : segments) {
            int i = indexNoeud(noeuds, s[0], s[1]);
            int j = indexNoeud(noeuds, s[2], s[3]);
            if (i == -1 || j == -1 || i == j) continue;
            if (!adj.get(i).contains(j)) adj.get(i).add(j);
            if (!adj.get(j).contains(i)) adj.get(j).add(i);
        }

        // --- 3. DFS depuis chaque nœud → collecte tous les cycles simples ---
        List<List<Integer>> tousLesCycles = new ArrayList<>();
        boolean[] visite = new boolean[N];

        for (int depart = 0; depart < N; depart++) {
            List<Integer> chemin = new ArrayList<>();
            chemin.add(depart);
            dfsCycles(depart, depart, -1, chemin, adj, tousLesCycles, visite, N);
            // On ne marque pas visite[depart] = true car un nœud peut appartenir
            // à plusieurs cycles différents
        }

        // --- 4. Sélection du cycle minimal contenant (px, py) ---
        List<Point> meilleur = null;
        double meilleureAire = Double.MAX_VALUE;

        for (List<Integer> cycle : tousLesCycles) {
            if (cycle.size() < 3) continue;

            List<Point> polygone = new ArrayList<>();
            for (int idx : cycle) {
                polygone.add(new Point(noeuds.get(idx)[0], noeuds.get(idx)[1]));
            }

            if (!pointDansPolygone(px, py, polygone)) continue;

            double aire = Math.abs(calculerAire(polygone));
            if (aire < meilleureAire) {
                meilleureAire = aire;
                meilleur = polygone;
            }
        }

        return meilleur;
    }

    /**
     * DFS récursif pour trouver les cycles simples.
     *
     * Règle de déduplication : on n'explore un voisin que si son index
     * est > depart (évite d'énumérer A→B→C→A et A→C→B→A comme deux cycles).
     * On revient au nœud de départ uniquement quand le chemin a ≥ 3 sommets.
     */
    private void dfsCycles(int depart, int courant, int parent,
                            List<Integer> chemin,
                            List<List<Integer>> adj,
                            List<List<Integer>> cycles,
                            boolean[] visite, int N) {

        // Limite la profondeur pour éviter une explosion combinatoire
        if (chemin.size() > N) return;

        for (int voisin : adj.get(courant)) {
            if (voisin == parent) continue; // ne pas rebrousser chemin immédiatement

            if (voisin == depart && chemin.size() >= 3) {
                // Cycle fermé trouvé — on l'enregistre
                cycles.add(new ArrayList<>(chemin));
                continue;
            }

            // N'explore les nœuds non visités et d'index > depart
            // (garantit qu'on ne génère pas deux fois le même cycle)
            if (!chemin.contains(voisin) && voisin > depart) {
                chemin.add(voisin);
                dfsCycles(depart, voisin, courant, chemin, adj, cycles, visite, N);
                chemin.remove(chemin.size() - 1);
            }
        }
    }

    // =========================================================================
    // UTILITAIRES GÉOMÉTRIQUES
    // =========================================================================

    /** Ajoute [x,y] dans la liste si aucun nœud existant n'est à TOL près. */
    private void ajouterNoeudSiAbsent(List<double[]> noeuds, double x, double y) {
        for (double[] n : noeuds) {
            if (Math.abs(n[0] - x) < TOL && Math.abs(n[1] - y) < TOL) return;
        }
        noeuds.add(new double[]{x, y});
    }

    /** Retourne l'index du nœud correspondant à (x,y), ou -1 si absent. */
    private int indexNoeud(List<double[]> noeuds, double x, double y) {
        for (int i = 0; i < noeuds.size(); i++) {
            double[] n = noeuds.get(i);
            if (Math.abs(n[0] - x) < TOL && Math.abs(n[1] - y) < TOL) return i;
        }
        return -1;
    }

    /**
     * Ray casting : retourne true si (px,py) est strictement à l'intérieur
     * du polygone (liste ordonnée de sommets).
     */
    private boolean pointDansPolygone(double px, double py, List<Point> poly) {
        if (poly == null || poly.size() < 3) return false;
        int n = poly.size(), intersections = 0;
        for (int i = 0; i < n; i++) {
            Point a = poly.get(i);
            Point b = poly.get((i + 1) % n);
            double ax = a.getX(), ay = a.getY(), bx = b.getX(), by = b.getY();
            // Rayon horizontal vers +∞
            if ((ay <= py && by > py) || (by <= py && ay > py)) {
                double xInter = ax + (py - ay) / (by - ay) * (bx - ax);
                if (px < xInter) intersections++;
            }
        }
        return (intersections % 2) == 1;
    }

    /**
     * Formule du lacet (shoelace) pour l'aire signée d'un polygone.
     * Signe positif = sens antihoraire, négatif = sens horaire.
     */
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

                // Remet le trait plein pour les éléments dessinés après
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
        vue.setInstructions(
            "Cliquez à l'intérieur d'une zone fermée pour créer un appartement");
    }

    /** Active le mode navigation (pan uniquement, aucun dessin). */
    public void activerModeNavigation() {
        annulerMurEnCours();
        mode = "AUCUN";
        vue.getCanvas().setPanActif(true);
        vue.setInstructions("Navigation — molette pour zoomer, clic droit pour déplacer");
    }

    public NiveauView getVue() { return vue; }
}