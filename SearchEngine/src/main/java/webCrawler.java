import com.mongodb.ConnectionString;
import com.mongodb.MongoException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import org.jsoup.Connection;
import com.mongodb.client.*;

import java.util.concurrent.ConcurrentLinkedQueue;

public class webCrawler implements Runnable {
    private static final int maxDepth = 5;
    private Thread th;
    private String seed;
    private Vector<String> oldLinks = new Vector<String>();
    private static HashMap<String, String> oldBodies = new HashMap<>();
    private int id;
    private static final String CRAWLER_DATABASE = "SearchEngine";
    private static final String CONNECTION_STRING = "mongodb://localhost:27017";
    private MongoCollection<Document> documentCollection;
    private FileWriter Writer;
    private  static final int MaxLinks = 7000;

    @Override
    public void run() {
        try {
            Crawl(1, seed);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    ConcurrentLinkedQueue<Document> GetParsedDocs() throws IOException {
        ConcurrentLinkedQueue<Document> parsed = new ConcurrentLinkedQueue<>();
        try (MongoClient mongoClient = MongoClients.create(CONNECTION_STRING)) {
            MongoDatabase database = mongoClient.getDatabase(CRAWLER_DATABASE);
            documentCollection = database.getCollection("documents", Document.class);

            FindIterable<Document> documents = documentCollection.find();

            for (Document document : documents) {
                parsed.add(document);
            }
        }
        return parsed;
    }

    public static MongoClient createConnection() {
        ConnectionString connectionString = new ConnectionString("mongodb://localhost:27017");
        try {
            MongoClient client = MongoClients.create(CONNECTION_STRING);
            return client;
        } catch (MongoException e) {
            System.err.println("Error creating connection: " + e.getMessage());
            return null;
        }
    }

    public static void insertURL(MongoClient client, String currentUrl , Set<String> set) {
        try {
            // Ensure connection is closed after use
            try (client) {
                MongoDatabase database = client.getDatabase(CRAWLER_DATABASE);
                MongoCollection<org.bson.Document> collection = database.getCollection("MYDOCUMENTS");
                // Create document to insert
                org.bson.Document CrawlerDoc = new org.bson.Document();
                CrawlerDoc.append("link", currentUrl).append("links",set);
                collection.insertOne(CrawlerDoc);
            }
        } catch (MongoException e) {
            System.err.println("Error inserting result: " + e.getMessage());
        }

    }

    public static Boolean chkBody(String link) throws IOException, NoSuchAlgorithmException {
        String sha = calculateSHA(link);
        if (oldBodies.containsKey(sha)) {
            return false;
        } else {
            oldBodies.put(sha, link);
            return true;
        }
    }

    public webCrawler(String link, int idd, FileWriter myWriter, Vector<String>linkss) {
        System.out.print("webCrawler was made");
        seed = link;
        id = idd;
        Writer = myWriter;
        oldLinks=linkss;
        th = new Thread(this);
        th.start();
    }

    public static boolean isCrawlAllowed(String websiteUrl) throws IOException {
        URL url = new URL(websiteUrl + "/robots.txt");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().startsWith("User-agent: *")) {
                    while ((line = reader.readLine()) != null) {
                        if (line.trim().startsWith("Disallow:")) {
                            String disallowPath = line.trim().substring("Disallow:".length()).trim();
                            if (disallowPath.equals("/")) {
                                return false; // Crawling not allowed for the entire site
                            } else if (disallowPath.length() > 0) {
                                if (websiteUrl.endsWith("/")) {
                                    websiteUrl = websiteUrl.substring(0, websiteUrl.length() - 1);
                                }
                                String disallowUrl = websiteUrl + disallowPath;
                                if (websiteUrl.contains(disallowUrl)) {
                                    return false; // Crawling not allowed for this specific path
                                }
                            }
                        }
                    }
                }
            }
            reader.close();
            return true; // No specific disallow rules for user-agent '*'
        } else {
            // If "robots.txt" doesn't exist or there was another issue accessing it, assume crawling is allowed
            return true;
        }
    }

    public static String calculateSHA(String websiteUrl) throws IOException, NoSuchAlgorithmException {
        URL url = new URL(websiteUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
            reader.close();

            // Calculate SHA-256 hash of the website's content
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(content.toString().getBytes());

            // Convert byte array to hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } else {
            throw new IOException("HTTP response code: " + responseCode);
        }
    }


    private Document req(String link) {
        try {
            if(oldLinks.size()>MaxLinks)
                return null;
            URL url = new URL(link); // Validate URL format
            Connection con = Jsoup.connect(url.toString());
            Document doc = con.get();
            if (con.response().statusCode() == 200 && isCrawlAllowed(String.valueOf(url)) && chkBody(String.valueOf(url))) {
                System.out.println("--> bot " + id + " has connected successfully to " + url);
                String title = doc.title();
                System.out.println(title);
                Writer.write(url + "\n");
                synchronized (oldLinks) {
                    oldLinks.add(url.toString());
                }
                return doc;
            }
        } catch (MalformedURLException e) {
            System.out.println("--> bot " + id + "* received an invalid URL: " + link);
        } catch (IOException | NoSuchAlgorithmException e) {
            System.out.println("--> bot " + id + "* can not connect to* " + link);
        }
        return null;
    }


    private void Crawl(int depth, String seed) throws FileNotFoundException {
        File file = new File("C:\\Users\\karee\\OneDrive\\Documents\\school\\APT\\WebCrawler_ver1.1\\src\\seed.txt");
        if (oldLinks.size()<MaxLinks && depth <= maxDepth) {
            System.out.println("--> bot " + id + " q = " +oldLinks.size()) ;
            System.out.println("*************") ;
            Document doc = req(seed);
            if (doc != null) {
                Set<String> ConLinks = new HashSet<>();
                for (Element link : doc.select("a[href]")) {
                    String new_link = link.absUrl("href");
                    ConLinks.add(new_link);
                    if (!oldLinks.contains(new_link)) {
                        Crawl(depth + 1, new_link);
                    }
                }
                for (String element : ConLinks) {
                    System.out.println(element);
                }
                MongoClient c = createConnection();
                insertURL(c,seed,ConLinks);
            }
        }
    }

    public Thread getTh() {
        return th;
    }
}
