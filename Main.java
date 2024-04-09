import java.io.File;
import java.io.FileNotFoundException;
import java.lang.module.ModuleFinder;
import java.util.HashMap;
import java.util.Scanner;
public class Main {
    public static  void main(String [] args)
    {
        Indexer x = new Indexer();
        x.setFN("C:\\Users\\seif\\IdeaProjects\\Indexer\\src\\ComingFromCrawler.html");
        x.run();
        Indexer y=new Indexer();
        y.setFN("C:\\Users\\seif\\IdeaProjects\\Indexer\\src\\TanyMara.html");
        y.run();
        y.run();
        y.run();
        y.run();
    }
}
