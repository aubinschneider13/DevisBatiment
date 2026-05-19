package insa.aubin.devisbatiment.view;

import insa.aubin.devisbatiment.modele.*;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ChoixRevetementDialog extends Dialog<Revetement> {

    public ChoixRevetementDialog(CatalogueRevetements catalogue) {
        setTitle("Matériaux et Revêtements");
        setHeaderText("Sélectionnez un revêtement à appliquer aux surfaces choisies.");

        ButtonType validerButtonType = new ButtonType("Appliquer le matériau", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(validerButtonType, ButtonType.CANCEL);

        VBox root = new VBox(15);
        root.setPadding(new Insets(15));

        // CORRECTION : On agrandit la fenêtre par défaut pour éviter la barre de défilement horizontale
        root.setPrefWidth(600);

        // --- SECTION 1 : LISTE DES REVÊTEMENTS EXISTANTS ---
        Label lblListe = new Label("Catalogue disponible :");
        ListView<Revetement> listView = new ListView<>();
        listView.setItems(catalogue.getListeRevetements());
        listView.setPrefHeight(200);

        listView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Revetement item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null); // Nettoyage propre
                } else {
                    StringBuilder sb = new StringBuilder();
                    sb.append(String.format("%s - %.2f €/m²", item.getDesignation(), item.getPrixUnitaire()));
                    if (item instanceof Peinture p) {
                        sb.append(String.format("  [ Couleur : %s | Finition : %s ]", p.getCouleur(), p.getFinition()));
                    } else if (item instanceof Carrelage c) {
                        sb.append(String.format("  [ Dim : %s | Matière : %s ]", c.getDimension(), c.getMatiere()));
                    } else if (item instanceof Parquet pq) {
                        sb.append(String.format("  [ Bois : %s | Finition : %s ]", pq.getEssenceBois(), pq.getFinition()));
                    }

                    setText(sb.toString());

                    // CORRECTION : Force le texte très long à passer à la ligne suivante
                    setWrapText(true);
                }
            }
        });

        // --- SECTION 2 : FORMULAIRE D'AJOUT ---
        TitledPane formulairePane = new TitledPane();
        formulairePane.setText("Créer un nouveau matériau");
        formulairePane.setExpanded(false);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(10));

        // Éléments de base
        ComboBox<String> cbType = new ComboBox<>(FXCollections.observableArrayList("Revetement Standard", "Peinture", "Carrelage", "Parquet"));
        cbType.setValue("Revetement Standard");

        TextField txtDesignation = new TextField(); txtDesignation.setPromptText("Ex: Peinture Rouge");
        TextField txtPrix = new TextField(); txtPrix.setPromptText("Ex: 15.50");
        CheckBox chkMur = new CheckBox("Mur"); CheckBox chkSol = new CheckBox("Sol"); CheckBox chkPlafond = new CheckBox("Plafond");

        // Éléments dynamiques (invisibles par défaut)
        Label lblAttr1 = new Label(""); TextField txtAttr1 = new TextField();
        Label lblAttr2 = new Label(""); TextField txtAttr2 = new TextField();
        lblAttr1.setVisible(false); txtAttr1.setVisible(false);
        lblAttr2.setVisible(false); txtAttr2.setVisible(false);

        // Mécanique de changement d'affichage
        cbType.setOnAction(e -> {
            String type = cbType.getValue();
            boolean isSpecifique = !type.equals("Revetement Standard");
            lblAttr1.setVisible(isSpecifique); txtAttr1.setVisible(isSpecifique);
            lblAttr2.setVisible(isSpecifique); txtAttr2.setVisible(isSpecifique);

            switch (type) {
                case "Peinture":
                    lblAttr1.setText("Couleur :"); txtAttr1.setPromptText("Ex: Rouge");
                    lblAttr2.setText("Finition :"); txtAttr2.setPromptText("Ex: Mate");
                    break;
                case "Carrelage":
                    lblAttr1.setText("Dimension :"); txtAttr1.setPromptText("Ex: 45x45");
                    lblAttr2.setText("Matière :"); txtAttr2.setPromptText("Ex: Grès");
                    break;
                case "Parquet":
                    lblAttr1.setText("Essence :"); txtAttr1.setPromptText("Ex: Pin");
                    lblAttr2.setText("Finition :"); txtAttr2.setPromptText("Ex: Vernis");
                    break;
            }
        });

        // Disposition dans la grille
        grid.add(new Label("Nature :"), 0, 0); grid.add(cbType, 1, 0);
        grid.add(new Label("Nom :"), 0, 1); grid.add(txtDesignation, 1, 1);
        grid.add(new Label("Prix (€/m²) :"), 0, 2); grid.add(txtPrix, 1, 2);

        HBox boxSurfaces = new HBox(10, chkMur, chkSol, chkPlafond);
        grid.add(new Label("Surfaces :"), 0, 3); grid.add(boxSurfaces, 1, 3);

        grid.add(lblAttr1, 0, 4); grid.add(txtAttr1, 1, 4);
        grid.add(lblAttr2, 0, 5); grid.add(txtAttr2, 1, 5);

        Button btnAjouter = new Button("Enregistrer dans le catalogue");
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

            // CORRECTION : On vérifie qu'au moins une surface est cochée
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
                chkMur.setSelected(false); chkSol.setSelected(false); chkPlafond.setSelected(false);

            } catch (NumberFormatException ex) {
                showAlert("Erreur", "Le format du prix est invalide.");
            }
        });

        root.getChildren().addAll(lblListe, listView, formulairePane);
        getDialogPane().setContent(root);

        setResultConverter(dialogButton -> {
            if (dialogButton == validerButtonType) {
                return listView.getSelectionModel().getSelectedItem();
            }
            return null;
        });
    }

    private void showAlert(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText(titre);
        alert.showAndWait();
    }
}