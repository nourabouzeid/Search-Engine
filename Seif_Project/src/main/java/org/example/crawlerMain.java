package org.example;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.Vector;

public class crawlerMain {


    public static void main(String[] args) throws IOException {
        File file = new File("C:\\Users\\karee\\OneDrive\\Documents\\school\\APT\\WebCrawler_ver1.1\\src\\seed.txt");
        Scanner s = new Scanner(file);
        FileWriter myWriter = new FileWriter("Links.txt");
        Vector<webCrawler> bots = new Vector<>();
        int i =0;
        while (s.hasNext()){
            bots.add(new webCrawler(s.next(),i++,myWriter));
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