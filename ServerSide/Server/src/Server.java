import com.mongodb.client.MongoCollection;
import org.bson.Document;

import javax.xml.crypto.Data;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import static com.mongodb.client.model.Filters.eq;

public class Server {
    private Database database;

    private HashMap<String, String> verifyMap;

    public Server() {
        database = new Database();
        verifyMap = new HashMap<>();
    }
    private void setUpNetworking() throws Exception {
        ServerSocket serverSocket = new ServerSocket(4242);
        while (true) {
            Socket clientSocket = serverSocket.accept();
            Thread t = new Thread(new ClientHandler(database, clientSocket, new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))));
            t.start();
            System.out.println("got a connection");
        }
    }

    class ClientHandler implements Runnable {
        private final Database database;
        private final BufferedReader reader;
        private final PrintWriter writer;
        private final ObjectOutputStream output;
        public ClientHandler(Database d, Socket s, BufferedReader r) throws IOException {
            reader = r;
            database = d;
            writer = new PrintWriter(s.getOutputStream());
            output = new ObjectOutputStream(s.getOutputStream());
        }
        @Override
        public void run() {
            // to handle each client that has connected
            String message;
            while (true) {
                try {
                    if (((message = reader.readLine()) != null)) {
                        parse(message);
                    }
                    else {
                        break;
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }
        }

        public void parse(String s) throws IOException {
            String[] args = s.split(",");
            // split into specific requests coming in from client side

            String request = args[0];
            if (request.equals("AUTH")) {
                boolean ret = database.checkAuth(args[1], args[2]);
                output.writeObject("AUTH" + "," + ret);
            }

            else if (request.equals("CATALOG")) {
                ArrayList<Document> catalog = database.getCatalog();
                output.writeObject(catalog); // writes to client side to display in catalog
            }

            else if (request.equals("CHECK")) {
                // client trying to check out a book
                String currUser = args[2];
                String title = args[1];

                if (database.Checkout(currUser, title)) {
                    // successfully checked out under current user
                    String history = database.getBorrowHistory(title);
                    String time = database.getLastChecked(title);

                    output.writeObject("HISTORY," + history);
                    output.writeObject("TIME," +"true," + time);
                }
                else {
                    output.writeObject("TIME," + "false," + database.getLastChecked(title));
                }
                System.out.println("sent");
            }

            else if (request.equals("DOCUMENT")) {
                output.writeObject(database.getDoc(args[1])); // gives back updated document form database
            }

            else if (request.equals("PROFILE")) {
                output.writeObject(database.getProfile(args[1])); // gives back updated profile information
            }

            else if (request.equals("RETURN")) {
                // implement hold stuff here as well
                String title = args[1];
                // do actual return
                database.returnItem(title);
                output.writeObject("RETURN," + title);
            }

            else if (request.equals("REVIEW")) {
                String title = args[1];
                String user = args[2];
                String review = args[3];
                database.addReview(title, user, review);
                output.writeObject("REVIEW");
            }

            else if (request.equals("NEW")) {
                String username = args[1];
                String password = args[2];
                String verify = args[3];
                String email = args[4];

                if (!verifyMap.get(email).equals(verify)) {
                    // not correct verification
                    output.writeObject("VERIFICATION,FAIL");
                }
                else if (database.createNewAccount(email, username, password)) {
                    // successfully created new account
                    output.writeObject("NEW,SUCCESS");
                }
                else {
                    // error account already exists
                    output.writeObject("NEW,FAIL");
                }
            }

            else if (request.equals("HOLD")) {
                if (!database.addHold(args[1], args[2])) {
                    // cannot add to hold list since user already has book or on hold list already
                    output.writeObject("HOLD,FAIL");
                }
                else {
                    // success hold
                    int count = database.getHoldCount(args[2], args[1]);
                    output.writeObject("HOLD,SUCCESS," + count);
                }

            }

            else if (request.equals("VERIFY")) {
                String email = args[1];

                // come up with verification code
                Random rnd = new Random();
                int number = rnd.nextInt(999999);
                String vCode = String.format("%06d", number);

                Emails.send(email, "Your eLibrary Verification Code", "Your verification code is: " + vCode);
                // add to server hashmap
                verifyMap.put(email, vCode);
            }

            else if (request.equals("FORGOT")) {
                String user = args[1];
                if (!database.forgetPW(user)) {
                    output.writeObject("FORGOT,FAIL");
                }
                else {
                    output.writeObject("FORGOT,SUCCESS");
                }
            }

            else if (request.equals("PWV")) {
                String user = args[1];
                String code = args[2];

                if (!database.verify(code, user)) {
                    // not correct
                    output.writeObject("PWV,FAIL");
                }
                else {
                    output.writeObject("PWV,SUCCESS");
                }
            }
            else if (request.equals("PWRESET")) {
                String user = args[1];
                String pw = args[2];
                database.resetPW(user, pw);
                output.writeObject("PWRESET");
            }

        }

    }

    public static void main(String[] args) throws Exception {
        // set up the database connection
        // set up networking for server
        Server s = new Server();
        s.setUpNetworking();
    }

}
