package authentication;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import javax.swing.*;
import javax.xml.soap.Text;
import java.util.regex.Pattern;

public class NewUserController {

    @FXML
    private TextField username;

    @FXML
    private TextField password;

    @FXML
    private TextField rePassword;

    @FXML
    private Button create;

    @FXML
    public Label error;

    @FXML
    private Hyperlink returner;

    @FXML
    private TextField email;

    @FXML
    private TextField verifyField;

    @FXML
    private Button verifyButton;

    private Stage prev;

    private Stage curr;

    private ClientNetwork clientNetwork;

    @FXML
    void verify(ActionEvent event) {
        clientNetwork.newUserController = this;
        // check that something was entered
        if (verifyField.getText().equals("")) {
            error.setText("Please enter a verification code.");
            return;
        }

        clientNetwork.writer.println("NEW," + username.getText() + "," + password.getText() + "," + verifyField.getText() + "," + email.getText());
        clientNetwork.writer.flush();
    }

    @FXML
    void createAccount(ActionEvent event) {
        clientNetwork.newUserController = this;

        if (username.getText().equals("") || password.getText().equals("") || rePassword.getText().equals("") || email.getText().equals("")) {
            // user must fill out all fields to create an account
            error.setText("Please fill out all fields.");
            return;
        }

        if (!password.getText().equals(rePassword.getText())) {
            // both passwords must be the same
            error.setText("Passwords do not match.");
            return;
        }

        // check for valid email
        if (!isValid(email.getText())) {
            error.setText("Please enter a valid email format.");
            return;
        }
        clientNetwork.writer.println("VERIFY," + email.getText());
        clientNetwork.writer.flush();

        this.verifyButton.setVisible(true);
        this.verifyField.setVisible(true);
    }

    @FXML
    public void returnOnClick(ActionEvent event) {
        prev.show();
        curr.close();
    }

    void showSuccess() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Successful Account Creation");
        alert.setHeaderText(null);
        alert.setContentText("User account successfully created! Please login using new credentials.");
        alert.showAndWait();
    }

    void showFail() {
        error.setText("Username or email already exists, please use a new one.");
    }

    void loginReturn() {
        prev.show();
        curr.close();
    }

    public void setVars(ClientNetwork c, Stage prev, Stage curr) {
        this.prev = prev;
        this.curr = curr;
        clientNetwork = c;

        // pre verification
        verifyField.setVisible(false);
        verifyButton.setVisible(false);

    }

    private boolean isValid(String email) {
        String test = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$";
        Pattern pattern = Pattern.compile(test);
        return pattern.matcher(email).matches();
    }

}
