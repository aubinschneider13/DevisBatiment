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
import java.util.LinkedHashMap;
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

    // --- Maps de suivi (Fusion des vôtres et de celles de votre collègue) ---
    private final Map<TreeItem<String>, Piece> mapItemPiece = new HashMap<>();
    private final Map<TreeItem<String>, TreeItem<String>> mapPieceVersAppart = new HashMap<>();
    private final Map<Appartement, ContextePiece> contextePieces = new HashMap<>();
    private final Map<Piece, ContexteSousPiece> contexteSousPieces = new HashMap<>();
    private final Map<TreeItem<String>, Niveau> mapItemNiveau = new HashMap<>();
    private final Map<TreeItem<String>, Appartement> mapItemAppartement = new HashMap<>();
    private final Map<TreeItem<String>, Couloir> mapItemCouloir = new HashMap<>();
    private final Map<Couloir, ContexteCouloir> contexteCouloirs = new HashMap<>();
    private javafx.beans.value.ChangeListener<javafx.scene.control.Toggle> listenerEchelle = null;

    // =========================================================================
    // CONSTRUCTEUR 1 : NOUVEAU PROJET
    // =========================================================================
    public AppControleur(AppView appView, Stage stage,
                         GestionnaireSauvegarde gestionnaire) {
        this.appView      = appView;
        this.stage        = stage;
        this.gestionnaire = gestionnaire;

        this.catalogue = new CatalogueRevetements("src/main/resources/data/revetements.txt");

        brancherToolBar();
        brancherToolBarDevis();
        brancherNavigateur();

        appView.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                onEchap();
            }
        });

        immeubleControleur = new ImmeubleControleur(
                appView.getCanvasAire(), appView, gestionnaire
        );

        basculerContexte(new ContexteAire(immeubleControleur, appView, this));
        Platform.runLater(this::demanderNomImmeuble);
    }

    // =========================================================================
    // CONSTRUCTEUR 2 : RECHARGEMENT D'UN PROJET EXISTANT
    // =========================================================================
    public AppControleur(AppView appView, Stage stage,
                         GestionnaireSauvegarde gestionnaire, Immeuble immeubleExistant) {
        this.appView      = appView;
        this.stage        = stage;
        this.gestionnaire = gestionnaire;
        this.catalogue    = new CatalogueRevetements("src/main/resources/data/revetements.txt");

        brancherToolBar();
        brancherToolBarDevis();
        brancherNavigateur();

        appView.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) onEchap();
        });

        immeubleControleur = new ImmeubleControleur(
                appView.getCanvasAire(), appView, gestionnaire);

        // Restaurer l'aire depuis l'immeuble chargé
        AireImmeuble aire = new AireImmeuble(immeubleExistant.getPoint1());
        aire.setP2(immeubleExistant.getPoint2());
        aire.setP3(immeubleExistant.getPoint3());
        aire.valider();
        immeubleControleur.setAireImmeuble(aire);

        this.immeuble = immeubleExistant;

        // Mettre à jour le navigateur
        appView.getNavigateurView().setNomImmeuble(immeubleExistant.getNomBatiment());
        appView.activerVoile();

        // Reconstruire niveaux + appartements + pièces dans l'UI
        rechargerNiveaux(immeubleExistant);

        // Démarrer sur le contexte Aire (voile actif, lecture seule)
        basculerContexte(new ContexteAire(immeubleControleur, appView, this));
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

            ChoixRevetementDialog dialog = new ChoixRevetementDialog(catalogue, selection);
            Optional<Revetement> resultat = dialog.showAndWait();

            if (resultat.isPresent()) {
                Revetement revChoisi = resultat.get();
                boolean targetMurs = dialog.isAppliquerAuxMurs();
                boolean targetSol = dialog.isAppliquerAuSol();
                boolean targetPlafond = dialog.isAppliquerAuPlafond();

                for (SurfaceAvecRevetement surface : selection) {
                    boolean compatibleEtVise = false;

                    // ✅ CORRECTION : On teste "CoteMur" (qui hérite bien de SurfaceAvecRevetement)
                    if (surface instanceof CoteMur && targetMurs) {
                        compatibleEtVise = surface.estCompatibleAvec(revChoisi);
                    }
                    else if (surface instanceof Sol && targetSol) {
                        compatibleEtVise = surface.estCompatibleAvec(revChoisi);
                    }
                    else if (surface instanceof Plafond && targetPlafond) {
                        compatibleEtVise = surface.estCompatibleAvec(revChoisi);
                    }

                    if (compatibleEtVise) {
                        surface.getRevetements().clear();
                        surface.ajouterRevetement(revChoisi);
                    }
                }

                if (contexteActif instanceof ContexteSousPiece ctx) {
                    ctx.viderSelection();
                    double totalDevis = ctx.getPiece().calculerDevis();
                    tbDevis.getLabelTotalDevis().setText(String.format("Total estimé : %.2f €", totalDevis));
                } else if (contexteActif instanceof ContextePiece ctx) {
                    ctx.viderSelection();
                    double totalDevis = ctx.getAppartement().calculerDevis();
                    tbDevis.getLabelTotalDevis().setText(String.format("Total estimé : %.2f €", totalDevis));
                }

                appView.setInstructions("Matériau " + revChoisi.getDesignation() + " appliqué avec succès !");

                // Rafraîchir le panneau de propriétés
                TreeItem<String> itemSelectionne = appView.getNavigateurView()
                        .getTreeView().getSelectionModel().getSelectedItem();
                if (itemSelectionne != null) {
                    NavigateurView nav = appView.getNavigateurView();
                    Piece pieceSelectionnee = mapItemPiece.get(itemSelectionne);
                    if (pieceSelectionnee != null) {
                        nav.afficherProprietesPiece(pieceSelectionnee);
                    } else {
                        Appartement appart = mapItemAppartement.get(itemSelectionne);
                        if (appart != null) {
                            nav.afficherProprietesAppartement(appart);
                        }
                    }
                }
            }
        });

        tbDevis.getBtnExporterDevis().setOnAction(e -> {
            if (immeuble == null) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Aucun immeuble à exporter !");
                alert.showAndWait();
                return;
            }
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Exporter le devis");
            fileChooser.setInitialFileName("devis_" + immeuble.getNomBatiment() + ".txt");
            fileChooser.getExtensionFilters().add(
                    new javafx.stage.FileChooser.ExtensionFilter("Fichiers Texte (*.txt)", "*.txt")
            );
            java.io.File file = fileChooser.showSaveDialog(stage);
            if (file != null) {
                try {
                    DevisExporter.exporterDevisVersFichier(immeuble, file);
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Le devis a été exporté avec succès !");
                    alert.setHeaderText("Export réussi");
                    alert.showAndWait();
                } catch (java.io.IOException ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Erreur lors de l'exportation : " + ex.getMessage());
                    alert.showAndWait();
                }
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

        // --- Clic sur la racine (immeuble) ---
        if (item == nav.getRootItem()) {
            nav.effacerProprietes();
            if (immeuble != null) {
                double total = immeuble.calculerDevisTotal();
                tbDevis.getLabelTotalDevis().setText(String.format("TOTAL IMMEUBLE : %.2f €", total));
            }
            return;
        }

        // --- Clic sur "Aire de l'immeuble" ---
        if (item == nav.getItemAire()) {
            nav.effacerProprietes();
            basculerContexte(new ContexteAire(immeubleControleur, appView, this));
            tbDevis.getLabelTotalDevis().setText("Total estimé : 0.00 €");
            return;
        }

        // --- Clic sur un niveau ---
        int idxNiveau = itemsNiveau.indexOf(item);
        if (idxNiveau >= 0) {
            NiveauControleur ctrl = niveauControleurs.get(idxNiveau);
            itemNiveauActif = item;
            basculerContexte(new ContexteNiveau(ctrl, appView, this));
            tbDevis.getLabelTotalDevis().setText("Total estimé : -- €");

            Niveau niveau = mapItemNiveau.get(item);
            if (niveau != null) nav.afficherProprietesNiveau(niveau);
            return;
        }

        // --- Clic sur un appartement ---
        for (int i = 0; i < niveauControleurs.size(); i++) {
            NiveauControleur ctrl = niveauControleurs.get(i);
            Appartement appart = ctrl.getMapItemAppartement().get(item);
            if (appart == null) appart = mapItemAppartement.get(item); // Fallback pour les rechargements

            if (appart != null) {
                itemNiveauActif = itemsNiveau.get(i);

                boolean estNouveau = !contextePieces.containsKey(appart);

                ContextePiece ctx = contextePieces.computeIfAbsent(appart, a ->
                        new ContextePiece(a, appView, this, stage, gestionnaire, item)
                );
                basculerContexte(ctx);

                ctx.getPieceControleur().synchroniserOuverturesVersAppartement();

                if (estNouveau && !appart.getPieces().isEmpty()) {
                    ctx.getPieceControleur().rechargerPieces(appart.getPieces());
                }

                double totalDevis = appart.calculerDevis();
                tbDevis.getLabelTotalDevis().setText(String.format("Total estimé : %.2f €", totalDevis));
                nav.afficherProprietesAppartement(appart);
                return;
            }
        }

        // --- Clic sur un couloir ---
        Couloir couloir = mapItemCouloir.get(item);
        if (couloir != null) {
            ContexteCouloir ctx = contexteCouloirs.computeIfAbsent(couloir, c ->
                    new ContexteCouloir(c, appView, this, stage, gestionnaire));
            basculerContexte(ctx);
            tbDevis.getLabelTotalDevis().setText("Total estimé : -- €");
            return;
        }

        // --- Clic sur une pièce ---
        Piece piece = mapItemPiece.get(item);
        if (piece != null) {
            ContexteSousPiece ctx = contexteSousPieces.computeIfAbsent(piece, p ->
                    new ContexteSousPiece(p, appView, this, stage, gestionnaire)
            );

            basculerContexte(ctx);

            ctx.getPieceControleur().synchroniserOuverturesVersPiece();

            double totalDevis = piece.calculerDevis();
            tbDevis.getLabelTotalDevis().setText(String.format("Total estimé : %.2f €", totalDevis));
            nav.afficherProprietesPiece(piece);
            return;
        }
    }

    // =========================================================================
    // ÉVÉNEMENTS TOOLBAR
    // =========================================================================

    public void onAireValidee() {
        String label = appView.getNavigateurView().getRootItem().getValue();
        String nom   = extraireNomImmeuble(label);
        immeuble = new Immeuble(nom, immeubleControleur.getAireImmeuble());

        // Sauvegarde de l'immeuble (Code collègue)
        gestionnaire.sauvegarderBatiment(immeuble);

        appView.activerVoile();
        basculerContexte(new ContexteAire(immeubleControleur, appView, this));
    }

    public void onBtnAjouterNiveau() {
        if (immeuble == null) return;

        Niveau niveau = immeuble.ajouterNiveau(2.5);

        // Sauvegarde du niveau (Code collègue)
        gestionnaire.sauvegarderNiveau(niveau, immeuble);

        String nomNiveau = niveauControleurs.isEmpty()
                ? "RDC"
                : "Niveau " + niveauControleurs.size();

        TreeItem<String> itemNiveau =
                appView.getNavigateurView().ajouterItemNiveau(nomNiveau);
        itemsNiveau.add(itemNiveau);
        mapItemNiveau.put(itemNiveau, niveau); // Pour vos propriétés

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
            mapItemAppartement.put(itemAppart, appart);

            // Sauvegarde de l'appartement (Code collègue)
            gestionnaire.sauvegarderAppartement(appart, niveau, immeuble);

            // Rafraîchir le panneau (Votre code)
            Niveau niveauCourant = mapItemNiveau.get(itemNiveau);
            if (niveauCourant != null) {
                appView.getNavigateurView().afficherProprietesNiveau(niveauCourant);
            }

            return itemAppart;
        });

        Map<TreeItem<String>, Couloir> itemsCouloirDuNiveau = new HashMap<>();
        ctrl.setOnCouloirCree(couloir -> {
            // Vider les anciens items couloir de ce niveau dans le navigateur
            itemNiveau.getChildren().removeAll(itemsCouloirDuNiveau.keySet());
            itemsCouloirDuNiveau.clear();

            TreeItem<String> itemCouloir = appView.getNavigateurView()
                    .ajouterItemCouloir(itemNiveau, couloir.toString());
            itemsCouloirDuNiveau.put(itemCouloir, couloir);
            mapItemCouloir.put(itemCouloir, couloir);
            return itemCouloir;
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

        if (visible && listenerEchelle == null) {
            listenerEchelle = (obs, oldVal, newVal) -> {
                if (newVal == null) return;
                appliquerEchelle(echelleVue.getEchelleSelectionnee());
            };
            echelleVue.getGroupeEchelle()
                    .selectedToggleProperty()
                    .addListener(listenerEchelle);
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
        } else if (contexteActif instanceof ContexteSousPiece ctx) {
            ctx.onEchap();
        } else if (contexteActif instanceof ContextePiece ctx) {
            ctx.getPieceControleur().annulerConstruction();
            ctx.getPieceControleur().changerEtat(PieceControleur.ETAT_RIEN);
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

    // =========================================================================
    // ENREGISTREMENT ET CHARGEMENT
    // =========================================================================

    public void enregistrerPiece(TreeItem<String> itemPiece, Piece piece,
                                 TreeItem<String> itemAppart) {
        mapItemPiece.put(itemPiece, piece);
        mapPieceVersAppart.put(itemPiece, itemAppart);

        // Sauvegarde de la pièce (Code collègue)
        for (int i = 0; i < niveauControleurs.size(); i++) {
            NiveauControleur ctrl = niveauControleurs.get(i);
            Appartement appart = ctrl.getMapItemAppartement().get(itemAppart);
            if (appart != null) {
                Niveau niveau = immeuble.getNiveaux().get(i);
                gestionnaire.sauvegarderPiece(piece, appart, niveau, immeuble);
                return;
            }
        }
    }

    private void rechargerNiveaux(Immeuble immeuble) {
        for (Niveau niveau : immeuble.getNiveaux()) {
            String nomNiveau = niveauControleurs.isEmpty()
                    ? "RDC" : "Niveau " + niveauControleurs.size();

            TreeItem<String> itemNiveau =
                    appView.getNavigateurView().ajouterItemNiveau(nomNiveau);
            itemsNiveau.add(itemNiveau);
            mapItemNiveau.put(itemNiveau, niveau);

            NiveauView niveauView = new NiveauView();
            NiveauControleur ctrl = new NiveauControleur(
                    niveauView,
                    immeubleControleur.getAireImmeuble(),
                    itemNiveau,
                    niveau
            );

            Map<TreeItem<String>, Appartement> mapApparts = new LinkedHashMap<>();

            for (Appartement appart : niveau.getAppartements()) {
                TreeItem<String> itemAppart = appView.getNavigateurView()
                        .ajouterItemAppartement(itemNiveau, appart.toString());
                mapApparts.put(itemAppart, appart);
                mapItemAppartement.put(itemAppart, appart);

                for (Piece piece : appart.getPieces()) {
                    TreeItem<String> itemPiece = appView.getNavigateurView()
                            .ajouterItemPiece(itemAppart, piece.toString());
                    mapItemPiece.put(itemPiece, piece);
                    mapPieceVersAppart.put(itemPiece, itemAppart);
                }
            }

            ctrl.rechargerAppartements(niveau.getAppartements(), mapApparts);

            final Niveau niveauFinal = niveau;
            ctrl.setOnAppartementCree(appart -> {
                TreeItem<String> itemAppart = appView.getNavigateurView()
                        .ajouterItemAppartement(itemNiveau, appart.toString());
                mapItemAppartement.put(itemAppart, appart);
                gestionnaire.sauvegarderAppartement(appart, niveauFinal, immeuble);
                return itemAppart;
            });

            niveauControleurs.add(ctrl);
        }
    }
}