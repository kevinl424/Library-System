package authentication;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.bson.Document;

import javax.xml.soap.Text;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;

public class CatalogController {

    @FXML
    private VBox vbox;

    @FXML
    private Button refresh;

    @FXML
    private ImageView refreshIcon;

    @FXML
    private TextField searchBar;

    public ClientNetwork clientNetwork;
    private String currUser;
    private ArrayList<ItemController> items;
    private ArrayList<Parent> views;
    private Stage curr;
    private Stage prev;


    @FXML
    void logout(ActionEvent event) throws IOException {
        prev.show();
        curr.close();
    }

    @FXML
    void onRefresh(ActionEvent event) {
        //delete all elements of the vbox first
        vbox.getChildren().clear();
        items.clear();

        // update all the documents making up the catalog
        clientNetwork.writer.println("CATALOG");
        clientNetwork.writer.flush();
    }


    @FXML
    void createProfileView(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("profile.fxml"));

        Parent root = loader.load();
        ProfileController c = loader.getController();

        Stage thisStage = (Stage) vbox.getScene().getWindow();

        // create new stage for profile
        Stage profile = new Stage();
        c.setVars(profile, thisStage, clientNetwork, currUser); // sets the name of the profile view
        thisStage.hide(); // hide stage while user uses checkout screen

        // show stage of checkout screen from here
        profile.setTitle("Profile");
        profile.setScene(new Scene(root));
        profile.show();
    }


    public void setVars(Stage prev, Stage curr, ClientNetwork clientNetwork, String username) {
        this.prev = prev;
        this.curr = curr;

        items = new ArrayList<>();
        views = new ArrayList<>();
        // set refresh image
        Image img = new Image("https://upload.wikimedia.org/wikipedia/commons/thumb/c/ce/Ic_refresh_48px.svg/1200px-Ic_refresh_48px.svg.png");
        this.refreshIcon.setImage(img);

        this.clientNetwork = clientNetwork;
        currUser = username;
        clientNetwork.catalogController = this; // one instance of catalogController
    }

    public void insertItem(Document d) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("item.fxml"));

        Parent root = loader.load();

        ItemController c = loader.getController();
        // add to the items array for searching
        items.add(c);
        views.add(root);

        c.setVars(d, (String) d.get("title"), (String) d.get("author"), (String) d.get("summary"), (Integer) d.get("pages"), (String) d.get("img"), clientNetwork, currUser);
        vbox.getChildren().add(root);
    }

    public void searchFor(javafx.scene.input.KeyEvent keyEvent) {
        String s = searchBar.getText();
        s = s.toLowerCase();
        vbox.getChildren().clear();

        for (int i = 0; i < items.size(); i++) {
            String title = items.get(i).title.getText().toLowerCase();
            if (title.contains(s)) {
                vbox.getChildren().add(views.get(i));
            }
        }
    }
}
