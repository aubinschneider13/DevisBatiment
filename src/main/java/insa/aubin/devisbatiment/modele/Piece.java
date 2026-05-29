package insa.aubin.devisbatiment.modele;

import java.util.ArrayList;
import java.util.List;
import insa.aubin.devisbatiment.modele.GeometrieUtils.MurOriente;

/**
 * Représente une pièce fermée à l'intérieur d'un appartement.
 * <p>
 * Une pièce est un élément structurel délimité par une succession de cloisons ou de murs 
 * formant un contour polygonal fermé. Elle possède :
 * <ul>
 *   <li>Une hauteur sous plafond spécifique (héritée ou surchargée).</li>
 *   <li>Un ensemble de sommets 2D ({@link Point}) définissant son polygone de base.</li>
 *   <li>Une liste de murs ({@link Mur}) formant son périmètre.</li>
 *   <li>Un revêtement de {@link Sol} et un revêtement de {@link Plafond} associés à sa surface.</li>
 *   <li>Une liste de {@link CoteMur} correspondant aux faces de murs qui font strictement face à 
 *       l'intérieur de cette pièce (indispensable pour l'isolation et la décoration).</li>
 *   <li>Un usage descriptif (ex: "Salon", "Chambre", "Cuisine").</li>
 * </ul>
 * </p>
 * 
 * @see ElementDeConstruction
 * @see Sol
 * @see Plafond
 * @see CoteMur
 */
public class Piece extends ElementDeConstruction {

    /** La hauteur physique sous plafond de la pièce (en mètres). */
    private double hauteurPlafond;
    /** Liste ordonnée des sommets (points 2D) délimitant la forme géométrique de la pièce. */
    private List<Point> points;
    /** Liste des murs physiques constituant le contour géométrique de la pièce. */
    private List<Mur> murs;
    /** Liste des murs orientés dans le sens trigonométrique pour assurer la cohérence géométrique. */
    private List<MurOriente> mursOrientes;
    /** Faces des murs de contour faisant strictement face à l'intérieur de la pièce. */
    private List<CoteMur> cotesMurs;
    /** Description ou nature fonctionnelle de la pièce (ex : "Chambre 1"). */
    private String usage;
    /** Le sol physique de la pièce supportant les revêtements de sol. */
    private Sol sol;
    /** Le plafond de la pièce supportant les revêtements de plafond. */
    private Plafond plafond;
    /** Compteur global statique pour l'attribution des numéros uniques de pièces. */
    private static int compteur = 0;
    /** Numéro séquentiel unique attribué à cette pièce pour l'IHM et les devis. */
    private final int numero;

    // =========================================================================
    // CONSTRUCTEURS
    // =========================================================================

    /**
     * Construit une pièce à partir d'un ensemble de murs fermés formant un cycle.
     * <p>
     * <b>Processus d'initialisation géométrique :</b>
     * <ol>
     *   <li>Les murs bruts sont ordonnés de manière cyclique et continue via {@link GeometrieUtils#ordonnerMurs}.</li>
     *   <li>Les points 2D de sommets sont extraits séquentiellement de ces murs ordonnés.</li>
     *   <li>Pour chaque mur du périmètre, l'algorithme détermine de manière déterministe quelle face 
     *       (Gauche ou Droite) est orientée vers l'intérieur du polygone de la pièce. Cette face spécifique 
     *       est enregistrée dans {@link #cotesMurs}.</li>
     *   <li>La surface totale est calculée via la formule de Gauss/Shoelace pour instancier le sol et le plafond.</li>
     * </ol>
     * </p>
     * 
     * @param murs           La liste non ordonnée des murs formant la boucle de la pièce.
     * @param hauteurPlafond La hauteur sous plafond associée.
     */
    public Piece(List<Mur> murs, double hauteurPlafond) {
        super("Piece");
        compteur++;
        this.numero = compteur;
        this.hauteurPlafond = hauteurPlafond;
        this.mursOrientes = new ArrayList<>(GeometrieUtils.ordonnerMurs(murs));
        this.murs = new ArrayList<>();
        for (MurOriente murOriente : this.mursOrientes) {
            this.murs.add(murOriente.mur());
        }
        this.cotesMurs = new ArrayList<>();
        this.usage = "";

        // Dériver les points depuis les murs ordonnés
        this.points = new ArrayList<>();
        for (MurOriente m : this.mursOrientes) {
            this.points.add(m.getPoint1());
        }

        // Pour chaque mur ordonné, déterminer quel côté (coteGauche ou coteDroit) fait face à la pièce
        for (MurOriente murOriente : this.mursOrientes) {
            Mur m = murOriente.mur();
            // Le mur 'm' est ordonné dans le sens du contour de la pièce.
            // On regarde si son côté gauche fait face à l'intérieur du polygone de la pièce.
            boolean gaucheInterieur = estCoteGaucheDansPiece(murOriente, this.points);
            
            // Le vrai mur d'origine sur le canevas (pour ne pas perdre les références)
            Mur vraiMur = m;
            
            if (murOriente.inverse()) {
                // 'm' est inversé par ordonnerMurs.
                // Donc le côté gauche de 'm' correspond au côté droit de 'vraiMur', et inversement.
                if (gaucheInterieur) {
                    this.cotesMurs.add(vraiMur.getCoteDroit());
                } else {
                    this.cotesMurs.add(vraiMur.getCoteGauche());
                }
            } else {
                // 'm' n'est pas inversé.
                if (gaucheInterieur) {
                    this.cotesMurs.add(vraiMur.getCoteGauche());
                } else {
                    this.cotesMurs.add(vraiMur.getCoteDroit());
                }
            }
        }

        double surface   = calculerSurfaceTotale();
        this.sol         = new Sol(surface);
        this.sol.setPolygonePiece(this.points);
        this.plafond     = new Plafond(surface);
    }

    // =========================================================================
    // CALCULS
    // =========================================================================

    /**
     * Calcule la surface au sol de la pièce en appliquant la <b>Formule du Lacet (Shoelace Algorithm ou formule de Gauss)</b>.
     * <p>
     * <b>Principe mathématique :</b>
     * L'algorithme calcule l'aire signée du polygone simple à partir des coordonnées cartésiennes de ses sommets :
     * $$\text{Aire} = \frac{1}{2} \left| \sum_{i=0}^{n-1} (x_i y_{i+1} - x_{i+1} y_i) \right|$$
     * La prise en compte de la valeur absolue garantit un résultat strictement positif, 
     * indépendamment du sens de parcours des sommets (horaire ou trigonométrique).
     * </p>
     * 
     * @return La surface habitable de la pièce en mètres carrés (m²).
     */
    public double calculerSurfaceTotale() {
        double surface = 0;
        int n = points.size();
        for (int i = 0; i < n; i++) {
            Point p1 = points.get(i);
            Point p2 = points.get((i + 1) % n);
            surface += p1.getX() * p2.getY();
            surface -= p2.getX() * p1.getY();
        }
        return Math.abs(surface) / 2.0;
    }

    /**
     * Calcule le montant total estimé du devis pour cette pièce.
     * <p>
     * Le calcul intègre de manière exhaustive :
     * <ul>
     *   <li>Le coût de tous les revêtements intérieurs appliqués sur les faces de cloisons orientées vers la pièce ({@link CoteMur}).</li>
     *   <li>Le coût du revêtement de {@link Sol} posé.</li>
     *   <li>Le coût du revêtement de {@link Plafond} appliqué.</li>
     * </ul>
     * </p>
     * 
     * @return Le montant cumulé estimé de la pièce en euros (€).
     */
    public double calculerDevis() {
        double total = 0;
        for (CoteMur cm : cotesMurs) {
            total += cm.calculerPrixRevetement();
        }
        total += sol.calculerPrixRevetement();
        total += plafond.calculerPrixRevetement();
        return total;
    }

    // =========================================================================
    // USAGE
    // =========================================================================

    /**
     * Retourne l'usage ou la nature de cette pièce.
     * 
     * @return L'usage sous forme de chaîne de caractères.
     */
    public String getUsage() {
        return usage;
    }

    /**
     * Attribue ou modifie l'usage descriptif de la pièce (ex : "Salon").
     * 
     * @param usage Le libellé décrivant l'usage.
     */
    public void setUsage(String usage) {
        this.usage = usage != null ? usage.trim() : "";
    }

    // =========================================================================
    // GETTERS / SETTERS
    // =========================================================================

    public double getHauteurPlafond()              { return hauteurPlafond; }
    public void setHauteurPlafond(double h)        { this.hauteurPlafond = h; }
    public List<Point> getPoints()                 { return points; }
    
    /**
     * Retourne les murs constituant le périmètre physique de la pièce.
     * 
     * @return Liste de murs {@link Mur}.
     */
    public List<Mur> getMurs() {
        return murs;
    }



    /**
     * Algorithme géométrique de détection de face intérieure.
     * <p>
     * Détermine si le côté gauche d'un mur orienté regarde vers l'intérieur du polygone de la pièce.
     * <p>
     * <b>Méthodologie :</b>
     * <ol>
     *   <li>Calcule le milieu géométrique du mur $(m_x, m_y)$.</li>
     *   <li>Extrait le vecteur normal unitaire pointant à gauche par rapport au sens trigonométrique du segment.</li>
     *   <li>Génère un point de test temporaire légèrement décalé (offset de $1\text{ cm}$) le long de cette normale.</li>
     *   <li>Applique un test de Ray-Casting (point-dans-polygone) via {@link GeometrieUtils#pointDansPolygone} 
     *       pour vérifier si ce point de test réside à l'intérieur du périmètre de la pièce.</li>
     * </ol>
     * </p>
     */
    private boolean estCoteGaucheDansPiece(MurOriente mur, List<Point> pointsPiece) {
        if (pointsPiece == null || pointsPiece.size() < 3) return true;
        double mx = (mur.getPoint1().getX() + mur.getPoint2().getX()) / 2.0;
        double my = (mur.getPoint1().getY() + mur.getPoint2().getY()) / 2.0;
        double dx = mur.getPoint2().getX() - mur.getPoint1().getX();
        double dy = mur.getPoint2().getY() - mur.getPoint1().getY();
        double len = Math.hypot(dx, dy);
        if (len < 1e-6) return true;
        double tx = mx + 0.01 * (-dy / len);
        double ty = my + 0.01 * (dx / len);
        return GeometrieUtils.pointDansPolygone(tx, ty, pointsPiece);
    }
    
    /**
     * Retourne les côtés de murs faisant face à l'intérieur de la pièce.
     * 
     * @return Liste de {@link CoteMur}.
     */
    public List<CoteMur> getCotesMurs() {
        return cotesMurs;
    }
    
    /**
     * Construit dynamiquement des arêtes géométriques de murs orientées dans le bon sens, 
     * synchronisées avec les ouvertures et revêtements d'origine, prêtes à être dessinées dans la vue isolée.
     * 
     * @return Liste de murs temporaires synchronisés pour l'affichage graphique de la pièce.
     */
    public List<Mur> construireMursAffichage() {
        List<Point> pts = getPoints();
        if (pts == null || pts.size() < 3) return getMurs();
        List<Mur> result = new ArrayList<>();
        for (int i = 0; i < pts.size(); i++) {
            Point p1 = pts.get(i);
            Point p2 = pts.get((i + 1) % pts.size());
            Mur original = getMurs().stream()
                .filter(m -> GeometrieUtils.mursOntUnSupportCommun(m, new Mur(p1, p2, getHauteurPlafond())))
                .findFirst().orElse(null);

            // Si l'arête du polygone va en sens inverse de l'original,
            // on la retourne pour que copierPourMur ne flippe pas l'orientation
            Point mp1 = p1, mp2 = p2;
            if (original != null && !GeometrieUtils.memeSens(original, new Mur(p1, p2, getHauteurPlafond()))) {
                mp1 = p2;
                mp2 = p1;
            }

            Mur murAffichage = new Mur(mp1, mp2, original != null ? original.getHauteur() : getHauteurPlafond());
            if (original != null) {
                murAffichage.setOriginal(original);
                murAffichage.setTypeMur(original.getTypeMur());
                original.getListeOuvertures().forEach(o ->
                    OuvertureUtils.ajouterCopieSiAbsente(murAffichage, o, original));
                
                // Copier les revêtements pour l'affichage immédiat
                if (original.getCoteGauche().getRevetements() != null) {
                    murAffichage.getCoteGauche().getRevetements().clear();
                    murAffichage.getCoteGauche().getRevetements().addAll(original.getCoteGauche().getRevetements());
                }
                if (original.getCoteDroit().getRevetements() != null) {
                    murAffichage.getCoteDroit().getRevetements().clear();
                    murAffichage.getCoteDroit().getRevetements().addAll(original.getCoteDroit().getRevetements());
                }
            }
            result.add(murAffichage);
        }
        return result;
    }

    public Sol getSol()                            { return sol; }
    public Plafond getPlafond()                    { return plafond; }
    public static void resetCompteur()             { compteur = 0; }
    public static void setCompteur(int valeur)      { compteur = Math.max(0, valeur); }
    public int getNumero()                         { return numero; }

    // =========================================================================
    // SÉRIALISATION
    // =========================================================================

    /**
     * Sérialise cette pièce au format CSV reconnu par {@link GestionnaireSauvegarde}.
     * <p>
     * <b>Format CSV généré :</b>
     * <pre>
     * PIECE;[ID];[NUMERO];[SURFACE];[HAUTEUR_PLAFOND];[USAGE];[X1];[Y1];[X2];[Y2]...
     * </pre>
     * </p>
     * 
     * @return La chaîne sérialisée.
     */
    @Override
    public String toCSV() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(java.util.Locale.US,
            "PIECE;%s;%d;%.2f;%.2f",
            getId(), numero, calculerSurfaceTotale(), hauteurPlafond));
        sb.append(";").append(encoderChampCSV(usage));
        for (Point p : points) {
            sb.append(String.format(java.util.Locale.US, ";%.2f;%.2f", p.getX(), p.getY()));
        }
        return sb.toString();
    }

    /**
     * Libellé d'affichage abrégé pour l'arborescence du NavigateurView.
     * 
     * @return Chaîne descriptive incluant le numéro unique, l'usage et la surface de la pièce.
     */
    @Override
    public String toString() {
        String suffixeUsage = usage == null || usage.isBlank() ? "" : " - " + usage;
        return "Pièce " + numero + suffixeUsage
             + String.format(" (%.1f m²)", calculerSurfaceTotale());
    }

    /**
     * Libellé de texte formel tracé sur le canvas interactif.
     * 
     * @return Texte de dessin 2D de la pièce.
     */
    public String getLibelleCanvas() {
        return "Pi\u00e8ce " + numero
             + String.format(" (%.1f m\u00b2)", calculerSurfaceTotale());
    }

    /**
     * Encode une chaîne pour éviter des collisions avec le séparateur CSV point-virgule et les sauts de lignes.
     * 
     * @param valeur La valeur textuelle brute.
     * @return La valeur nettoyée et sécurisée.
     */
    private String encoderChampCSV(String valeur) {
        if (valeur == null) return "";
        return valeur.replace(";", ",")
                .replace("\r", " ")
                .replace("\n", " ")
                .trim();
    }
}
