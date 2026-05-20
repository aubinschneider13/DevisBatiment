package insa.aubin.devisbatiment.view;

import insa.aubin.devisbatiment.modele.*;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChoixRevetementDialog extends Dialog<Revetement> {
    private CheckBox chkTargetMur;
    private CheckBox chkTargetSol;
    private CheckBox chkTargetPlafond;

    public ChoixRevetementDialog(CatalogueRevetements catalogue, List<SurfaceAvecRevetement> surfacesSelectionnees) {
        setTitle("Matériaux et Revêtements");
        
        // Supprimer le header natif par défaut pour un rendu plus premium
        setHeaderText(null);

        ButtonType validerButtonType = new ButtonType("Appliquer le matériau", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(validerButtonType, ButtonType.CANCEL);

        VBox root = new VBox(15);
        root.setPadding(new Insets(15));
        root.setPrefWidth(620);

        // --- EN-TÊTE DYNAMIQUE ET BANDEAU INFORMATIF ---
        int nbMurs = 0;
        int nbSols = 0;
        int nbPlafonds = 0;
        if (surfacesSelectionnees != null) {
            for (SurfaceAvecRevetement s : surfacesSelectionnees) {
                if (s instanceof CoteMur) nbMurs++;
                else if (s instanceof Sol) nbSols++;
                else if (s instanceof Plafond) nbPlafonds++;
            }
        }

        final int finalNbMurs = nbMurs;
        final int finalNbSols = nbSols;
        final int finalNbPlafonds = nbPlafonds;

        // --- BLOC DE CIBLAGE DES SURFACES ---
        VBox targetCard = new VBox(8);
        targetCard.setPadding(new Insets(12, 16, 12, 16));
        targetCard.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #cbd5e1; -fx-border-width: 1px; -fx-border-radius: 8px; -fx-background-radius: 8px;");

        Label lblTargetTitle = new Label("Surfaces de destination à cibler :");
        lblTargetTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        chkTargetMur = new CheckBox("🧱 Murs");
        chkTargetSol = new CheckBox("🪵 Sol");
        chkTargetPlafond = new CheckBox("🏠 Plafond");

        String chkStyle = "-fx-font-size: 12px; -fx-text-fill: #334155; -fx-font-weight: bold; -fx-cursor: hand;";
        chkTargetMur.setStyle(chkStyle);
        chkTargetSol.setStyle(chkStyle);
        chkTargetPlafond.setStyle(chkStyle);

        // Auto-cochage selon la sélection
        chkTargetMur.setSelected(nbMurs > 0);
        chkTargetSol.setSelected(nbSols > 0);
        chkTargetPlafond.setSelected(nbPlafonds > 0);

        // UX Premium : Désactiver si non présent dans la sélection initiale
        chkTargetMur.setDisable(nbMurs == 0);
        chkTargetSol.setDisable(nbSols == 0);
        chkTargetPlafond.setDisable(nbPlafonds == 0);

        HBox targetChks = new HBox(20, chkTargetMur, chkTargetSol, chkTargetPlafond);
        targetCard.getChildren().addAll(lblTargetTitle, targetChks);

        VBox headerCard = new VBox(6);
        headerCard.setPadding(new Insets(12, 16, 12, 16));
        headerCard.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #cbd5e1; -fx-border-width: 1px; -fx-border-radius: 8px; -fx-background-radius: 8px;");

        Label lblHeaderTitle = new Label("Application de revêtement");
        lblHeaderTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        HBox badgeBox = new HBox(8);
        badgeBox.setPadding(new Insets(4, 0, 4, 0));
        
        if (nbMurs > 0) {
            Label badge = new Label("🧱 " + nbMurs + " Mur" + (nbMurs > 1 ? "s" : ""));
            badge.setStyle("-fx-background-color: #e0f2fe; -fx-text-fill: #0369a1; -fx-font-weight: bold; -fx-padding: 4 8; -fx-background-radius: 12; -fx-font-size: 11px;");
            badgeBox.getChildren().add(badge);
        }
        if (nbSols > 0) {
            Label badge = new Label("🪵 " + nbSols + " Sol" + (nbSols > 1 ? "s" : ""));
            badge.setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #b45309; -fx-font-weight: bold; -fx-padding: 4 8; -fx-background-radius: 12; -fx-font-size: 11px;");
            badgeBox.getChildren().add(badge);
        }
        if (nbPlafonds > 0) {
            Label badge = new Label("🏠 " + nbPlafonds + " Plafond" + (nbPlafonds > 1 ? "s" : ""));
            badge.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-font-weight: bold; -fx-padding: 4 8; -fx-background-radius: 12; -fx-font-size: 11px;");
            badgeBox.getChildren().add(badge);
        }
        if (nbMurs == 0 && nbSols == 0 && nbPlafonds == 0) {
            Label badge = new Label("Aucune surface sélectionnée");
            badge.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-font-weight: bold; -fx-padding: 4 8; -fx-background-radius: 12; -fx-font-size: 11px;");
            badgeBox.getChildren().add(badge);
        }

        Label lblHeaderSubtitle = new Label("Le catalogue a été automatiquement filtré pour présenter uniquement les matériaux compatibles.");
        lblHeaderSubtitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

        headerCard.getChildren().addAll(lblHeaderTitle, badgeBox, lblHeaderSubtitle);

        // --- SECTION 1 : LISTE DES REVÊTEMENTS EXISTANTS (FILTRÉE) ---
        Label lblListe = new Label("Catalogue disponible :");
        lblListe.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        
        ListView<Revetement> listView = new ListView<>();
        listView.setPrefHeight(200);
        listView.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #cbd5e1; -fx-background-color: white;");

        // Filtrage dynamique lié aux checkboxes de ciblage
        FilteredList<Revetement> filteredList = new FilteredList<>(catalogue.getListeRevetements());
        
        Runnable updateFilter = () -> {
            filteredList.setPredicate(r -> {
                if (r == null) return false;
                
                boolean targetMurs = chkTargetMur.isSelected();
                boolean targetSol = chkTargetSol.isSelected();
                boolean targetPlafond = chkTargetPlafond.isSelected();
                
                if (!targetMurs && !targetSol && !targetPlafond) {
                    return false;
                }
                
                boolean compatible = false;
                if (targetMurs && r.isPourMur()) compatible = true;
                if (targetSol && r.isPourSol()) compatible = true;
                if (targetPlafond && r.isPourPlafond()) compatible = true;
                return compatible;
            });
        };
        
        chkTargetMur.selectedProperty().addListener((obs, oldVal, newVal) -> updateFilter.run());
        chkTargetSol.selectedProperty().addListener((obs, oldVal, newVal) -> updateFilter.run());
        chkTargetPlafond.selectedProperty().addListener((obs, oldVal, newVal) -> updateFilter.run());
        
        updateFilter.run();
        listView.setItems(filteredList);

        // Rendu des cellules haut de gamme
        listView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Revetement item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox cellContainer = new VBox(4);
                    cellContainer.setPadding(new Insets(8, 10, 8, 10));

                    HBox header = new HBox(10);
                    Label lblName = new Label(item.getDesignation());
                    lblName.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b; -fx-font-size: 13px;");
                    Label lblPrice = new Label(String.format(Locale.US, "%.2f €/m²", item.getPrixUnitaire()));
                    lblPrice.setStyle("-fx-text-fill: #2563eb; -fx-font-weight: bold; -fx-font-size: 13px;");
                    
                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);
                    header.getChildren().addAll(lblName, spacer, lblPrice);

                    Label lblDetails = new Label();
                    lblDetails.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
                    
                    if (item instanceof Peinture p) {
                        lblDetails.setText(String.format("🎨 Peinture • Couleur : %s • Finition : %s", p.getCouleur(), p.getFinition()));
                    } else if (item instanceof Carrelage c) {
                        lblDetails.setText(String.format("🧱 Carrelage • Dim : %s • Matière : %s", c.getDimension(), c.getMatiere()));
                    } else if (item instanceof Parquet pq) {
                        lblDetails.setText(String.format("🪵 Parquet • Bois : %s • Finition : %s", pq.getEssenceBois(), pq.getFinition()));
                    } else {
                        lblDetails.setText("🛠️ Revêtement Standard");
                    }

                    // Petits badges de support de surface
                    HBox surfaceSupport = new HBox(4);
                    surfaceSupport.setPadding(new Insets(2, 0, 0, 0));
                    if (item.isPourMur()) {
                        Label m = new Label("Mur");
                        m.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-font-size: 9px; -fx-padding: 2 6; -fx-background-radius: 4;");
                        surfaceSupport.getChildren().add(m);
                    }
                    if (item.isPourSol()) {
                        Label s = new Label("Sol");
                        s.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-font-size: 9px; -fx-padding: 2 6; -fx-background-radius: 4;");
                        surfaceSupport.getChildren().add(s);
                    }
                    if (item.isPourPlafond()) {
                        Label p = new Label("Plafond");
                        p.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-font-size: 9px; -fx-padding: 2 6; -fx-background-radius: 4;");
                        surfaceSupport.getChildren().add(p);
                    }

                    cellContainer.getChildren().addAll(header, lblDetails, surfaceSupport);
                    setGraphic(cellContainer);
                }
            }
        });

        // Sélectionner le premier élément par défaut s'il y en a un
        if (!filteredList.isEmpty()) {
            listView.getSelectionModel().select(0);
        }

        // --- SECTION 2 : FORMULAIRE D'AJOUT VISUELLEMENT MODERNE ---
        TitledPane formulairePane = new TitledPane();
        formulairePane.setText("Créer et ajouter un nouveau matériau");
        formulairePane.setExpanded(false);
        formulairePane.setStyle("-fx-font-size: 13px; -fx-text-fill: #1e293b;");

        GridPane grid = new GridPane();
        grid.setHgap(12); 
        grid.setVgap(12); 
        grid.setPadding(new Insets(15));
        grid.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #e2e8f0; -fx-border-width: 1px;");

        // Éléments de base
        ComboBox<String> cbType = new ComboBox<>(FXCollections.observableArrayList("Revetement Standard", "Peinture", "Carrelage", "Parquet"));
        cbType.setValue("Revetement Standard");

        TextField txtDesignation = new TextField(); 
        txtDesignation.setPromptText("Ex: Peinture Rouge");
        
        TextField txtPrix = new TextField(); 
        txtPrix.setPromptText("Ex: 15.50");

        CheckBox chkMur = new CheckBox("Mur"); 
        CheckBox chkSol = new CheckBox("Sol"); 
        CheckBox chkPlafond = new CheckBox("Plafond");
        
        // UX Premium : Pré-cocher les cases selon la sélection active !
        if (finalNbMurs > 0) chkMur.setSelected(true);
        if (finalNbSols > 0) chkSol.setSelected(true);
        if (finalNbPlafonds > 0) chkPlafond.setSelected(true);

        // Appliquer un style moderne aux inputs
        String fieldStyle = "-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #cbd5e1; -fx-padding: 6 10; -fx-font-size: 12px; -fx-background-color: white;";
        txtDesignation.setStyle(fieldStyle);
        txtPrix.setStyle(fieldStyle);
        cbType.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #cbd5e1; -fx-font-size: 12px; -fx-background-color: white;");

        // Éléments dynamiques (invisibles par défaut)
        Label lblAttr1 = new Label(""); 
        TextField txtAttr1 = new TextField();
        Label lblAttr2 = new Label(""); 
        TextField txtAttr2 = new TextField();
        
        txtAttr1.setStyle(fieldStyle);
        txtAttr2.setStyle(fieldStyle);
        
        lblAttr1.setVisible(false); txtAttr1.setVisible(false);
        lblAttr2.setVisible(false); txtAttr2.setVisible(false);

        String labelStyle = "-fx-font-weight: bold; -fx-text-fill: #475569; -fx-font-size: 12px;";

        // Mécanique de bascule dynamique
        cbType.setOnAction(e -> {
            String type = cbType.getValue();
            boolean isSpecifique = !type.equals("Revetement Standard");
            lblAttr1.setVisible(isSpecifique); txtAttr1.setVisible(isSpecifique);
            lblAttr2.setVisible(isSpecifique); txtAttr2.setVisible(isSpecifique);

            switch (type) {
                case "Peinture":
                    lblAttr1.setText("Couleur :"); txtAttr1.setPromptText("Ex: Rouge");
                    lblAttr2.setText("Finition :"); txtAttr2.setPromptText("Ex: Mate");
                    // Auto-cocher peinture pour murs et plafonds
                    if (finalNbMurs == 0 && finalNbSols == 0 && finalNbPlafonds == 0) {
                        chkMur.setSelected(true); chkSol.setSelected(false); chkPlafond.setSelected(true);
                    }
                    break;
                case "Carrelage":
                    lblAttr1.setText("Dimension :"); txtAttr1.setPromptText("Ex: 45x45");
                    lblAttr2.setText("Matière :"); txtAttr2.setPromptText("Ex: Grès");
                    if (finalNbMurs == 0 && finalNbSols == 0 && finalNbPlafonds == 0) {
                        chkMur.setSelected(true); chkSol.setSelected(true); chkPlafond.setSelected(false);
                    }
                    break;
                case "Parquet":
                    lblAttr1.setText("Essence :"); txtAttr1.setPromptText("Ex: Chêne");
                    lblAttr2.setText("Finition :"); txtAttr2.setPromptText("Ex: Vernis");
                    if (finalNbMurs == 0 && finalNbSols == 0 && finalNbPlafonds == 0) {
                        chkMur.setSelected(false); chkSol.setSelected(true); chkPlafond.setSelected(false);
                    }
                    break;
                default:
                    lblAttr1.setText(""); lblAttr2.setText("");
                    break;
            }
        });

        // Ajout des labels avec styles
        Label l1 = new Label("Nature :"); l1.setStyle(labelStyle);
        Label l2 = new Label("Nom :"); l2.setStyle(labelStyle);
        Label l3 = new Label("Prix (€/m²) :"); l3.setStyle(labelStyle);
        Label l4 = new Label("Surfaces :"); l4.setStyle(labelStyle);
        lblAttr1.setStyle(labelStyle);
        lblAttr2.setStyle(labelStyle);

        // Disposition dans la grille
        grid.add(l1, 0, 0); grid.add(cbType, 1, 0);
        grid.add(l2, 0, 1); grid.add(txtDesignation, 1, 1);
        grid.add(l3, 0, 2); grid.add(txtPrix, 1, 2);

        HBox boxSurfaces = new HBox(15, chkMur, chkSol, chkPlafond);
        boxSurfaces.setPadding(new Insets(4, 0, 4, 0));
        grid.add(l4, 0, 3); grid.add(boxSurfaces, 1, 3);

        grid.add(lblAttr1, 0, 4); grid.add(txtAttr1, 1, 4);
        grid.add(lblAttr2, 0, 5); grid.add(txtAttr2, 1, 5);

        Button btnAjouter = new Button("Enregistrer dans le catalogue");
        btnAjouter.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand;");
        btnAjouter.setOnMouseEntered(ev -> btnAjouter.setStyle("-fx-background-color: #1d4ed8; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand;"));
        btnAjouter.setOnMouseExited(ev -> btnAjouter.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand;"));

        grid.add(btnAjouter, 1, 6);
        formulairePane.setContent(grid);

        // --- ACTION D'AJOUT ---
        btnAjouter.setOnAction(e -> {
            String designation = txtDesignation.getText().trim();
            String prixStr = txtPrix.getText().trim().replace(",", ".");

            if (designation.isEmpty() || prixStr.isEmpty()) {
                showAlert("Erreur", "Veuillez remplir le nom et le prix.");
                return;
            }

            if (!chkMur.isSelected() && !chkSol.isSelected() && !chkPlafond.isSelected()) {
                showAlert("Erreur", "Le matériau doit être applicable sur au moins une surface (Mur, Sol ou Plafond).");
                return;
            }

            try {
                double prix = Double.parseDouble(prixStr);
                String nouvelId = catalogue.genererNouvelId();
                String attr1 = txtAttr1.getText().trim();
                String attr2 = txtAttr2.getText().trim();

                Revetement nouveau = switch (cbType.getValue()) {
                    case "Peinture" -> new Peinture(nouvelId, designation, chkMur.isSelected(), chkSol.isSelected(), chkPlafond.isSelected(), prix, attr1, attr2);
                    case "Carrelage" -> new Carrelage(nouvelId, designation, chkMur.isSelected(), chkSol.isSelected(), chkPlafond.isSelected(), prix, attr1, attr2);
                    case "Parquet" -> new Parquet(nouvelId, designation, chkMur.isSelected(), chkSol.isSelected(), chkPlafond.isSelected(), prix, attr1, attr2);
                    default -> new Revetement(nouvelId, designation, chkMur.isSelected(), chkSol.isSelected(), chkPlafond.isSelected(), prix);
                };

                catalogue.ajouterEtSauvegarderRevetement(nouveau);
                listView.getSelectionModel().select(nouveau);
                formulairePane.setExpanded(false);

                // Reset formulaire
                txtDesignation.clear(); txtPrix.clear(); txtAttr1.clear(); txtAttr2.clear();
                
                // Rétablir l'état pré-coché selon la sélection
                chkMur.setSelected(finalNbMurs > 0);
                chkSol.setSelected(finalNbSols > 0);
                chkPlafond.setSelected(finalNbPlafonds > 0);

            } catch (NumberFormatException ex) {
                showAlert("Erreur", "Le format du prix est invalide.");
            }
        });

        root.getChildren().addAll(headerCard, targetCard, lblListe, listView, formulairePane);

        // Envelopper le contenu dans un ScrollPane pour gérer les débordements visuels
        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPrefHeight(500); // Hauteur maximale pour le contenu
        scrollPane.setStyle("-fx-background: white; -fx-background-color: transparent; -fx-padding: 0;");

        getDialogPane().setContent(scrollPane);

        // Personnalisation des boutons système du DialogPane
        DialogPane dialogPane = getDialogPane();
        dialogPane.setStyle("-fx-background-color: white;");

        Button btnOk = (Button) dialogPane.lookupButton(validerButtonType);
        if (btnOk != null) {
            btnOk.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand;");
            btnOk.setOnMouseEntered(ev -> btnOk.setStyle("-fx-background-color: #1d4ed8; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand;"));
            btnOk.setOnMouseExited(ev -> btnOk.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand;"));
        }

        Button btnCancel = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
        if (btnCancel != null) {
            btnCancel.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand;");
            btnCancel.setOnMouseEntered(ev -> btnCancel.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #475569; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand;"));
            btnCancel.setOnMouseExited(ev -> btnCancel.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand;"));
        }

        setResultConverter(dialogButton -> {
            if (dialogButton == validerButtonType) {
                return listView.getSelectionModel().getSelectedItem();
            }
            return null;
        });
    }

    public boolean isAppliquerAuxMurs() {
        return chkTargetMur.isSelected();
    }

    public boolean isAppliquerAuSol() {
        return chkTargetSol.isSelected();
    }

    public boolean isAppliquerAuPlafond() {
        return chkTargetPlafond.isSelected();
    }

    private void showAlert(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText(titre);
        alert.showAndWait();
    }
}