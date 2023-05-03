package authentication;

import com.sun.org.apache.xpath.internal.operations.Bool;
import com.sun.xml.internal.ws.util.xml.CDATA;
import javafx.application.Platform;
import javafx.concurrent.Task;
import org.bson.Document;
import org.omg.CORBA.WCharSeqHelper;

import javax.print.Doc;
import javax.print.FlavorException;
import javax.sound.midi.SysexMessage;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ClientNetwork {
    // model
    public PrintWriter writer;
    public Object message;
    public ObjectInputStream input;
    public ClientController clientController;
    public CatalogController catalogController;
    public String checkoutMessage;
    public CheckoutController checkoutController;
    public ProfileController profileController;
    public ProfileItemController profileItemController;
    public NewUserController newUserController;
    private Thread t;
    private Socket socket;

    public void setUpNetworking(ClientController controller) throws IOException {
        this.clientController = controller;
        socket = new Socket("18.222.144.186", 4242);
        //18.222.144.186
        System.out.println("networking established");
        input = new ObjectInputStream(socket.getInputStream());
        writer = new PrintWriter(socket.getOutputStream());
        t = new Thread(new startReader());
        t.start();
        checkoutMessage = "";
    }

    public void stopNetworking() throws IOException {
        socket.close();
        t.stop();
    }

    class startReader implements Runnable {
        @Override
        public void run() {
            try {
                while((message = input.readObject()) != null) {
                    System.out.println(message);
                    parse(message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        public void parse(Object o) {
            if (o instanceof String) {
                String s = (String) o;
                String[] feedback = s.split(",");

                if (feedback[0].equals("AUTH")) {
                    if (feedback[1].equals("false")) {
                        Platform.runLater(() -> clientController.feedback.setText("Incorrect username or password"));
                    }
                    else {
                        // correct user/password, so move on to catalog page
                        Platform.runLater(() -> {
                            try {
                                clientController.switchCatalog();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }
                }
                else if (feedback[0].equals("HISTORY")) {
                    // incoming customer history

                }
                else if (feedback[0].equals("TIME")) {
                    // incoming last checked out time information
                    if (feedback[1].equals("true")) {
//                        checkoutMessage = "Item successfully checked out at: " + feedback[2];
                        Platform.runLater(() -> {
                            checkoutController.autoRefresh();
                            checkoutController.showSuccessfulCheckout(feedback[2]);
                        });

                    }
                    else {
                        Platform.runLater(() -> {
                            checkoutController.autoRefresh();
                            checkoutController.showUnsuccessfulCheckout();
                        });
                    }
                }
                else if (feedback[0].equals("RETURN")) {
                    Platform.runLater(() -> {
                        profileController.showCheckin();
                        profileController.onRefresh();
                    });
                }
                else if (feedback[0].equals("REVIEW")) {
                    Platform.runLater(() -> {
                        checkoutController.autoRefresh();
                    });
                }
                else if (feedback[0].equals("NEW")) {
                    if (feedback[1].equals("SUCCESS")) {
                        Platform.runLater(() -> {
                            newUserController.showSuccess();
                            newUserController.loginReturn();
                        });
                    }
                    else {
                        Platform.runLater(() -> {
                            newUserController.showFail();
                        });
                    }
                }
                else if (feedback[0].equals("HOLD")) {
                    if (feedback[1].equals("FAIL")) {
                        // failed hold
                        Platform.runLater(() -> {
                            checkoutController.unSuccessfulHold();
                        });
                    }
                    else {
                        Platform.runLater(() -> {
                            checkoutController.successfulHold(Integer.parseInt(feedback[2]));
                        });
                    }
                }
                else if (feedback[0].equals("VERIFICATION")) {
                    if (feedback[1].equals("FAIL")) {
                        Platform.runLater(() -> {
                            newUserController.error.setText("Wrong verification code, please try again.");
                        });
                    }
                }
                else if (feedback[0].equals("FORGOT")) {
                    if (feedback[1].equals("FAIL")) {
                        Platform.runLater(() -> {
                            clientController.feedback.setText("User does not exist.");
                        });
                    }
                    else {
                        Platform.runLater(() -> {
                            clientController.showVerify();
                        });
                    }
                }
                else if (feedback[0].equals("PWV")) {
                    if (feedback[1].equals("FAIL")) {
                        Platform.runLater(() -> {
                            clientController.feedback.setText("Wrong verification code.");
                        });
                    }
                    else {
                        Platform.runLater(() -> {
                            clientController.showReset();
                        });
                    }
                }
                else if (feedback[0].equals("PWRESET")) {
                    Platform.runLater(() -> {
                        clientController.doneReset();
                    });
                }
            }
            else if (o instanceof ArrayList<?>) {
                // return of catalog
                ArrayList<Document> d = (ArrayList<Document>) o;
//
                Platform.runLater(() -> {
                    for (Document doc : d) {
                        try {
                            catalogController.insertItem(doc);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }
            else if (o instanceof Document) {
                // sort types of documents being returned
                // either a profile or an item
                if (((Document) o).containsKey("username")) {
                    // is profile
                    Platform.runLater(() -> {
                        System.out.println("getting key");
                        try {
                            profileController.fillProfile((Document) o);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
                else {
                    // is item
                    Platform.runLater(() -> {
                        checkoutController.updateViewFromDoc((Document) o);
                    });
                }

            }

        }
    }

}
