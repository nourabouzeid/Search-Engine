import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.Vector;

public class crawlerMain {


    public static void main(String[] args) throws IOException {
        File file = new File("seed.txt");
        Scanner s = new Scanner(file);
        FileWriter myWriter = new FileWriter("Links.txt");
        Vector<webCrawler> bots = new Vector<>();
        Vector<String> links = new Vector<>();
        int i =0;
        while (s.hasNext()){
            bots.add(new webCrawler(s.next(),i++,myWriter ,links));
        }
        for (webCrawler bot : bots){
            try{
                bot.getTh().join();
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    }
}