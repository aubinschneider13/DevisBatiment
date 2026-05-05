package insa.aubin.devisbatiment.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.canvas.Canvas;
import javafx.stage.Stage;

public class ImmeubleView extends BorderPane {

    private Button btnAjouterNiveau;
    private TreeView<String> treeView;
    private TreeItem<String> rootItem;
    private DessinCanvas canvas; // On réutilise ton canvas existant

    public ImmeubleView(Stage stage) {
        // --- 1. BARRE D'OUTILS (TOP) ---
        TabPane tabPaneImmeuble = new TabPane();
        Tab tabConstruction = new Tab("Construction");
        tabConstruction.setClosable(false);

        HBox toolBar = new HBox();
        toolBar.setSpacing(10);
        toolBar.setPadding(new Insets(10));
        toolBar.setAlignment(Pos.CENTER_LEFT);

        btnAjouterNiveau = new Button("Ajouter\nNiveau");
        btnAjouterNiveau.setStyle("-fx-cursor: hand; -fx-font-weight: bold; -fx-text-alignment: center;");
        btnAjouterNiveau.setPrefSize(80, 60);

        toolBar.getChildren().add(btnAjouterNiveau);
        tabConstruction.setContent(toolBar);
        tabPaneImmeuble.getTabs().add(tabConstruction);

        this.setTop(tabPaneImmeuble);

        // --- 2. ZONE CENTRALE (SPLITPANE) ---
        SplitPane splitPane = new SplitPane();

        // Gauche : Navigateur
        VBox navigateurBox = new VBox();
        navigateurBox.setMinWidth(200);
        Label lblNav = new Label(" Navigateur de l'immeuble");
        lblNav.setStyle("-fx-background-color: #ecf0f1; -fx-font-weight: bold;");
        lblNav.setMaxWidth(Double.MAX_VALUE);

        rootItem = new TreeItem<>("Immeuble");
        rootItem.setExpanded(true);
        treeView = new TreeView<>(rootItem);

        VBox.setVgrow(treeView, Priority.ALWAYS);
        navigateurBox.getChildren().addAll(lblNav, treeView);

        // Droite : Canvas
        StackPane zoneDessin = new StackPane();
        zoneDessin.setStyle("-fx-background-color: white;");
        canvas = new DessinCanvas(); // Ton composant de dessin

        // On lie la taille du canvas à celle du conteneur
        canvas.widthProperty().bind(zoneDessin.widthProperty());
        canvas.heightProperty().bind(zoneDessin.heightProperty());

        zoneDessin.getChildren().add(canvas);

        splitPane.getItems().addAll(navigateurBox, zoneDessin);
        splitPane.setDividerPositions(0.2); // 20% pour le navigateur

        this.setCenter(splitPane);
    }

    // Getters pour le contrôleur
    public Button getBtnAjouterNiveau() { return btnAjouterNiveau; }
    public TreeView<String> getTreeView() { return treeView; }
    public TreeItem<String> getRootItem() { return rootItem; }
    public DessinCanvas getCanvas() { return canvas; }
}