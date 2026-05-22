package insa.aubin.devisbatiment.modele;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Couloir extends ElementDeConstruction {

    private final List<List<Mur>> zonesDelimiteurs;
    private final double hauteurPlafond;
    private static int compteur = 0;
    private final int numero;

    public Couloir(double hauteurPlafond) {
        super("Couloir");
        this.zonesDelimiteurs = new ArrayList<>();
        this.hauteurPlafond = hauteurPlafond;
        compteur++;
        this.numero = compteur;
    }

    public void ajouterZone(List<Mur> murs) {
        zonesDelimiteurs.add(new ArrayList<>(murs));
    }

    public List<List<Point>> getPolygones() {
        List<List<Point>> polygones = new ArrayList<>();

        for (List<Mur> zone : zonesDelimiteurs) {
            List<Point> polygone = new ArrayList<>();
            for (Mur mur : zone) {
                polygone.add(mur.getPoint1());
            }
            polygones.add(polygone);
        }

        return polygones;
    }

    public List<Point> getPolygone() {
        if (zonesDelimiteurs.isEmpty()) {
            return new ArrayList<>();
        }
        return getPolygones().get(0);
    }

    public List<List<Mur>> getZonesDelimiteurs() {
        return zonesDelimiteurs;
    }

    public double getHauteurPlafond() {
        return hauteurPlafond;
    }

    @Override
    public String toCSV() {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format(Locale.US,
                "COULOIR;%s;%d;%.2f;%d",
                getId(), numero, hauteurPlafond, zonesDelimiteurs.size()));

        for (List<Mur> zone : zonesDelimiteurs) {
            sb.append(";").append(zone.size());

            for (Mur mur : zone) {
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
