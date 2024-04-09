import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Scanner;
public class Indexer {
   private  static MyHashMap<String,String,ObjectStoredInHashMap> TheDataBase = new MyHashMap<>();
   private  String FN;
   public  void setFN (String fileName) {
       this.FN = fileName;
   }
    //   "C:\\Users\\seif\\IdeaProjects\\Indexer\\src\\ComingFromCrawler.html"
    private boolean isHeaderTag(String token) {
        return token.matches("<h[1-6]>.*?</h[1-6]>"); // Check if token matches <h1> to <h6> header tag pattern
    }
    private boolean isTitleTag(String token) {
        // Check if the token matches the <title> tag pattern
        return token.matches("<title>.*?</title>");
    }
    public static String removeTitle (String input)
    {
        String result=input;
       result= result.replace("<title>","");
        result= result.replace("</title>","");
        return result;
    }
    public static String removePlainTextTags(String input)
    {
        String result=input;
        result=result.replace("<p>","");
        result=result.replace("</p>","");
        return result;
    }

    public static String removeH1Tags(String input) {
        // Replace <h1> with an empty string
        String result=input;
             result = result.replace("<h1>", "");
             result = result.replace("<h2>", "");
             result =result.replace("<h3>", "");
             result = result.replace("<h4>", "");
             result = result.replace("<h5>", "");
             result = result.replace("<h6>", "");
        // Replace </h1> with an empty string in the updated result
             result = result.replace("</h1>", "");
             result = result.replace("</h2>", "");
             result = result.replace("</h3>", "");
             result = result.replace("</h4>", "");
             result = result.replace("</h5>", "");
             result = result.replace("</h6>", "");
        return result;
    }
   public  void run()
   {

       while(FN==null);
       try (Scanner scanner = new Scanner(new File(FN))) {

           while (scanner.hasNext()) {
               String word = scanner.next();
               String position;
               if (isHeaderTag(word)) {
                   position = "header";
               } else if (isTitleTag(word)) {
                   position = "title";
               } else {
                   position = "plaintext";
               }
                word=removeH1Tags(word);
                word=removeTitle(word);
                word=removePlainTextTags(word);
               if (!word.isEmpty()) {
                   System.out.println("Indexing word: " + word);
                   System.out.println(word+" "+position);
                   HashMap<String, ObjectStoredInHashMap> Temp = TheDataBase.get(word);
                   if(Temp != null) {
                       if(Temp.get(FN) != null) {
                           Temp.get(FN).setTF(Temp.get(FN).getTF() + 1);
                           TheDataBase.put(word, FN, Temp.get(FN));
                       }
                       else
                       {
                           ObjectStoredInHashMap x =new ObjectStoredInHashMap();
                           Temp.put(FN,x);
                           Temp.get(FN).setTF(1);
                           TheDataBase.put(word, FN, Temp.get(FN));
                       }
                   }
                   else
                   {
                       ObjectStoredInHashMap x =new ObjectStoredInHashMap();
                       Temp=new HashMap<String,ObjectStoredInHashMap>();
                       Temp.put(FN,x);
                       Temp.get(FN).setTF(1);
                       TheDataBase.put(word, FN, Temp.get(FN));
                   }
               }
           }
       } catch (FileNotFoundException e) {
           System.err.println("File not found: " + e.getMessage());
       } catch (Exception e) {
           System.err.println("Error reading input: " + e.getMessage());
       }
       String name="Word2";
       String name1=FN;
       HashMap<String, ObjectStoredInHashMap> internalHashMap = TheDataBase.get(name);
       int x= TheDataBase.getValueFromNestedMap(internalHashMap, name1).getTF();
       System.out.println(x);
       x=TheDataBase.getValueFromNestedMap(internalHashMap,"C:\\Users\\seif\\IdeaProjects\\Indexer\\src\\ComingFromCrawler.html" ).getTF();
       System.out.println(x);
       System.out.println("HENAAAAA "+internalHashMap.size());
   }
}
