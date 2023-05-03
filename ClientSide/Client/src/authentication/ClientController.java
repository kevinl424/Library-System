package authentication;

import com.mongodb.BasicDBObject;
import com.sun.java.accessibility.util.AccessibilityListenerList;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.bson.Document;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ClientController {
    private ClientNetwork clientNetwork;

    @FXML
    private VBox vbox;
    @FXML
    private PasswordField password;

    @FXML
    private TextField username;
    @FXML
    public Label feedback;

    @FXML
    private Label code;
    @FXML
    private TextField verifyField;
    @FXML
    private Button verifyButton;
    @FXML
    private Label newPassword;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button resetButton;
    @FXML
    private ImageView closeImage;
    private static boolean loginInd;

    public ClientController() throws IOException {
        if (!loginInd) {
            clientNetwork = new ClientNetwork();
            clientNetwork.setUpNetworking(this); // sets up network for this particular client
            loginInd = true;
        }
    }

    public void setVisible() {
        // hide forget password elements
        Image i = new Image("https://icones.pro/wp-content/uploads/2022/05/icone-fermer-et-x-rouge.png");
        this.closeImage.setImage(i);
        code.setVisible(false);
        verifyField.setVisible(false);
        verifyButton.setVisible(false);
        newPassword.setVisible(false);
        passwordField.setVisible(false);
        resetButton.setVisible(false);
    }

    @FXML
    void commitQuit(ActionEvent event) throws IOException {
        clientNetwork.stopNetworking();
        Platform.exit();
    }

    @FXML
    void resetPW(ActionEvent event) {
        feedback.setText("");
        if (passwordField.getText().equals("")) {
            feedback.setText("Please enter a new password");
            return;
        }
        // reset the password
        clientNetwork.writer.println("PWRESET," + username.getText() + "," + passwordField.getText());
        clientNetwork.writer.flush();

    }

    @FXML
    void verifyCode(ActionEvent event) {
        feedback.setText("");
        if (verifyField.getText().equals("")) {
            feedback.setText("Please enter a verification code.");
            return;
        }
        // check for valid verification code
        clientNetwork.writer.println("PWV," + username.getText() + "," + verifyField.getText());
        clientNetwork.writer.flush();
    }

    @FXML
    void forgotPassword(ActionEvent event) {
        // send verification email first
        if (username.getText().equals("")) {
            feedback.setText("Please enter a valid username above first.");
            return;
        }
        clientNetwork.clientController = this;
        clientNetwork.writer.println("FORGOT," + username.getText());
        clientNetwork.writer.flush();

    }

    void showVerify() {
        code.setVisible(true);
        verifyField.setVisible(true);
        verifyButton.setVisible(true);
    }

    void showReset() {
        code.setVisible(false);
        verifyField.setVisible(false);
        verifyButton.setVisible(false);
        newPassword.setVisible(true);
        passwordField.setVisible(true);
        resetButton.setVisible(true);
    }

    void doneReset() {
        newPassword.setVisible(false);
        passwordField.setVisible(false);
        resetButton.setVisible(false);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Successful Reset");
        alert.setHeaderText(null);
        alert.setContentText("Successfully reset password. Please login with new credentials.");
        alert.showAndWait();
    }


    @FXML
    void loginOnClick(ActionEvent event) {
        String userName = username.getText();
        String passWord = password.getText();
        String message = "AUTH," + userName + "," + passWord;

        clientNetwork.writer.println(message); // sends username and
        // password to server to verify
        clientNetwork.writer.flush();
    }

    void setVars(ClientNetwork clientNetwork) {
        this.clientNetwork = clientNetwork;
    }

    @FXML
    void newUserClick(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("newUser.fxml"));
        Parent root = loader.load();
        Stage thisStage = (Stage) username.getScene().getWindow();
        NewUserController c = loader.getController();

        Stage newUser = new Stage();
        c.setVars(clientNetwork, thisStage, newUser);
        thisStage.hide();

        newUser.setTitle("New User");
        newUser.setScene(new Scene(root));
        newUser.show();

    }

    @FXML
    public void switchCatalog() throws IOException {
        Stage thisStage = (Stage) feedback.getScene().getWindow();
        Stage catalog = new Stage();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("catalog.fxml"));
        Parent root = loader.load();
        CatalogController c = loader.getController();
        c.setVars(thisStage, catalog, clientNetwork, username.getText());



        thisStage.hide();
        catalog.setTitle("Catalog");
        catalog.setScene(new Scene(root));
        catalog.show();

        username.clear();
        password.clear();
//        thisStage.show();
        clientNetwork.writer.println("CATALOG");
        clientNetwork.writer.flush();
    }
}
