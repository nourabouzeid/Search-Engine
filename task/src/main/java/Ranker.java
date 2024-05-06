import com.mongodb.client.*;
import com.mongodb.client.model.Aggregates;
import org.bson.Document;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bson.Document;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
public class Ranker {
        public static String findParagraphWithWord(String filePath, String targetWord) {
            StringBuilder paragraph = new StringBuilder();
            boolean found = false;

            try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
                String line;
                while ((line = br.readLine()) != null) {

                        String cleanLine = removeHtmlTags(line);
                        String[] words = cleanLine.split("\\s+");
                    if (words.length>10) {
                        for(int i=0;i<words.length;i++)
                        {
                            if(words[i].equals(targetWord))
                            {
                                found = true;
                                if(i<=20)
                                {
                                    int up= Math.min(words.length, 20);
                                    for(int j=0;j<up;j++) {
                                        paragraph.append(words[j]).append(" ");
                                    }
                                }
                                else {
                                    int up= Math.min(words.length, i+10);
                                    for(int j=i-20;j<up;j++) {
                                        paragraph.append(words[j]);
                                    }
                                }
                                return paragraph.toString();
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return "The word '" + targetWord + "' was not found in the HTML file.";
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
                    while (cursor.hasNext()) {
                        Document doc = cursor.next();
                        String filePath = "C:\\Users\\kabou\\Desktop\\Nour\\UNI Tingz\\Junior\\Term 2\\APT\\Project\\Seif_Project"+"\\"+doc.getString("file");
                        String line=findParagraphWithWord(filePath, subquery);
                        String temp=doc.getString("Link");
                        Double cur=doc.getDouble("tf-idf");
                        Double priority= Double.valueOf(doc.getInteger("Priority"));
                        Pair<Double,String> pre=Results.get(temp);
                        if(pre!=null)
                        {
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
