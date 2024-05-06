import ca.rmen.porterstemmer.PorterStemmer;
import java.util.HashSet;
import java.util.Set;
import java.io.*;

import java.util.ArrayList;
import java.util.List;
import com.mongodb.client.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bson.Document;
import java.util.Arrays;

public class Stemmer extends HttpServlet {
    private static final int PAGE_SIZE = 10; // The maximum size for a page
    private static Set<String> stopWords = new HashSet<>(); // Initialize with an empty set

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        loadStopWords(); // Load stop words from a file

        // Get and validate the 'Query' parameter
        String statement = request.getParameter("Query");
        if (statement == null || statement.trim().isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or empty 'Query' parameter.");
            return;
        }

        // Process the statement (remove punctuation, split into words, and apply stemming)
        String cleanedStatement = removePunctuation(statement);
        List<String> words = splitIntoWords(cleanedStatement);
        List<String> stemmedWords = new ArrayList<>();
        for (String word : words) {
            stemmedWords.add(stemWord(word));
        }

        // Reconstruct the stemmed words into a single string
        StringBuilder result = new StringBuilder();
        for (String stemmedWord : stemmedWords) {
            if (!stemmedWord.equals("-1")) { // Use 'equals' to compare strings
                if (result.length() > 0) {
                    result.append(" "); // Add a space between words
                }
                result.append(stemmedWord);
            }
        }
        Ranker.Run(result.toString());
        String pageParam = request.getParameter("page");
        int currentPage = (pageParam == null) ? 1 : Integer.parseInt(pageParam);

        List<String> dbContents = new ArrayList<>(); //website title
        List<String> links= new ArrayList<>();//URL
        List<String> versions= new ArrayList<>(); //paragraph
        try (MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017")) {
            MongoDatabase database = mongoClient.getDatabase("SearchEngine");
            MongoCollection<Document> collection = database.getCollection("Results");
            Document sortCriteria = new Document("score", -1); // Change "fieldName" to the name of the field you want to sort by

            // Perform the query and sort the results
            FindIterable<Document> results = collection.find().sort(sortCriteria);
            for (Document doc : results) {
                String title = doc.getString("title");
                String link = doc.getString("link");
                String info = doc.getString("info");
                dbContents.add(title);
                links.add(link);
                versions.add(info);
            }
        }
        // Fetch the data for different fields


        int dataSize = Math.min(Math.min(dbContents.size(), links.size()), versions.size());
        int totalPages = (int) Math.ceil(dataSize / (double) PAGE_SIZE);
        int startIndex = (currentPage - 1) * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, dataSize);

        // Prepare HTTP response
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        out.println("<html><head>");
        out.println("<title>Search Results</title>");
        out.println("<style>");
        out.println("body { font-family: Arial, sans-serif; margin: 20px; padding: 20px; background-color: #f9f9f9; }");
        out.println(".result { padding: 15px; margin-bottom: 10px; background-color: #f5f5f5; border-radius: 5px; box-shadow: 0px 4px 10px rgba(0, 0, 0, 0.1); }");
        out.println(".pagination { text-align: center; margin-top: 20px; }");
        out.println(".page-link { margin: 0 10px; text-decoration: none; color: #0066cc; cursor: pointer; }");
        out.println(".page-link.active { font-weight: bold; text-decoration: underline; }");
        out.println("</style>");
        out.println("</head><body>");

        // Display the reconstructed statement
        out.println("<h1>" + result.toString() + "</h1>");

        // Display the results for the current page
        for (int i = startIndex; i < endIndex; i++) {
            out.println("<div class='result'>");
            out.println("<div class='title'>" + (i < dbContents.size() ? dbContents.get(i) : "N/A") + "</div>");
            out.println("<div class='link'>" + (i < links.size() ? links.get(i) : "N/A") + "</div>");
            out.println("<div class='version'>Version: " + (i < versions.size() ? versions.get(i) : "N/A") + "</div>");
            out.println("</div>");
        }

        // Pagination controls with 'Query' parameter
        out.println("<div class='pagination'>");
        if (currentPage > 1) {
            out.println("<a class='page-link' href='?page=" + (currentPage - 1) + "&Query=" + statement + "'>Previous</a>");
        }
        for (int i = 1; i <= totalPages; i++) {
            if (i == currentPage) {
                out.println("<span class='page-link active'>" + i + "</span>");
            } else {
                out.println("<a class='page-link' href='?page=" + i + "&Query=" + statement + "'>" + i + "</a>");
            }
        }
        if (currentPage < totalPages) {
            out.println("<a class='page-link' href='?page=" + (currentPage + 1) + "&Query=" + statement + "'>Next</a>");
        }
        out.println("</div>");

        out.println("</body></html>");
        out.close();
    }



    private static void loadStopWords() {
        stopWords.clear(); // Clear previous stop words

        // Try to read the stop words from a resource file
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Stemmer.class.getResourceAsStream("/stopwords.txt")))) {
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

    private static String removePunctuation(String text) {
        return text.replaceAll("[\\p{Punct}]", ""); // Removes punctuation
    }

    private static List<String> splitIntoWords(String text) {
        String[] wordsArray = text.trim().split("\\s+");
        return new ArrayList<>(Arrays.asList(wordsArray));
    }

    public static String stemWord(String word) {
        if (stopWords.contains(word.toLowerCase())) { // Check if the word is a stop word
            return "-1"; // Indicates a stop word
        }

        PorterStemmer porterStemmer = new PorterStemmer(); // Create a new PorterStemmer instance
        return porterStemmer.stemWord(word); // Return the stemmed word
    }
}
