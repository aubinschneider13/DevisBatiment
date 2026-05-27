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

    public static boolean vueIsoleePieceActive = false;

    @Override
    public boolean estCompatibleAvec(Revetement r) {
        if (r == null || !r.isPourMur()) {
            return false;
        }
        boolean estMurExterieur = (murParent.getTypeMur() == Mur.TypeMur.EXTERIEUR);
        boolean estIsolant = (r instanceof Isolant);

        if (vueIsoleePieceActive) {
            // En vue isolée de pièce, tous les murs (y compris extérieurs) reçoivent des revêtements classiques
            // et aucun isolant ne peut être appliqué depuis l'intérieur de la pièce.
            return !estIsolant;
        } else {
            // En vue globale d'appartement :
            if (estMurExterieur) {
                return estIsolant; // Les façades extérieures n'acceptent QUE des isolants
            } else {
                return !estIsolant; // Les cloisons intérieures n'acceptent PAS d'isolants
            }
        }
    }

    @Override
    public void ajouterRevetement(Revetement r) {
        System.out.println("[DEBUG MODEL] Affectation sur CoteMur de la classe: " + this.toString());
        System.out.println("  -> Mur Parent (Instance Java): " + this.getMurParent().hashCode() + " | Num Unique: " + this.getMurParent().toString());
        System.out.println("  -> Est-ce un Mur Original ? " + (this.getMurParent().getOriginal() == this.getMurParent()));
        System.out.println("  -> Contenu avant affectation - Revêtements: " + this.getRevetements().size() + " | Isolants: N/A");

        if (r == null) return;

        if (estCompatibleAvec(r)) {
            if (r instanceof Isolant iso) {
                // Logique d'isolation : remplace l'isolant existant, sans toucher à la finition esthétique
                this.getRevetements().removeIf(ex -> ex instanceof Isolant);
                this.getRevetements().add(iso);
            } else {
                // Logique esthétique : remplace la peinture/papier peint (les non-isolants), sans toucher à l'isolant
                this.getRevetements().removeIf(ex -> !(ex instanceof Isolant));
                this.getRevetements().add(r);
            }
        }
    }

    @Override
    public String toString() {
        return "Côté de " + murParent.toString();
    }
}
