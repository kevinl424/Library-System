package authentication;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.bson.Document;

import javax.print.Doc;
import java.io.IOException;
import java.lang.annotation.Documented;

public class ItemController {
    @FXML
    private AnchorPane anchor;
    @FXML
    public Hyperlink title;

    @FXML
    private Label author;

    @FXML
    private Label summary;

    @FXML
    private Label pages;

    @FXML
    private ImageView image;

    private ClientNetwork clientNetwork;
    private String imgURL;
    private String currUser;
    private Document d;

    @FXML
    void openItem(ActionEvent event) throws IOException {
        // changes the view after user clicks on an item to see status
        FXMLLoader loader = new FXMLLoader(getClass().getResource("checkout.fxml"));

        Parent root = loader.load();
        CheckoutController c = loader.getController();
        Stage thisStage = (Stage) pages.getScene().getWindow();

        // create new stage for checkout page
        Stage checkout = new Stage();
        c.checkoutInit(d, title.getText(), author.getText(), pages.getText(), imgURL, clientNetwork, thisStage, checkout, currUser); // initialize specific vars
        thisStage.hide(); // hide stage while user uses checkout screen

        // show stage of checkout screen from here
        checkout.setTitle("Checkout");
        checkout.setScene(new Scene(root));
        checkout.show();

    }


    void setVars(Document d, String title, String author, String summary, Integer pages, String imgURL, ClientNetwork c, String currUser) {
        this.title.setText(title);
        String type = (String) d.get("type");
        // author/creator
        if (type.equals("book") || type.equals("a-book")) {
            this.author.setText("By: " + author);
        }
        else if (type.equals("movie")) {
            this.author.setText("Directed By: " + author);
        }
        else {
            this.author.setText(author);
        }
        this.summary.setText(summary);
        // pages/runtime
        if (type.equals("book")) {
            this.author.setText("Pages: " + pages.toString());
        }
        else if (type.equals("game")) {
            this.author.setText("");
        }
        else {
            this.author.setText("Runtime (mins): " + pages.toString());
        }
        this.imgURL = imgURL;
        this.clientNetwork = c;
        this.currUser = currUser;
        this.d = d;
        image.setImage(new Image(imgURL));
    }

}
