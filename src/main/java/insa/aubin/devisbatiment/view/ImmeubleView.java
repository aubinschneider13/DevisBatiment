package insa.aubin.devisbatiment.view;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class ImmeubleView extends BorderPane {
    public ImmeubleView(){
        TabPane tabPaneImmeuble = new TabPane();
        Tab tabImmeuble = new Tab("Construction");
        tabImmeuble.setClosable(false);

        HBox hBox = new HBox();
        hBox.setSpacing(10);
        hBox.setPadding(new Insets(10));

        Button premierButton = new Button("Premier");
        premierButton.setStyle("-fx-cursor: hand;" +
                "-fx-font-family: 'Arial';" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #34495e;");
        premierButton.setPrefSize(60, 60);

        hBox.getChildren().add(premierButton);





    }


}
