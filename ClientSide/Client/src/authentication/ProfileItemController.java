package authentication;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ProfileItemController {

    @FXML
    private Label title;

    @FXML
    private ImageView image;

    @FXML
    private Button returns;

    private ClientNetwork clientNetwork;

    @FXML
    void returnBook(ActionEvent event) {
        clientNetwork.profileItemController = this;
        clientNetwork.writer.println("RETURN," + title.getText());
        clientNetwork.writer.flush();
    }


    void setVars(String title, String img, ClientNetwork clientNetwork) {
        this.clientNetwork = clientNetwork;
        this.title.setText(title);
        image.setImage(new Image(img));
    }

}
