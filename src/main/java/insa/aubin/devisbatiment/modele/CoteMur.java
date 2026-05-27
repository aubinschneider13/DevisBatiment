package insa.aubin.devisbatiment.modele;

/**
 * Représente un côté spécifique d'un mur (gauche ou droit).
 * <p>
 * Cette classe hérite de {@link SurfaceAvecRevetement} et permet de gérer séparément 
 * les finitions et les isolants appliqués sur chaque face du mur. 
 * Cette architecture est cruciale pour permettre :
 * <ul>
 *   <li>La cohabitation d'un isolant thermique (sur la face externe) et d'un revêtement esthétique (sur la face interne).</li>
 *   <li>L'indépendance des calculs de devis et de surfaces pour les murs mitoyens partagés entre deux pièces différentes.</li>
 * </ul>
 * </p>
 * 
 * @see SurfaceAvecRevetement
 * @see Mur
 * @see Revetement
 * @see Isolant
 */
public class CoteMur extends SurfaceAvecRevetement {
    /** Le mur parent auquel appartient ce côté. */
    private final Mur murParent;

    /**
     * Construit une face de mur associée à son mur parent.
     * 
     * @param murParent Le mur physique contenant ce côté.
     */
    public CoteMur(Mur murParent) {
        super("CoteMur");
        this.murParent = murParent;
    }

    /**
     * Retourne le mur physique parent associé à ce côté.
     * 
     * @return Le mur parent de type {@link Mur}.
     */
    public Mur getMurParent() {
        return murParent;
    }

    /**
     * Calcule la surface nette de ce côté de mur.
     * La surface nette exclut l'ensemble des surfaces occupées par les ouvertures (portes et fenêtres).
     * 
     * @return La surface nette utile en mètres carrés (m²).
     */
    @Override
    public double calculerSurface() {
        return murParent.calculerSurfaceNette();
    }

    /**
     * Indicateur global définissant si l'utilisateur est actuellement positionné 
     * dans la vue isolée d'une pièce spécifique.
     * <p>
     * Ce commutateur statique permet d'ajuster dynamiquement les règles de compatibilité :
     * <ul>
     *   <li>Si {@code true} : Seuls les revêtements intérieurs (non-isolants) peuvent être posés 
     *       sur les faces visibles de la pièce active.</li>
     *   <li>Si {@code false} : L'utilisateur navigue au niveau global de l'appartement. Les façades 
     *       extérieures n'acceptent alors que des isolants.</li>
     * </ul>
     * </p>
     */
    public static boolean vueIsoleePieceActive = false;

    /**
     * Vérifie la compatibilité d'un revêtement ou isolant avec ce côté de mur.
     * <p>
     * Les règles de compatibilité sont :
     * <ul>
     *   <li>Le revêtement doit être explicitement marqué comme utilisable sur un mur ({@link Revetement#isPourMur()}).</li>
     *   <li>Si {@link #vueIsoleePieceActive} est actif (vue isolée de pièce) : Aucun isolant n'est 
     *       autorisé depuis l'intérieur, seuls les revêtements classiques (peinture, papier peint, etc.) sont acceptés.</li>
     *   <li>Si nous sommes en vue globale d'appartement :
     *     <ul>
     *       <li>Les murs extérieurs (façades) n'acceptent <b>que</b> des isolants ({@link Isolant}).</li>
     *       <li>Les cloisons intérieures n'acceptent <b>pas</b> d'isolants.</li>
     *     </ul>
     *   </li>
     * </ul>
     * </p>
     * 
     * @param r Le revêtement à tester.
     * @return {@code true} si le revêtement est compatible avec cette face de mur, {@code false} sinon.
     */
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

    /**
     * Ajoute ou remplace un revêtement sur ce côté de mur en appliquant les contraintes 
     * de coexistence entre isolants et finitions.
     * <p>
     * <b>Logique de coexistence stricte :</b>
     * <ul>
     *   <li>Si le nouveau revêtement est un {@link Isolant} : Il remplace tout isolant préexistant 
     *       sur cette face sans modifier le revêtement esthétique de finition.</li>
     *   <li>Si le nouveau revêtement est une finition classique (non-isolant) : Il remplace la finition 
     *       précédente sans altérer la couche d'isolation en place.</li>
     * </ul>
     * </p>
     * 
     * @param r Le revêtement à appliquer.
     */
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

    /**
     * Représentation sous forme de chaîne de caractères de ce côté de mur.
     * 
     * @return Une chaîne décrivant le côté et son mur associé.
     */
    @Override
    public String toString() {
        return "Côté de " + murParent.toString();
    }
}
