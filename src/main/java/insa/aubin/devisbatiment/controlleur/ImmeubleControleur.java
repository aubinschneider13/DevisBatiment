package insa.aubin.devisbatiment.controlleur;

import insa.aubin.devisbatiment.modele.*;
import insa.aubin.devisbatiment.view.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ImmeubleControleur {
    private ImmeubleView vue;
    private Stage stage;
    private GestionnaireSauvegarde gestionnaire;
    private javafx.beans.value.ChangeListener<javafx.scene.control.Toggle> listenerEchelle = null;

    // --- Aire de l'immeuble ---
    private AireImmeuble aireImmeuble;
    private int etapeAire = 0;
    private boolean aireValidee = false;
    private int coteEnCoursDeDeplacement = -1;

    // ✅ DOC 13 : modèle métier
    private Immeuble immeuble = null;

    // --- Niveaux ---
    private final List<NiveauControleur> niveauControleurs = new ArrayList<>();
    private TreeItem<String> itemNiveaux;
    private NiveauControleur niveauActuel = null;

    // ✅ DOC 11 : PieceView courante
    private PieceView pieceVueCourante = null;

    public ImmeubleControleur(ImmeubleView vue, Stage stage,
                              GestionnaireSauvegarde gestionnaire) {
        this.vue = vue;
        this.stage = stage;
        this.gestionnaire = gestionnaire;

        itemNiveaux = new TreeItem<>("Niveaux");
        itemNiveaux.setExpanded(true);

        TreeItem<String> itemAire = new TreeItem<>("Aire de l'immeuble");
        this.vue.getRootItem().getChildren().addAll(itemAire, itemNiveaux);

        // ✅ FUSION : listener complet avec guard aireValidee + logique appartement
        this.vue.getTreeView().getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal == null) return;

                    // ✅ DOC 11 : guard — ne pas traiter avant validation de l'aire
                    if (!aireValidee) return;

                    if (newVal == itemAire) {
                        niveauActuel = null;
                        pieceVueCourante = null;
                        this.vue.afficherCanvasAire();
                        return;
                    }

                    for (int i = 0; i < itemNiveaux.getChildren().size(); i++) {
                        TreeItem<String> itemNiveau = itemNiveaux.getChildren().get(i);
                        NiveauControleur ctrl = niveauControleurs.get(i); // ✅ ctrl déclaré ici

                        if (newVal == itemNiveau) {
                            // Clic sur le niveau → canvas du niveau
                            pieceVueCourante = null;
                            basculerVersNiveau(i);
                            return;
                        }

                        // ✅ DOC 11 : clic sur un appartement → PieceView
                        Appartement appart = ctrl.getMapItemAppartement().get(newVal);
                        if (appart != null) {
                            basculerVersNiveau(i);
                            ouvrirPieceDepuisAppartement(appart);
                            return;
                        }
                    }
                }
        );

        // Listeners canvas de l'aire
        this.vue.getCanvas().setOnMouseClicked(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY
                    && e.isStillSincePress()) clicAire(e);
        });
        this.vue.getCanvas().setOnMouseMoved(e -> mouvementAire(e));
        this.vue.getCanvas().setOnMousePressed(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) pressionAire(e);
        });
        this.vue.getCanvas().setOnMouseDragged(e -> glisserAire(e));
        this.vue.getCanvas().setOnMouseReleased(e -> relacherAire(e));

        Platform.runLater(this::demanderNomImmeuble);
    }

    // =========================================================================
    // BASCULE ENTRE NIVEAUX
    // =========================================================================

    private void basculerVersNiveau(int index) {
        niveauActuel = niveauControleurs.get(index);
        this.vue.afficherNiveau(niveauActuel.getVue());
    }

    // ✅ DOC 11 : ouverture de la PieceView
    private void ouvrirPieceDepuisAppartement(Appartement appartement) {
        pieceVueCourante = new PieceView(stage, gestionnaire, appartement);
        this.vue.afficherPiece(pieceVueCourante);
        this.vue.setInstructions(
                "Vue pièce de « " + appartement + " » — dessinez les murs intérieurs"
        );
    }

    // =========================================================================
    // BOUTONS DE LA TOOLBAR
    // =========================================================================

    public void btnNavigation(ActionEvent t) {
        if (niveauActuel != null) niveauActuel.activerModeNavigation();
        else this.vue.getCanvas().setPanActif(true);
    }

    public void btnMur(ActionEvent t) {
        if (niveauActuel != null) niveauActuel.activerModeMur();
    }

    public void btnAppartement(ActionEvent t) {
        if (niveauActuel != null) niveauActuel.activerModeAppartement();
    }

    public void btnEchelle(ActionEvent t) {
        boolean visible = !this.vue.getEchelleVue().isVisible();
        this.vue.getEchelleVue().setVisible(visible);

        if (visible && listenerEchelle == null) {
            listenerEchelle = (obs, oldVal, newVal) -> {
                if (newVal != null) {
                    double echelle = this.vue.getEchelleVue().getEchelleSelectionnee();
                    if (niveauActuel != null) {
                        niveauActuel.getVue().getCanvas().setGridSize(echelle);
                    } else {
                        this.vue.getCanvas().setGridSize(echelle);
                    }
                }
            };
            this.vue.getEchelleVue().getGroupeEchelle()
                .selectedToggleProperty().addListener(listenerEchelle);
        }
    }

    // ✅ DOC 13 : utilise immeuble.ajouterNiveau()
    public void btnAjouterNiveau(ActionEvent t) {
        if (immeuble == null) return;

        int nb = niveauControleurs.size();
        String nomNiveau = (nb == 0) ? "RDC" : "Niveau " + nb;

        Niveau niveau = immeuble.ajouterNiveau(2.5);

        NiveauView niveauView = new NiveauView();
        TreeItem<String> itemNiveau = new TreeItem<>(nomNiveau);
        itemNiveaux.getChildren().add(itemNiveau);

        NiveauControleur ctrl = new NiveauControleur(
                niveauView, aireImmeuble, itemNiveau, niveau
        );
        niveauControleurs.add(ctrl);

        this.vue.getTreeView().getSelectionModel().select(itemNiveau);
    }

    // =========================================================================
    // GESTION DE L'AIRE
    // =========================================================================

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
            this.vue.getRootItem().setValue(
                    "Immeuble : ( " + result.get().trim() + " )"
            );
        } else {
            retourDashboard();
        }
    }

    private void clicAire(javafx.scene.input.MouseEvent e) {
        if (aireValidee) return;

        Point2D snap = this.vue.getCanvas().snapToGrid(e.getX(), e.getY());
        Point pClic = new Point(snap.getX(), snap.getY());

        switch (etapeAire) {
            case 0:
                aireImmeuble = new AireImmeuble(pClic);
                this.vue.getCanvas().ajouterElement(aireImmeuble);
                etapeAire = 1;
                this.vue.setInstructions(
                        "Cliquez pour définir le deuxième coin (côté 1)");
                break;
            case 1:
                aireImmeuble.setP2(pClic);
                etapeAire = 2;
                this.vue.setInstructions(
                        "Cliquez pour définir la largeur (côté 2)");
                break;
            case 2:
                Point p3Contraint = calculerPointOrthogonal(
                        aireImmeuble.getP2(), pClic,
                        aireImmeuble.getP1(), aireImmeuble.getP2()
                );
                aireImmeuble.setP3(p3Contraint);
                etapeAire = 3;
                this.vue.setInstructions(
                        "Glissez les côtés pour ajuster, puis cliquez sur « Valider »");
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
            int coteSurvole = aireImmeuble.detecterCote(
                    snap.getX(), snap.getY(), 0.3);
            aireImmeuble.setCoteGlisse(coteSurvole);
        }
        this.vue.getCanvas().redrawAll();
    }

    private void pressionAire(javafx.scene.input.MouseEvent e) {
        if (aireValidee || aireImmeuble == null || etapeAire != 3) return;

        Point2D snap = this.vue.getCanvas().snapToGrid(e.getX(), e.getY());
        coteEnCoursDeDeplacement = aireImmeuble.detecterCote(
                snap.getX(), snap.getY(), 0.3);
        if (coteEnCoursDeDeplacement != -1) {
            aireImmeuble.setCoteGlisse(coteEnCoursDeDeplacement);
            this.vue.getCanvas().redrawAll();
        }
    }

    private void glisserAire(javafx.scene.input.MouseEvent e) {
        if (coteEnCoursDeDeplacement == -1 || aireImmeuble == null) return;

        Point2D snap = this.vue.getCanvas().snapToGrid(e.getX(), e.getY());
        aireImmeuble.deplacerCote(
                coteEnCoursDeDeplacement, snap.getX(), snap.getY());
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
        this.vue.setInstructions(
                "Cliquez pour définir le premier coin de l'immeuble");
        this.vue.getCanvas().redrawAll();
    }

    // ✅ DOC 13 : instancie Immeuble + extraireNomImmeuble
    public void btnValiderAire(ActionEvent t) {
        if (aireImmeuble == null || etapeAire != 3) return;
        aireImmeuble.valider();
        aireValidee = true;

        String nomImmeuble = extraireNomImmeuble(
                this.vue.getRootItem().getValue());
        immeuble = new Immeuble(nomImmeuble, aireImmeuble);

        this.vue.activerVoile();
        this.vue.basculerBoutonsApresValidation();
        this.vue.setInstructions(
                "Aire validée — ajoutez des niveaux via le bouton ou le navigateur");

        this.vue.getCanvas().setOnMouseClicked(null);
        this.vue.getCanvas().setOnMouseMoved(null);
        this.vue.getCanvas().setOnMousePressed(null);
        this.vue.getCanvas().setOnMouseDragged(null);
        this.vue.getCanvas().setOnMouseReleased(null);
    }

    // ✅ DOC 13
    private String extraireNomImmeuble(String label) {
        int debut = label.indexOf("( ");
        int fin   = label.indexOf(" )");
        if (debut != -1 && fin != -1 && fin > debut) {
            return label.substring(debut + 2, fin);
        }
        return "Immeuble";
    }

    public void retourDashboard() {
        DashBoardView dashBoardView = new DashBoardView();
        Scene dashScene = new Scene(dashBoardView);
        stage.setScene(dashScene);
        stage.setTitle("InsaBuilder - Tableau de bord");
        new DashBoardControleur(dashBoardView, stage, gestionnaire);
    }

    private Point calculerPointOrthogonal(Point centre, Point cible,
                                          Point refP1, Point refP2) {
        double dx = refP2.getX() - refP1.getX();
        double dy = refP2.getY() - refP1.getY();
        double perpX = -dy;
        double perpY = dx;
        double ux = cible.getX() - centre.getX();
        double uy = cible.getY() - centre.getY();
        double scalaire = (ux * perpX + uy * perpY)
                / (perpX * perpX + perpY * perpY);
        return new Point(
                centre.getX() + scalaire * perpX,
                centre.getY() + scalaire * perpY
        );
    }
}