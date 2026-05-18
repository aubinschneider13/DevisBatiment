package insa.aubin.devisbatiment.view;

import insa.aubin.devisbatiment.modele.CatalogueRevetements;
import insa.aubin.devisbatiment.modele.Revetement;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.UUID;

public class ChoixRevetementDialog extends Dialog<Revetement> {

    public ChoixRevetementDialog(CatalogueRevetements catalogue) {
        setTitle("Matériaux et Revêtements");
        setHeaderText("Sélectionnez un revêtement à appliquer aux surfaces choisies.");

        // Ajout des boutons natifs de la boîte de dialogue (Appliquer / Annuler)
        ButtonType validerButtonType = new ButtonType("Appliquer le matériau", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(validerButtonType, ButtonType.CANCEL);

        VBox root = new VBox(15);
        root.setPadding(new Insets(15));

        // =====================================================================
        // SECTION 1 : LISTE DES REVÊTEMENTS EXISTANTS
        // =====================================================================
        Label lblListe = new Label("Catalogue disponible :");
        ListView<Revetement> listView = new ListView<>();
        // On lie la liste au catalogue (mise à jour automatique si ajout)
        listView.setItems(catalogue.getListeRevetements());
        listView.setPrefHeight(200);

        // Personnalisation de l'affichage dans la liste
        listView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Revetement item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    // Affichage clair : Désignation et Prix
                    setText(String.format("%s - %.2f €/m²", item.getDesignation(), item.getPrixUnitaire()));
                }
            }
        });

        // =====================================================================
        // SECTION 2 : FORMULAIRE D'AJOUT D'UN NOUVEAU MATÉRIAU
        // =====================================================================
        TitledPane formulairePane = new TitledPane();
        formulairePane.setText("Créer un nouveau matériau");
        formulairePane.setExpanded(false); // Replié par défaut pour ne pas surcharger l'UI

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        TextField txtDesignation = new TextField();
        txtDesignation.setPromptText("Ex: Peinture Acrylique");

        TextField txtPrix = new TextField();
        txtPrix.setPromptText("Ex: 15.50");

        CheckBox chkMur = new CheckBox("Pour Mur");
        CheckBox chkSol = new CheckBox("Pour Sol");
        CheckBox chkPlafond = new CheckBox("Pour Plafond");

        grid.add(new Label("Désignation :"), 0, 0);
        grid.add(txtDesignation, 1, 0);

        grid.add(new Label("Prix Unitaire (€) :"), 0, 1);
        grid.add(txtPrix, 1, 1);

        grid.add(chkMur, 0, 2);
        grid.add(chkSol, 1, 2);
        grid.add(chkPlafond, 0, 3);

        Button btnAjouter = new Button("Sauvegarder dans le catalogue");
        grid.add(btnAjouter, 1, 4);

        formulairePane.setContent(grid);

        // Action d'ajout
        btnAjouter.setOnAction(e -> {
            String designation = txtDesignation.getText().trim();
            String prixStr = txtPrix.getText().trim().replace(",", ".");

            if (designation.isEmpty() || prixStr.isEmpty()) {
                showAlert("Erreur", "Veuillez remplir le nom et le prix.");
                return;
            }
            if (!chkMur.isSelected() && !chkSol.isSelected() && !chkPlafond.isSelected()) {
                showAlert("Erreur", "Le matériau doit être applicable sur au moins une surface.");
                return;
            }

            try {
                double prix = Double.parseDouble(prixStr);
                // Génération d'un ID unique temporaire ou séquentiel
                //String nouvelId = String.valueOf((int)(Math.random() * 10000) + 100);
                String nouvelId = UUID.randomUUID().toString();

                Revetement nouveau = new Revetement(nouvelId, designation,
                        chkMur.isSelected(), chkSol.isSelected(), chkPlafond.isSelected(), prix);

                // Ajout et sauvegarde
                catalogue.ajouterEtSauvegarderRevetement(nouveau);

                // Sélectionne automatiquement le nouveau matériau créé
                listView.getSelectionModel().select(nouveau);

                // Réinitialise le formulaire
                txtDesignation.clear();
                txtPrix.clear();
                chkMur.setSelected(false);
                chkSol.setSelected(false);
                chkPlafond.setSelected(false);
                formulairePane.setExpanded(false);

            } catch (NumberFormatException ex) {
                showAlert("Erreur", "Le prix doit être un nombre valide (ex: 15.50).");
            }
        });

        root.getChildren().addAll(lblListe, listView, formulairePane);
        getDialogPane().setContent(root);

        // =====================================================================
        // RÉSULTAT DE LA BOÎTE DE DIALOGUE
        // =====================================================================
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