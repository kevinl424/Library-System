package authentication;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.bson.Document;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;

public class CheckoutController {
    @FXML
    private VBox vbox;

    @FXML
    private Label title;

    @FXML
    private Label author;

    @FXML
    private Label pages;

    @FXML
    private Button checkoutButton;

    @FXML
    private Hyperlink backLink;

    @FXML
    private ImageView img;
    @FXML
    private Button refresh;
    @FXML
    private ImageView refreshIcon;
    @FXML
    private VBox reviewVbox;
    @FXML
    private Button addReview;
    @FXML
    private TextField reviewField;
    @FXML
    private Hyperlink hold;
    private ClientNetwork clientNetwork;
    private Stage prev;
    private Stage checkout;
    private String currUser;
    private String email;

    @FXML
    void addHold(ActionEvent event) {
        clientNetwork.checkoutController = this;
        clientNetwork.writer.println("HOLD," + currUser + "," + title.getText());
        clientNetwork.writer.flush();
    }

    void unSuccessfulHold() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Unsuccessful Add to Hold");
        alert.setHeaderText(null);
        alert.setContentText(title.getText() + " was not successfully added to hold list. Check your current hold list or inventory for item.");
        alert.showAndWait();
    }

    void successfulHold(int count) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Successful Add to Hold");
        alert.setHeaderText(null);
        alert.setContentText(title.getText() + " was successfully added to hold list. Current number of people ahead of you on the hold list: " + count + ".");
        alert.showAndWait();
    }

    @FXML
    void addReview(ActionEvent event) {
        clientNetwork.checkoutController = this;
        if (reviewField.getText().equals("")) {
            // no review was left
            return;
        }
        // a real review was left
        clientNetwork.writer.println("REVIEW," + title.getText() + "," + currUser + "," + reviewField.getText());
        clientNetwork.writer.flush(); // add to database
    }
    @FXML
    void onRefresh(ActionEvent event) {
        clientNetwork.checkoutController = this; // set current target
        clientNetwork.writer.println("DOCUMENT," + title.getText());
        clientNetwork.writer.flush();
    }

    void autoRefresh() {
        clientNetwork.checkoutController = this; // set current target
        clientNetwork.writer.println("DOCUMENT," + title.getText());
        clientNetwork.writer.flush();
    }

    void updateViewFromDoc(Document d) {
        if (d.get("available").equals(false)) {
            // now checked out
            this.checkoutButton.setText("Not Available");
            this.checkoutButton.setDisable(true);
            hold.setVisible(true);
        }
        else if (d.get("available").equals(true)) {
            // now available
            this.checkoutButton.setText("Checkout");
            this.checkoutButton.setDisable(false);
            hold.setVisible(false);
        }

        reviewVbox.getChildren().clear();
        ArrayList<String> reviews = (ArrayList<String>) d.get("reviews");
        for (String review : reviews) {
            Label r = new Label(review);
            reviewVbox.getChildren().add(r);
        }
    }

    void checkoutInit(Document d, String title, String author, String pages, String imgURL, ClientNetwork c, Stage prev, Stage checkout, String currUser) {
        String type = (String) d.get("type");
        this.checkout = checkout;
        this.title.setText(title);
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

        // pages/runtime
        if (type.equals("book")) {
            this.author.setText("Pages: " + pages);
        }
        else if (type.equals("game")) {
            this.author.setText("");
        }
        else {
            this.author.setText("Runtime (mins): " + pages);
        }

        this.clientNetwork = c;
        this.prev = prev;
        this.currUser = currUser;
        this.refreshIcon.setImage(new Image("https://upload.wikimedia.org/wikipedia/commons/thumb/c/ce/Ic_refresh_48px.svg/1200px-Ic_refresh_48px.svg.png"));

        if (d.get("available").equals(false)) {
            this.checkoutButton.setText("Not Available");
            this.checkoutButton.setDisable(true);
        }
        else {
            // hide the hold option if users can check out
            hold.setVisible(false);
        }

        ArrayList<String> pastReviews = (ArrayList<String>) d.get("reviews");
        for (String review : pastReviews) { // adds reviews on start up
            Label r = new Label(review);
            reviewVbox.getChildren().add(r);
        }

        img.setImage(new Image(imgURL));
    }

    @FXML
    void commitCheckout(ActionEvent event) {
        clientNetwork.checkoutController = this;
        clientNetwork.writer.println("CHECK," + title.getText() + "," + currUser);
        clientNetwork.writer.flush();

    }

    void showSuccessfulCheckout(String time) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Successful Checkout");
        alert.setHeaderText(null);
        alert.setContentText(title.getText() + " was successfully checked out by " + currUser + " at " + time);
        alert.showAndWait();
    }

    void showUnsuccessfulCheckout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Unsuccessful Checkout");
        alert.setHeaderText(null);
        alert.setContentText(title.getText() + "was unsuccessfully checked out. " +
                "Please refresh the page for the latest availability information.");
        alert.showAndWait();
    }

    @FXML
    void returnCatalog(ActionEvent event) {
        prev.show();
        checkout.close();
    }

}
