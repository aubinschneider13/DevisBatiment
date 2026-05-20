package insa.aubin.devisbatiment.modele;

public class CoteMur extends SurfaceAvecRevetement {
    private final Mur murParent;

    public CoteMur(Mur murParent) {
        super("CoteMur");
        this.murParent = murParent;
    }

    public Mur getMurParent() {
        return murParent;
    }

    @Override
    public double calculerSurface() {
        return murParent.calculerSurfaceNette();
    }

    @Override
    public boolean estCompatibleAvec(Revetement r) {
        return r != null && r.isPourMur();
    }

    @Override
    public String toString() {
        return "Côté de " + murParent.toString();
    }
}
