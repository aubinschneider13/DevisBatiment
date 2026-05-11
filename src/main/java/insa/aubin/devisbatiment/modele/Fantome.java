package insa.aubin.devisbatiment.modele;

import javafx.scene.canvas.GraphicsContext;

/**
 * Interface pour les éléments pouvant être dessinés
 * en mode "fantôme" (aperçu avant insertion).
 */
public interface Fantome {
    /**
     * Dessine l'aperçu fantôme à une position donnée,
     * orienté selon l'angle du mur survolé.
     *
     * @param gc     le contexte graphique
     * @param x      position X dans le repère modèle
     * @param y      position Y dans le repère modèle
     * @param angle  angle du mur survolé en degrés
     * @param actif  true = mur survolé (vert), false = curseur libre (gris)
     */
    void dessinerFantome(GraphicsContext gc, double x, double y,
                         double angle, boolean actif);
}