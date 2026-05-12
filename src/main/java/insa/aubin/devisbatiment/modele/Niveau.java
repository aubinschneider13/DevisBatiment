package insa.aubin.devisbatiment.modele;

import java.util.ArrayList;
import java.util.List;

/**
 * Représente un niveau (RDC, Niveau 1, …) d'un bâtiment.
 * Défini par ses murs délimiteurs (les 4 murs du périmètre du bâtiment
 * à cette hauteur), ses appartements, et la hauteur sous plafond.
 */
public class Niveau extends ElementDeConstruction {

    // Les 4 murs du périmètre du niveau (issus des coins du Batiment)
    private final List<Mur> mursDelimiteurs;

    private double hauteurPlafond;
    private final ArrayList<Appartement> appartements;

    /**
     * @param mursDelimiteurs les 4 murs du périmètre (construits par Batiment)
     * @param hauteurPlafond  hauteur sous plafond en mètres
     */
    public Niveau(List<Mur> mursDelimiteurs, double hauteurPlafond) {
        super("Niveau");
        this.mursDelimiteurs = new ArrayList<>(mursDelimiteurs);
        this.hauteurPlafond  = hauteurPlafond;
        this.appartements    = new ArrayList<>();
    }

    // =========================================================================
    // DÉRIVATION DU POLYGONE DEPUIS LES MURS
    // =========================================================================

    /**
     * Retourne les sommets du périmètre dans l'ordre de chaînage des murs
     * (point1 de chaque mur délimiteur).
     */
    public List<Point> getPolygone() {
        List<Point> polygone = new ArrayList<>();
        for (Mur m : mursDelimiteurs) {
            polygone.add(m.getPoint1());
        }
        return polygone;
    }

    // =========================================================================
    // GESTION DES APPARTEMENTS
    // =========================================================================

    /**
     * Crée un appartement à partir d'une liste de murs délimiteurs détectés
     * dans le canvas du niveau, et l'ajoute à ce niveau.
     *
     * @param mursAppartement murs formant le périmètre fermé de l'appartement
     * @return l'appartement créé
     */
    public Appartement ajouterAppartement(List<Mur> mursAppartement) {
        Appartement a = new Appartement(mursAppartement, this.hauteurPlafond);
        this.appartements.add(a);
        return a;
    }

    public int getNbAppartements()              { return appartements.size(); }
    public ArrayList<Appartement> getAppartements() { return appartements; }
    public List<Mur> getMursDelimiteurs()       { return mursDelimiteurs; }
    public double getHauteurPlafond()           { return hauteurPlafond; }
    public void setHauteurPlafond(double h)     { this.hauteurPlafond = h; }

    @Override
    public String toCSV() {
        return "NIVEAU;" + getId() + ";" + getNbAppartements() + ";" + hauteurPlafond;
    }

    @Override
    public String toString() {
        return "Niveau [id=" + getId() + ", nbAppartements=" + getNbAppartements()
                + ", hauteurPlafond=" + hauteurPlafond + "]";
    }
}