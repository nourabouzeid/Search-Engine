import com.mongodb.client.*;
import org.bson.Document;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Ranker {
    public static void Run(String query) {

        HashMap<String,Double> Results=new HashMap<String,Double>();
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
                        String temp=doc.getString("file");
                        Double cur=doc.getDouble("tf-idf");
                        Double priority= Double.valueOf(doc.getInteger("Priority"));
                        Double pre=Results.get(temp);
                        if(pre!=null)
                        {
                            Results.put(temp,cur+pre);
                        }
                        else {
                            Results.put(temp+priority,cur);
                        }
                    }
                }
            }

            Results.forEach((file,score)-> {
                Document document = new Document("link", file);
                for (Document doc : docscollection.find(document)) {
                    score+=doc.getDouble("rank");
                }
                document=document.append("score", score)
                        .append("title","title")
                        .append("info","this is a paragraph");

                resultcollection.insertOne(document);
            });


            // Print the latest name
            System.out.println("Latest name inserted: " + query);

            // Close the connection
            mongoClient.close();
        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
        }

    }
}
