package insa.aubin.devisbatiment.modele;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Représente un niveau (RDC, Niveau 1, …) d'un bâtiment.
 * Défini par ses murs délimiteurs (les 4 murs du périmètre du bâtiment
 * à cette hauteur), ses appartements, ses couloirs, et la hauteur sous plafond.
 */
public class Niveau extends ElementDeConstruction {

    // Les 4 murs du périmètre du niveau, issus des coins du bâtiment.
    private final List<Mur> mursDelimiteurs;

    private double hauteurPlafond;
    private final ArrayList<Appartement> appartements;
    private final ArrayList<Couloir> couloirs;

    /**
     * @param mursDelimiteurs les 4 murs du périmètre construits par Batiment
     * @param hauteurPlafond hauteur sous plafond en mètres
     */
    public Niveau(List<Mur> mursDelimiteurs, double hauteurPlafond) {
        super("Niveau");
        this.mursDelimiteurs = new ArrayList<>(mursDelimiteurs);
        this.hauteurPlafond = hauteurPlafond;
        this.appartements = new ArrayList<>();
        this.couloirs = new ArrayList<>();
    }

    public double calculerDevis() {
        double total = 0;

        for (Appartement appartement : appartements) {
            total += appartement.calculerDevis();
        }

        return total;
    }

    // =========================================================================
    // DÉRIVATION DU POLYGONE DEPUIS LES MURS
    // =========================================================================

    /**
     * Retourne les sommets du périmètre dans l'ordre de chaînage des murs,
     * c'est-à-dire le point1 de chaque mur délimiteur.
     */
    public List<Point> getPolygone() {
        List<Point> polygone = new ArrayList<>();

        for (Mur mur : mursDelimiteurs) {
            polygone.add(mur.getPoint1());
        }

        return polygone;
    }

    // =========================================================================
    // GESTION DES APPARTEMENTS
    // =========================================================================

    public Appartement ajouterAppartement(Collection<Mur> mursAppartement) {
        Appartement appartement = new Appartement(mursAppartement, this.hauteurPlafond);
        this.appartements.add(appartement);
        return appartement;
    }

   public Appartement ajouterAppartement(List<GeometrieUtils.MurOriente> mursAppartement) {
        Appartement appartement = new Appartement(mursAppartement, this.hauteurPlafond);
        this.appartements.add(appartement);
        // NE PAS ajouter dans mursDelimiteurs — ce sont des cloisons intérieures
        // mursDelimiteurs ne contient que les 4 murs du périmètre du bâtiment
        return appartement;
    }

    private boolean contientMurGeometriquement(List<Mur> murs, Mur mur) {
        if (murs.contains(mur)) return true;
        for (Mur existant : murs) {
            if (GeometrieUtils.mursIdentiques(existant, mur)) {
                return true;
            }
        }
        return false;
    }

    // =========================================================================
    // GESTION DES COULOIRS
    // =========================================================================

    public Couloir ajouterCouloir() {
        Couloir couloir = new Couloir(this.hauteurPlafond);
        this.couloirs.add(couloir);
        return couloir;
    }

    public void ajouterCouloir(Couloir couloir) {
        if (couloir != null && !this.couloirs.contains(couloir)) {
            this.couloirs.add(couloir);
        }
    }

    public void viderCouloirs() {
        this.couloirs.clear();
    }

    // =========================================================================
    // GETTERS ET SETTERS
    // =========================================================================

    public int getNbAppartements() {
        return appartements.size();
    }

    public ArrayList<Appartement> getAppartements() {
        return appartements;
    }

    public List<Mur> getMursDelimiteurs() {
        return mursDelimiteurs;
    }

    public double getHauteurPlafond() {
        return hauteurPlafond;
    }

    public void setHauteurPlafond(double hauteurPlafond) {
        this.hauteurPlafond = hauteurPlafond;
    }

    public ArrayList<Couloir> getCouloirs() {
        return couloirs;
    }

    @Override
    public String toCSV() {
        return String.format(Locale.US,
                "NIVEAU;%s;%d;%.2f",
                getId(), getNbAppartements(), hauteurPlafond);
    }

    @Override
    public String toString() {
        return "Niveau [id=" + getId()
                + ", nbAppartements=" + getNbAppartements()
                + ", hauteurPlafond=" + hauteurPlafond
                + "]";
    }
}
