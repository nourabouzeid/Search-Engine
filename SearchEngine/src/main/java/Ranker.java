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
                //System.out.println(words+"\n"+targetWord);
                    for (int i = 0; i < words.length; i++) {
                        if (removePunctuation(stemWord(words[i])).equals(targetWord)) {
                            //System.out.println("found "+words[i]);
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
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String findParagraphWithWords(String filePath, String[] targetWords) {
        if(filePath.equals("C:\\Users\\kabou\\Desktop\\Nour\\UNI Tingz\\Junior\\Term 2\\APT\\Project\\SearchEngine\\24.html"))
            System.out.println("IN RIGHT FILE "+ Arrays.toString(targetWords));
        StringBuilder paragraph = new StringBuilder();
        boolean found = false;

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            int k=1;
            while ((line = br.readLine()) != null) {
                String cleanLine = removeHtmlTags(line);
                String[] words = cleanLine.split("\\s+");
                    for (int i = 0; i < words.length; i++) {
                        int t=i;
                        for(int j = 0; j < targetWords.length; j++)
                        {

                            if(k==17&&filePath.equals("C:\\Users\\kabou\\Desktop\\Nour\\UNI Tingz\\Junior\\Term 2\\APT\\Project\\SearchEngine\\59.html"))
                            System.out.println(words[t]+" "+targetWords[j]);
                        if (removePunctuation(words[t]).equals(removePunctuation(targetWords[j]))) {
                            //System.out.println("found "+words[i]);
                            words[t]="<b>"+words[t]+"</b>";
                            found = true;
                            paragraph.append(words[t]).append(" ");
                            t++;
                            if(j==targetWords.length-1) {
                                int start = Math.max(0, i - 10);
                                int end = Math.min(words.length, i + 30);

                                for (int f = start; f < end; f++) {
                                    paragraph.append(words[f]).append(" ");
                                }
                                return paragraph.toString();
                            }
                            if(t==words.length)
                                break;
                        }
                        else
                        {
                            paragraph= new StringBuilder("");
                            break;
                        }
                    }}
k++;
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
    public static void Run(String query,String unstemmed) {

        HashMap<String,Pair<Double,String,String>> Results=new HashMap<String,Pair<Double,String,String>>();
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
            //System.out.println("query is "+ query);
            String[] subqueries=query.split(" ");
            String[] subqueriess=unstemmed.split(" ");
            String[] sentence={null,null,null,null,null};
            int isPhrase=0;
            if(subqueriess[0].charAt(0)==('"'))
            {
                isPhrase=1;
                Pattern pattern = Pattern.compile("\"([^\"]*)\"(?:\\s+(AND|OR|NOT))?\\s*(?:\"([^\"]*)\"(?:\\s+(AND|OR|NOT))?\\s*)?(?:\"([^\"]*)\")?");
                Matcher matcher = pattern.matcher(unstemmed);

                // Check if the input matches the pattern
                if (matcher.matches()) {
                    // Extract the sentences and operations
                    for(int i=0;i<5;i++)
                    {
                       sentence[i]=matcher.group(i+1);
                    }
                }
            }
            if(isPhrase==1)
            for(String s:sentence)
            {
                System.out.println(s+" ");
            }

            for(String subquery:subqueries) {
                Double  idf;
                Document querydoc = new Document("word", subquery);
                try (MongoCursor<Document> cursor = wcollection.find(querydoc).iterator()) {
                    // Iterate over the results
                    String line=null;
                    while (cursor.hasNext()) {
                        Document doc = cursor.next();
                        String file=doc.getString("file");
                        if(file.equals("24.html"))
                            System.out.println("HEREEEEEEEEE");
                        String filePath = "C:\\Users\\kabou\\Desktop\\Nour\\UNI Tingz\\Junior\\Term 2\\APT\\Project\\SearchEngine\\"+file;
                        //System.out.println(filePath);
                        if(isPhrase==0)
                            line=findParagraphWithWord(filePath, subquery);
                        String temp=doc.getString("Link");
                        Double cur=doc.getDouble("tf-idf");
                        String title=doc.getString("title");
                        Double rank=doc.getDouble("rank");
                        int position=doc.getInteger("Position");
                        Double priority= Double.valueOf(doc.getInteger("Priority"));
                        Pair<Double,String,String> pre=Results.get(temp);
                        if(pre!=null)
                        {
                            if(pre.getSecond()==null&&line!=null)
                            {
                                Results.put(temp, new Pair<Double, String,String>(cur + pre.getFirst(), line,title,file));
                            }
                        }
                        else
                        {
                            if(cur!=null)
                            Results.put(temp,new Pair<Double,String,String>(cur+priority/4+rank,line,title,file));
                        }
                    }
                }
            }


           // System.out.println("made itt");

            int finalIsPhrase = isPhrase;
            Results.forEach((link, pair)-> {
                boolean add=true;
//                if(finalIsPhrase==0&&pair.getSecond()==null)
//                    add=false;
                if(finalIsPhrase ==1)
                {
                    if(pair.getPosition().equals("24.html"))
                        System.out.println("here gian");
                    String line="";
                    String[]lines={null,null,null};
                    String filePath = "C:\\Users\\kabou\\Desktop\\Nour\\UNI Tingz\\Junior\\Term 2\\APT\\Project\\SearchEngine\\"+pair.getPosition();
                    int i=0;
                    while(i<5&&sentence[i]!=null)
                    {
                        System.out.println(sentence[i]);
                        lines[i/2]=findParagraphWithWords(filePath, sentence[i].split(" "));
                        i+=2;
                    }
                    boolean res= lines[0] != null;
                    for(i=1;i<3;i++)
                    {
                        if(sentence[2*i-1]!=null)
                        {
                            res=operation(res,sentence[2*i-1],lines[i]);
                        }
                    }
                    if(!res)
                    {
                        add=false;
                    }
                    else
                    {
                        for(i=0;i<3;i++)
                            line+=(lines[i]!=null)?(lines[i]+" "):"";
                        pair.setSecond(line);
                    }
                }
                Document document = new Document("link", link);
               // System.out.println(link);

                    document = document.append("score", pair.getFirst())
                            .append("title", pair.getCount())
                            .append("info", pair.getSecond());
                    if(add)
                    resultcollection.insertOne(document);
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
    static boolean operation(boolean s1, String op, String s2)
    {
        if(op.equals("AND"))
            return (s1&&s2!=null);
        else if(op.equals("OR"))
            return (s1||s2!=null);
        else
            return (s1&&s2==null);
    }
}
