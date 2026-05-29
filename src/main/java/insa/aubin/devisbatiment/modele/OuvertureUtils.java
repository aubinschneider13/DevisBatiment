package insa.aubin.devisbatiment.modele;

import java.util.ArrayList;
import java.util.List;

public final class OuvertureUtils {

    private static final double TOLERANCE_POSITION = 0.02;

    private OuvertureUtils() {
    }

    public static Ouverture copier(Ouverture ouverture, double positionSurMur) {
        if (ouverture instanceof Porte porte) {
            Porte copie = new Porte(positionSurMur);
            copie.setOrientation(porte.getOrientation());
            return copie;
        }
        return new Fenetre(positionSurMur);
    }

    public static Ouverture copierPourMur(Ouverture ouverture, Mur murSource, Mur murDestination) {
        Point position = murSource.getPointSurMur(ouverture.getPositionSurMur());
        double tDestination = murDestination.calculerPositionSurMur(position);
        tDestination = Math.max(0.0, Math.min(1.0, tDestination));

        Ouverture copie = copier(ouverture, tDestination);
        if (copie instanceof Porte porteCopie
                && ouverture instanceof Porte porteSource
                && !memeSens(murSource, murDestination)) {
            porteCopie.setOrientation(-porteSource.getOrientation());
        }
        return copie;
    }

    public static void ajouterCopieSiAbsente(Mur destination, Ouverture ouverture, Mur murSource) {
        Point position = murSource.getPointSurMur(ouverture.getPositionSurMur());

        if (!positionSurMur(destination, position)) return;

        if (contientOuvertureA(destination, position)) return;
        destination.ajouterOuverture(copierPourMur(ouverture, murSource, destination));
    }

    public static boolean contientOuvertureA(Mur mur, Point positionAbsolue) {
        double t = mur.calculerPositionSurMur(positionAbsolue);
        for (Ouverture existante : mur.getListeOuvertures()) {
            if (Math.abs(existante.getPositionSurMur() - t) < TOLERANCE_POSITION) {
                return true;
            }
        }
        return false;
    }

    private static boolean memeSens(Mur a, Mur b) {
        double dxA = a.getPoint2().getX() - a.getPoint1().getX();
        double dyA = a.getPoint2().getY() - a.getPoint1().getY();
        double dxB = b.getPoint2().getX() - b.getPoint1().getX();
        double dyB = b.getPoint2().getY() - b.getPoint1().getY();
        return dxA * dxB + dyA * dyB >= 0;
    }
    
    public static boolean positionSurMur(Mur mur, Point positionAbsolue) {
        double x1 = mur.getPoint1().getX(), y1 = mur.getPoint1().getY();
        double x2 = mur.getPoint2().getX(), y2 = mur.getPoint2().getY();
        double dx = x2 - x1, dy = y2 - y1;
        double len2 = dx * dx + dy * dy;
        if (len2 < 1e-10) return false;
        double t = ((positionAbsolue.getX() - x1) * dx + (positionAbsolue.getY() - y1) * dy) / len2;
        return t >= -0.01 && t <= 1.01;
    }
    
    public static List<Mur> collecterSourcesMurs(Appartement appartement, List<Dessin> elements) {
        List<Mur> sources = new ArrayList<>();
        if (appartement != null) {
            for (Mur m : appartement.getMurs()) ajouterSiAbsent(sources, m);
        }
        for (Dessin d : elements) {
            if (d instanceof Mur m) ajouterSiAbsent(sources, m);
        }
        return sources;
    }

    private static void ajouterSiAbsent(List<Mur> sources, Mur mur) {
        if (mur != null && sources.stream().noneMatch(e -> e == mur)) sources.add(mur);
    }

    public static void propagerOuverturesSurMur(Mur cible, List<Mur> sources) {
        for (Mur source : sources) {
            if (source == cible) continue;
            if (GeometrieUtils.mursOntUnSupportCommun(source, cible)) {
                for (Ouverture ouv : source.getListeOuvertures()) {
                    ajouterCopieSiAbsente(cible, ouv, source);
                }
            }
        }
    }

}
