


import java.io.FileNotFoundException;
import java.util.*;

import com.mongodb.client.*;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.File;

import static com.mongodb.client.model.Accumulators.*;
import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Projections.*;

public class IndexerMain {


    public static MongoCollection<Document> collection;
    public static MongoCollection<Document> collection2;
    public static MongoCollection<Document> collection3;
    public static void main(String[] args)
    {
//        List<String> Documents= Arrays.asList("0.html","1.html");
//        List<String> Outputs= Arrays.asList("imdb.com","test.com");
        String connectionString = "mongodb://localhost:27017";
        MongoDatabase database = null;
        MongoClient mongoClient = MongoClients.create(connectionString);
        database = mongoClient.getDatabase("SearchEngine");
        database.runCommand(new Document("ping", 1));
        String collectionString = "MYCOLLECTION";
        database.createCollection(collectionString);
        database.createCollection("MYDOCUMENTS");
        database.createCollection("MYWORDS");
        collection = database.getCollection("MYCOLLECTION");
        collection2 = database.getCollection("MYDOCUMENTS");
        collection3 = database.getCollection("MYWORDS");
        collection.deleteMany(new Document());
        collection2.deleteMany(new Document());
        collection3.deleteMany(new Document());
//        Indexer x=new Indexer();
//        x.setCollection(collection);
        Scanner scanner;
        try {
           scanner  = new Scanner(new File("Links.txt"));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        int counter =1;
        String inputFilePath = null;
        List<Thread> threads = new ArrayList<>();
        while(scanner.hasNext()) {
            inputFilePath = counter+".html";
            String URL = scanner.next();
            counter++;

            Indexer x = new Indexer();
            x.setLink(URL);
            x.setFN(inputFilePath);
            x.setCollection(collection);
            x.setCollection2(collection2);
            x.setCollection3(collection3);
            Thread t = new Thread(x);
            t.start();
            threads.add(t);
        }
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
System.out.println("WE FONEEEE");
            //        x.setFN(outputFilePath);
            //        x.run();
            //        for(int i=0;i<6000;i++)
            //        {
            //            Thread t = new Thread(new Main());
            //            t.setName(String.valueOf(i));
            //            t.start();
            //        }
            //        for(int i=0;i<6000;i++) {
            //            Indexer x = new Indexer();
            //            x.setFN(String.valueOf(i)+".html");
            //            x.setCollection(collection);
            //            Thread t = new Thread(x);
            //            t.setName(String.valueOf(i));
            //            t.start();
            //        }








        //idf
        List<Bson> pipeline = new ArrayList<>();
        pipeline.add(group("$word", push("documents", "$$ROOT")));
        pipeline.add(project(fields(include("category", "documents"), computed("count", new Document("$size", "$documents")))));


        // Execute aggregation
        List<Document> results = collection.aggregate(pipeline).into(new ArrayList<>());

        // Output results
        for (Document result : results) {
            String category = result.getString("_id");
            List<Document> documents = result.getList("documents", Document.class);
            int count = result.getInteger("count");
            if(category.equals("2019)"))
            System.out.println("Category: " + category + ", Count: " + count);
            Double idf=Math.log((double) counter /count)+0.00001;
            Document document = new Document("word", category)
                    .append("idf", idf);
            collection3.insertOne(document);
            for(Document doc:documents)
            {
                Double tf=doc.getDouble("TF%");
                Double tf_idf=tf*idf;
                Document update = new Document("$set", new Document("tf-idf", tf_idf));
                collection.updateOne(doc, update);
            }
        }

        //page rank test
//        database.createCollection("Test");
//        MongoCollection<Document> collectiont = database.getCollection("Test");
//        collectiont.deleteMany(new Document());
//        List<String> links=List.of("A","B","C","D");
//        List<List<String>> links_links=List.of(
//                List.of("B","C"),
//            List.of("D"),
//            List.of("A","B","D"),
//            List.of("C")
//    );
//        for(int c=0;c<4;c++) {
//            Document document = new Document("link", links.get(c))
//                    .append("links", links_links.get(c));
//            collectiont.insertOne(document);
//        }

        //end page rank test

        FindIterable<Document> documents = collection2.find();
        int size=(int)collection2.countDocuments();
        Map<String, Integer> myMap = new HashMap<>();
        Node[] nodes=new Node[size];
        int i=0;
        for (Document document : documents) {
            Node node = new Node(); // Create a new Node object for each document
            node.id = document.getString("link");
            node.neighbours = (List<String>) document.get("links");
            node.in=new ArrayList<>();
            node.rank=0.1 ; //1/(double)size;
            node.links=node.neighbours.size();
            nodes[i] = node; // Assign the Node object to the array element
            myMap.put(node.id,i);
            i++;
        }
        System.out.println("size of map"+i+"\nSize of nodes"+size);
        PageRank(nodes,myMap);
        for (Document document : documents) {
            int j=myMap.get(document.getString("link"));
            Double rank=nodes[j].rank;
            System.out.println(rank);
            Document update = new Document("$set", new Document("rank", rank));
            collection2.updateOne(document, update);
        }
    }



    public static void PageRank(Node[] nodes, Map<String, Integer> myMap)
    {
        for(Node node:nodes)
        {
            for(String neighbour:node.neighbours)
            {
                if(neighbour!=null&&myMap.get(neighbour)!=null)
                    nodes[myMap.get(neighbour)].in.add(node.id);
            }
        }
        for(int i=0;i<2;i++)
        {
            double[]ranks=new double[nodes.length];
            int j=0;
            for(Node node:nodes)
            {
                ranks[j]=0;
                for(String l:node.in)
                {
                    ranks[j]+=nodes[myMap.get(l)].rank/nodes[myMap.get(l)].links;
                }
                j++;
            }
            j=0;
            for(Node node:nodes)
            {
                node.rank=ranks[j];
                j++;
            }
        }
    }
}

