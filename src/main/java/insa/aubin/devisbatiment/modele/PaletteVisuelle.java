package insa.aubin.devisbatiment.modele;

import javafx.scene.paint.Color;

/**
 * Centralise les palettes de couleurs utilisées pour le rendu visuel
 * des éléments de construction (Appartement, Piece, etc.).
 *
 * Chaque élément obtient une couleur selon son numéro via getIndexCouleur().
 * La palette tourne automatiquement si le numéro dépasse le nombre d'entrées.
 *
 * Pour ajouter une couleur : ajouter une entrée dans PALETTES.
 */
public class PaletteVisuelle {

    /**
     * Chaque entrée : { fond_hex, fond_opacite, contour_hex }
     * fond_opacite ∈ [0.0, 1.0]
     */
    private static final Object[][] PALETTES = {
        // fond_hex        fond_opacite  contour_hex
        { "#e74c3c",       0.18,         "#c0392b" },  // 0 — rouge
        { "#2ecc71",       0.18,         "#27ae60" },  // 1 — vert
        { "#f39c12",       0.18,         "#e67e22" },  // 2 — orange
        { "#9b59b6",       0.18,         "#8e44ad" },  // 3 — violet
        { "#1abc9c",       0.18,         "#16a085" },  // 4 — turquoise
        { "#3498db",       0.18,         "#2980b9" },  // 5 — bleu
        { "#e67e22",       0.18,         "#d35400" },  // 6 — orange foncé
        { "#2ecc71",       0.15,         "#1e8449" },  // 7 — vert foncé
    };

    // =========================================================================
    // PALETTES SPÉCIALISÉES (sous-ensembles ou surcharges)
    // =========================================================================

    /** Indices de la palette utilisés pour les Appartements. */
    private static final int[] IDX_APPARTEMENT = { 0, 1, 2, 3, 4 };

    /** Indices de la palette utilisés pour les Pièces. */
    private static final int[] IDX_PIECE       = { 5, 6, 7, 2, 4 };

    // =========================================================================
    // API PUBLIQUE
    // =========================================================================

    /**
     * Couleur de fond pour un Appartement.
     *
     * @param numero numéro de l'appartement (1-based)
     * @return couleur de fond semi-transparente
     */
    public static Color fondAppartement(int numero) {
        int idx = IDX_APPARTEMENT[(numero - 1) % IDX_APPARTEMENT.length];
        return fond(idx);
    }

    /**
     * Couleur de contour pour un Appartement.
     *
     * @param numero numéro de l'appartement (1-based)
     * @return couleur de contour
     */
    public static Color contourAppartement(int numero) {
        int idx = IDX_APPARTEMENT[(numero - 1) % IDX_APPARTEMENT.length];
        return contour(idx);
    }

    /**
     * Couleur de fond pour une Pièce.
     *
     * @param numero numéro de la pièce (1-based)
     * @return couleur de fond semi-transparente
     */
    public static Color fondPiece(int numero) {
        int idx = IDX_PIECE[(numero - 1) % IDX_PIECE.length];
        return fond(idx);
    }

    /**
     * Couleur de contour pour une Pièce.
     *
     * @param numero numéro de la pièce (1-based)
     * @return couleur de contour
     */
    public static Color contourPiece(int numero) {
        int idx = IDX_PIECE[(numero - 1) % IDX_PIECE.length];
        return contour(idx);
    }

    // =========================================================================
    // UTILITAIRES PRIVÉS
    // =========================================================================

    private static Color fond(int idx) {
        String hex     = (String) PALETTES[idx][0];
        double opacite = (double) PALETTES[idx][1];
        return Color.web(hex, opacite);
    }

    private static Color contour(int idx) {
        String hex = (String) PALETTES[idx][2];
        return Color.web(hex);
    }
}