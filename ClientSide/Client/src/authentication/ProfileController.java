package authentication;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.bson.Document;

import javax.print.Doc;
import javax.sound.midi.SysexMessage;
import java.io.IOException;
import java.util.ArrayList;

public class ProfileController {

    @FXML
    private Label name;

    @FXML
    private VBox checkoutVBox;

    @FXML
    private VBox reviewVBox;
    private ClientNetwork clientNetwork;

    @FXML
    private Hyperlink returns;

    private Stage prev;

    private Stage profile;
    private String username;
    private boolean firstFill;

    @FXML
    void returnToCat(ActionEvent event) {
        prev.show();
        profile.close();
    }


    public void onRefresh() {
        checkoutVBox.getChildren().clear();
        clientNetwork.writer.println("PROFILE," + username);
        clientNetwork.writer.flush();
    }

    public void showCheckin() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Successful CheckIn");
        alert.setHeaderText(null);
        alert.setContentText("You have successfully checked the item in.");
        alert.showAndWait();
    }

    public void setVars(Stage profile, Stage prev, ClientNetwork clientNetwork, String userName) {
        name.setText("Welcome " + userName + "!");
        this.clientNetwork = clientNetwork;
        clientNetwork.profileController = this;
        this.username = userName;

        // keep all stages
        this.prev = prev;
        this.profile = profile;

        // set up all the reviews and books that users have checked out/left
        this.clientNetwork.writer.println("PROFILE," + userName);
        this.clientNetwork.writer.flush();
    }

    public void fillProfile(Document profile) throws IOException {
        ArrayList<String> checked = (ArrayList<String>) profile.get("checked");
        // fill the checkout box

        System.out.println("Now filling the profiles");
        System.out.println(checked);

        for (String item : checked) {
            String[] chars = item.split(",");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("profileItem.fxml"));

            Parent root = loader.load();
            ProfileItemController c = loader.getController();
            c.setVars(chars[0], chars[1], clientNetwork);
            checkoutVBox.getChildren().add(root);
        }

        if (!firstFill) {
            fillReviews(profile);
            firstFill = true;
        }



    }

    public void fillReviews(Document profile) throws IOException {
        //fill the review box
        ArrayList<String> reviews = (ArrayList<String>) profile.get("reviews");
        for (String review : reviews) {
            // fill each review box
            Label r = new Label(review);
            r.setWrapText(true);
            reviewVBox.getChildren().add(r);
        }
    }

}
