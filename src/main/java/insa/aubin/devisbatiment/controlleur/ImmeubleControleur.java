package insa.aubin.devisbatiment.controlleur;

import insa.aubin.devisbatiment.modele.AireImmeuble;
import insa.aubin.devisbatiment.modele.GestionnaireSauvegarde;
import insa.aubin.devisbatiment.modele.Point;
import insa.aubin.devisbatiment.view.DessinCanvas;
import insa.aubin.devisbatiment.view.DashBoardView;
import insa.aubin.devisbatiment.view.ImmeubleView;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeItem;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ImmeubleControleur {
    private ImmeubleView vue;
    private Stage stage;
    private GestionnaireSauvegarde gestionnaire;

    // Aire de l'immeuble
    private AireImmeuble aireImmeuble;
    private int etapeAire = 0;
    private boolean aireValidee = false;
    private int coteEnCoursDeDeplacement = -1;

    // Niveaux : chaque niveau a son propre canvas
    private List<DessinCanvas> canvasNiveaux = new ArrayList<>();
    // TreeItems des niveaux
    private TreeItem<String> itemNiveaux;

    public ImmeubleControleur(ImmeubleView vue, Stage stage, GestionnaireSauvegarde gestionnaire) {
        this.vue = vue;
        this.stage = stage;
        this.gestionnaire = gestionnaire;

        // Nœud "Niveaux" dans le TreeView
        itemNiveaux = new TreeItem<>("Niveaux");
        itemNiveaux.setExpanded(true);

        // Nœud "Aire" cliquable dans le TreeView
        TreeItem<String> itemAire = new TreeItem<>("Aire de l'immeuble");
        this.vue.getRootItem().getChildren().addAll(itemAire, itemNiveaux);

        // Clic sur le TreeView → bascule de canvas
        this.vue.getTreeView().getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (newVal == null) return;
                if (newVal == itemAire) {
                    // Affiche le canvas de l'aire
                    this.vue.afficherCanvasAire();
                } else {
                    // Cherche si c'est un item de niveau
                    int index = itemNiveaux.getChildren().indexOf(newVal);
                    if (index >= 0 && index < canvasNiveaux.size()) {
                        this.vue.afficherCanvas(canvasNiveaux.get(index));
                    }
                }
            }
        );

        // Listeners de dessin de l'aire
        this.vue.getCanvas().setOnMouseClicked(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY
                    && e.isStillSincePress()) {
                this.clicAire(e);
            }
        });
        this.vue.getCanvas().setOnMouseMoved(e -> this.mouvementAire(e));
        this.vue.getCanvas().setOnMousePressed(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                this.pressionAire(e);
            }
        });
        this.vue.getCanvas().setOnMouseDragged(e -> this.glisserAire(e));
        this.vue.getCanvas().setOnMouseReleased(e -> this.relacherAire(e));

        Platform.runLater(this::demanderNomImmeuble);
    }

    private void demanderNomImmeuble() {
        TextInputDialog dialog = new TextInputDialog("Nouvel Immeuble");
        dialog.setTitle("Nom de l'immeuble");
        dialog.setHeaderText("Initialisation du projet");
        dialog.setContentText("Veuillez entrer le nom de l'immeuble :");
        dialog.setGraphic(null);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (dialog.getEditor().getText().trim().isEmpty()) {
                event.consume();
                dialog.getEditor().setStyle("-fx-border-color: red;");
            }
        });

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            this.vue.getRootItem().setValue("Aire de l'Immeuble : ( " + result.get().trim() + " )");
        } else {
            retourDashboard();
        }
    }

    // --- Gestion de l'aire ---

    private void clicAire(javafx.scene.input.MouseEvent e) {
        if (aireValidee) return;

        Point2D snap = this.vue.getCanvas().snapToGrid(e.getX(), e.getY());
        Point pClic = new Point(snap.getX(), snap.getY());

        switch (etapeAire) {
            case 0:
                aireImmeuble = new AireImmeuble(pClic);
                this.vue.getCanvas().ajouterElement(aireImmeuble);
                etapeAire = 1;
                this.vue.setInstructions("Cliquez pour définir le deuxième coin (côté 1)");
                break;
            case 1:
                aireImmeuble.setP2(pClic);
                etapeAire = 2;
                this.vue.setInstructions("Cliquez pour définir la largeur (côté 2)");
                break;
            case 2:
                Point p3Contraint = calculerPointOrthogonal(
                    aireImmeuble.getP2(), pClic,
                    aireImmeuble.getP1(), aireImmeuble.getP2()
                );
                aireImmeuble.setP3(p3Contraint);
                etapeAire = 3;
                this.vue.setInstructions("Glissez les côtés pour ajuster, puis cliquez sur « Valider »");
                this.vue.getBtnValiderAire().setDisable(false);
                break;
        }
        this.vue.getCanvas().redrawAll();
    }

    private void mouvementAire(javafx.scene.input.MouseEvent e) {
        if (aireValidee || aireImmeuble == null) return;

        Point2D snap = this.vue.getCanvas().snapToGrid(e.getX(), e.getY());
        Point pSouris = new Point(snap.getX(), snap.getY());

        if (etapeAire == 1) {
            aireImmeuble.setP2(pSouris);
        } else if (etapeAire == 2) {
            Point p3Contraint = calculerPointOrthogonal(
                aireImmeuble.getP2(), pSouris,
                aireImmeuble.getP1(), aireImmeuble.getP2()
            );
            aireImmeuble.setP3(p3Contraint);
        } else if (etapeAire == 3) {
            // Surbrillance du côté survolé
            int coteSurvole = aireImmeuble.detecterCote(snap.getX(), snap.getY(), 0.3);
            aireImmeuble.setCoteGlisse(coteSurvole);
        }
        this.vue.getCanvas().redrawAll();
    }

    private void pressionAire(javafx.scene.input.MouseEvent e) {
        if (aireValidee || aireImmeuble == null || etapeAire != 3) return;

        Point2D snap = this.vue.getCanvas().snapToGrid(e.getX(), e.getY());
        coteEnCoursDeDeplacement = aireImmeuble.detecterCote(snap.getX(), snap.getY(), 0.3);
        if (coteEnCoursDeDeplacement != -1) {
            aireImmeuble.setCoteGlisse(coteEnCoursDeDeplacement);
            this.vue.getCanvas().redrawAll();
        }
    }

    private void glisserAire(javafx.scene.input.MouseEvent e) {
        if (coteEnCoursDeDeplacement == -1 || aireImmeuble == null) return;

        Point2D snap = this.vue.getCanvas().snapToGrid(e.getX(), e.getY());
        aireImmeuble.deplacerCote(coteEnCoursDeDeplacement, snap.getX(), snap.getY());
        this.vue.getCanvas().redrawAll();
    }

    private void relacherAire(javafx.scene.input.MouseEvent e) {
        if (coteEnCoursDeDeplacement != -1) {
            coteEnCoursDeDeplacement = -1;
            aireImmeuble.setCoteGlisse(-1);
            this.vue.getCanvas().redrawAll();
        }
    }

    public void annulerAire() {
        if (aireValidee) return;
        if (aireImmeuble != null) {
            this.vue.getCanvas().getElements().remove(aireImmeuble);
        }
        aireImmeuble = null;
        etapeAire = 0;
        coteEnCoursDeDeplacement = -1;
        this.vue.getBtnValiderAire().setDisable(true);
        this.vue.setInstructions("Cliquez pour définir le premier coin de l'immeuble");
        this.vue.getCanvas().redrawAll();
    }

    public void btnValiderAire(ActionEvent t) {
        if (aireImmeuble == null || etapeAire != 3) return;
        aireImmeuble.valider();
        aireValidee = true;

        // Voile cadenas + bascule des boutons
        this.vue.activerVoile();
        this.vue.basculerBoutonsApresValidation();
        this.vue.setInstructions("Aire validée — ajoutez des niveaux via le bouton ou le navigateur");

        // Désactive les listeners de dessin de l'aire
        this.vue.getCanvas().setOnMouseClicked(null);
        this.vue.getCanvas().setOnMouseMoved(null);
        this.vue.getCanvas().setOnMousePressed(null);
        this.vue.getCanvas().setOnMouseDragged(null);
        this.vue.getCanvas().setOnMouseReleased(null);
    }

    /** Ajoute un niveau : RDC pour le premier, puis Niveau 1, 2, ... */
    public void btnAjouterNiveau(ActionEvent t) {
        int nb = canvasNiveaux.size();
        String nomNiveau = (nb == 0) ? "RDC" : "Niveau " + nb;

        // Nouveau canvas dédié à ce niveau
        DessinCanvas canvasNiveau = new DessinCanvas();
        canvasNiveaux.add(canvasNiveau);

        // Ajout dans le TreeView
        TreeItem<String> itemNiveau = new TreeItem<>(nomNiveau);
        itemNiveaux.getChildren().add(itemNiveau);

        // Sélection automatique du nouveau niveau → affiche son canvas
        this.vue.getTreeView().getSelectionModel().select(itemNiveau);
    }

    // --- Autres boutons ---

    public void btnNavigation(ActionEvent t) {
        this.vue.getCanvas().setPanActif(true);
    }

    public void btnEchelle(ActionEvent t) {
        boolean visible = !this.vue.getEchelleVue().isVisible();
        this.vue.getEchelleVue().setVisible(visible);

        if (visible) {
            this.vue.getEchelleVue().getGroupeEchelle().selectedToggleProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        double echelle = this.vue.getEchelleVue().getEchelleSelectionnee();
                        this.vue.getCanvas().setGridSize(echelle);
                    }
                }
            );
        }
    }

    public void retourDashboard() {
        DashBoardView dashBoardView = new DashBoardView();
        Scene dashScene = new Scene(dashBoardView);
        stage.setScene(dashScene);
        stage.setTitle("InsaBuilder - Tableau de bord");
        new DashBoardControleur(dashBoardView, stage, gestionnaire);
    }

    private Point calculerPointOrthogonal(Point centre, Point cible, Point refP1, Point refP2) {
        double dx = refP2.getX() - refP1.getX();
        double dy = refP2.getY() - refP1.getY();
        double perpX = -dy;
        double perpY = dx;
        double ux = cible.getX() - centre.getX();
        double uy = cible.getY() - centre.getY();
        double scalaire = (ux * perpX + uy * perpY) / (perpX * perpX + perpY * perpY);
        return new Point(centre.getX() + scalaire * perpX, centre.getY() + scalaire * perpY);
    }
}