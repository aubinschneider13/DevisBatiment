package insa.aubin.devisbatiment.controlleur;

import insa.aubin.devisbatiment.modele.*;
import insa.aubin.devisbatiment.modele.GeometrieUtils.SegmentSource;
import insa.aubin.devisbatiment.modele.GeometrieUtils.MurOriente;
import insa.aubin.devisbatiment.view.NiveauView;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.TreeItem;
import javafx.scene.paint.Color;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Contrôleur d'un niveau (étage) de l'immeuble.
 *
 * Responsabilité : gérer le dessin interactif sur le DessinCanvas du niveau
 * (murs libres, murs rectangulaires, appartements par détection de cycle minimal).
 *
 * La géométrie (détection de point dans polygone, cycle minimal, subdivision
 * des segments, etc.) est entièrement déléguée à {@link GeometrieUtils}.
 */
public class NiveauControleur {

    // =========================================================================
    // ATTRIBUTS
    // =========================================================================

    private final NiveauView       vue;
    private final AireImmeuble     aireImmeuble;
    private final TreeItem<String> itemNiveau;
    private final Niveau           niveau;

    // Appartements créés sur ce niveau
    private final List<Appartement>                    appartements       = new ArrayList<>();
    private final Map<TreeItem<String>, Appartement>   mapItemAppartement = new HashMap<>();
    private int compteurAppartements = 0;

    // Mode courant : "AUCUN" | "MUR" | "APPARTEMENT"
    private String mode = "AUCUN";

    // État du dessin de mur libre
    private Mur mur1EnCours = null;

    // État du dessin de mur rectangulaire
    private Mur   mur1Rect       = null;
    private Mur   mur2Rect       = null;
    private Point p1Rect         = null;
    private Point p2Rect         = null;
    private int   etapeRectangle = 0;

    // Listener du toggle de forme (conservé pour pouvoir le retirer)
    private javafx.beans.value.ChangeListener<javafx.scene.control.Toggle> listenerForme = null;

    // Callback notifiant AppControleur lors de la création d'un appartement
    private Function<Appartement, TreeItem<String>> onAppartementCree = null;
    private Function<Couloir, TreeItem<String>> onCouloirCree = null;
    private Runnable onCouloirsRecalcules = null;
    private Function<Tremie, Boolean> onTremiePlacee = null;
    private Consumer<Tremie> onTremieSupprimee = null;
    private final List<Couloir> couloirs = new ArrayList<>();
    private final Map<TreeItem<String>, Couloir> mapItemCouloir = new HashMap<>();

    // =========================================================================
    // CONSTRUCTEUR
    // =========================================================================

    public NiveauControleur(NiveauView vue, AireImmeuble aireImmeuble,
                            TreeItem<String> itemNiveau, Niveau niveau) {
        this.vue          = vue;
        this.aireImmeuble = aireImmeuble;
        this.itemNiveau   = itemNiveau;
        this.niveau       = niveau;

        if (aireImmeuble != null && aireImmeuble.isComplete()) {
            vue.getCanvas().ajouterElement(creerContourAire());
        }

        for (Mur m : niveau.getMursDelimiteurs()) {
            vue.getCanvas().ajouterElement(m);
        }

        brancherListeners();
    }

    private boolean segmentsSeRecouvrent(double ax1, double ay1, double ax2, double ay2,
                                         double bx1, double by1, double bx2, double by2,
                                         double tol) {
        double adx = ax2 - ax1;
        double ady = ay2 - ay1;
        double bdx = bx2 - bx1;
        double bdy = by2 - by1;
        double lenA = Math.hypot(adx, ady);
        double lenB = Math.hypot(bdx, bdy);
        if (lenA < tol || lenB < tol) return false;

        double crossDirs = Math.abs(adx * bdy - ady * bdx);
        if (crossDirs > tol * lenA * lenB) return false;

        double crossB1 = Math.abs((bx1 - ax1) * ady - (by1 - ay1) * adx);
        double crossB2 = Math.abs((bx2 - ax1) * ady - (by2 - ay1) * adx);
        if (crossB1 > tol * lenA || crossB2 > tol * lenA) return false;

        double ux = adx / lenA;
        double uy = ady / lenA;
        double b1 = (bx1 - ax1) * ux + (by1 - ay1) * uy;
        double b2 = (bx2 - ax1) * ux + (by2 - ay1) * uy;
        double debut = Math.max(0, Math.min(b1, b2));
        double fin = Math.min(lenA, Math.max(b1, b2));

        return fin - debut > tol;
    }

    // =========================================================================
    // CALLBACK — enregistrement par AppControleur après construction
    // =========================================================================

    /**
     * Enregistre le callback appelé à chaque création d'appartement.
     * Permet à AppControleur de peupler le NavigateurView sans couplage direct.
     *
     * @param callback fonction recevant l'Appartement créé et renvoyant
     *                 le TreeItem associé
     */
    public void setOnAppartementCree(Function<Appartement, TreeItem<String>> callback) {
        this.onAppartementCree = callback;
    }
    
    public void setOnCouloirCree(Function<Couloir, TreeItem<String>> callback) {
        this.onCouloirCree = callback;
    }

    public void setOnCouloirsRecalcules(Runnable callback) {
        this.onCouloirsRecalcules = callback;
    }

    public void setOnTremiePlacee(Function<Tremie, Boolean> callback) {
        this.onTremiePlacee = callback;
    }

    public void setOnTremieSupprimee(Consumer<Tremie> callback) {
        this.onTremieSupprimee = callback;
    }

    // =========================================================================
    // BRANCHEMENT DES LISTENERS SOURIS / CLAVIER
    // =========================================================================

    private void brancherListeners() {
        vue.getCanvas().setOnMouseClicked(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY
                    && e.isStillSincePress()) {
                clicCanvas(e);
            }
        });

        vue.getCanvas().setOnMouseMoved(this::mouvementCanvas);

        vue.getCanvas().setFocusTraversable(true);
        vue.getCanvas().setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                revenirEtatNeutre();
            } else if (e.getCode() == javafx.scene.input.KeyCode.DELETE
                    || e.getCode() == javafx.scene.input.KeyCode.BACK_SPACE) {
                supprimerTremieSelectionnee();
            }
        });

        // Le canvas capte le focus dès qu'on clique dessus (pour recevoir Échap)
        vue.getCanvas().setOnMousePressed(e -> vue.getCanvas().requestFocus());
    }

    // =========================================================================
    // DISPATCH DES CLICS
    // =========================================================================

    private void clicCanvas(javafx.scene.input.MouseEvent e) {
        Point2D snap = vue.getCanvas().snapToGrid(e.getX(), e.getY());

        if (!estDansAire(snap.getX(), snap.getY())) return;

        switch (mode) {
            case "MUR"         -> gererClicMur(snap);
            case "APPARTEMENT" -> gererClicAppartement(snap);
            case "ESCALIER"    -> gererClicTremie(snap, true);
            case "ASCENSEUR"   -> gererClicTremie(snap, false);
            case "SELECTION"   -> gererClicSelection(snap);
        }
    }

    private void mouvementCanvas(javafx.scene.input.MouseEvent e) {
        Point2D snap = vue.getCanvas().snapToGrid(e.getX(), e.getY());

        if (mode.equals("ESCALIER") || mode.equals("ASCENSEUR")) {
            Tremie fantome = mode.equals("ESCALIER")
                    ? new Escalier(new Point(snap.getX(), snap.getY()))
                    : new Ascenseur(new Point(snap.getX(), snap.getY()));
            vue.getCanvas().setFantome(fantome, snap.getX(), snap.getY(), 0,
                    tremieRentreeDansCouloir(snap.getX(), snap.getY()));
            vue.getCanvas().redrawAll();
            return;
        }

        if (!mode.equals("MUR")) return;

        if (vue.getOptionsMurVue().estRectangulaire()) {
            if (etapeRectangle == 1 && mur1Rect != null) {
                mur1Rect.setPoint2(new Point(snap.getX(), snap.getY()));
            } else if (etapeRectangle == 2 && mur2Rect != null) {
                mur2Rect.setPoint2(calculerPointOrthogonal(p2Rect,
                        new Point(snap.getX(), snap.getY())));
            }
        } else {
            if (mur1EnCours != null) {
                mur1EnCours.setPoint2(new Point(snap.getX(), snap.getY()));
            }
        }
        vue.getCanvas().redrawAll();
    }

    private void gererClicTremie(Point2D snap, boolean escalier) {
        double x = snap.getX();
        double y = snap.getY();

        if (!tremieRentreeDansCouloir(x, y)) {
            vue.setInstructions("Placement impossible : la trémie 2x2 m doit tenir entièrement dans un couloir.");
            return;
        }

        if (niveau.getTremies().stream().anyMatch(t -> tremiesSeChevauchent(t, x, y))) {
            vue.setInstructions("Placement impossible : une trémie existe déjà à cet emplacement.");
            return;
        }

        Tremie tremie = escalier ? new Escalier(new Point(x, y)) : new Ascenseur(new Point(x, y));
        boolean propagee = onTremiePlacee != null && Boolean.TRUE.equals(onTremiePlacee.apply(tremie));
        if (!propagee) {
            ajouterTremieDepuisPropagation(tremie);
        }
        vue.setInstructions((escalier ? "Escalier" : "Ascenseur")
                + " placé sur tous les niveaux du bâtiment.");
    }

    private boolean tremiesSeChevauchent(Tremie tremie, double x, double y) {
        return Math.abs(tremie.getX() - x) < Tremie.COTE
                && Math.abs(tremie.getY() - y) < Tremie.COTE;
    }

    private boolean tremieRentreeDansCouloir(double centreX, double centreY) {
        if (couloirs.isEmpty()) return false;

        double demi = Tremie.COTE / 2.0;
        double[][] points = {
                { centreX, centreY },
                { centreX - demi, centreY - demi },
                { centreX + demi, centreY - demi },
                { centreX + demi, centreY + demi },
                { centreX - demi, centreY + demi },
                { centreX, centreY - demi },
                { centreX + demi, centreY },
                { centreX, centreY + demi },
                { centreX - demi, centreY }
        };

        for (Couloir couloir : couloirs) {
            for (List<Point> polygone : couloir.getPolygones()) {
                boolean tousDedans = true;
                for (double[] point : points) {
                    if (!GeometrieUtils.estDansZone(point[0], point[1], polygone)) {
                        tousDedans = false;
                        break;
                    }
                }
                if (tousDedans) return true;
            }
        }
        return false;
    }

    private void gererClicSelection(Point2D snap) {
        Point pClic = new Point(snap.getX(), snap.getY());
        Tremie selection = trouverTremie(pClic);
        vue.getCanvas().setElementSelectionne(selection);
        if (selection != null) {
            vue.setInstructions("Trémie sélectionnée — Suppr pour la supprimer sur tous les niveaux.");
        } else {
            vue.setInstructions("Aucun élément sélectionné.");
        }
    }

    private Tremie trouverTremie(Point p) {
        for (int i = niveau.getTremies().size() - 1; i >= 0; i--) {
            Tremie tremie = niveau.getTremies().get(i);
            if (p.getX() >= tremie.getMinX() && p.getX() <= tremie.getMaxX()
                    && p.getY() >= tremie.getMinY() && p.getY() <= tremie.getMaxY()) {
                return tremie;
            }
        }
        return null;
    }

    public void supprimerTremieSelectionnee() {
        Object selection = vue.getCanvas().getElementSelectionne();
        if (selection instanceof Tremie tremie) {
            if (onTremieSupprimee != null) {
                onTremieSupprimee.accept(tremie);
            } else {
                supprimerTremieDepuisPropagation(tremie.getId());
            }
        }
    }

    public void ajouterTremieDepuisPropagation(Tremie tremie) {
        if (tremie == null) return;
        niveau.ajouterTremie(tremie);
        boolean existeCanvas = vue.getCanvas().getElements().stream()
                .anyMatch(d -> d instanceof Tremie t && t.getId().equals(tremie.getId()));
        if (!existeCanvas) {
            vue.getCanvas().ajouterElement(tremie);
        }
        vue.getCanvas().redrawAll();
    }

    public void supprimerTremieDepuisPropagation(String idTremie) {
        if (idTremie == null) return;
        niveau.supprimerTremie(idTremie);
        vue.getCanvas().getElements().removeIf(d -> d instanceof Tremie t && idTremie.equals(t.getId()));
        Object selection = vue.getCanvas().getElementSelectionne();
        if (selection instanceof Tremie t && idTremie.equals(t.getId())) {
            vue.getCanvas().setElementSelectionne(null);
        }
        vue.getCanvas().redrawAll();
    }

    // =========================================================================
    // GESTION DES MURS
    // =========================================================================

    private void gererClicMur(Point2D snap) {
        Point pClic = new Point(snap.getX(), snap.getY());

        if (vue.getOptionsMurVue().estRectangulaire()) {
            gererClicMurRectangulaire(pClic);
        } else {
            gererClicMurLibre(pClic);
        }
        vue.getCanvas().redrawAll();
    }

    private void gererClicMurLibre(Point pClic) {
        if (mur1EnCours == null) {
            mur1EnCours = new Mur(pClic, pClic);
            vue.getCanvas().ajouterElement(mur1EnCours);
            vue.setInstructions("Cliquez pour poser l'extrémité — Échap pour annuler");
        } else {
            mur1EnCours.setPoint2(pClic);
            mur1EnCours = null;
            vue.setInstructions("Cliquez pour démarrer un nouveau mur");
        }
    }

    private void gererClicMurRectangulaire(Point pClic) {
        switch (etapeRectangle) {
            case 0 -> {
                p1Rect   = pClic;
                mur1Rect = new Mur(p1Rect, p1Rect);
                vue.getCanvas().ajouterElement(mur1Rect);
                vue.setInstructions("Premier coin posé — cliquez pour la longueur");
                etapeRectangle = 1;
            }
            case 1 -> {
                p2Rect = pClic;
                mur1Rect.setPoint2(p2Rect);
                mur2Rect = new Mur(p2Rect, p2Rect);
                vue.getCanvas().ajouterElement(mur2Rect);
                vue.setInstructions("Longueur définie — cliquez pour la largeur");
                etapeRectangle = 2;
            }
            case 2 -> {
                Point p3 = calculerPointOrthogonal(p2Rect, pClic);
                mur2Rect.setPoint2(p3);
                Point p4 = new Point(
                        p1Rect.getX() + (p3.getX() - p2Rect.getX()),
                        p1Rect.getY() + (p3.getY() - p2Rect.getY()));
                vue.getCanvas().ajouterElement(new Mur(p3, p4));
                vue.getCanvas().ajouterElement(new Mur(p4, p1Rect));
                vue.setInstructions("Rectangle créé — cliquez pour un nouveau ou changez d'outil");
                reinitialiserRectangle();
            }
        }
    }

    /** Annule le mur ou le rectangle en cours de construction. */
    public void annulerMurEnCours() {
        if (mur1EnCours != null) {
            vue.getCanvas().getElements().remove(mur1EnCours);
            mur1EnCours = null;
        }
        if (mur1Rect != null) {
            vue.getCanvas().getElements().remove(mur1Rect);
        }
        if (mur2Rect != null) {
            vue.getCanvas().getElements().remove(mur2Rect);
        }
        reinitialiserRectangle();
        vue.getCanvas().clearFantome();
        vue.getCanvas().redrawAll();
    }

    public void revenirEtatNeutre() {
        annulerMurEnCours();
        vue.getCanvas().setElementSelectionne(null);
        mode = "AUCUN";
        vue.getCanvas().setPanActif(true);
        vue.getOptionsMurVue().setVisible(false);
        vue.setInstructions("Navigation â€” molette pour zoomer, clic droit pour dÃ©placer");
    }

    private void reinitialiserRectangle() {
        mur1Rect       = null;
        mur2Rect       = null;
        p1Rect         = null;
        p2Rect         = null;
        etapeRectangle = 0;
    }

    // =========================================================================
    // GESTION DES APPARTEMENTS
    // =========================================================================

    private void gererClicAppartement(Point2D snap) {
        double px = snap.getX(), py = snap.getY();

        // Refus si un appartement occupe déjà ce point
        for (Appartement a : appartements) {
            if (GeometrieUtils.pointDansPolygone(px, py, a.getPolygone())) {
                vue.setInstructions("Un appartement existe déjà dans cette zone.");
                return;
            }
        }

        // Détection de la zone fermée via l'algorithme de cycle minimal
        List<SegmentSource> sources = collecterSegmentsSources();
        List<SegmentSource> cycle   = GeometrieUtils.trouverCycleMinimal(px, py, sources);

        if (cycle == null || cycle.size() < 3) {
            vue.setInstructions(
                    "Aucune zone fermée ici — vérifiez que les murs se rejoignent bien.");
            return;
        }

        // Construire et ordonner les murs délimiteurs de l'appartement
        List<Mur> mursBruts = new ArrayList<>();
        for (SegmentSource ss : cycle) {
            mursBruts.add(ss.mur);
        }
        List<MurOriente> mursOrientes = GeometrieUtils.ordonnerMurs(mursBruts);
        List<Point> polygoneAppartement = new ArrayList<>();
        for (MurOriente murOriente : mursOrientes) {
            polygoneAppartement.add(murOriente.getPoint1());
        }

        if (zoneChevaucheTremie(polygoneAppartement)) {
            vue.setInstructions("Impossible de créer un appartement sur une trémie.");
            return;
        }

        // Créer l'appartement et l'ajouter au canvas
        compteurAppartements++;
        Appartement appart = niveau.ajouterAppartement(mursOrientes);
        appartements.add(appart);
        vue.getCanvas().ajouterElement(appart);

        // Notifier AppControleur pour peupler le navigateur
        if (onAppartementCree != null) {
            TreeItem<String> itemAppart = onAppartementCree.apply(appart);
            if (itemAppart != null) {
                mapItemAppartement.put(itemAppart, appart);
            }
        }

        vue.setInstructions(
                "« " + appart + " » créé — cliquez dans une autre zone pour en ajouter un.");
        vue.getCanvas().redrawAll();
        
        recalculerCouloirs();
    }
    
    private boolean zoneChevaucheTremie(List<Point> polygone) {
        if (polygone == null || polygone.size() < 3) return false;

        for (Tremie tremie : niveau.getTremies()) {
            List<Point> carre = List.of(
                    new Point(tremie.getMinX(), tremie.getMinY()),
                    new Point(tremie.getMaxX(), tremie.getMinY()),
                    new Point(tremie.getMaxX(), tremie.getMaxY()),
                    new Point(tremie.getMinX(), tremie.getMaxY())
            );

            if (GeometrieUtils.estDansZone(tremie.getX(), tremie.getY(), polygone)) {
                return true;
            }
            for (Point coin : carre) {
                if (GeometrieUtils.estDansZone(coin.getX(), coin.getY(), polygone)) {
                    return true;
                }
            }
            for (Point sommet : polygone) {
                if (sommet.getX() >= tremie.getMinX() && sommet.getX() <= tremie.getMaxX()
                        && sommet.getY() >= tremie.getMinY() && sommet.getY() <= tremie.getMaxY()) {
                    return true;
                }
            }
        }
        return false;
    }

    public void recalculerCouloirs() {
        for (Couloir c : couloirs) vue.getCanvas().getElements().remove(c);
        couloirs.clear();
        mapItemCouloir.clear();
        niveau.viderCouloirs();

        if (aireImmeuble == null || !aireImmeuble.isComplete()) return;

        List<Point> polyAire = List.of(
                aireImmeuble.getP1(), aireImmeuble.getP2(),
                aireImmeuble.getP3(), aireImmeuble.getP4()
        );

        double minX = polyAire.stream().mapToDouble(Point::getX).min().orElse(0);
        double maxX = polyAire.stream().mapToDouble(Point::getX).max().orElse(0);
        double minY = polyAire.stream().mapToDouble(Point::getY).min().orElse(0);
        double maxY = polyAire.stream().mapToDouble(Point::getY).max().orElse(0);

        double pas = 0.5;
        List<List<Point>> zonesDejaDetectees = new ArrayList<>();

        Couloir couloir = new Couloir(niveau.getHauteurPlafond());

        for (double x = minX + pas / 2; x < maxX; x += pas) {
            for (double y = minY + pas / 2; y < maxY; y += pas) {
                final double fx = x, fy = y;

                if (!GeometrieUtils.estDansZone(fx, fy, polyAire)) continue;

                boolean dansAppart = appartements.stream().anyMatch(a ->
                        GeometrieUtils.pointDansPolygone(fx, fy, a.getPolygone()));
                if (dansAppart) continue;

                boolean dejaCouvert = zonesDejaDetectees.stream().anyMatch(poly ->
                        GeometrieUtils.pointDansPolygone(fx, fy, poly));
                if (dejaCouvert) continue;

                List<SegmentSource> sources = collecterSegmentsSources();
                List<SegmentSource> cycle = GeometrieUtils.trouverCycleMinimal(fx, fy, sources);
                if (cycle == null || cycle.size() < 3) continue;

                List<Mur> mursZone = new ArrayList<>();
                for (SegmentSource ss : cycle) mursZone.add(ss.mur);
                List<MurOriente> mursZoneOrientes = GeometrieUtils.ordonnerMurs(mursZone);
                mursZone = new ArrayList<>();
                for (MurOriente murOriente : mursZoneOrientes) {
                    mursZone.add(murOriente.mur());
                }

                List<Point> polygone = new ArrayList<>();
                for (MurOriente m : mursZoneOrientes) polygone.add(m.getPoint1());

                boolean estAppart = appartements.stream().anyMatch(a ->
                        polygonesSontEquivalents(polygone, a.getPolygone()));
                if (estAppart) continue;

                zonesDejaDetectees.add(polygone);
                couloir.ajouterZoneOriente(mursZoneOrientes); // ajouter au couloir unique
            }
        }

        if (!couloir.getPolygones().isEmpty()) {
            couloirs.add(couloir);
            niveau.ajouterCouloir(couloir);

            if (onCouloirCree != null) {
                TreeItem<String> itemCouloir = onCouloirCree.apply(couloir);
                if (itemCouloir != null) mapItemCouloir.put(itemCouloir, couloir);
            }
        }

        vue.getCanvas().redrawAll();
        if (onCouloirsRecalcules != null) {
            onCouloirsRecalcules.run();
        }
    }

    private boolean polygonesSontEquivalents(List<Point> p1, List<Point> p2) {
        if (p1.size() != p2.size()) return false;
        double tol = 0.05;
        return p1.stream().allMatch(a ->
                p2.stream().anyMatch(b ->
                        Math.abs(a.getX() - b.getX()) < tol &&
                        Math.abs(a.getY() - b.getY()) < tol));
    }
    
    private void marquerMursAdjacents(Couloir couloir) {
        double tol = 0.05;
        List<Mur> mursDelimiteurs = niveau.getMursDelimiteurs();

        // Collecter tous les segments du couloir
        List<double[]> segmentsCouloir = new ArrayList<>();
        for (List<Mur> zone : couloir.getZonesDelimiteurs()) {
            List<Point> polygone = new ArrayList<>();
            for (Mur m : zone) polygone.add(m.getPoint1());
            int n = polygone.size();
            for (int i = 0; i < n; i++) {
                Point a = polygone.get(i);
                Point b = polygone.get((i + 1) % n);
                segmentsCouloir.add(new double[]{a.getX(), a.getY(), b.getX(), b.getY()});
            }
        }

        // Parcourir tous les murs du canvas pour trouver lesquels touchent le couloir
        for (Object el : vue.getCanvas().getElements()) {
            if (!(el instanceof Mur mur)) continue;
            if (mursDelimiteurs.contains(mur)) continue;
            if (mur.getTypeMur() == Mur.TypeMur.EXTERIEUR) continue;

            double ax1 = mur.getPoint1().getX(), ay1 = mur.getPoint1().getY();
            double ax2 = mur.getPoint2().getX(), ay2 = mur.getPoint2().getY();

            for (double[] seg : segmentsCouloir) {
                if (segmentsSeRecouvrent(ax1, ay1, ax2, ay2,
                                          seg[0], seg[1], seg[2], seg[3], tol)) {
                    // Si le mur touche le couloir, on lui donne le type ADJ_COULOIR
                    // Ancien marquage direct volontairement neutralise.
                    
                    // Si ce morceau appartient à un mur original, on met aussi à jour l'original
                    if (mur.getOriginal() != null) {
                        // Ancien marquage direct volontairement neutralise.
                    }
                    break;
                }
            }
        }
    }

    // =========================================================================
    // COLLECTE DES SEGMENTS (délègue la subdivision à GeometrieUtils)
    // =========================================================================

    /**
     * Collecte tous les segments pertinents du niveau (contour de l'aire +
     * murs intérieurs), les subdivise aux intersections et les déduplique
     * via {@link GeometrieUtils#subdiviserEtDeduplicer(List)}.
     */
    private List<SegmentSource> collecterSegmentsSources() {
        List<SegmentSource> bruts = new ArrayList<>();

        // Frontière de l'aire de l'immeuble
        if (aireImmeuble != null && aireImmeuble.isComplete()) {
            Point p1 = aireImmeuble.getP1(), p2 = aireImmeuble.getP2();
            Point p3 = aireImmeuble.getP3(), p4 = aireImmeuble.getP4();
            bruts.add(new SegmentSource(p1, p2, new Mur(p1, p2)));
            bruts.add(new SegmentSource(p2, p3, new Mur(p2, p3)));
            bruts.add(new SegmentSource(p3, p4, new Mur(p3, p4)));
            bruts.add(new SegmentSource(p4, p1, new Mur(p4, p1)));
        }

        // Murs intérieurs dessinés sur le canvas (hors murs délimiteurs du niveau)
        List<Mur> mursDelimiteurs = niveau.getMursDelimiteurs();
        for (Object el : vue.getCanvas().getElements()) {
            if (el instanceof Mur m && !mursDelimiteurs.contains(m)) {
                bruts.add(new SegmentSource(m.getPoint1(), m.getPoint2(), m));
            }
        }

        return GeometrieUtils.subdiviserEtDeduplicer(bruts);
    }

    // =========================================================================
    // UTILITAIRES GÉOMÉTRIQUES LOCAUX
    // =========================================================================

    /** Vérifie si un point est dans le polygone de l'aire (ou sur son contour). */
    private boolean estDansAire(double px, double py) {
        if (aireImmeuble == null || !aireImmeuble.isComplete()) return true;
        List<Point> poly = List.of(
                aireImmeuble.getP1(), aireImmeuble.getP2(),
                aireImmeuble.getP3(), aireImmeuble.getP4());
        return GeometrieUtils.estDansZone(px, py, poly);
    }

    /**
     * Calcule le point sur la perpendiculaire à [p1Rect→p2Rect] passant par
     * {@code centre}, projeté depuis {@code cible}.
     */
    private Point calculerPointOrthogonal(Point centre, Point cible) {
        double dx    = p2Rect.getX() - p1Rect.getX();
        double dy    = p2Rect.getY() - p1Rect.getY();
        double perpX = -dy, perpY = dx;
        double ux    = cible.getX() - centre.getX();
        double uy    = cible.getY() - centre.getY();
        double s     = (ux * perpX + uy * perpY) / (perpX * perpX + perpY * perpY);
        return new Point(centre.getX() + s * perpX, centre.getY() + s * perpY);
    }
    
    public boolean estAdjacentsAuCouloir(Mur mur) {
        double x1 = mur.getPoint1().getX(), y1 = mur.getPoint1().getY();
        double x2 = mur.getPoint2().getX(), y2 = mur.getPoint2().getY();
        double dx = x2 - x1, dy = y2 - y1;
        double len = Math.hypot(dx, dy);
        if (len < 1e-6) return false;

        if (estSurBordCouloir(mur)) return true;

        double nx = -dy / len;
        double ny = dx / len;
        double decalage = 0.10;
        double[] positions = {0.10, 0.25, 0.50, 0.75, 0.90};

        for (double t : positions) {
            double mx = x1 + t * dx;
            double my = y1 + t * dy;
            if (pointDansUnCouloir(mx + decalage * nx, my + decalage * ny)
                    || pointDansUnCouloir(mx - decalage * nx, my - decalage * ny)) {
                return true;
            }
        }
        return false;
    }

    private boolean estSurBordCouloir(Mur mur) {
        double tol = 0.05;
        double ax1 = mur.getPoint1().getX(), ay1 = mur.getPoint1().getY();
        double ax2 = mur.getPoint2().getX(), ay2 = mur.getPoint2().getY();

        for (Couloir couloir : couloirs) {
            for (List<Point> polygone : couloir.getPolygones()) {
                int n = polygone.size();
                for (int i = 0; i < n; i++) {
                    Point b1 = polygone.get(i);
                    Point b2 = polygone.get((i + 1) % n);
                    if (segmentsSeRecouvrent(ax1, ay1, ax2, ay2,
                            b1.getX(), b1.getY(), b2.getX(), b2.getY(), tol)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean pointDansUnCouloir(double x, double y) {
        for (Couloir couloir : couloirs) {
            for (List<Point> polygone : couloir.getPolygones()) {
                if (GeometrieUtils.pointDansPolygone(x, y, polygone)) {
                    return true;
                }
            }
        }
        return false;
    }

    // =========================================================================
    // CONTOUR DE L'AIRE (dessin en lecture seule)
    // =========================================================================

    private Dessin creerContourAire() {
        return new Dessin() {
            @Override
            public void dessiner(GraphicsContext gc) {
                Point p1 = aireImmeuble.getP1(), p2 = aireImmeuble.getP2();
                Point p3 = aireImmeuble.getP3(), p4 = aireImmeuble.getP4();
                double[] xs = {p1.getX(), p2.getX(), p3.getX(), p4.getX()};
                double[] ys = {p1.getY(), p2.getY(), p3.getY(), p4.getY()};

                gc.setFill(Color.web("#4a90d9", 0.05));
                gc.fillPolygon(xs, ys, 4);

                gc.setStroke(Color.web("#999999", 0.7));
                gc.setLineWidth(0.05);
                gc.setLineDashes(0.2, 0.15);
                gc.strokePolygon(xs, ys, 4);
                gc.setLineDashes();
            }

            @Override public Color getColor()        { return Color.GRAY; }
            @Override public void  setColor(Color c) { }
        };
    }

    // =========================================================================
    // CONTRÔLE DU MODE
    // =========================================================================

    public void activerModeMur() {
        annulerMurEnCours();
        vue.getCanvas().setElementSelectionne(null);
        mode = "MUR";
        vue.getCanvas().setPanActif(false);
        vue.getOptionsMurVue().setVisible(true);

        // Remplacer l'ancien listener de forme pour éviter les doublons
        if (listenerForme != null) {
            vue.getOptionsMurVue().getGroupeForme()
               .selectedToggleProperty().removeListener(listenerForme);
        }
        listenerForme = (obs, oldVal, newVal) -> mettreAJourInstructionsMur();
        vue.getOptionsMurVue().getGroupeForme()
           .selectedToggleProperty().addListener(listenerForme);

        mettreAJourInstructionsMur();
    }

    public void activerModeAppartement() {
        annulerMurEnCours();
        vue.getCanvas().setElementSelectionne(null);
        mode = "APPARTEMENT";
        vue.getCanvas().setPanActif(false);
        vue.getOptionsMurVue().setVisible(false);
        vue.setInstructions(
                "Cliquez à l'intérieur d'une zone fermée pour créer un appartement");
    }

    public void activerModeNavigation() {
        annulerMurEnCours();
        vue.getCanvas().setElementSelectionne(null);
        mode = "AUCUN";
        vue.getCanvas().setPanActif(true);
        vue.getOptionsMurVue().setVisible(false);
        vue.setInstructions(
                "Navigation — molette pour zoomer, clic droit pour déplacer");
    }


    public void activerModeSelection() {
        annulerMurEnCours();
        mode = "SELECTION";
        vue.getCanvas().setPanActif(false);
        vue.getOptionsMurVue().setVisible(false);
        vue.setInstructions(
                "Outil Sélection/Édition actif — Sélectionnez un élément pour voir ses propriétés");
    }

    public void activerModeEscalier() {
        annulerMurEnCours();
        vue.getCanvas().setElementSelectionne(null);
        mode = "ESCALIER";
        vue.getCanvas().setPanActif(false);
        vue.getOptionsMurVue().setVisible(false);
        vue.setInstructions("Cliquez dans un couloir pour placer un escalier 2x2 m sur tous les niveaux");
    }

    public void activerModeAscenseur() {
        annulerMurEnCours();
        vue.getCanvas().setElementSelectionne(null);
        mode = "ASCENSEUR";
        vue.getCanvas().setPanActif(false);
        vue.getOptionsMurVue().setVisible(false);
        vue.setInstructions("Cliquez dans un couloir pour placer un ascenseur 2x2 m sur tous les niveaux");
    }

    private void mettreAJourInstructionsMur() {
        if (vue.getOptionsMurVue().estRectangulaire()) {
            vue.setInstructions("Mode rectangle — cliquez pour le premier coin");
        } else {
            vue.setInstructions("Mode libre — cliquez pour le début du mur");
        }
    }
   
   public void rechargerAppartements(List<Appartement> appartementsCharges,
                                      Map<TreeItem<String>, Appartement> mapItems) {
       for (Map.Entry<TreeItem<String>, Appartement> entry : mapItems.entrySet()) {
           Appartement appart = entry.getValue();
           appartements.add(appart);
           mapItemAppartement.put(entry.getKey(), appart);

           // Ajouter les murs de l'appartement au canvas (pour que le dessin soit complet)
           for (Mur mur : appart.getMursDelimiteurs()) {
               if (!vue.getCanvas().getElements().contains(mur)) {
                   vue.getCanvas().getElements().add(mur);
               }
           }

           // Ajouter le polygone coloré de l'appartement
           vue.getCanvas().getElements().add(appart);
       }
       vue.getCanvas().redrawAll();
   }

   public void rechargerCouloirs(List<Couloir> couloirsCharges) {
       if (couloirsCharges == null) return;

       for (Couloir couloir : couloirsCharges) {
           if (couloir == null) continue;
           couloirs.add(couloir);

           if (onCouloirCree != null) {
               TreeItem<String> itemCouloir = onCouloirCree.apply(couloir);
               if (itemCouloir != null) mapItemCouloir.put(itemCouloir, couloir);
           }
       }
       vue.getCanvas().redrawAll();
   }

   public void rechargerTremies() {
       for (Tremie tremie : niveau.getTremies()) {
           if (tremie == null) continue;
           boolean existeCanvas = vue.getCanvas().getElements().stream()
                   .anyMatch(d -> d instanceof Tremie t && t.getId().equals(tremie.getId()));
           if (!existeCanvas) {
               vue.getCanvas().ajouterElement(tremie);
           }
       }
       vue.getCanvas().redrawAll();
   }

    // =========================================================================
    // GETTERS
    // =========================================================================

    public NiveauView   getVue()    { return vue;    }
    public Niveau       getNiveau() { return niveau; }

    public Map<TreeItem<String>, Appartement> getMapItemAppartement() { return mapItemAppartement; }
    public Map<TreeItem<String>, Couloir> getMapItemCouloir() { return mapItemCouloir; }
    public List<Couloir> getCouloirs() { return couloirs; }
    public List<Tremie> getTremies() { return niveau.getTremies(); }
}
