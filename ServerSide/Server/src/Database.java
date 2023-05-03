import static com.mongodb.MongoClientSettings.getDefaultCodecRegistry;
import static com.mongodb.client.model.Filters.eq;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.sun.org.apache.xpath.internal.operations.Mod;
import org.bson.Document;

import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import javax.annotation.processing.Filer;
import javax.print.Doc;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;


public class Database {
    public MongoDatabase d;
    public HashMap<String, String> verifyMap;

    public Database() {
        String uri = "mongodb+srv://kevinli20742:Penguinboy1003@cluster0.9yteara.mongodb.net/?retryWrites=true&w=majority";
        MongoClient mongoClient = MongoClients.create(uri);
        d = mongoClient.getDatabase("eLibrary");
        verifyMap = new HashMap<>();
    }

    public String passwordHash(String password) {
        Random rd = new Random();
        byte[] salt = new byte[16];
        rd.nextBytes(salt);
        byte[] encrypt = (BCrypt.withDefaults().hash(10, salt, password.getBytes(StandardCharsets.UTF_8)));
        return new String(encrypt);
    }

    public boolean checkPassword(String password, String hashed) {
        BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), hashed.toCharArray());
        if (result.verified) {
            return true;
        }
        return false;
    }

    public synchronized boolean checkAuth(String user, String pass) { // only one access to database at a time
        MongoCollection<Document> collection = d.getCollection("Users"); // get specific collection to load
        // checks username first
        Document d = collection.find(eq("username", user)).first();
        if (d == null) {
            return false; // no match for username
        } else {
            return checkPassword(pass, (String) d.get("password")); // password for username must match
        }
    }

    public synchronized ArrayList<Document> getCatalog() {
        ArrayList<Document> ret = new ArrayList<>();
        MongoCollection<Document> collection = d.getCollection("items");

        FindIterable<Document> iterable = collection.find();

        for (Document doc : iterable) {
            ret.add(doc);
        }

        System.out.println(ret);
        return ret;
    }


    public synchronized boolean Checkout(String username, String title) throws IOException {
        MongoCollection<Document> collection = d.getCollection("items");
        Document d = collection.find(eq("title", title)).first();
        if (d == null) {
            return false;
        }

        // check the status of the book
        if (d.get("available").equals(true)) {
            // can check out, proceed to do
            // set the available status to false now
            collection.updateOne(Filters.eq("title", title), Updates.set("available", false));
            // update the last checked field
            // put date and time in the correct format
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime curr = LocalDateTime.now();
            String formatted = curr.format(dateTimeFormatter);

            collection.updateOne(Filters.eq("title", title), Updates.set("last_checked", formatted));
            ArrayList<String> borrowList = (ArrayList<String>) d.get("current");
            borrowList.add(username);
            collection.updateOne(Filters.eq("title", title), Updates.set("current", borrowList)); // add back to database

            // update information on the user side
            MongoCollection<Document> collection1 = this.d.getCollection("Users");
            Document document = collection1.find(eq("username", username)).first();
            ArrayList<String> personalBorrowed = (ArrayList<String>) document.get("checked");
            personalBorrowed.add(title + "," + d.get("img")); // add to borrowed list of this particular user
            collection1.updateOne(Filters.eq("username", username), Updates.set("checked", personalBorrowed)); // add back to database

            // now send emails
            String email = (String) document.get("email");
            Emails.send(email, "Checkout Receipt", "Thanks for checking out " + title + " on " + formatted + ".");

            return true;
        }
        return false;
    }

    public synchronized String getBorrowHistory(String title) {
        MongoCollection<Document> collection = d.getCollection("items");
        Document doc = collection.find(eq("title", title)).first();

        ArrayList<String> history = (ArrayList<String>) doc.get("current");
        if (history.size() == 0) {
            return "";
        }
        String ret = history.get(0);
        for (int i = 1; i < history.size(); i ++) {
            ret += ",";
            ret += history.get(i);
        }
        return ret;
    }

    public synchronized String getLastChecked(String title) {
        MongoCollection<Document> collection = d.getCollection("items");
        Document doc = collection.find(eq("title", title)).first();

        return (String) doc.get("last_checked");
    }

    public synchronized Document getDoc(String title) {
        MongoCollection<Document> collection = d.getCollection("items");
        Document doc = collection.find(eq("title", title)).first();

        return doc;
    }

    public synchronized Document getProfile(String name) {
        MongoCollection<Document> collection = d.getCollection("Users");
        Document doc = collection.find(eq("username", name)).first();

        return doc;

    }

    public synchronized void returnItem(String title) throws IOException {
        MongoCollection<Document> collection = d.getCollection("items");
        Document doc = collection.find(eq("title", title)).first();

        // get current user that has checked out
        ArrayList<String> pastBorrowers = (ArrayList<String>) doc.get("current");
        String curr = pastBorrowers.get(pastBorrowers.size() - 1); // the current person who has it checked out
        collection.updateOne(Filters.eq("title", title), Updates.set("available", true));

        MongoCollection<Document> collection1 = d.getCollection("Users");
        Document doc1 = collection1.find(eq("username", curr)).first();

        // now remove from the user collection side
        ArrayList<String> currBorrowed = (ArrayList<String>) doc1.get("checked");
        // finding the one to remove
        for (int i = 0; i < currBorrowed.size(); i++) {
            String[] args = currBorrowed.get(i).split(",");
            if (args[0].equals(title)) {
                currBorrowed.remove(i); // remove the selected one
                break;
            }
        }
        // insert the list back into the database
        collection1.updateOne(Filters.eq("username", curr), Updates.set("checked", currBorrowed));

        // send receipt for checkin to email
        String email = (String) doc1.get("email");
        Emails.send(email, "Checkout Receipt", "You have successfully returned " + title + ". Hope to see you soon!");

        // check for holds
        ArrayList<String> holds = (ArrayList<String>) doc.get("holds");
        if (holds.size() > 0) {
            String nextUser = holds.get(0);
            // is the latest person on the hold list
            holds.remove(nextUser);
            collection.updateOne(Filters.eq("title", title), Updates.set("holds", holds));

            // now to update the user side hold list and check out the book for that person
            doc1 = collection1.find(eq("username", nextUser)).first();
            holds = (ArrayList<String>) doc1.get("holds");
            holds.remove(title); // remove book from the user side hold list
            collection1.updateOne(Filters.eq("username", nextUser), Updates.set("holds", holds)); // update to database

            // now check the book out for the user
            Checkout(nextUser, title);

        }

    }

    public synchronized void addReview(String title, String user, String review) {
        // add to user side first
        MongoCollection<Document> collection = d.getCollection("Users");
        Document doc = collection.find(eq("username", user)).first();

        ArrayList<String> pastReviews = (ArrayList<String>) doc.get("reviews");
        pastReviews.add("@" + title + ": " + review);
        collection.updateOne(Filters.eq("username", user), Updates.set("reviews", pastReviews));

        // now add to item side
        collection = d.getCollection("items");
        doc = collection.find(eq("title", title)).first();

        pastReviews = (ArrayList<String>) doc.get("reviews");
        pastReviews.add("@" + user + ": " + review);
        collection.updateOne(Filters.eq("title", title), Updates.set("reviews", pastReviews)); // added to database
    }

    public synchronized boolean createNewAccount(String email, String username, String password) {
        MongoCollection<Document> collection = d.getCollection("Users");
        Document doc = collection.find(eq("username", username)).first();

        if (doc != null) {
            // user already exists, choose another username
            return false;
        }

        // check for same username
        doc = collection.find(eq("email", email)).first();
        if (doc != null) {
            return false;
        }

        // create a new user account in the database
        Document newDoc = new Document();
        newDoc.append("username", username);
        String hashed = passwordHash(password);
        newDoc.append("password", hashed);
        newDoc.append("checked", new ArrayList<String>());
        newDoc.append("reviews", new ArrayList<String>());
        newDoc.append("holds", new ArrayList<String>());
        newDoc.append("email", email);
        // insert into the database
        d.getCollection("Users").insertOne(newDoc);
        return true;
    }

    public synchronized boolean addHold(String username, String title) {
        MongoCollection<Document> collection = d.getCollection("Users");
        Document doc = collection.find(eq("username", username)).first();

        // check first if user already borrowed the book, then can't add to hold list
        ArrayList<String> checked = (ArrayList<String>) doc.get("checked");
        for (String item : checked) {
            String[] parts = item.split(",");
            if (parts[0].equals(title)) {
                // user already has the book
                return false;
            }
        }

        // check if user is already on this book's hold list
        ArrayList<String> holds = (ArrayList<String>) doc.get("holds");
        for (String hold : holds) {
            if (hold.equals(title)) {
                // already on their hold list
                return false;
            }
        }

        // now can add holds to user side and items side
        MongoCollection<Document> collection1 = d.getCollection("items");
        Document doc1 = collection1.find(eq("title", title)).first();
        ArrayList<String> itemHolds = (ArrayList<String>) doc1.get("holds");

        holds.add(title);
        itemHolds.add(username);
        collection.updateOne(Filters.eq("username", username), Updates.set("holds", holds));
        collection1.updateOne(Filters.eq("title", title), Updates.set("holds", itemHolds));

        return true;

    }

    public synchronized int getHoldCount(String title, String username) {
        MongoCollection<Document> collection1 = d.getCollection("items");
        Document doc1 = collection1.find(eq("title", title)).first();
        ArrayList<String> itemHolds = (ArrayList<String>) doc1.get("holds");

        int count = -1;
        for (int i = 0; i < itemHolds.size(); i++) {
            if (itemHolds.get(i).equals(username)) {
                count = i;
                break;
            }
        }
        return count;
    }

    public synchronized boolean forgetPW(String username) throws IOException {
        MongoCollection<Document> collection1 = d.getCollection("Users");
        Document doc1 = collection1.find(eq("username", username)).first();

        if (doc1 == null) {
            // account does not exist
            return false;
        }

        Random rnd = new Random();
        int number = rnd.nextInt(999999);
        String vCode = String.format("%06d", number);

        String email = (String) doc1.get("email");
        Emails.send(email, "Verification Code", "Your verification code is " + vCode + ".");
        verifyMap.put(email, vCode);
        return true;
    }

    public synchronized boolean verify(String code, String user) {
        MongoCollection<Document> collection1 = d.getCollection("Users");
        Document doc1 = collection1.find(eq("username", user)).first();
        String email = (String) doc1.get("email");

        return verifyMap.get(email).equals(code);
    }

    public synchronized void resetPW(String user, String pw) {
        MongoCollection<Document> collection1 = d.getCollection("Users");
        Document doc1 = collection1.find(eq("username", user)).first();
        String hashed = passwordHash(pw);
        collection1.updateOne(Filters.eq("username", user), Updates.set("password", hashed));
    }




//    public static void main(String[] args) {
////        Database d = new Database();
//////        d.getCatalog();
////        String name = "kevin";
////        MongoCollection<Document> collection = d.d.getCollection("Users");
////        Document doc = collection.find(eq("username", name)).first();
//////        collection.updateOne(Filters.eq("title", "Harry Potter"), Updates.set("available", true));
////
////        System.out.println(doc);
//
//        Random rnd = new Random();
//        int number = rnd.nextInt(999999);
//
//        String vCode = String.format("%06d", number);
//    }

}