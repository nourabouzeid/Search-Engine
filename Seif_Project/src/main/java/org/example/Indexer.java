package org.example;

import java.io.*;
import java.util.*;

import ca.rmen.porterstemmer.PorterStemmer;
import com.mongodb.client.*;
import com.sun.jdi.event.StepEvent;
import org.bson.Document;
public class Indexer implements Runnable{
    private static Set<String> stopWords = new HashSet<>();
    private static String removePunctuation(String text) {
        return text.replaceAll("[\\p{Punct}]", ""); // Removes punctuation
    }
    public static String stemWord(String word) {
        if (stopWords.contains(word.toLowerCase())) { // Check if the word is a stop word
            return "-1"; // Indicates a stop word
        }

        PorterStemmer porterStemmer = new PorterStemmer(); // Create a new PorterStemmer instance
        return porterStemmer.stemWord(word); // Return the stemmed word
    }
    private static void loadStopWords() {
        stopWords.clear(); // Clear previous stop words

        // Try to read the stop words from a resource file
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Indexer.class.getResourceAsStream("/stopwords.txt")))) {
            String line;

            // Read each line and add the lower-cased word to the stopWords set
            while ((line = reader.readLine()) != null) {
                stopWords.add(line.trim().toLowerCase());
            }
        } catch (IOException e) { // Catch IO exceptions
            System.err.println("Error loading stop words due to I/O issue: " + e.getMessage());
        } catch (NullPointerException e) { // Catch cases where the resource is missing
            System.err.println("Error loading stop words: resource not found.");
        }
    }
    private MyHashMap<String,String , ObjectStoredInHashMap> hash;
    private MongoCollection<Document> collection;
    private MongoCollection<Document> collection2;
    private MongoCollection<Document> collection3;
    private  String FN;
    public  void setFN (String fileName) {
        this.FN = fileName;
    }
    public  void setCollection(MongoCollection<Document>x) {this.collection=x;}
    public  void setCollection2(MongoCollection<Document>x) {this.collection2=x;}
    public  void setCollection3(MongoCollection<Document>x) {this.collection3=x;}
    //   "C:\\Users\\seif\\IdeaProjects\\Indexer\\src\\ComingFromCrawler.html"
    public  void run() {
        loadStopWords();
        hash=new MyHashMap<String,String , ObjectStoredInHashMap>();
        int position=0;
        int totalSize=0;
        // 0: plain text
        // 1: header
        // 2 : title

        List<String> Links = new ArrayList<>();
        while(FN==null);
        try (Scanner scanner = new Scanner(new File(FN)))
        {

            while (scanner.hasNext()) {
                String word = scanner.next();


                if(word.equals("<h1>") ||  word.equals("<h2>") || word.equals("<h3>") || word.equals("<h4>") || word.equals("<h5>") || word.equals("<h6>")|| word.equals("<a") )
                {
                    if(position <=1)
                        position=1;
                    continue;
                }
                else if(word.equals("<title>"))
                {
                    if(position<=2)
                        position=2;
                    continue;
                }
                else if ( word.equals("</title>"))
                {
                    position=0;
                    continue;
                }
                else if ( word.equals("</h1>") ||  word.equals("</h2>") || word.equals("</h3>") || word.equals("</h4>") || word.equals("</h5>") || word.equals("</h6>")|| word.equals("</a>"))
                {
                    position=0;
                    continue;
                }
                else if(word.equals("<body>")||word.equals("<html>") || word.equals("<head>") || word.equals("</body>")||word.equals("</html>") || word.equals("</head>") || word.equals("<p>") || word.equals("</p>") || word.equals("<li>" ) || word.equals("</li>")) {
                    position=0;
                    continue;
                }
                if (!word.isEmpty())
                {
                    if(word.length()>11&&word.substring(0, 4).equals("href")) {
                        if (word.length() > 11 && word.substring(6, 11).equals("https")) {
                            word = word.substring(6, word.length() - 3);
                            Links.add(word);
                        }
                        else
                            continue;
                    }

                    totalSize++;
                    word=stemWord(word);
                    word=removePunctuation(word);
                    if(word.equals("1")||word.isEmpty())
                        continue;
                    HashMap<String, ObjectStoredInHashMap> Temp = hash.get(word);

                    if(Temp == null)
                    {
                        ObjectStoredInHashMap Temp1 = new ObjectStoredInHashMap();
                        Temp1.setPosition(position);
                        Temp1.setTF(1);
                        hash.put(word,FN,Temp1);
                    }
                    else
                    {
                        ObjectStoredInHashMap Temp1 = Temp.get(FN);
                        if(Temp1 == null)
                        {
                            Temp1 = new ObjectStoredInHashMap();
                            Temp1.setPosition(position);
                            Temp1.setTF(1);
                            Temp.put(FN,Temp1);
                            hash.put(word,FN,Temp1);

                        }
                        else
                        {
                            Temp1.setTF(Temp1.getTF()+1);
                            Temp1.setPosition(position);
                            hash.update(word,FN,Temp1);
                        }
                    }
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error reading input: " + e.getMessage());
        }
        if(totalSize!=0) {
            int finalTotalSize = totalSize;
            hash.forEachEntry((key, nestedMap) -> {
                nestedMap.forEach((nestedKey, value) -> {
                    value.setTFInPercentage((float)value.getTF()/(float) finalTotalSize);
                });
            });
        }

        if(totalSize!=0) {
            int finalTotalSize = totalSize;
            hash.forEachEntry((key, nestedMap) -> {
                nestedMap.forEach((nestedKey, value) -> {
                    value.setTFInPercentage((float)value.getTF()/(float) finalTotalSize);
                    Document document = new Document("word", key)
                                .append("file", FN)
                                .append("Priority",value.getPosition())
                                .append("TF", value.getTF())
                                .append("TF%",value.getTFInPercentage() * 100);
                        collection.insertOne(document);
                });
            });
            Document document = new Document("file", FN)
                    .append("link",Links);
            collection2.insertOne(document);
        }
    }

}
