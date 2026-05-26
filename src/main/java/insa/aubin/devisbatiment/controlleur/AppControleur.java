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

        this.catalogue = gestionnaire.getCatalogue();

        brancherToolBar();
        brancherToolBarDevis();
        brancherNavigateur();

        brancherClavier();

        Appartement.resetCompteur();
        Piece.resetCompteur();

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
        this.catalogue    = gestionnaire.getCatalogue();

        brancherToolBar();
        brancherToolBarDevis();
        brancherNavigateur();

        brancherClavier();

        immeubleControleur = new ImmeubleControleur(
                appView.getCanvasAire(), appView, gestionnaire);

        // Restaurer l'aire depuis l'immeuble chargé
        AireImmeuble aire = new AireImmeuble(immeubleExistant.getPoint1());
        aire.setP2(immeubleExistant.getPoint2());
        aire.setP3(immeubleExistant.getPoint3());
        aire.valider();
        immeubleControleur.setAireImmeuble(aire);

        this.immeuble = immeubleExistant;
        recalibrerCompteursBatiment(immeubleExistant);

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

    /**
     * Fusion master + modifications :
     * - ECHAP lance l'annulation propre selon le contexte actif ;
     * - les autres touches restent transmises au contexte, comme dans le master.
     */
    private void brancherClavier() {
        appView.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                onEchap();
                return;
            }

            if (contexteActif != null) {
                contexteActif.gererToucheClavier(e);
            }
        });
    }

    private void brancherToolBar() {
        ToolBarView tb = appView.getToolBarView();

        tb.getBtnNavigation()   .setOnAction(e -> contexteActif.onBtnNavigation());
        tb.getBtnSelection()    .setOnAction(e -> contexteActif.onBtnSelection());
        tb.getBtnEchelle()      .setOnAction(e -> contexteActif.onBtnEchelle());
        tb.getBtnMur()          .setOnAction(e -> contexteActif.onBtnMur());
        tb.getBtnAppartement()  .setOnAction(e -> contexteActif.onBtnAppartement());
        tb.getBtnEscalier()     .setOnAction(e -> contexteActif.onBtnEscalier());
        tb.getBtnAscenseur()    .setOnAction(e -> contexteActif.onBtnAscenseur());
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

                System.out.println("[DEBUG GUI] Clic Validation Matériau: " + revChoisi.getDesignation() + " (ID: " + revChoisi.getId() + ")");
                System.out.println("[DEBUG GUI] Nombre de surfaces sélectionnées à traiter: " + selection.size());

                Appartement appartActuel = null;
                Niveau niveauActuel = null;
                if (contexteActif instanceof ContextePiece ctx) {
                    appartActuel = ctx.getAppartement();
                } else if (contexteActif instanceof ContexteSousPiece ctx) {
                    Piece p = ctx.getPiece();
                    if (immeuble != null) {
                        for (Niveau niv : immeuble.getNiveaux()) {
                            for (Appartement app : niv.getAppartements()) {
                                if (app.getPieces().contains(p)) {
                                    appartActuel = app;
                                    break;
                                }
                            }
                            if (appartActuel != null) break;
                        }
                    }
                }
                
                if (appartActuel != null && immeuble != null) {
                    for (Niveau niv : immeuble.getNiveaux()) {
                        if (niv.getAppartements().contains(appartActuel)) {
                            niveauActuel = niv;
                            break;
                        }
                    }
                }

                final Appartement finalAppartActuel = appartActuel;

                for (SurfaceAvecRevetement surface : selection) {
                    if (surface instanceof CoteMur coteSel && targetMurs) {
                        Mur parent = coteSel.getMurParent();
                        
                        // Chercher le vrai mur correspondant dans l'appartement actif par ses coordonnées géométriques
                        Mur original = parent;
                        if (finalAppartActuel != null) {
                            original = finalAppartActuel.getMurs().stream()
                                    .filter(wall -> GeometrieUtils.mursIdentiques(wall, parent))
                                    .findFirst()
                                    .orElse(parent);
                        }

                        boolean estGauche = (coteSel == parent.getCoteGauche());
                        CoteMur coteOriginal = estGauche ? original.getCoteGauche() : original.getCoteDroit();

                        if (coteOriginal.estCompatibleAvec(revChoisi)) {
                            coteOriginal.ajouterRevetement(revChoisi);
                        }

                        // Appliquer également au clone graphique pour l'IHM immédiate
                        if (coteSel != coteOriginal && coteSel.estCompatibleAvec(revChoisi)) {
                            coteSel.ajouterRevetement(revChoisi);
                        }

                        // Appliquer également à tous les clones graphiques du même mur sur le canvas actif pour IHM immédiate
                        if (contexteActif instanceof ContextePiece ctx) {
                            for (Dessin d : ctx.getPieceControleur().getVue().getCanvas().getElements()) {
                                if (d instanceof Mur murClone && murClone.getOriginal() == original) {
                                    CoteMur coteClone = estGauche ? murClone.getCoteGauche() : murClone.getCoteDroit();
                                    if (coteClone.estCompatibleAvec(revChoisi)) {
                                        coteClone.ajouterRevetement(revChoisi);
                                    }
                                }
                            }
                        } else if (contexteActif instanceof ContexteSousPiece ctx) {
                            for (Dessin d : ctx.getPieceControleur().getVue().getCanvas().getElements()) {
                                if (d instanceof Mur murClone && murClone.getOriginal() == original) {
                                    CoteMur coteClone = estGauche ? murClone.getCoteGauche() : murClone.getCoteDroit();
                                    if (coteClone.estCompatibleAvec(revChoisi)) {
                                        coteClone.ajouterRevetement(revChoisi);
                                    }
                                }
                            }
                        }
                    }
                    else if (surface instanceof Sol && targetSol) {
                        if (surface.estCompatibleAvec(revChoisi)) {
                            surface.getRevetements().clear();
                            surface.ajouterRevetement(revChoisi);
                        }
                    }
                    else if (surface instanceof Plafond && targetPlafond) {
                        if (surface.estCompatibleAvec(revChoisi)) {
                            surface.getRevetements().clear();
                            surface.ajouterRevetement(revChoisi);
                        }
                    }
                }

                if (appartActuel != null && niveauActuel != null) {
                    gestionnaire.sauvegarderAppartementComplet(appartActuel, niveauActuel, immeuble);
                    appartActuel.calculerDevis();
                }

                if (contexteActif instanceof ContextePiece ctx) {
                    ctx.getPieceControleur().redraw();
                } else if (contexteActif instanceof ContexteSousPiece ctx) {
                    ctx.getPieceControleur().redraw();
                }

                if (contexteActif instanceof ContexteSousPiece ctx) {
                    ctx.viderSelection();
                } else if (contexteActif instanceof ContextePiece ctx) {
                    ctx.viderSelection();
                }

                sauvegarderDetailsOuvertures();

                appView.setInstructions("Mat\u00e9riau " + revChoisi.getDesignation() + " appliqu\u00e9 avec succ\u00e8s !");

                rafraichirDevisEtProprietes();
            }
        });

        tbDevis.getBtnExporterDevis().setOnAction(e -> {
            if (immeuble == null) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Aucun immeuble à exporter !");
                alert.showAndWait();
                return;
            }
            List<String> appartementsInvalides = appartementsSansPorteCouloir();
            if (!appartementsInvalides.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING,
                        "Chaque appartement doit avoir au moins une porte sur un mur adjacent au couloir.\n\n"
                        + "Appartement(s) a corriger :\n- "
                        + String.join("\n- ", appartementsInvalides));
                alert.setHeaderText("Export impossible");
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

    private List<String> appartementsSansPorteCouloir() {
        List<String> invalides = new ArrayList<>();
        if (immeuble == null) return invalides;

        for (int i = 0; i < niveauControleurs.size(); i++) {
            NiveauControleur ctrl = niveauControleurs.get(i);
            Niveau niveau = i < immeuble.getNiveaux().size() ? immeuble.getNiveaux().get(i) : null;
            if (niveau == null) continue;

            for (Appartement appartement : niveau.getAppartements()) {
                if (!appartementAPorteSurCouloir(appartement, ctrl)) {
                    invalides.add(appartement.toString());
                }
            }
        }
        return invalides;
    }

    private boolean appartementAPorteSurCouloir(Appartement appartement, NiveauControleur ctrl) {
        if (appartement == null || ctrl == null) return false;

        for (Mur mur : appartement.getMurs()) {
            if (!ctrl.estAdjacentsAuCouloir(mur)) continue;
            if (mur.getListeOuvertures().stream().anyMatch(o -> o instanceof Porte)) {
                return true;
            }
        }
        return false;
    }

    private double calculerDevisAppartement(Appartement appartement) {
        if (appartement == null) return 0;

        if (immeuble != null) {
            for (Niveau niveau : immeuble.getNiveaux()) {
                if (niveau.getAppartements().contains(appartement)) {
                    return appartement.calculerDevis(niveau.getTremies());
                }
            }
        }
        return appartement.calculerDevis();
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
                sauvegarderDetailsOuvertures();
                itemNiveauActif = itemsNiveau.get(i);

                ContextePiece ctx = contextePieces.computeIfAbsent(appart, a ->
                        new ContextePiece(a, appView, this, stage, gestionnaire, item)
                        
                );
                
                basculerContexte(ctx);
                NiveauControleur niveauCtrl = niveauControleurs.get(i);
                ctx.getPieceControleur().setNiveauControleur(niveauCtrl);
                Niveau niveau = i < immeuble.getNiveaux().size() ? immeuble.getNiveaux().get(i) : null;
                ctx.getPieceControleur().setContexteSauvegarde(appart, niveau, immeuble);
                ctx.getPieceControleur().rechargerVueAppartementDepuisDisque();

                double totalDevis = calculerDevisAppartement(appart);
                tbDevis.getLabelTotalDevis().setText(String.format("Total estimé : %.2f €", totalDevis));
                nav.afficherProprietesAppartement(appart);
                return;
            }
        }

        // --- Clic sur un couloir ---
        Couloir couloir = mapItemCouloir.get(item);
        if (couloir != null) {
            NiveauControleur niveauCtrl = trouverNiveauControleurPourCouloir(couloir);
            ContexteCouloir ctx = new ContexteCouloir(couloir, appView, this, stage, gestionnaire, niveauCtrl);
            contexteCouloirs.put(couloir, ctx);
            basculerContexte(ctx);
            tbDevis.getLabelTotalDevis().setText("Total estimé : -- €");
            return;
        }

        // --- Clic sur une pièce ---
        Piece piece = mapItemPiece.get(item);
        if (piece != null) {
            sauvegarderDetailsOuvertures();
            ContexteSousPiece ctx = contexteSousPieces.computeIfAbsent(piece, p ->
                    new ContexteSousPiece(p, appView, this, stage, gestionnaire)
            );

            basculerContexte(ctx);

            NiveauControleur niveauCtrl = trouverNiveauControleurPourPiece(item);
            if (niveauCtrl != null) {
                ctx.getPieceControleur().setNiveauControleur(niveauCtrl);
            }

            Appartement appartParent = trouverAppartementPourPiece(item);
            if (appartParent != null && niveauCtrl != null) {
                int idx = niveauControleurs.indexOf(niveauCtrl);
                Niveau niveau = idx >= 0 && idx < immeuble.getNiveaux().size()
                        ? immeuble.getNiveaux().get(idx)
                        : null;
                ctx.getPieceControleur().setContexteSauvegarde(appartParent, niveau, immeuble);
            }
            ctx.getPieceControleur().rechargerVuePieceDepuisDisque(piece);

            double totalDevis = piece.calculerDevis();
            tbDevis.getLabelTotalDevis().setText(String.format("Total estimé : %.2f €", totalDevis));
            nav.afficherProprietesPiece(piece);
            return;
        }
    }

    private NiveauControleur trouverNiveauControleurPourPiece(TreeItem<String> itemPiece) {
        TreeItem<String> itemAppart = mapPieceVersAppart.get(itemPiece);
        if (itemAppart == null) return null;

        for (NiveauControleur ctrl : niveauControleurs) {
            if (ctrl.getMapItemAppartement().containsKey(itemAppart)) {
                return ctrl;
            }
        }
        return null;
    }

    private Appartement trouverAppartementPourPiece(TreeItem<String> itemPiece) {
        TreeItem<String> itemAppart = mapPieceVersAppart.get(itemPiece);
        if (itemAppart == null) return null;
        Appartement appart = mapItemAppartement.get(itemAppart);
        if (appart != null) return appart;
        for (NiveauControleur ctrl : niveauControleurs) {
            appart = ctrl.getMapItemAppartement().get(itemAppart);
            if (appart != null) return appart;
        }
        return null;
    }

    private NiveauControleur trouverNiveauControleurPourCouloir(Couloir couloir) {
        if (couloir == null) return null;
        for (NiveauControleur ctrl : niveauControleurs) {
            if (ctrl.getNiveau().getCouloirs().contains(couloir)) {
                return ctrl;
            }
        }
        return null;
    }

    public void activerPlacementTremieDepuisCouloir(NiveauControleur niveauControleur,
                                                    boolean escalier) {
        if (niveauControleur == null) return;

        int idx = niveauControleurs.indexOf(niveauControleur);
        if (idx >= 0 && idx < itemsNiveau.size()) {
            itemNiveauActif = itemsNiveau.get(idx);
            appView.getNavigateurView().selectionner(itemNiveauActif);
        }

        basculerContexte(new ContexteNiveau(niveauControleur, appView, this));
        if (escalier) {
            niveauControleur.activerModeEscalier();
        } else {
            niveauControleur.activerModeAscenseur();
        }
    }

    /**
     * Recalcule instantanément le devis et met à jour le panneau des propriétés
     * à droite selon l'élément actuellement sélectionné dans l'arbre.
     */
    public void rafraichirDevisEtProprietes() {
        ToolBarDevisView tbDevis = appView.getToolBarDevisView();
        NavigateurView nav = appView.getNavigateurView();

        // 1. Mise à jour du libellé de devis selon le contexte actif
        if (contexteActif instanceof ContextePiece ctx) {
            double totalDevis = calculerDevisAppartement(ctx.getAppartement());
            tbDevis.getLabelTotalDevis().setText(String.format("Total estimé : %.2f €", totalDevis));
        } else if (contexteActif instanceof ContexteSousPiece ctx) {
            double totalDevis = ctx.getPiece().calculerDevis();
            tbDevis.getLabelTotalDevis().setText(String.format("Total estimé : %.2f €", totalDevis));
        }

        // 2. Mise à jour de la fiche de propriétés latérale droite
        if (contexteActif instanceof ContexteSousPiece ctx) {
            nav.afficherProprietesPiece(ctx.getPiece());
        } else if (contexteActif instanceof ContextePiece ctx) {
            nav.afficherProprietesAppartement(ctx.getAppartement());
        } else {
            TreeItem<String> itemSelectionne = nav.getTreeView().getSelectionModel().getSelectedItem();
            if (itemSelectionne != null) {
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
        gestionnaire.sauvegarderNiveau(niveau, immeuble);

        String nomNiveau = niveauControleurs.isEmpty()
                ? "RDC"
                : "Niveau " + niveauControleurs.size();

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

        ctrl.setOnAppartementCree(appart -> {
            TreeItem<String> itemAppart = appView.getNavigateurView()
                    .ajouterItemAppartement(itemNiveau, appart.toString());
            mapItemAppartement.put(itemAppart, appart);
            gestionnaire.sauvegarderAppartement(appart, niveau, immeuble);
            Niveau niveauCourant = mapItemNiveau.get(itemNiveau);
            if (niveauCourant != null) {
                appView.getNavigateurView().afficherProprietesNiveau(niveauCourant);
            }
            return itemAppart;
        });

        Map<TreeItem<String>, Couloir> itemsCouloirDuNiveau = new HashMap<>();
        ctrl.setOnCouloirCree(couloir -> {
            itemNiveau.getChildren().removeAll(itemsCouloirDuNiveau.keySet());
            itemsCouloirDuNiveau.clear();
            String nomCouloir = "Couloir du " + itemNiveau.getValue();
            TreeItem<String> itemCouloir = appView.getNavigateurView().ajouterItemCouloir(itemNiveau, nomCouloir);
            itemsCouloirDuNiveau.put(itemCouloir, couloir);
            mapItemCouloir.put(itemCouloir, couloir);
            gestionnaire.sauvegarderCouloirs(niveau, immeuble);
            return itemCouloir;
        });

        // Notifie TOUS les ContextePiece ouverts pour ce niveau
        ctrl.setOnCouloirsRecalcules(() -> {
            gestionnaire.sauvegarderCouloirs(niveau, immeuble);
            for (ContextePiece ctx : contextePieces.values()) {
                if (ctx.getPieceControleur() != null) {
                    ctx.getPieceControleur().rafraichirTypesMursAffichage();
                }
            }
        });

        ctrl.setOnModification(() -> {
            gestionnaire.sauvegarderCouloirs(niveau, immeuble);
            rafraichirDevisEtProprietes();
        });
        ctrl.setOnAppartementSupprime(appart -> oublierAppartementSupprime(appart, niveau));

        brancherTremiesGlobales(ctrl);

        niveauControleurs.add(ctrl);
        recopierTremiesExistantesSurNouveauNiveau(ctrl, niveau);

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
            ctx.getNiveauControleur().revenirEtatNeutre();
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

    public void oublierPieceSupprimee(Piece piece) {
        if (piece == null) return;

        TreeItem<String> itemPiece = trouverItemPiece(piece);
        if (itemPiece == null) return;

        TreeItem<String> itemAppart = mapPieceVersAppart.get(itemPiece);
        Appartement appart = itemAppart != null ? mapItemAppartement.get(itemAppart) : null;
        Niveau niveau = null;
        NiveauControleur niveauCtrl = trouverNiveauControleurPourPiece(itemPiece);
        if (niveauCtrl != null) {
            int idx = niveauControleurs.indexOf(niveauCtrl);
            if (idx >= 0 && idx < immeuble.getNiveaux().size()) {
                niveau = immeuble.getNiveaux().get(idx);
            }
        }

        TreeItem<String> parent = itemPiece.getParent();
        if (parent != null) {
            parent.getChildren().remove(itemPiece);
        }
        mapItemPiece.remove(itemPiece);
        mapPieceVersAppart.remove(itemPiece);
        contexteSousPieces.remove(piece);

        if (appart != null && niveau != null) {
            gestionnaire.sauvegarderAppartementComplet(appart, niveau, immeuble);
        }

        if (parent != null) {
            appView.getNavigateurView().selectionner(parent);
        } else {
            appView.getNavigateurView().effacerProprietes();
        }
        rafraichirDevisEtProprietes();
    }

    private TreeItem<String> trouverItemPiece(Piece piece) {
        for (Map.Entry<TreeItem<String>, Piece> entry : mapItemPiece.entrySet()) {
            if (entry.getValue() == piece) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void oublierAppartementSupprime(Appartement appartement, Niveau niveau) {
        if (appartement == null || niveau == null) return;

        TreeItem<String> itemAppart = trouverItemAppartement(appartement);
        if (itemAppart != null) {
            List<TreeItem<String>> enfants = new ArrayList<>(itemAppart.getChildren());
            for (TreeItem<String> itemPiece : enfants) {
                Piece piece = mapItemPiece.remove(itemPiece);
                if (piece != null) {
                    contexteSousPieces.remove(piece);
                }
                mapPieceVersAppart.remove(itemPiece);
            }

            TreeItem<String> parent = itemAppart.getParent();
            if (parent != null) {
                parent.getChildren().remove(itemAppart);
            }
        }

        mapItemAppartement.entrySet().removeIf(entry -> entry.getValue() == appartement);
        contextePieces.remove(appartement);
        for (Piece piece : new ArrayList<>(appartement.getPieces())) {
            contexteSousPieces.remove(piece);
        }

        gestionnaire.supprimerAppartement(appartement, niveau, immeuble);
        sauvegarderDetailsOuvertures();
        appView.getNavigateurView().effacerProprietes();
        if (itemNiveauActif != null) {
            appView.getNavigateurView().selectionner(itemNiveauActif);
        }
        rafraichirDevisEtProprietes();
    }

    private TreeItem<String> trouverItemAppartement(Appartement appartement) {
        for (Map.Entry<TreeItem<String>, Appartement> entry : mapItemAppartement.entrySet()) {
            if (entry.getValue() == appartement) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void sauvegarderDetailsOuvertures() {
        if (immeuble == null || gestionnaire == null || !gestionnaire.isSauvegardeActive()) return;

        for (int i = 0; i < niveauControleurs.size(); i++) {
            NiveauControleur ctrl = niveauControleurs.get(i);
            Niveau niveau = i < immeuble.getNiveaux().size() ? immeuble.getNiveaux().get(i) : null;
            if (niveau == null) continue;

            for (Appartement appart : niveau.getAppartements()) {
                gestionnaire.sauvegarderAppartementComplet(appart, niveau, immeuble);
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

            Map<TreeItem<String>, Couloir> itemsCouloirDuNiveau = new HashMap<>();
            ctrl.setOnCouloirCree(couloir -> {
                itemNiveau.getChildren().removeAll(itemsCouloirDuNiveau.keySet());
                itemsCouloirDuNiveau.clear();
                String nomCouloir = "Couloir du " + itemNiveau.getValue();
                TreeItem<String> itemCouloir = appView.getNavigateurView().ajouterItemCouloir(itemNiveau, nomCouloir);
                itemsCouloirDuNiveau.put(itemCouloir, couloir);
                mapItemCouloir.put(itemCouloir, couloir);
                gestionnaire.sauvegarderCouloirs(niveauFinal, immeuble);
                return itemCouloir;
            });

            // Notifie TOUS les ContextePiece ouverts pour ce niveau
            ctrl.setOnCouloirsRecalcules(() -> {
                gestionnaire.sauvegarderCouloirs(niveauFinal, immeuble);
                for (ContextePiece ctx : contextePieces.values()) {
                    if (ctx.getPieceControleur() != null) {
                        ctx.getPieceControleur().rafraichirTypesMursAffichage();
                    }
                }
            });

            ctrl.setOnModification(() -> {
                gestionnaire.sauvegarderCouloirs(niveauFinal, immeuble);
                rafraichirDevisEtProprietes();
            });
            ctrl.setOnAppartementSupprime(appart -> oublierAppartementSupprime(appart, niveauFinal));

            ctrl.rechargerCouloirs(niveau.getCouloirs());
            ctrl.rechargerTremies();
            brancherTremiesGlobales(ctrl);
            niveauControleurs.add(ctrl);
        }
    }

    private void brancherTremiesGlobales(NiveauControleur ctrl) {
        ctrl.setOnTremiePlacee(tremie -> {
            placerTremieSurTousLesNiveaux(tremie);
            return true;
        });
        ctrl.setOnTremieSupprimee(this::supprimerTremieSurTousLesNiveaux);
    }

    private void placerTremieSurTousLesNiveaux(Tremie modele) {
        if (modele == null || immeuble == null) return;

        for (int i = 0; i < niveauControleurs.size(); i++) {
            NiveauControleur ctrl = niveauControleurs.get(i);
            Tremie copie = copierTremie(modele);
            ctrl.ajouterTremieDepuisPropagation(copie);

            Niveau niveau = i < immeuble.getNiveaux().size() ? immeuble.getNiveaux().get(i) : null;
            if (niveau != null) {
                gestionnaire.sauvegarderTremies(niveau, immeuble);
            }
        }
    }

    private void supprimerTremieSurTousLesNiveaux(Tremie tremie) {
        if (tremie == null || immeuble == null) return;

        for (int i = 0; i < niveauControleurs.size(); i++) {
            NiveauControleur ctrl = niveauControleurs.get(i);
            ctrl.supprimerTremieDepuisPropagation(tremie.getId());

            Niveau niveau = i < immeuble.getNiveaux().size() ? immeuble.getNiveaux().get(i) : null;
            if (niveau != null) {
                gestionnaire.sauvegarderTremies(niveau, immeuble);
            }
        }
    }

    private void recopierTremiesExistantesSurNouveauNiveau(NiveauControleur ctrl, Niveau niveau) {
        if (ctrl == null || niveau == null || niveauControleurs.isEmpty()) return;

        for (Tremie tremie : niveauControleurs.get(0).getTremies()) {
            ctrl.ajouterTremieDepuisPropagation(copierTremie(tremie));
        }
        if (!niveau.getTremies().isEmpty()) {
            gestionnaire.sauvegarderTremies(niveau, immeuble);
        }
    }

    private Tremie copierTremie(Tremie source) {
        Point centre = new Point(source.getX(), source.getY());
        Tremie copie = source instanceof Escalier ? new Escalier(centre) : new Ascenseur(centre);
        copie.recopierIdDepuis(source);
        return copie;
    }

    private void recalibrerCompteursBatiment(Immeuble immeuble) {
        int maxAppartement = 0;
        int maxPiece = 0;

        if (immeuble != null) {
            for (Niveau niveau : immeuble.getNiveaux()) {
                for (Appartement appartement : niveau.getAppartements()) {
                    maxAppartement = Math.max(maxAppartement, appartement.getNumero());

                    for (Piece piece : appartement.getPieces()) {
                        maxPiece = Math.max(maxPiece, piece.getNumero());
                    }
                }
            }
        }

        Appartement.setCompteur(maxAppartement);
        Piece.setCompteur(maxPiece);
    }
}
