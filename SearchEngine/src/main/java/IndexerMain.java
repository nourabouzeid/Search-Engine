


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
    public static MongoCollection<Document> collectionCount;
    public static void main(String[] args)
    {
//        List<String> Documents= Arrays.asList("0.html","1.html");
//        List<String> Outputs= Arrays.asList("imdb.com","test.com");
        String connectionString = "mongodb://localhost:27017";
        MongoDatabase database = null;
        MongoClient mongoClient = MongoClients.create(connectionString);
        database = mongoClient.getDatabase("SearchEngine");
        database.runCommand(new Document("ping", 1));
        collection = database.getCollection("MYCOLLECTION");
        collectionCount = database.getCollection("Counter");
        collection2 = database.getCollection("MYDOCUMENTS");
        collection3 = database.getCollection("MYWORDS");
        //collection.deleteMany(new Document());
        //collection2.deleteMany(new Document());
        collection3.deleteMany(new Document());
        long counter =collectionCount.find().first().getLong("count");
        AggregateIterable<Document> duplicates = collection2.aggregate(Arrays.asList(
                new Document("$group", new Document("_id", "$link")
                        .append("count", new Document("$sum", 1))
                        .append("docs", new Document("$push", "$_id"))
                ),
                new Document("$match", new Document("count", new Document("$gt", 1)))
        ));

        // Step 2: Delete Duplicate Documents
        for (Document doc : duplicates) {
            @SuppressWarnings("unchecked")
            ArrayList<String> docIds = doc.get("docs", ArrayList.class);
            docIds.remove(0); // Keep one document, remove others
            collection2.deleteMany(new Document("_id", new Document("$in", docIds)));
        }

        Scanner scanner;
        try {
           scanner  = new Scanner(new File("Links.txt"));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        String inputFilePath = null;
        Thread[] threads =new Thread[12];
        while(scanner.hasNext()) {
            inputFilePath = counter+".html";
            String URL = scanner.next();
            //System.out.println(URL);
            counter++;
            for(int i=0;i<12;i++)
            {
                if(counter%12==i)
                {
                    try {
                        if(threads[i]!=null)
                        threads[i].join();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    Indexer x=new Indexer();
                    x.setLink(URL);
                    x.setFN(inputFilePath);
                    x.setCollection(collection);
                    x.setCollection2(collection2);
                    x.setCollection3(collection3);
                    threads[i]=new Thread(x);
                    threads[i].start();
                }
            }
        }
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
System.out.println("WE FONEEEE");
        collectionCount.deleteMany(new Document());
        collectionCount.insertOne(new Document("count",counter));




        //idf
        List<Bson> pipeline = new ArrayList<>();
        pipeline.add(group("$word", push("documents", "$$ROOT")));
        pipeline.add(project(fields(include("category", "documents"), computed("count", new Document("$size", "$documents")))));


        // Execute aggregation
        List<Document> results = collection.aggregate(pipeline).into(new ArrayList<>());

        // Output results
        for (Document result : results) {
            System.out.println("AGGREGATED");
            String category = result.getString("_id");
            List<Document> documents = result.getList("documents", Document.class);
            int count = result.getInteger("count");
            Double idf=Math.log((double) counter /count)+0.00001;
            Document document = new Document("word", category)
                    .append("idf", idf);
            System.out.println("WORD: "+category);
            collection3.insertOne(document);
            for(Document doc:documents)
            {
                Double tf=doc.getDouble("TF%");
                Double tf_idf=tf*idf;
                Document update = new Document("$set", new Document("tf-idf", tf_idf));
                collection.updateOne(doc, update);
            }
        }

       // page rank test
//        database.createCollection("Test");
//        MongoCollection<Document> collectiont = database.getCollection("Test");    //send to mina
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
            node.title=document.getString("title");
            node.neighbours = (List<String>) document.get("links");
            node.in=new ArrayList<>();
            node.rank=1/(double)size;
            node.links=node.neighbours.size();
            nodes[i] = node; // Assign the Node object to the array element
            myMap.put(node.id,i);
            i++;
        }
        System.out.println("size of map"+i+"\nSize of nodes"+size);
        double max=PageRank(nodes,myMap);
        for (Document document : documents) {
            String templink=document.getString("link" );
            int j=myMap.get(templink);
            Double rank=nodes[j].rank/max;
            String title=nodes[j].title;
            System.out.println(rank);
            Document update1 = new Document("$set", new Document("rank", 1-rank));
            collection2.updateOne(document, update1);
            Document filter = new Document("Link", templink);

            // Define the update to add a new attribute with a certain value
            Document update = new Document("$set", new Document("rank", 1-rank).append("title",title));

            // Update the matching documents
            collection.updateMany(filter, update);
        }
    }


    private static double diffandset(double[]ranks,Node[]nodes,double[] max) {
        double sum = 0;
//        double avg=0;
//        for(double rank:ranks)
//        {
//            avg+=rank;
//        }
//        avg/= ranks.length;
        for(int i=0;i<nodes.length;i++) {
            sum += Math.abs(ranks[i] - nodes[i].rank);
            nodes[i].rank=ranks[i];
            if(ranks[i]>max[0])
                max[0]=ranks[i];
        }
        return(Math.sqrt(sum));
    }
    public static double PageRank(Node[] nodes, Map<String, Integer> myMap)
    {
        double[] max={-10};
        double eps=0.001;
        for(Node node:nodes)
        {
            for(String neighbour:node.neighbours)
            {
                if(neighbour!=null&&myMap.get(neighbour)!=null)
                    nodes[myMap.get(neighbour)].in.add(node.id);
            }
        }
    for (Node node:nodes)
        System.out.println(node.in);
        double diff = Double.MAX_VALUE;
        double[]ranks=new double[nodes.length];

        while(diff>eps)
        {
            double BETA=0.85;
            int j=0;
            for(Node node:nodes)
            {
                ranks[j]=1-BETA;
                for(String l:node.in)
                {
                    ranks[j]+=BETA*nodes[myMap.get(l)].rank/nodes[myMap.get(l)].links;
                }
                j++;
            }
            diff=diffandset(ranks,nodes,max);
        }
        return max[0];
    }
}

