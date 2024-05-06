import ca.rmen.porterstemmer.PorterStemmer;
import com.mongodb.client.*;
import com.mongodb.client.model.Aggregates;
import org.bson.Document;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bson.Document;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
public class Ranker {
    public static String stemWord(String word) {
        if (Stemmer.stopWords.contains(word.toLowerCase())) { // Check if the word is a stop word
            return "-1"; // Indicates a stop word
        }

        PorterStemmer porterStemmer = new PorterStemmer(); // Create a new PorterStemmer instance
        return porterStemmer.stemWord(word); // Return the stemmed word
    }
    private static String removePunctuation(String text) {
        return text.replaceAll("[\\p{Punct}]", ""); // Removes punctuation
    }
    public static String findParagraphWithWord(String filePath, String targetWord) {
        StringBuilder paragraph = new StringBuilder();
        boolean found = false;

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String cleanLine = removeHtmlTags(line);
                String[] words = cleanLine.split("\\s+");
                System.out.println(words+"\n"+targetWord);
                if (words.length > 10) {
                    for (int i = 0; i < words.length; i++) {
                        if (removePunctuation(stemWord(words[i])).equals(targetWord)) {
                            System.out.println("found "+words[i]);
                            words[i]="<b>"+words[i]+"</b>";
                            found = true;
                            int start = Math.max(0, i - 20);
                            int end = Math.min(words.length, i + 20);

                            for (int j = start; j < end; j++) {
                                paragraph.append(words[j]).append(" ");
                            }
                            return paragraph.toString().trim();
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String removeHtmlTags(String line) {
        // Regular expression to match HTML tags
        String htmlTagPattern = "<[^>]*>";

        // Remove HTML tags using regular expression
        Pattern pattern = Pattern.compile(htmlTagPattern);
        Matcher matcher = pattern.matcher(line);
        return matcher.replaceAll("");
    }
    public static void Run(String query) {

        HashMap<String,Pair<Double,String>> Results=new HashMap<String,Pair<Double,String>>();
        String connectionString = "mongodb://localhost:27017"; // Change this according to your MongoDB server

        try {
            // Connect to MongoDB
            MongoClient mongoClient = MongoClients.create(connectionString);

            // Access the database
            MongoDatabase db = mongoClient.getDatabase("SearchEngine"); // Change "mydatabase" to your database name
            System.out.println("Connected to the database successfully!");

            MongoCollection<Document> resultcollection = db.getCollection("Results");
            MongoCollection<Document> wcollection = db.getCollection("MYCOLLECTION");
            MongoCollection<Document> docscollection = db.getCollection("MYDOCUMENTS");
//            List<String> queries = Arrays.asList("Action Movies", "Best Movies 2022", "Worst Movies 2023", "How to watch free movies online");
//            List<String> Documents = Arrays.asList("yahoo.com", "ok.com", "movies.com", "stuff.com");
//            List<List<String>> Links = Arrays.asList(
//                    Arrays.asList("ok.com","movies.com"),
//                    Arrays.asList("stuff.com"),
//                    Arrays.asList("yahoo.com", "stuff.com","ok.com"),
//                    Arrays.asList("movies.com")
//            );
            resultcollection.deleteMany(new Document());
            // Get the value of the "names" field from the latest document
            System.out.println("query is "+ query);
            String[] subqueries=query.split(" ");
            for(String subquery:subqueries) {
                Double  idf;
                Document querydoc = new Document("word", subquery);
                try (MongoCursor<Document> cursor = wcollection.find(querydoc).iterator()) {
                    // Iterate over the results
                    String line=null;
                    while (cursor.hasNext()) {
                        Document doc = cursor.next();
                        String filePath = "C:\\Users\\kabou\\Desktop\\Nour\\UNI Tingz\\Junior\\Term 2\\APT\\Project\\SearchEngine\\"+doc.getString("file");
                        System.out.println(filePath);
                        line=findParagraphWithWord(filePath, subquery);
                        String temp=doc.getString("Link");
                        Double cur=doc.getDouble("tf-idf");
                        Double priority= Double.valueOf(doc.getInteger("Priority"));
                        Pair<Double,String> pre=Results.get(temp);
                        if(pre!=null)
                        {
                            if(pre.getSecond()==null&&line!=null)
                                Results.put(temp,new Pair<Double,String>(cur+pre.getFirst(),line));
                        }
                        else {
                            Results.put(temp,new Pair<Double,String>(cur+1/(priority+10),line));
                        }
                    }
                }
            }

            Results.forEach((link,pair)-> {
                Document document = new Document("link", link);
                System.out.println(link);
                Document doc = docscollection.find(document).first();
                if(doc!=null) {
                    pair.setFirst(pair.getFirst() + doc.getDouble("rank"));
                    document = document.append("score", pair.getFirst())
                            .append("title", doc.getString("title"))
                            .append("info", pair.getSecond());

                    resultcollection.insertOne(document);
                }
            });


            // Perform the aggregation with sorting


            // Print the latest name
            System.out.println("Latest name inserted: " + query);

            // Close the connection
            mongoClient.close();
        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
        }

    }
}
