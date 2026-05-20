package insa.aubin.devisbatiment.modele;

import java.util.*;

/**
 * Utilitaires géométriques partagés entre NiveauControleur et PieceControleur.
 *
 * Centralise :
 *   - la détection de point dans polygone / sur contour
 *   - la collecte et subdivision des segments (pour la détection de zones fermées)
 *   - l'algorithme de cycle minimal (faces planaires)
 *   - l'ordonnancement des murs
 *   - les opérations de base sur les nœuds
 */
public class GeometrieUtils {

    public static final double TOL = 1e-6;

    // =========================================================================
    // POINT DANS POLYGONE / SUR CONTOUR
    // =========================================================================

    public static boolean pointDansPolygone(double px, double py, List<Point> poly) {
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

    public static boolean pointSurContour(double px, double py, List<Point> polygone) {
        if (polygone == null) return false;
        int n = polygone.size();
        for (int i = 0; i < n; i++) {
            Point a = polygone.get(i);
            Point b = polygone.get((i + 1) % n);
            double dx = b.getX() - a.getX(), dy = b.getY() - a.getY();
            double len2 = dx * dx + dy * dy;
            if (len2 < 1e-10) continue;
            double t = ((px - a.getX()) * dx + (py - a.getY()) * dy) / len2;
            if (t < -TOL || t > 1 + TOL) continue;
            double cx = a.getX() + t * dx, cy = a.getY() + t * dy;
            if (Math.hypot(px - cx, py - cy) < TOL) return true;
        }
        return false;
    }

    public static boolean estDansZone(double px, double py, List<Point> polygone) {
        if (polygone == null) return true;
        return pointDansPolygone(px, py, polygone)
            || pointSurContour(px, py, polygone);
    }

    // =========================================================================
    // SEGMENTS
    // =========================================================================

    public static boolean pointSurSegment(double px, double py,
                                          double x1, double y1,
                                          double x2, double y2) {
        double cross = (px - x1) * (y2 - y1) - (py - y1) * (x2 - x1);
        if (Math.abs(cross) > TOL) return false;
        double dx = x2 - x1, dy = y2 - y1;
        double len2 = dx * dx + dy * dy;
        if (len2 < TOL) return false;
        double t = ((px - x1) * dx + (py - y1) * dy) / len2;
        return t >= -TOL && t <= 1.0 + TOL;
    }

    public static boolean correspondA(Point p1, Point p2) {
        return Math.abs(p1.getX() - p2.getX()) < TOL
            && Math.abs(p1.getY() - p2.getY()) < TOL;
    }

    // =========================================================================
    // NŒUDS
    // =========================================================================

    public static void ajouterNoeudSiAbsent(List<double[]> noeuds, double x, double y) {
        for (double[] n : noeuds) {
            if (Math.abs(n[0] - x) < TOL && Math.abs(n[1] - y) < TOL) return;
        }
        noeuds.add(new double[]{x, y});
    }

    public static int indexNoeud(List<double[]> noeuds, double x, double y) {
        for (int i = 0; i < noeuds.size(); i++) {
            double[] n = noeuds.get(i);
            if (Math.abs(n[0] - x) < TOL && Math.abs(n[1] - y) < TOL) return i;
        }
        return -1;
    }

    // =========================================================================
    // AIRE ET ORDONNANCEMENT
    // =========================================================================

    public static double calculerAire(List<Point> poly) {
        if (poly == null || poly.isEmpty()) return 0.0;
        double aire = 0;
        int n = poly.size();
        for (int i = 0; i < n; i++) {
            Point a = poly.get(i);
            Point b = poly.get((i + 1) % n);
            aire += a.getX() * b.getY() - b.getX() * a.getY();
        }
        return aire / 2.0;
    }

    public static List<Mur> ordonnerMurs(List<Mur> murs) {
        if (murs == null || murs.isEmpty()) return new ArrayList<>();
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
                if (candidat == null) continue;
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
    // COLLECTE ET SUBDIVISION DES SEGMENTS
    // =========================================================================

    /**
     * Collecte une liste de segments bruts, les subdivise aux intersections,
     * et retourne la liste déduplicée de SegmentSource.
     *
     * @param bruts liste de segments bruts (non subdivisés)
     * @return segments subdivisés et dédupliqués
     */
    public static List<SegmentSource> subdiviserEtDeduplicer(List<SegmentSource> bruts) {
        if (bruts == null) return new ArrayList<>();
        List<SegmentSource> sources = new ArrayList<>();

        // Collecter tous les nœuds
        List<double[]> tousLesPoints = new ArrayList<>();
        for (SegmentSource ss : bruts) {
            if (ss == null) continue;
            ajouterNoeudSiAbsent(tousLesPoints, ss.x1, ss.y1);
            ajouterNoeudSiAbsent(tousLesPoints, ss.x2, ss.y2);
        }

        // Subdiviser chaque segment aux intersections
        for (SegmentSource ss : bruts) {
            if (ss == null) continue;
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

            double dx = ss.x2 - ss.x1, dy = ss.y2 - ss.y1;
            double len2 = dx * dx + dy * dy;
            if (len2 < TOL) {
                continue; // Éviter division par zéro et ignorer segment dégénéré
            }
            pointsSurSegment.sort((a, b) -> {
                double ta = ((a[0] - ss.x1) * dx + (a[1] - ss.y1) * dy) / len2;
                double tb = ((b[0] - ss.x1) * dx + (b[1] - ss.y1) * dy) / len2;
                return Double.compare(ta, tb);
            });

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

    public static void ajouterSegmentSiAbsent(List<SegmentSource> sources,
                                               Point a, Point b, Mur mur) {
        for (SegmentSource ss : sources) {
            Point ssA = new Point(ss.x1, ss.y1);
            Point ssB = new Point(ss.x2, ss.y2);
            if ((correspondA(ssA, a) && correspondA(ssB, b))
             || (correspondA(ssA, b) && correspondA(ssB, a))) return;
        }
        sources.add(new SegmentSource(a, b, mur));
    }

    // =========================================================================
    // CYCLE MINIMAL (algorithme des faces planaires)
    // =========================================================================

    /**
     * Trouve la plus petite face planaire contenant le point (px, py)
     * parmi les segments fournis.
     *
     * @param px       coordonnée X du point cible
     * @param py       coordonnée Y du point cible
     * @param sources  segments subdivisés (résultat de subdiviserEtDeduplicer)
     * @return liste de SegmentSource formant le cycle minimal, ou null si aucun
     */
    public static List<SegmentSource> trouverCycleMinimal(double px, double py,
                                                           List<SegmentSource> sources) {
        List<double[]> noeuds = new ArrayList<>();
        for (SegmentSource ss : sources) {
            ajouterNoeudSiAbsent(noeuds, ss.x1, ss.y1);
            ajouterNoeudSiAbsent(noeuds, ss.x2, ss.y2);
        }
        int N = noeuds.size();
        if (N < 3) return null;

        // Liste d'adjacence
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

        // Trier par angle (Y inversé JavaFX)
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
        List<List<Integer>> faces = new ArrayList<>();
        Set<String> visitees = new HashSet<>();

        for (int i = 0; i < N; i++) {
            for (int[] arete : adj.get(i)) {
                int j = arete[0];
                if (visitees.contains(i + "->" + j)) continue;

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

                    int prochainNoeud = voisins.get((idxCourant + 1) % voisins.size())[0];
                    courant = suivant;
                    suivant = prochainNoeud;

                    if (suivant == i && courant == j) break;
                    if (courant == i) { faces.add(new ArrayList<>(face)); break; }
                }
            }
        }

        // Face minimale contenant (px, py)
        List<SegmentSource> meilleur = null;
        double meilleureAire = Double.MAX_VALUE;

        for (List<Integer> face : faces) {
            if (face.size() < 3) continue;
            List<Point> polygone = new ArrayList<>();
            for (int idx : face) polygone.add(new Point(noeuds.get(idx)[0], noeuds.get(idx)[1]));
            if (!pointDansPolygone(px, py, polygone)) continue;
            double aire = Math.abs(calculerAire(polygone));
            if (aire < meilleureAire && aire > TOL) {
                meilleureAire = aire;
                meilleur = new ArrayList<>();
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
    // CONTRAINDRE UN POINT À UNE ZONE
    // =========================================================================

    /**
     * Si le point est dans la zone, le retourne tel quel.
     * Sinon, le projette sur le bord le plus proche du polygone.
     *
     * @param px      coordonnée X
     * @param py      coordonnée Y
     * @param coins   sommets du polygone (tableau de Points)
     * @param n       nombre de sommets
     * @return {x, y} contraint
     */
    public static double[] contraindreAZone(double px, double py,
                                             Point[] coins, int n) {
        double bestX = px, bestY = py, bestDist = Double.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            Point a = coins[i];
            Point b = coins[(i + 1) % n];
            double dx = b.getX() - a.getX(), dy = b.getY() - a.getY();
            double l2 = dx * dx + dy * dy;
            if (l2 < 1e-10) continue;
            double t = Math.max(0, Math.min(1,
                ((px - a.getX()) * dx + (py - a.getY()) * dy) / l2));
            double cx = a.getX() + t * dx;
            double cy = a.getY() + t * dy;
            double dist = Math.hypot(px - cx, py - cy);
            if (dist < bestDist) { bestDist = dist; bestX = cx; bestY = cy; }
        }
        return new double[]{bestX, bestY};
    }

    // =========================================================================
    // CLASSE INTERNE — SegmentSource
    // =========================================================================

    /**
     * Représente un segment orienté avec son mur source.
     * Partagé entre NiveauControleur et PieceControleur.
     */
    public static class SegmentSource {
        public final double x1, y1, x2, y2;
        public final Mur mur;

        public SegmentSource(Point a, Point b, Mur mur) {
            this.x1 = a.getX(); this.y1 = a.getY();
            this.x2 = b.getX(); this.y2 = b.getY();
            this.mur = mur;
        }
    }
}