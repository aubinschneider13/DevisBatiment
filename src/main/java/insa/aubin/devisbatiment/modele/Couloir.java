package insa.aubin.devisbatiment.modele;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Couloir extends ElementDeConstruction {

    private final List<List<GeometrieUtils.MurOriente>> zonesOrientees;
    private final double hauteurPlafond;
    private static int compteur = 0;
    private final int numero;

    public Couloir(double hauteurPlafond) {
        super("Couloir");
        this.zonesOrientees = new ArrayList<>();
        this.hauteurPlafond = hauteurPlafond;
        compteur++;
        this.numero = compteur;
    }

    public void ajouterZone(List<Mur> murs) {
        List<GeometrieUtils.MurOriente> zone = new ArrayList<>();
        for (Mur mur : murs) {
            zone.add(new GeometrieUtils.MurOriente(mur, false));
        }
        zonesOrientees.add(zone);
    }

    public void ajouterZoneOriente(List<GeometrieUtils.MurOriente> murs) {
        zonesOrientees.add(new ArrayList<>(murs));
    }

    public List<List<Point>> getPolygones() {
        List<List<Point>> polygones = new ArrayList<>();
        for (List<GeometrieUtils.MurOriente> zone : zonesOrientees) {
            List<Point> polygone = new ArrayList<>();
            for (GeometrieUtils.MurOriente mur : zone) {
                polygone.add(mur.getPoint1()); // getPoint1() tient déjà compte de inverse
            }
            polygones.add(polygone);
        }
        return polygones;
    }

    public List<Point> getPolygone() {
        if (zonesOrientees.isEmpty()) {
            return new ArrayList<>();
        }
        return getPolygones().get(0);
    }

    public List<List<Mur>> getZonesDelimiteurs() {
        List<List<Mur>> zones = new ArrayList<>();
        for (List<GeometrieUtils.MurOriente> zoneOrientee : zonesOrientees) {
            List<Mur> zone = new ArrayList<>();
            for (GeometrieUtils.MurOriente murOriente : zoneOrientee) {
                // Créer un mur orienté dans le bon sens si nécessaire
                if (murOriente.inverse()) {
                    Mur murCorrige = new Mur(murOriente.mur().getPoint2(), 
                                             murOriente.mur().getPoint1(),
                                             murOriente.mur().getHauteur());
                    murCorrige.setTypeMur(murOriente.mur().getTypeMur());
                    zone.add(murCorrige);
                } else {
                    zone.add(murOriente.mur());
                }
            }
            zones.add(zone);
        }
        return zones;
    }

    public double getHauteurPlafond() {
        return hauteurPlafond;
    }

    @Override
    public String toCSV() {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format(Locale.US,
                "COULOIR;%s;%d;%.2f;%d",
                getId(), numero, hauteurPlafond, zonesOrientees.size()));

        for (List<GeometrieUtils.MurOriente> zone : zonesOrientees) {
            sb.append(";").append(zone.size());

            for (GeometrieUtils.MurOriente mur : zone) {
                Point p = mur.getPoint1();
                sb.append(String.format(Locale.US,
                        ";%.2f;%.2f",
                        p.getX(), p.getY()));
            }
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return "Couloir " + numero;
    }
}
