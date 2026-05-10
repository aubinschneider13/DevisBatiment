package insa.aubin.devisbatiment.controlleur;

import insa.aubin.devisbatiment.modele.GestionnaireSauvegarde;
import insa.aubin.devisbatiment.modele.AireImmeuble;
import insa.aubin.devisbatiment.modele.Point;
import insa.aubin.devisbatiment.view.DashBoardView;
import insa.aubin.devisbatiment.view.ImmeubleView;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;

import java.util.Optional;

public class ImmeubleControleur {
    private ImmeubleView vue;
    private Stage stage;
    private GestionnaireSauvegarde gestionnaire;
    private AireImmeuble aireImmeuble;
    private int etapeAire = 0; // 0=attente p1, 1=attente p2, 2=attente p3
    private boolean aireValidee = false;

    public ImmeubleControleur(ImmeubleView vue, Stage stage, GestionnaireSauvegarde gestionnaire) {
        this.vue = vue;
        this.stage = stage;
        this.gestionnaire = gestionnaire;

        // Lance la demande de nom dès que l'interface est affichée
        Platform.runLater(this::demanderNomImmeuble);
        
        // Active le mode tracé d'aire au lancement
        this.vue.getCanvas().setOnMouseClicked(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                this.clicAire(e);
            }
        });
        this.vue.getCanvas().setOnMouseMoved(e -> this.mouvementAire(e));
    }

    private void demanderNomImmeuble() {
        TextInputDialog dialog = new TextInputDialog("Nouvel Immeuble");
        dialog.setTitle("Nom de l'immeuble");
        dialog.setHeaderText("Initialisation du projet");
        dialog.setContentText("Veuillez entrer le nom de l'immeuble :");
        dialog.setGraphic(null);

        // Récupération du bouton OK pour forcer la validation
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (dialog.getEditor().getText().trim().isEmpty()) {
                event.consume(); // Empêche la fermeture si vide
                dialog.getEditor().setStyle("-fx-border-color: red;");
            }
        });

        Optional<String> result = dialog.showAndWait();

        if (result.isPresent() && !result.get().trim().isEmpty()) {
            // Met à jour le TreeItem racine dans la vue
            this.vue.getRootItem().setValue("Immeuble : " + result.get().trim());
        } else {
            // Si l'utilisateur annule ou ferme via la croix, retour forcé au dashboard
            retourDashboard();
        }
    }
    
    /** Gère les clics pour poser les 3 points de l'aire */
    private void clicAire(javafx.scene.input.MouseEvent e) {
        if (aireValidee) return;

        javafx.geometry.Point2D snap = this.vue.getCanvas().snapToGrid(e.getX(), e.getY());
        Point pClic = new Point(snap.getX(), snap.getY());

        switch (etapeAire) {
            case 0:
                // Premier coin
                aireImmeuble = new AireImmeuble(pClic);
                this.vue.getCanvas().ajouterElement(aireImmeuble);
                etapeAire = 1;
                this.vue.setInstructions("Cliquez pour définir le deuxième coin (côté 1)");
                break;
            case 1:
                // Deuxième coin
                aireImmeuble.setP2(pClic);
                etapeAire = 2;
                this.vue.setInstructions("Cliquez pour définir la largeur (côté 2)");
                break;
            case 2:
                // Troisième coin — p3 contraint orthogonalement
                Point p3Contraint = calculerPointOrthogonal(
                    aireImmeuble.getP2(), pClic,
                    aireImmeuble.getP1(), aireImmeuble.getP2()
                );
                aireImmeuble.setP3(p3Contraint);
                etapeAire = 3;
                this.vue.setInstructions("Vérifiez l'aire puis cliquez sur « Valider »");
                this.vue.getBtnValiderAire().setDisable(false);
                break;
        }
        this.vue.getCanvas().redrawAll();
    }

    /** Preview en temps réel pendant le déplacement de la souris */
    private void mouvementAire(javafx.scene.input.MouseEvent e) {
        if (aireValidee || aireImmeuble == null) return;

        javafx.geometry.Point2D snap = this.vue.getCanvas().snapToGrid(e.getX(), e.getY());
        Point pSouris = new Point(snap.getX(), snap.getY());

        if (etapeAire == 1) {
            aireImmeuble.setP2(pSouris);
        } else if (etapeAire == 2) {
            Point p3Contraint = calculerPointOrthogonal(
                aireImmeuble.getP2(), pSouris,
                aireImmeuble.getP1(), aireImmeuble.getP2()
            );
            aireImmeuble.setP3(p3Contraint);
        }
        this.vue.getCanvas().redrawAll();
    }

    /** Valide l'aire et déverrouille "Ajouter Niveau" */
    public void btnValiderAire(ActionEvent t) {
        if (aireImmeuble == null || etapeAire != 3) return;
        aireImmeuble.valider();
        aireValidee = true;
        this.vue.getBtnAjouterNiveau().setDisable(false);
        this.vue.getBtnValiderAire().setDisable(true);
        this.vue.setInstructions("Aire validée — vous pouvez ajouter des niveaux");
        // Désactive les listeners de dessin
        this.vue.getCanvas().setOnMouseClicked(null);
        this.vue.getCanvas().setOnMouseMoved(null);
    }

    /** Calcule p3 contraint orthogonalement au vecteur p1→p2 */
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

    public void btnNavigation(ActionEvent t) {
        this.vue.getCanvas().setPanActif(true);
    }

    public void retourDashboard() {
        DashBoardView dashBoardView = new DashBoardView();
        Scene dashScene = new Scene(dashBoardView);
        stage.setScene(dashScene);
        stage.setTitle("InsaBuilder - Tableau de bord");
        new DashBoardControleur(dashBoardView, stage, gestionnaire);
    }

    public void btnAjouterNiveau(ActionEvent t) {
        // Logique future
        System.out.println("Ajout de niveau");
    }
    
    public void btnEchelle(ActionEvent t) {
        // Bascule la visibilité du panneau échelle
        boolean visible = !this.vue.getEchelleVue().isVisible();
        this.vue.getEchelleVue().setVisible(visible);

        if (visible) {
            // Écouter les changements d'échelle en temps réel
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
}