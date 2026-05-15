package insa.aubin.devisbatiment.controlleur;

import insa.aubin.devisbatiment.controlleur.*;
import insa.aubin.devisbatiment.modele.*;
import insa.aubin.devisbatiment.view.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.TreeItem;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Chef d'orchestre unique de l'application.
 *
 * AppControleur remplace ImmeubleControleur dans son rôle de coordinateur
 * global. Il est le seul à :
 *   - connaître le Stage et à en changer la scène (retour dashboard)
 *   - instancier et basculer les contextes (ContexteAire, ContexteNiveau, ContextePiece)
 *   - peupler le NavigateurView (ajout de niveaux et d'appartements)
 *   - brancher tous les listeners de la ToolBarView et du TreeView
 *
 * ImmeubleControleur est allégé : il ne gère plus que le dessin de l'aire
 * (clics, glissés, validation). PieceControleur ne gère plus que le dessin
 * intérieur. Aucun des deux ne connaît AppControleur.
 *
 * Pattern utilisé : State (via l'interface Contexte).
 * AppControleur délègue chaque événement toolbar/TreeView au contexteActif,
 * qui sait quoi faire sans qu'AppControleur ait à tester le mode courant.
 */
public class AppControleur {

    // --- Fenêtre et services ---
    private final AppView appView;
    private final Stage stage;
    private final GestionnaireSauvegarde gestionnaire;

    // --- Modèle métier ---
    private Immeuble immeuble = null;

    // --- Contexte actif (pattern State) ---
    private Contexte contexteActif = null;

    // --- Contrôleur de l'aire (partagé entre AppControleur et ContexteAire) ---
    private ImmeubleControleur immeubleControleur;

    // --- Niveaux : contrôleurs + items TreeView ---
    private final List<NiveauControleur> niveauControleurs = new ArrayList<>();
    private final List<TreeItem<String>> itemsNiveau       = new ArrayList<>();

    // --- Item TreeView actuellement sélectionné comme "niveau actif" ---
    private TreeItem<String> itemNiveauActif = null;

    public AppControleur(AppView appView, Stage stage,
                         GestionnaireSauvegarde gestionnaire) {
        this.appView      = appView;
        this.stage        = stage;
        this.gestionnaire = gestionnaire;

        // Branchement de la toolbar
        brancherToolBar();

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

    /**
     * Branche les actions de chaque bouton de la ToolBarView.
     * Les boutons délèguent toujours au contexteActif — AppControleur ne
     * teste jamais le mode courant lui-même.
     */
    private void brancherToolBar() {
        ToolBarView tb = appView.getToolBarView();

        tb.getBtnNavigation()   .setOnAction(e -> contexteActif.onBtnNavigation());
        tb.getBtnEchelle()      .setOnAction(e -> contexteActif.onBtnEchelle());
        tb.getBtnMur()          .setOnAction(e -> contexteActif.onBtnMur());
        tb.getBtnAppartement()  .setOnAction(e -> contexteActif.onBtnAppartement());
        tb.getBtnPorte()        .setOnAction(e -> contexteActif.onBtnPorte());
        tb.getBtnFenetre()      .setOnAction(e -> contexteActif.onBtnFenetre());
        tb.getBtnValiderAire()  .setOnAction(e -> contexteActif.onBtnValiderAire());
        tb.getBtnAjouterNiveau().setOnAction(e -> onBtnAjouterNiveau());
        tb.getBtnRetour()       .setOnAction(e -> retourDashboard());
    }

    /**
     * Branche le listener de sélection du TreeView.
     * Toute sélection est transmise à onSelectionArbre() qui décide si
     * un changement de contexte est nécessaire.
     */
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

    /**
     * Bascule vers un nouveau contexte :
     *   1. Désinstalle le contexte sortant (nettoyage).
     *   2. Met à jour les boutons visibles de la toolbar.
     *   3. Installe le nouveau contexte (affiche le bon canvas, initialise l'état).
     *
     * @param nouveau contexte à activer
     */
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

    /**
     * Interprète la sélection dans le TreeView et bascule de contexte si nécessaire.
     *
     * Cas traités :
     *   - Item "Aire de l'immeuble" → ContexteAire
     *   - Item de niveau            → ContexteNiveau du niveau correspondant
     *   - Item d'appartement        → ContextePiece de l'appartement correspondant
     *
     * @param item item sélectionné (non null, garanti par le listener)
     */
    public void onSelectionArbre(TreeItem<String> item) {
        NavigateurView nav = appView.getNavigateurView();

        // --- Clic sur "Aire de l'immeuble" ---
        if (item == nav.getItemAire()) {
            basculerContexte(new ContexteAire(immeubleControleur, appView, this));
            return;
        }

        // --- Clic sur un item de niveau ---
        int idxNiveau = itemsNiveau.indexOf(item);
        if (idxNiveau >= 0) {
            NiveauControleur ctrl = niveauControleurs.get(idxNiveau);
            itemNiveauActif = item;
            basculerContexte(new ContexteNiveau(ctrl, appView, this));
            return;
        }

        // --- Clic sur un appartement (enfant d'un item de niveau) ---
        for (int i = 0; i < niveauControleurs.size(); i++) {
            NiveauControleur ctrl = niveauControleurs.get(i);
            Appartement appart = ctrl.getMapItemAppartement().get(item);
            if (appart != null) {
                itemNiveauActif = itemsNiveau.get(i);
                basculerContexte(new ContextePiece(
                    appart, appView, this, stage, gestionnaire
                ));
                return;
            }
        }
    }

    // =========================================================================
    // ÉVÉNEMENTS TOOLBAR GÉRÉS DIRECTEMENT PAR APPCONTROLEUR
    // =========================================================================

    /**
     * Appelé par ContexteAire après que ImmeubleControleur a validé l'aire.
     * Crée le modèle Immeuble, active le voile cadenas, et rebascule en
     * ContexteAire pour mettre à jour la liste de boutons visibles.
     */
    public void onAireValidee() {
        String label = appView.getNavigateurView().getRootItem().getValue();
        String nom   = extraireNomImmeuble(label);
        immeuble = new Immeuble(nom, immeubleControleur.getAireImmeuble());

        appView.activerVoile();

        // Rebascule en ContexteAire : getBoutonsVisibles() pourra retourner
        // la liste post-validation si on la différencie (ex. ajouterNiveau visible)
        basculerContexte(new ContexteAire(immeubleControleur, appView, this));
    }

    /**
     * Ajoute un nouveau niveau à l'immeuble, crée les objets associés
     * et bascule automatiquement vers ce nouveau niveau.
     */
    public void onBtnAjouterNiveau() {
        if (immeuble == null) return;

        // --- Modèle ---
        Niveau niveau = immeuble.ajouterNiveau(2.5);

        // --- Libellé ---
        String nomNiveau = niveauControleurs.isEmpty()
                           ? "RDC"
                           : "Niveau " + niveauControleurs.size();

        // --- TreeView ---
        TreeItem<String> itemNiveau =
            appView.getNavigateurView().ajouterItemNiveau(nomNiveau);
        itemsNiveau.add(itemNiveau);

        // --- Vue ---
        NiveauView niveauView = new NiveauView();

        // --- Contrôleur ---
        NiveauControleur ctrl = new NiveauControleur(
            niveauView,
            immeubleControleur.getAireImmeuble(),
            itemNiveau,
            niveau
        );

        // ✅ Callback : quand NiveauControleur crée un appartement, il notifie
        // AppControleur qui peuple le navigateur (découplage propre).
        ctrl.setOnAppartementCree(appart -> {
            TreeItem<String> itemAppart = appView.getNavigateurView()
                .ajouterItemAppartement(itemNiveau, appart.toString());
            return itemAppart;
        });

        niveauControleurs.add(ctrl);

        // --- Bascule automatique vers ce niveau ---
        itemNiveauActif = itemNiveau;
        appView.getNavigateurView().selectionner(itemNiveau);
        basculerContexte(new ContexteNiveau(ctrl, appView, this));
    }

    /**
     * Bascule la visibilité du panneau EchelleVue et branche le listener
     * de changement d'échelle la première fois qu'il est ouvert.
     * Commun à tous les contextes — centralisé ici pour éviter la duplication.
     */
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

    /**
     * Applique la taille de grille choisie au canvas du contexte actif.
     *
     * @param echelle taille de cellule en mètres
     */
    private void appliquerEchelle(double echelle) {
        if (contexteActif instanceof ContexteNiveau ctx) {
            ctx.getNiveauControleur().getVue().getCanvas().setGridSize(echelle);
        } else {
            appView.getCanvasAire().setGridSize(echelle);
        }
    }

    /**
     * Transmet la touche Échap au contrôleur du contexte actif pour
     * annuler un dessin en cours.
     */
    private void onEchap() {
        appView.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                if (contexteActif instanceof ContexteNiveau ctx) {
                    ctx.getNiveauControleur().annulerMurEnCours();
                } else if (contexteActif instanceof ContexteAire) {
                    immeubleControleur.annulerAire();
                }
            }
        });
    }

    // =========================================================================
    // RETOUR DASHBOARD
    // =========================================================================

    /**
     * Retourne au tableau de bord.
     * Seul AppControleur connaît le Stage — les contextes n'y ont pas accès.
     */
    public void retourDashboard() {
        Appartement.resetCompteur();
        DashBoardView dashBoardView = new DashBoardView();
        Scene dashScene = new Scene(dashBoardView);
        stage.setScene(dashScene);
        stage.setTitle("InsaBuilder - Tableau de bord");
        new DashBoardControleur(dashBoardView, stage, gestionnaire);
    }

    // =========================================================================
    // DIALOGUE NOM DE L'IMMEUBLE
    // =========================================================================

    /**
     * Affiche la boîte de dialogue de saisie du nom de l'immeuble au démarrage.
     * Si l'utilisateur annule ou laisse le champ vide, on retourne au dashboard.
     */
    private void demanderNomImmeuble() {
        javafx.scene.control.TextInputDialog dialog =
            new javafx.scene.control.TextInputDialog("Nouvel Immeuble");
        dialog.setTitle("Nom de l'immeuble");
        dialog.setHeaderText("Initialisation du projet");
        dialog.setContentText("Veuillez entrer le nom de l'immeuble :");
        dialog.setGraphic(null);

        // Empêche la validation si le champ est vide
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

    // =========================================================================
    // UTILITAIRE
    // =========================================================================

    /**
     * Extrait le nom de l'immeuble depuis le libellé de la racine du navigateur.
     * Format attendu : "Immeuble : ( NomImmeuble )"
     *
     * @param label libellé de rootItem
     * @return nom extrait, ou "Immeuble" si le format n'est pas reconnu
     */
    private String extraireNomImmeuble(String label) {
        int debut = label.indexOf("( ");
        int fin   = label.indexOf(" )");
        if (debut != -1 && fin != -1 && fin > debut) {
            return label.substring(debut + 2, fin);
        }
        return "Immeuble";
    }
}