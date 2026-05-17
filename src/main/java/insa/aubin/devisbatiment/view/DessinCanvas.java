package insa.aubin.devisbatiment.view;

import insa.aubin.devisbatiment.modele.Dessin;
import insa.aubin.devisbatiment.modele.Fantome;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.transform.Affine;
import java.util.ArrayList;
import java.util.List;
import insa.aubin.devisbatiment.modele.SurfaceAvecRevetement;
import insa.aubin.devisbatiment.modele.Mur;
import insa.aubin.devisbatiment.modele.Sol;

public class DessinCanvas extends Canvas {

    private List<Dessin> elements;
    private List<SurfaceAvecRevetement> selection = new ArrayList<>();

    // Zoom et translation
    private double zoomFactor = 50.0; // 50px = 1 unité modèle
    private double offsetX = 0;
    private double offsetY = 0;

    // Pour le pan (clic molette ou clic droit)
    private double panStartX, panStartY;
    private double panStartOffsetX, panStartOffsetY;

    private double gridSize = 1; // 1 unité modèle (modifiable via l'échelle)
    private boolean panActif = false;
    private boolean isPanning = false;

    private Fantome fantomeCourant = null;
    private double fantomeX = 0, fantomeY = 0;
    private double fantomeAngle = 0;
    private boolean fantomeActif = false;
    
    public void setGridSize(double gridSize) {
        this.gridSize = gridSize;
        redrawAll();
    }

    public double getGridSize() {
        return gridSize;
    }
    
    public DessinCanvas() {
        this.elements = new ArrayList<>();

        this.heightProperty().addListener(o -> {
        if (offsetY == 0) offsetY = getHeight() / 2;  // init une seule fois
            redrawAll();
        });
        this.widthProperty().addListener(o -> {
        if (offsetX == 0) offsetX = getWidth() / 2;   // init une seule fois
            redrawAll();
        });

        // Zoom centré sur la souris (molette)
        this.addEventHandler(ScrollEvent.SCROLL, event -> {
            double mouseX = event.getX();
            double mouseY = event.getY();

            double oldZoom = zoomFactor;
            if (event.getDeltaY() > 0) zoomFactor *= 1.1;
            else zoomFactor /= 1.1;

            // Limites de zoom
            zoomFactor = Math.max(10.0, Math.min(zoomFactor, 300.0));

            // Ajuster l'offset pour que le zoom soit centré sur la souris
            offsetX = mouseX - (mouseX - offsetX) * (zoomFactor / oldZoom);
            offsetY = mouseY - (mouseY - offsetY) * (zoomFactor / oldZoom);

            redrawAll();
            event.consume();
        });

        // Pan : début du déplacement
        // Clic droit ou molette toujours actifs, clic gauche uniquement si panActif
        this.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() == MouseButton.SECONDARY ||
                event.getButton() == MouseButton.MIDDLE ||
                (event.getButton() == MouseButton.PRIMARY && panActif)) {
                panStartX = event.getX();
                panStartY = event.getY();
                panStartOffsetX = offsetX;
                panStartOffsetY = offsetY;
                isPanning = true;
            }
        });

        // Pan : déplacement en cours
        this.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            if (isPanning) {
                offsetX = panStartOffsetX + (event.getX() - panStartX);
                offsetY = panStartOffsetY + (event.getY() - panStartY);
                redrawAll();
            }
        });

        // Pan : fin du déplacement
        this.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> {
            isPanning = false;
        });

        // Double-clic gauche : recentrer la vue sur l'origine
        this.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                offsetX = getWidth() / 2;
                offsetY = getHeight() / 2;
                zoomFactor = 50.0;
                redrawAll();
            }
        });
    }
    public void setFantome(Fantome f, double x, double y,
                           double angle, boolean actif) {
        this.fantomeCourant = f;
        this.fantomeX = x;
        this.fantomeY = y;
        this.fantomeAngle = angle;
        this.fantomeActif = actif;
    }

    public void clearFantome() {
        this.fantomeCourant = null;
    }

    /**
     * Convertit les coordonnées pixels en coordonnées modèle
     * avec magnétisme sur la grille (snap).
     * Le Y est inversé pour avoir les Y positifs vers le haut.
     */
    public Point2D snapToGrid(double px, double py) {
        // Conversion pixels → coordonnées modèle
        double mx = (px - offsetX) / zoomFactor;
        double my = -(py - offsetY) / zoomFactor; // Y inversé

        // Snap à la grille
        double snappedX = Math.round(mx / gridSize) * gridSize;
        double snappedY = Math.round(my / gridSize) * gridSize;

        return new Point2D(snappedX, snappedY);
    }

    /**
     * Méthode conservée pour compatibilité — retourne la valeur telle quelle,
     * le vrai snap se fait dans snapToGrid().
     */
    public double snap(double valeur) {
        return valeur;
    }

    public void redrawAll() {
        GraphicsContext gc = this.getGraphicsContext2D();
        gc.clearRect(0, 0, this.getWidth(), this.getHeight());

        // 1. Dessin de la grille en arrière-plan
        drawGrid(gc);

        // 2. Dessin des éléments avec transformation zoom + Y inversé
        gc.save();
        gc.translate(offsetX, offsetY);
        gc.scale(zoomFactor, -zoomFactor);

        for (Dessin d : this.elements) {
            d.dessiner(gc);
        }

        if (selection != null) {
            for (SurfaceAvecRevetement surface : selection) {
                if (surface instanceof Mur) {
                    Mur m = (Mur) surface;
                    gc.setStroke(Color.web("#00E5FF", 0.6)); // Cyan semi-transparent très visible
                    gc.setLineWidth(0.4); // Plus épais qu'un mur normal pour bien l'entourer
                    gc.strokeLine(m.getPoint1().getX(), m.getPoint1().getY(),
                            m.getPoint2().getX(), m.getPoint2().getY());
                }
                // Note : Pour le 'Sol', le retour visuel est plus complexe car il faut
                // redessiner le polygone de la pièce. Vous pouvez le laisser invisible
                // pour l'instant ou afficher une alerte textuelle.
            }
        }

        // Fantôme par-dessus tout
        if (fantomeCourant != null) {
            fantomeCourant.dessinerFantome(
                    gc, fantomeX, fantomeY, fantomeAngle, fantomeActif
            );
        }
        gc.restore();
    }

    private void drawGrid(GraphicsContext gc) {
        double w = getWidth();
        double h = getHeight();

        // Espacement de la grille en pixels selon le zoom actuel
        double gridPx = gridSize * zoomFactor;

        // Grille fine
        gc.setStroke(Color.web("#E8E8E8"));
        gc.setLineWidth(0.5);

        double startX = offsetX % gridPx;
        double startY = offsetY % gridPx;

        // Lignes verticales (droite et gauche depuis l'origine)
        for (double x = startX; x < w; x += gridPx) gc.strokeLine(x, 0, x, h);
        for (double x = startX - gridPx; x >= 0; x -= gridPx) gc.strokeLine(x, 0, x, h);

        // Lignes horizontales (bas et haut depuis l'origine)
        for (double y = startY; y < h; y += gridPx) gc.strokeLine(0, y, w, y);
        for (double y = startY - gridPx; y >= 0; y -= gridPx) gc.strokeLine(0, y, w, y);

        // Axes principaux (Style GeoGebra)
        gc.setStroke(Color.web("#888888"));
        gc.setLineWidth(1.5);
        gc.strokeLine(0, offsetY, w, offsetY); // Axe X
        gc.strokeLine(offsetX, 0, offsetX, h); // Axe Y

        // Labels des axes
        gc.setFill(Color.web("#555555"));
        gc.setFont(Font.font("Arial", 10));

        // Labels X (droite)
        for (double x = startX; x < w; x += gridPx) {
            double valeur = (x - offsetX) / zoomFactor;
            if (Math.abs(valeur) > 0.01) {
                gc.fillText(String.valueOf((int) Math.round(valeur)), x + 2, offsetY - 4);
            }
        }
        // Labels X (gauche)
        for (double x = startX - gridPx; x >= 0; x -= gridPx) {
            double valeur = (x - offsetX) / zoomFactor;
            if (Math.abs(valeur) > 0.01) {
                gc.fillText(String.valueOf((int) Math.round(valeur)), x + 2, offsetY - 4);
            }
        }

        // Labels Y (bas)
        for (double y = startY; y < h; y += gridPx) {
            double valeur = -(y - offsetY) / zoomFactor;
            if (Math.abs(valeur) > 0.01) {
                gc.fillText(String.valueOf((int) Math.round(valeur)), offsetX + 4, y + 4);
            }
        }
        // Labels Y (haut)
        for (double y = startY - gridPx; y >= 0; y -= gridPx) {
            double valeur = -(y - offsetY) / zoomFactor;
            if (Math.abs(valeur) > 0.01) {
                gc.fillText(String.valueOf((int) Math.round(valeur)), offsetX + 4, y + 4);
            }
        }

        // Label origine
        gc.setFill(Color.web("#333333"));
        gc.fillText("0", offsetX + 4, offsetY - 4);
    }

    /**
     * Retourne la transformation affine courante (zoom + translation + Y inversé)
     * utilisée pour convertir les coordonnées modèle en pixels.
     */
    public Affine getTransform() {
        Affine t = new Affine();
        t.appendTranslation(offsetX, offsetY);
        t.appendScale(zoomFactor, -zoomFactor);
        return t;
    }

    public void ajouterElement(Dessin d) {
        this.elements.add(d);
        this.redrawAll(); // On rafraîchit dès qu'on ajoute quelque chose
    }

    public List<Dessin> getElements() {
        return this.elements;
    }

    public void setPanActif(boolean panActif) {
        this.panActif = panActif;
    }
    
    public void centrerSur(double cx, double cy, double zoom) {
        this.zoomFactor = zoom;
        this.offsetX = getWidth() / 2 - cx * zoom;
        this.offsetY = getHeight() / 2 + cy * zoom; // Y inversé
        redrawAll();
    }

    public void setSelection(List<SurfaceAvecRevetement> selection) {
        this.selection = selection;
    }
}