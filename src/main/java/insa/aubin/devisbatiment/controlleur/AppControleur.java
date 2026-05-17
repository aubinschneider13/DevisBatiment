package insa.aubin.devisbatiment.controlleur;

import insa.aubin.devisbatiment.modele.*;
import insa.aubin.devisbatiment.view.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TreeItem;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Chef d'orchestre unique de l'application.
 *
 * AppControleur remplace ImmeubleControleur dans son rôle de coordinateur
 * global. Il est le seul à :
 * - connaître le Stage et à en changer la scène (retour dashboard)
 * - instancier et basculer les contextes (ContexteAire, ContexteNiveau, ContextePiece)
 * - peupler le NavigateurView (ajout de niveaux et d'appartements)
 * - brancher tous les listeners de la ToolBarView, ToolBarDevisView et du TreeView
 *
 * Pattern utilisé : State (via l'interface Contexte).
 */
public class AppControleur {

    // --- Fenêtre et services ---
    private final AppView appView;
    private final Stage stage;
    private final GestionnaireSauvegarde gestionnaire;

    // --- Modèle métier ---
    private Immeuble immeuble = null;

    // --- Catalogue des revêtements ---
    private CatalogueRevetements catalogue;

    // --- Contexte actif (pattern State) ---
    private Contexte contexteActif = null;

    // --- Contrôleur de l'aire (partagé entre AppControleur et ContexteAire) ---
    private ImmeubleControleur immeubleControleur;

    // --- Niveaux : contrôleurs + items TreeView ---
    private final List<NiveauControleur> niveauControleurs = new ArrayList<>();
    private final List<TreeItem<String>> itemsNiveau       = new ArrayList<>();

    // --- Item TreeView actuellement sélectionné comme "niveau actif" ---
    private TreeItem<String> itemNiveauActif = null;

    // --- Maps de suivi ---
    private final Map<TreeItem<String>, Piece> mapItemPiece = new HashMap<>();
    private final Map<TreeItem<String>, TreeItem<String>> mapPieceVersAppart = new HashMap<>();
    private final Map<Appartement, ContextePiece> contextePieces = new HashMap<>();
    private final Map<Piece, ContexteSousPiece> contexteSousPieces = new HashMap<>();

    public AppControleur(AppView appView, Stage stage,
                         GestionnaireSauvegarde gestionnaire) {
        this.appView      = appView;
        this.stage        = stage;
        this.gestionnaire = gestionnaire;

        // Initialisation du catalogue
        this.catalogue = new CatalogueRevetements("src/main/resources/data/revetements.txt");

        // Branchement de la toolbar Construction
        brancherToolBar();

        // Branchement de la toolbar Devis
        brancherToolBarDevis();

        // Branchement du TreeView
        brancherNavigateur();

        // Raccourci Échap : annule un dessin en cours dans n'importe quel contexte
        appView.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                onEchap();
            }
        });

        // Instanciation d'ImmeubleControleur (gestion du canvas de l'aire uniquement)
        immeubleControleur = new ImmeubleControleur(
                appView.getCanvasAire(), appView, gestionnaire
        );

        // Démarrage : contexte Aire + saisie du nom de l'immeuble
        basculerContexte(new ContexteAire(immeubleControleur, appView, this));
        Platform.runLater(this::demanderNomImmeuble);
    }

    // =========================================================================
    // BRANCHEMENT LISTENERS
    // =========================================================================

    private void brancherToolBar() {
        ToolBarView tb = appView.getToolBarView();

        tb.getBtnNavigation()   .setOnAction(e -> contexteActif.onBtnNavigation());
        tb.getBtnEchelle()      .setOnAction(e -> contexteActif.onBtnEchelle());
        tb.getBtnMur()          .setOnAction(e -> contexteActif.onBtnMur());
        tb.getBtnAppartement()  .setOnAction(e -> contexteActif.onBtnAppartement());
        tb.getBtnPiece()        .setOnAction(e -> contexteActif.onBtnPiece());
        tb.getBtnPorte()        .setOnAction(e -> contexteActif.onBtnPorte());
        tb.getBtnFenetre()      .setOnAction(e -> contexteActif.onBtnFenetre());
        tb.getBtnValiderAire()  .setOnAction(e -> contexteActif.onBtnValiderAire());
        tb.getBtnAjouterNiveau().setOnAction(e -> onBtnAjouterNiveau());
        tb.getBtnRetour()       .setOnAction(e -> retourDashboard());
    }

    private void brancherToolBarDevis() {
        ToolBarDevisView tbDevis = appView.getToolBarDevisView();

        // --- Clic sur "Appliquer un revêtement" ---
        tbDevis.getBtnAppliquerRevetement().setOnAction(e -> {
            if (contexteActif instanceof ContexteSousPiece ctx) {
                ctx.activerModeSelection();
                appView.setInstructions("Mode Matériaux : Cliquez sur les surfaces de la pièce, puis validez.");
            } else if (contexteActif instanceof ContextePiece ctx) {
                ctx.activerModeSelection();
                appView.setInstructions("Mode Matériaux : Cliquez sur les murs de l'appartement, puis validez.");
            } else {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Veuillez d'abord entrer dans une pièce ou un appartement pour appliquer des matériaux.");
                alert.setHeaderText("Action impossible");
                alert.showAndWait();
            }
        });

        // --- Clic sur "Valider la sélection" ---
        tbDevis.getBtnValiderRevetement().setOnAction(e -> {
            List<SurfaceAvecRevetement> selection = null;

            if (contexteActif instanceof ContexteSousPiece ctx) {
                selection = ctx.getSurfacesSelectionnees();
            } else if (contexteActif instanceof ContextePiece ctx) {
                selection = ctx.getSurfacesSelectionnees();
            }

            if (selection == null || selection.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Vous n'avez sélectionné aucune surface ! Cliquez d'abord sur 'Appliquer un revêtement' puis choisissez des murs.");
                alert.showAndWait();
                return;
            }

            // Ouverture de la boîte de dialogue
            ChoixRevetementDialog dialog = new ChoixRevetementDialog(catalogue);
            Optional<Revetement> resultat = dialog.showAndWait();

            // Si l'utilisateur a choisi et validé un matériau
            if (resultat.isPresent()) {
                Revetement revChoisi = resultat.get();

                for (SurfaceAvecRevetement surface : selection) {
                    surface.getRevetements().clear();
                    surface.ajouterRevetement(revChoisi);
                }

                // Nettoyage via les contextes et recalcul financier
                if (contexteActif instanceof ContexteSousPiece ctx) {
                    ctx.viderSelection();
                    // ✅ Recalcul dynamique du coût de la pièce
                    double totalDevis = ctx.getPiece().calculerDevis();
                    tbDevis.getLabelTotalDevis().setText(String.format("Total estimé : %.2f €", totalDevis));
                } else if (contexteActif instanceof ContextePiece ctx) {
                    ctx.viderSelection();
                    // ✅ Recalcul dynamique du coût de l'appartement complet
                    double totalDevis = ctx.getAppartement().calculerDevis();
                    tbDevis.getLabelTotalDevis().setText(String.format("Total estimé : %.2f €", totalDevis));
                }

                appView.setInstructions("Matériau " + revChoisi.getDesignation() + " appliqué avec succès !");
            }
        });
    }

    private void brancherNavigateur() {
        appView.getNavigateurView()
                .getTreeView()
                .getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> {
                    if (newVal == null) return;
                    onSelectionArbre(newVal);
                });
    }

    // =========================================================================
    // GESTION DES CONTEXTES
    // =========================================================================

    public void basculerContexte(Contexte nouveau) {
        if (contexteActif != null) {
            contexteActif.desinstaller();
        }
        contexteActif = nouveau;
        appView.getToolBarView().mettreAJourBoutons(nouveau.getBoutonsVisibles());
        nouveau.installer();
    }

    // =========================================================================
    // SÉLECTION DANS LE TREE VIEW
    // =========================================================================

    public void onSelectionArbre(TreeItem<String> item) {
        NavigateurView nav = appView.getNavigateurView();
        ToolBarDevisView tbDevis = appView.getToolBarDevisView();

        // --- Clic sur "Aire de l'immeuble" ---
        if (item == nav.getItemAire()) {
            basculerContexte(new ContexteAire(immeubleControleur, appView, this));
            tbDevis.getLabelTotalDevis().setText("Total estimé : 0.00 €");
            return;
        }

        // --- Clic sur un item de niveau ---
        int idxNiveau = itemsNiveau.indexOf(item);
        if (idxNiveau >= 0) {
            NiveauControleur ctrl = niveauControleurs.get(idxNiveau);
            itemNiveauActif = item;
            basculerContexte(new ContexteNiveau(ctrl, appView, this));
            // Afficher le coût global estimé du niveau (si implémenté dans votre modèle)
            tbDevis.getLabelTotalDevis().setText("Total estimé : -- €");
            return;
        }

        // --- Clic sur un appartement ---
        for (int i = 0; i < niveauControleurs.size(); i++) {
            NiveauControleur ctrl = niveauControleurs.get(i);
            Appartement appart = ctrl.getMapItemAppartement().get(item);
            if (appart != null) {
                itemNiveauActif = itemsNiveau.get(i);
                ContextePiece ctx = contextePieces.computeIfAbsent(appart, a ->
                        new ContextePiece(a, appView, this, stage, gestionnaire, item)
                );
                basculerContexte(ctx);

                // ✅ Met à jour la barre d'outils devis avec le prix cumulé de l'appartement
                double totalDevis = appart.calculerDevis();
                tbDevis.getLabelTotalDevis().setText(String.format("Total estimé : %.2f €", totalDevis));
                return;
            }
        }

        // --- Clic sur une pièce ---
        Piece piece = mapItemPiece.get(item);
        if (piece != null) {
            ContexteSousPiece ctx = contexteSousPieces.computeIfAbsent(piece, p ->
                    new ContexteSousPiece(p, appView, this, stage, gestionnaire)
            );
            basculerContexte(ctx);

            // ✅ Met à jour la barre d'outils devis avec le prix spécifique de cette pièce
            double totalDevis = piece.calculerDevis();
            tbDevis.getLabelTotalDevis().setText(String.format("Total estimé : %.2f €", totalDevis));
            return;
        }
    }

    // =========================================================================
    // ÉVÉNEMENTS TOOLBAR GÉRÉS DIRECTEMENT PAR APPCONTROLEUR
    // =========================================================================

    public void onAireValidee() {
        String label = appView.getNavigateurView().getRootItem().getValue();
        String nom   = extraireNomImmeuble(label);
        immeuble = new Immeuble(nom, immeubleControleur.getAireImmeuble());

        appView.activerVoile();
        basculerContexte(new ContexteAire(immeubleControleur, appView, this));
    }

    public void onBtnAjouterNiveau() {
        if (immeuble == null) return;

        Niveau niveau = immeuble.ajouterNiveau(2.5);

        String nomNiveau = niveauControleurs.isEmpty()
                ? "RDC"
                : "Niveau " + niveauControleurs.size();

        TreeItem<String> itemNiveau =
                appView.getNavigateurView().ajouterItemNiveau(nomNiveau);
        itemsNiveau.add(itemNiveau);

        NiveauView niveauView = new NiveauView();

        NiveauControleur ctrl = new NiveauControleur(
                niveauView,
                immeubleControleur.getAireImmeuble(),
                itemNiveau,
                niveau
        );

        ctrl.setOnAppartementCree(appart -> {
            TreeItem<String> itemAppart = appView.getNavigateurView()
                    .ajouterItemAppartement(itemNiveau, appart.toString());
            return itemAppart;
        });

        niveauControleurs.add(ctrl);

        itemNiveauActif = itemNiveau;
        appView.getNavigateurView().selectionner(itemNiveau);
        basculerContexte(new ContexteNiveau(ctrl, appView, this));
    }

    public void onBtnEchelle() {
        EchelleVue echelleVue = appView.getEchelleVue();
        boolean visible = !echelleVue.isVisible();
        echelleVue.setVisible(visible);

        if (visible) {
            echelleVue.getGroupeEchelle()
                    .selectedToggleProperty()
                    .addListener((obs, oldVal, newVal) -> {
                        if (newVal == null) return;
                        appliquerEchelle(echelleVue.getEchelleSelectionnee());
                    });
        }
    }

    private void appliquerEchelle(double echelle) {
        if (contexteActif instanceof ContexteNiveau ctx) {
            ctx.getNiveauControleur().getVue().getCanvas().setGridSize(echelle);
        } else {
            appView.getCanvasAire().setGridSize(echelle);
        }
    }

    private void onEchap() {
        if (contexteActif instanceof ContexteNiveau ctx) {
            ctx.getNiveauControleur().annulerMurEnCours();
        } else if (contexteActif instanceof ContexteAire) {
            immeubleControleur.annulerAire();
        }
    }

    public void retourDashboard() {
        Appartement.resetCompteur();
        Piece.resetCompteur();
        DashBoardView dashBoardView = new DashBoardView();
        Scene dashScene = new Scene(dashBoardView);
        stage.setScene(dashScene);
        stage.setTitle("InsaBuilder - Tableau de bord");
        new DashBoardControleur(dashBoardView, stage, gestionnaire);
    }

    private void demanderNomImmeuble() {
        javafx.scene.control.TextInputDialog dialog =
                new javafx.scene.control.TextInputDialog("Nouvel Immeuble");
        dialog.setTitle("Nom de l'immeuble");
        dialog.setHeaderText("Initialisation du projet");
        dialog.setContentText("Veuillez entrer le nom de l'immeuble :");
        dialog.setGraphic(null);

        javafx.scene.control.Button okButton =
                (javafx.scene.control.Button)
                        dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (dialog.getEditor().getText().trim().isEmpty()) {
                event.consume();
                dialog.getEditor().setStyle("-fx-border-color: red;");
            }
        });

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            appView.getNavigateurView().setNomImmeuble(result.get().trim());
        } else {
            retourDashboard();
        }
    }

    private String extraireNomImmeuble(String label) {
        int debut = label.indexOf("( ");
        int fin   = label.indexOf(" )");
        if (debut != -1 && fin != -1 && fin > debut) {
            return label.substring(debut + 2, fin);
        }
        return "Immeuble";
    }

    public AireImmeuble getAireImmeuble() {
        return immeubleControleur.getAireImmeuble();
    }

    public void enregistrerPiece(TreeItem<String> itemPiece, Piece piece,
                                 TreeItem<String> itemAppart) {
        mapItemPiece.put(itemPiece, piece);
        mapPieceVersAppart.put(itemPiece, itemAppart);
    }
}