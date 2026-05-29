package insa.aubin.devisbatiment.modele;

import java.util.ArrayList;
import java.util.List;

/**
 * Représente un bâtiment (Maison ou Immeuble).
 * Défini par 4 points formant son emprise au sol (dérivés de l'AireImmeuble),
 * un nom, un type, et une liste de niveaux.
 */
public abstract class Batiment extends ElementDeConstruction {

    private String nomBatiment;
    private String typeBatiment;

    // Les 4 coins de l'emprise au sol (issus de l'AireImmeuble)
    private final Point point1, point2, point3, point4;
    
    private final ArrayList<Niveau> niveaux;

    /**
     * @param nomBatiment  nom du bâtiment
     * @param typeBatiment "Maison" ou "Immeuble"
     * @param point1       premier coin de l'emprise
     * @param point2       deuxième coin (doit former un angle droit avec p1 et p3)
     * @param point3       troisième coin
     * @param point4       quatrième coin (déduit automatiquement si null)
     */
    public Batiment(String nomBatiment, String typeBatiment,
                    Point point1, Point point2, Point point3, Point point4) {
        super(typeBatiment);
        if (!typeBatiment.equals("Maison") && !typeBatiment.equals("Immeuble")) {
            throw new IllegalArgumentException("typeBatiment doit être 'Maison' ou 'Immeuble'");
        }
        if (!Point.sontOrthogonaux(point1, point2, point3)) {
            throw new IllegalArgumentException("Les points ne forment pas un angle droit en point2.");
        }

        this.nomBatiment  = nomBatiment;
        this.typeBatiment = typeBatiment;
        this.point1 = point1;
        this.point2 = point2;
        this.point3 = point3;
        // p4 = p1 + (p3 - p2), le coin opposé à p2
        this.point4 = (point4 != null) ? point4
                : new Point(point1.getX() + (point3.getX() - point2.getX()),
                            point1.getY() + (point3.getY() - point2.getY()));

        this.niveaux = new ArrayList<>();
    }

    /**
     * Construit un Batiment directement depuis une AireImmeuble validée.
     * Les 4 points de l'aire deviennent les coins de l'emprise du bâtiment.
     */
    public Batiment(String nomBatiment, String typeBatiment, AireImmeuble aire) {
        this(nomBatiment, typeBatiment,
             aire.getP1(), aire.getP2(), aire.getP3(), aire.getP4());
    }

    public double calculerDevisTotal() {
        double total = 0;
        for (Niveau n : niveaux) {
            total += n.calculerDevis();
        }
        return total;
    }

    // =========================================================================
    // GESTION DES NIVEAUX
    // =========================================================================

    /**
     * Crée et ajoute un niveau au bâtiment.
     * Les 4 murs délimiteurs du niveau sont construits depuis les coins du bâtiment.
     */
    public Niveau ajouterNiveau(double hauteurPlafond) {
        List<Mur> mursDelimiteurs = construireMursDelimiteurs();
        Niveau n = new Niveau(mursDelimiteurs, hauteurPlafond);
        this.niveaux.add(n);
        return n;
    }

    /**
     * Construit les 4 murs du périmètre du bâtiment dans le sens p1→p2→p3→p4→p1.
     */
    private List<Mur> construireMursDelimiteurs() {
        List<Mur> murs = new ArrayList<>();
        murs.add(new Mur(point1, point2));
        murs.add(new Mur(point2, point3));
        murs.add(new Mur(point3, point4));
        murs.add(new Mur(point4, point1));
        return murs;
    }

    // =========================================================================
    // GETTERS / SETTERS
    // =========================================================================

    public String getNomBatiment()              { return nomBatiment; }
    public String getTypeBatiment()             { return typeBatiment; }
    public Point getPoint1()                    { return point1; }
    public Point getPoint2()                    { return point2; }
    public Point getPoint3()                    { return point3; }
    public Point getPoint4()                    { return point4; }
    public int getNbNiveaux()                   { return niveaux.size(); }
    public ArrayList<Niveau> getNiveaux()       { return niveaux; }

    @Override
    public String toCSV() {
        return getId() + ";" + nomBatiment + ";" + typeBatiment + ";" + getNbNiveaux();
    }

    @Override
    public String toString() {
        return typeBatiment + " [id=" + getId() + ", nom=" + nomBatiment
                + ", niveaux=" + getNbNiveaux() + "]";
    }
}