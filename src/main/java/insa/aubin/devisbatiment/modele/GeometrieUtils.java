package insa.aubin.devisbatiment.modele;

import java.util.*;

/**
 * Utilitaires géométriques partagés entre les contrôleurs (NiveauControleur, PieceControleur) 
 * et la couche modèle de DevisBatiment.
 * <p>
 * Centralise et expose des algorithmes fondamentaux de géométrie computationnelle plane en 2D :
 * <ul>
 *   <li>Détection d'inclusion de points dans des polygones (picking interactif par Ray-Casting).</li>
 *   <li>Calculs d'adjacence, de colinéarité, et de chevauchement géométrique de segments.</li>
 *   <li>Subdivision dynamique de segments de murs aux points d'intersection (formation d'un graphe planaire cohérent).</li>
 *   <li>Détection déterministe de cycles minimaux (algorithme des faces planaires pour la création automatique de pièces).</li>
 *   <li>Ordonnancement cyclique et orientation trigonométrique des parois constituant le contour d'un espace fermé.</li>
 * </ul>
 * </p>
 * 
 * @see Point
 * @see Mur
 * @see Piece
 */
public class GeometrieUtils {

    /** Tolérance par défaut pour les calculs de double précision géométriques (1 micromètre). */
    public static final double TOL = 1e-6;

    // =========================================================================
    // POINT DANS POLYGONE / SUR CONTOUR
    // =========================================================================

    /**
     * Algorithme géométrique de Ray-Casting (Jordan Curve Theorem).
     * <p>
     * Détermine si un point {@code (px, py)} est situé strictement à l'intérieur d'un polygone simple.
     * </p>
     * <p>
     * <b>Principe algorithmique :</b>
     * Trace un rayon horizontal infini partant du point {@code (px, py)} vers la droite (X croissant).
     * L'algorithme compte le nombre d'intersections de ce rayon avec les arêtes du polygone :
     * <ul>
     *   <li>Un nombre <b>impair</b> d'intersections indique que le point est à l'<b>intérieur</b>.</li>
     *   <li>Un nombre <b>pair</b> d'intersections indique qu'il est à l'<b>extérieur</b>.</li>
     * </ul>
     * Le cas limite où l'arête est horizontale ou colinéaire au rayon est géré par des inégalités strictes sur l'ordonnée Y.
     * </p>
     * 
     * @param px   Coordonnée X du point à tester.
     * @param py   Coordonnée Y du point à tester.
     * @param poly Liste ordonnée des sommets constituant le polygone.
     * @return {@code true} si le point réside à l'intérieur, {@code false} sinon.
     */
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

    /**
     * Détermine si un point réside exactement sur le contour polygonal (la frontière physique) d'une zone.
     * 
     * @param px       Coordonnée X du point.
     * @param py       Coordonnée Y du point.
     * @param polygone Liste ordonnée de sommets délimitant la zone.
     * @return {@code true} si le point est sur une arête du contour, {@code false} sinon.
     */
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

    /**
     * Détermine si un point est inclus dans la zone délimitée par un polygone (soit à l'intérieur, soit sur sa frontière).
     * 
     * @param px       Coordonnée X du point.
     * @param py       Coordonnée Y du point.
     * @param polygone Sommets délimitant le polygone.
     * @return {@code true} si le point est dans la zone ou sur la frontière, {@code false} sinon.
     */
    public static boolean estDansZone(double px, double py, List<Point> polygone) {
        if (polygone == null) return true;
        return pointDansPolygone(px, py, polygone)
            || pointSurContour(px, py, polygone);
    }

    // =========================================================================
    // SEGMENTS
    // =========================================================================

    /**
     * Détermine si un point se situe sur un segment de droite défini par deux extrémités géométriques.
     * 
     * @param px Coordonnée X du point à tester.
     * @param py Coordonnée Y du point à tester.
     * @param x1 Coordonnée X de l'extrémité 1.
     * @param y1 Coordonnée Y de l'extrémité 1.
     * @param x2 Coordonnée X de l'extrémité 2.
     * @param y2 Coordonnée Y de l'extrémité 2.
     * @return {@code true} si le point est situé sur le segment de droite, {@code false} sinon.
     */
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

    /**
     * Vérifie la coïncidence géométrique stricte entre deux points en appliquant le seuil de tolérance {@link #TOL}.
     * 
     * @param p1 Premier point.
     * @param p2 Second point.
     * @return {@code true} si les points ont les mêmes coordonnées à epsilon près (tolérance TOL), {@code false} sinon.
     */
    public static boolean correspondA(Point p1, Point p2) {
        return Math.abs(p1.getX() - p2.getX()) < TOL
            && Math.abs(p1.getY() - p2.getY()) < TOL;
    }

    /**
     * Vérifie l'identité géométrique absolue entre deux instances de murs.
     * <p>
     * <b>Gestion de l'incertitude numérique :</b>
     * Afin de pallier les approximations inhérentes aux calculs de nombres réels en virgule flottante ({@code double}), 
     * cette comparaison applique une marge d'incertitude epsilon fixée à 10^-4 (soit 0.1 mm).
     * Elle teste :
     * <ul>
     *   <li>Le sens normal (extrémité 1 de m1 coïncide avec extrémité 1 de m2, et extrémité 2 de m1 avec extrémité 2 de m2).</li>
     *   <li>Le sens inversé (extrémité 1 de m1 coïncide avec extrémité 2 de m2, et extrémité 2 de m1 avec extrémité 1 de m2).</li>
     * </ul>
     * </p>
     * 
     * @param m1 Premier mur à comparer.
     * @param m2 Second mur à comparer.
     * @return {@code true} si les deux segments de murs coïncident géométriquement, {@code false} sinon.
     */
    public static boolean mursIdentiques(Mur m1, Mur m2) {
        double tol = 1e-4;
        boolean sensNormal =
                Math.abs(m1.getPoint1().getX() - m2.getPoint1().getX()) < tol &&
                Math.abs(m1.getPoint1().getY() - m2.getPoint1().getY()) < tol &&
                Math.abs(m1.getPoint2().getX() - m2.getPoint2().getX()) < tol &&
                Math.abs(m1.getPoint2().getY() - m2.getPoint2().getY()) < tol;

        boolean sensInverse =
                Math.abs(m1.getPoint1().getX() - m2.getPoint2().getX()) < tol &&
                Math.abs(m1.getPoint1().getY() - m2.getPoint2().getY()) < tol &&
                Math.abs(m1.getPoint2().getX() - m2.getPoint1().getX()) < tol &&
                Math.abs(m1.getPoint2().getY() - m2.getPoint1().getY()) < tol;

        return sensNormal || sensInverse;
    }

    /**
     * Vérifie si un mur (sous-mur de cloison) est entièrement colinéaire et superposé à un mur parent.
     * 
     * @param murParent Le mur parent servant de support géométrique.
     * @param sousMur   Le mur enfant à vérifier.
     * @return {@code true} si le sous-mur repose entièrement sur le segment du mur parent, {@code false} sinon.
     */
    public static boolean mursSuperposes(Mur murParent, Mur sousMur) {
        return pointSurSegmentAvecTolerance(sousMur.getPoint1(), murParent, 0.05)
            && pointSurSegmentAvecTolerance(sousMur.getPoint2(), murParent, 0.05);
    }

    /**
     * Détermine si un point réside sur un segment de mur avec une tolérance latérale et d'extrémité donnée.
     * 
     * @param p         Le point de test.
     * @param m         Le mur.
     * @param tolerance La distance maximale autorisée par rapport au segment.
     * @return {@code true} si le point est dans le périmètre d'accroche du mur, {@code false} sinon.
     */
    public static boolean pointSurSegmentAvecTolerance(Point p, Mur m, double tolerance) {
        double x1 = m.getPoint1().getX(), y1 = m.getPoint1().getY();
        double x2 = m.getPoint2().getX(), y2 = m.getPoint2().getY();
        double px = p.getX(), py = p.getY();

        double dx = x2 - x1;
        double dy = y2 - y1;
        double len2 = dx * dx + dy * dy;
        if (len2 < TOL) return false;

        double cross = (px - x1) * dy - (py - y1) * dx;
        double dist = Math.abs(cross) / Math.sqrt(len2);
        if (dist > tolerance) return false;

        double dot = (px - x1) * dx + (py - y1) * dy;
        return dot >= -tolerance && dot <= len2 + tolerance;
    }

    // =========================================================================
    // NŒUDS
    // =========================================================================

    /**
     * Ajoute un couple de coordonnées dans une liste de nœuds géométriques si aucun nœud 
     * similaire (à la tolérance {@link #TOL} près) n'y figure déjà.
     * 
     * @param noeuds Liste de nœuds existants.
     * @param x      Coordonnée X.
     * @param y      Coordonnée Y.
     */
    public static void ajouterNoeudSiAbsent(List<double[]> noeuds, double x, double y) {
        for (double[] n : noeuds) {
            if (Math.abs(n[0] - x) < TOL && Math.abs(n[1] - y) < TOL) return;
        }
        noeuds.add(new double[]{x, y});
    }

    /**
     * Localise l'index d'un nœud spécifique dans une liste de coordonnées réelles.
     * 
     * @param noeuds Liste de nœuds de référence.
     * @param x      Coordonnée X à chercher.
     * @param y      Coordonnée Y à chercher.
     * @return L'index 0-indexé du nœud trouvé, ou {@code -1} si absent.
     */
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

    /**
     * Calcule l'aire géométrique signée d'un polygone.
     * Le résultat est positif si le polygone est orienté dans le sens trigonométrique, 
     * et négatif s'il est orienté dans le sens horaire.
     * 
     * @param poly Liste ordonnée de sommets.
     * @return L'aire signée du polygone en mètres carrés (m²).
     */
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

    /**
     * Représente l'association structurée d'un mur physique et d'un indicateur d'inversion géométrique 
     * pour l'ordonnancement séquentiel des parois.
     */
    public static record MurOriente(Mur mur, boolean inverse) {
        public Point getPoint1() {
            return inverse ? mur.getPoint2() : mur.getPoint1();
        }

        public Point getPoint2() {
            return inverse ? mur.getPoint1() : mur.getPoint2();
        }
    }

    /**
     * Réordonne de manière continue et cyclique un ensemble de segments de murs non triés.
     * <p>
     * Garantit que l'extrémité finale de chaque mur correspond géométriquement à l'extrémité initiale 
     * du mur suivant (avec inversion de sens {@link MurOriente#inverse()} si nécessaire), formant 
     * ainsi un contour polygonal exploitable.
     * </p>
     * 
     * @param murs Liste non triée de murs.
     * @return Une liste ordonnée et continue d'instances {@link MurOriente}.
     */
    public static List<MurOriente> ordonnerMurs(List<Mur> murs) {
        if (murs == null || murs.isEmpty()) return new ArrayList<>();
        List<MurOriente> ordonne  = new ArrayList<>();
        List<Mur> restants = new ArrayList<>(murs);
        MurOriente courant = new MurOriente(restants.remove(0), false);
        ordonne.add(courant);
        while (!restants.isEmpty()) {
            Point dernierPoint = courant.getPoint2();
            boolean trouve = false;
            for (int i = 0; i < restants.size(); i++) {
                Mur candidat = restants.get(i);
                if (candidat == null) continue;
                if (correspondA(candidat.getPoint1(), dernierPoint)) {
                    courant = new MurOriente(candidat, false);
                    ordonne.add(courant);
                    restants.remove(i);
                    trouve = true;
                    break;
                }
                if (correspondA(candidat.getPoint2(), dernierPoint)) {
                    MurOriente inverse = new MurOriente(candidat, true);
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

    /**
     * Détermine si le côté gauche d'un segment de mur fait face à l'intérieur d'un polygone.
     * 
     * @param mur         Le mur.
     * @param pointsPiece Les sommets du polygone.
     * @return {@code true} si le vecteur normal gauche pointe vers l'intérieur du polygone, {@code false} sinon.
     */
    public static boolean estCoteGaucheDansPiece(Mur mur, List<Point> pointsPiece) {
        if (pointsPiece == null || pointsPiece.size() < 3) return true;
        
        // Calculer le milieu du segment du mur
        double mx = (mur.getPoint1().getX() + mur.getPoint2().getX()) / 2.0;
        double my = (mur.getPoint1().getY() + mur.getPoint2().getY()) / 2.0;
        
        // Vecteur directeur du segment
        double dx = mur.getPoint2().getX() - mur.getPoint1().getX();
        double dy = mur.getPoint2().getY() - mur.getPoint1().getY();
        double len = Math.hypot(dx, dy);
        
        if (len < 1e-6) return true;
        
        // Vecteur normal gauche unitaire
        double nx = -dy / len;
        double ny = dx / len;
        
        // Point test légèrement décalé à gauche
        double epsilon = 0.01;
        double tx = mx + epsilon * nx;
        double ty = my + epsilon * ny;
        
        return pointDansPolygone(tx, ty, pointsPiece);
    }

    // =========================================================================
    // COLLECTE ET SUBDIVISION DES SEGMENTS
    // =========================================================================

    /**
     * Traitement topologique : Subdivise les segments géométriques au niveau de toutes leurs intersections.
     * <p>
     * C'est une étape préparatoire cruciale à la détection de cycles fermés. Elle convertit un ensemble 
     * désorganisé de cloisons qui se croisent en un ensemble de sous-arêtes non chevauchantes et connectées 
     * uniquement à leurs extrémités communes, prêtes pour le parcours de graphe planaire.
     * </p>
     * 
     * @param bruts Liste de cloisons brutes potentiellement entrecroisées.
     * @return Liste de segments subdivisés géométriquement et dédupliqués.
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
                
                Mur sousMur = new Mur(pa, pb, ss.mur.getHauteur());
                sousMur.setTypeMur(ss.mur.getTypeMur());   // On propage le type (ADJ_COULOIR, EXTERIEUR, etc.)
                sousMur.setOriginal(ss.mur.getOriginal()); // On conserve le lien d'origine

                ajouterSegmentSiAbsent(sources, pa, pb, sousMur);
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
     * Algorithme géométrique de détection de cycle minimal orienté (Faces Planaires).
     * <p>
     * Cet algorithme isole la plus petite région fermée (pièce) entourant directement 
     * un clic souris {@code (px, py)} à partir d'un ensemble de segments de cloisons.
     * </p>
     * <p>
     * <b>Mécanique de l'algorithme :</b>
     * <ol>
     *   <li>Construit un graphe planaire où les sommets sont les jonctions de murs et les arêtes sont les cloisons subdivisées.</li>
     *   <li>Associe à chaque arête deux demi-arêtes orientées en sens inverses (Half-Edges).</li>
     *   <li>Trie angulairement (trigonométriquement) les voisins autour de chaque nœud pour définir un parcours déterministe.</li>
     *   <li>Parcourt le graphe en appliquant la règle systématique du <i>Next Half-Edge</i> (prendre le voisin suivant dans le tri 
     *       angulaire de proche en proche) afin d'isoler l'ensemble de toutes les faces planaires.</li>
     *   <li>Filtre les faces résultantes : conserve uniquement celles contenant le point cible {@code (px, py)}.</li>
     *   <li>Sélectionne la face ayant l'<b>aire géométrique minimale</b>. Cela permet d'exclure la face extérieure infinie et 
     *       d'autres contours enveloppants plus larges.</li>
     * </ol>
     * </p>
     * 
     * @param px      Coordonnée X du point cible.
     * @param py      Coordonnée Y du point cible.
     * @param sources Liste de segments subdivisés et dédupliqués de type {@link SegmentSource}.
     * @return La liste ordonnée des segments constituant le périmètre du cycle minimal trouvé, ou {@code null} si aucun.
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
     * Restreint les coordonnées d'un point à la frontière interne d'un polygone.
     * Si le point réside à l'extérieur de la zone autorisée, il est projeté orthogonalement 
     * sur le bord le plus proche de la frontière.
     * 
     * @param px    Coordonnée X du point à contraindre.
     * @param py    Coordonnée Y du point à contraindre.
     * @param coins Sommets délimitant la zone.
     * @param n     Nombre de sommets.
     * @return Tableau réel à deux éléments {x_contraint, y_contraint}.
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
     * Représente un segment orienté en coordonnées réelles lié à son mur physique source.
     * Sert de structure d'arête élémentaire pour l'ensemble des calculs topologiques.
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
    
    /**
     * Vérifie si deux arêtes de murs ont un support géométrique ou un tracé physique commun 
     * (soit identiques, soit superposées).
     * 
     * @param a Premier mur.
     * @param b Second mur.
     * @return {@code true} s'ils partagent le même axe physique de paroi, {@code false} sinon.
     */
    public static boolean mursOntUnSupportCommun(Mur a, Mur b) {
        return mursIdentiques(a, b) || mursSuperposes(a, b) || mursSuperposes(b, a);
    }
    
    /**
     * Évalue si deux segments de murs colinéaires ou identiques sont orientés dans le même sens directionnel.
     * 
     * @param a Premier mur.
     * @param b Second mur.
     * @return {@code true} s'ils pointent dans la même direction générale (produit scalaire positif), {@code false} sinon.
     */
    public static boolean memeSens(Mur a, Mur b) {
        double dxA = a.getPoint2().getX() - a.getPoint1().getX();
        double dyA = a.getPoint2().getY() - a.getPoint1().getY();
        double dxB = b.getPoint2().getX() - b.getPoint1().getX();
        double dyB = b.getPoint2().getY() - b.getPoint1().getY();
        return dxA * dxB + dyA * dyB >= 0;
    }
}
