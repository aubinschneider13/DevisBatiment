package insa.aubin.devisbatiment.modele;

public enum TypeIsolant {
    BARDAGE_BOIS("Bardage Bois"),
    LAINE_DE_ROCHE("Laine de roche"),
    POLYURETHANE_PROJETE("Polyuréthane projeté");

    private final String libelle;

    TypeIsolant(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    @Override
    public String toString() {
        return libelle;
    }

    public static TypeIsolant fromString(String text) {
        for (TypeIsolant t : TypeIsolant.values()) {
            if (t.libelle.equalsIgnoreCase(text) || t.name().equalsIgnoreCase(text)) {
                return t;
            }
        }
        return LAINE_DE_ROCHE; // Par défaut
    }
}
