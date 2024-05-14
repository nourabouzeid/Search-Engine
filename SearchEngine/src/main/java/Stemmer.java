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
import java.util.concurrent.TimeUnit;

public class Stemmer extends HttpServlet {
    private static final int PAGE_SIZE = 10; // The maximum size for a page
    public static Set<String> stopWords = new HashSet<>(); // Initialize with an empty set

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        long startTime = System.nanoTime();
        loadStopWords(); // Load stop words from a file

        // Get and validate the 'Query' parameter
        String statement = request.getParameter("Query");
        String unstemmed=statement;
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        if (statement == null || statement.trim().isEmpty()) {
            out.println("<html><head>");
            out.println("<script type='text/javascript'>");
            out.println("alert('Missing or empty Query parameter. Please provide a valid query.');");
            out.println("</script>");
            out.println("</head><body style='background-color: black; display: flex; flex-direction: column; justify-content: center; align-items: center; height: 100vh; margin: 0;'>");

            // Render the form again for the user to re-enter the query
            out.println(getFormHtml()); // Insert the form HTML
            out.println("</body></html>");

            out.close(); // Close the output stream
            return; // Exit to avoid further processing
        }

        // Process the statement (remove punctuation, split into words, and apply stemming)
        String cleanedStatement = removePunctuation(statement);
        if (cleanedStatement.equals("")||cleanedStatement.equals(" ")||cleanedStatement.equals("  ")||cleanedStatement.equals("\"\"")||cleanedStatement.equals("\"  \"")) {
            out.println("<html><head>");
            out.println("<script type='text/javascript'>");
            out.println("alert(' empty Query parameter. Please provide a valid query.');");
            out.println("</script>");
            out.println("</head><body style='background-color: black; display: flex; flex-direction: column; justify-content: center; align-items: center; height: 100vh; margin: 0;'>");

            // Render the form again for the user to re-enter the query
            out.println(getFormHtml()); // Insert the form HTML
            out.println("</body></html>");

            out.close(); // Close the output stream
            return; // Exit to avoid further processing
        }
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
        // Remove space from the beginning and end of the result
        String cleanedResult = result.toString().trim();

// Remove space before and after quotation marks
        cleanedResult = cleanedResult.replaceAll("\\s+\"", "\"").replaceAll("\"\\s+", "\"");

// Set the cleanedResult back to result
        result.setLength(0);
        result.append(cleanedResult);
        if (result.toString().matches("[\\s\"]+")||result.toString().length()==0) {
            out.println("<html><head>");
            out.println("<script type='text/javascript'>");
            out.println("alert(' empty Query parameter. Please provide a valid query.');");
            out.println("</script>");
            out.println("</head><body style='background-color: black; display: flex; flex-direction: column; justify-content: center; align-items: center; height: 100vh; margin: 0;'>");

            // Render the form again for the user to re-enter the query
            out.println(getFormHtml()); // Insert the form HTML
            out.println("</body></html>");

            out.close(); // Close the output stream
            return; // Exit to avoid further processi
        }
        Ranker.Run(result.toString(),unstemmed);
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
        long endTime = System.nanoTime(); // Record end time

        // Calculate the elapsed time
        long elapsedTimeNano = endTime - startTime; // Time in nanoseconds
        double elapsedTimeMs = TimeUnit.NANOSECONDS.toMillis(elapsedTimeNano); // Time in milliseconds

        // Prepare HTTP response


        out.println("<html><head>");
        out.println("<title>Search Results</title>");
        out.println("<style>");
        out.println("body { font-family: Arial, sans-serif; margin: 20px; padding: 20px; background-color: #000; color: white; }");
        out.println(".result { padding: 15px; margin-bottom: 10px; background-color: #333; border-radius: 15px; box-shadow: inset 2px 5px 10px rgba(0,0,0,0.3); transition: 300ms ease-in-out; }");
        out.println(".result:hover { background-color: white; color: black; transform: scale(1.05); box-shadow: 13px 13px 100px #969696, -13px -13px 100px #ffffff; }");
        out.println(".pagination { text-align: center; margin-top: 20px; }");
        out.println(".page-link { margin: 0 10px; text-decoration: none; color: #ffffff; cursor: pointer; }");
        out.println(".page-link:hover { color: #ccc; }");
        out.println(".page-link.active { font-weight: bold; text-decoration: underline; }");
        out.println("</style>");
        out.println("</head><body>");

// Display the reconstructed statement
        out.println("<h1>" + result.toString() + "</h1>");
// Display the elapsed time in small font at the top of the page
        out.println("<div class='small-text'>Searching Time: " + elapsedTimeMs + " ms</div>"); // Display the elapsed time in milliseconds

// Display the results for the current page
        for (int i = startIndex; i < endIndex; i++) {
            out.println("<div class='result'>");
            out.println("<div class='title'>" + (i < dbContents.size() ? dbContents.get(i) : "N/A") + "</div>");
            out.println("<a href='" + links.get(i) + "' target='_blank' style='color: orange; text-decoration: underline;'>" + links.get(i) + "</a>");


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
    private String getFormHtml() {
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <title>Search Form</title>\n" +
                "</head>\n" +
                "<body style=\"background-color: black; display: flex; flex-direction: column; justify-content: center; align-items: center; height: 100vh; margin: 0; position: relative;\">\n" +
                "\n" +
                "<!-- Positioning the image 50px below the top -->\n" +
                "<img src=\"./SpotLight_bgr.png\" alt=\"Spotlight Background\" style=\"max-width: 300px; position: absolute; top: 50px; align-self: center;\">\n" +
                "\n" +
                "<!-- Centering the form vertically and horizontally -->\n" +
                "<form action=\"searchForm\" method=\"GET\" id=\"searchForm\" style=\"display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 15px; margin-top: 150px; /* Increased to account for the image and provide space */\">\n" +
                "    <input type=\"text\" name=\"Query\" class=\"input\" style=\"width: 300px; border: none; outline: none; border-radius: 15px; padding: 1em; background-color: #ccc; box-shadow: inset 2px 5px 10px rgba(0,0,0,0.3); transition: 300ms ease-in-out;\"\n" +
                "           onfocus=\"this.style.backgroundColor='white'; this.style.transform='scale(1.05)'; this.style.boxShadow='13px 13px 100px #969696, -13px -13px 100px #eff2ac';\"\n" +
                "           onblur=\"this.style.backgroundColor='#ccc'; this.style.transform='scale(1); this.style.boxShadow='inset 2px 5px 10px rgba(0,0,0,0.3)';\"/>\n" +
                "\n" +
                "    <input type=\"submit\" class=\"my-submit-btn\" value=\"Search\" style=\"width: 6.5em; height: 2.3em; background: black; color: white; border: none; border-radius: 0.625em; font-size: 20px; font-weight: bold; cursor: pointer; transition: all 0.5s; position: relative; overflow: hidden;\"\n" +
                "           onmouseover=\"this.style.color='black'; this.style.background='white';\"\n" +
                "           onmouseout=\"this.style.color='white'; this.style.background='black';\"\n" +
                "           onmouseenter=\"this.style.color='black'; this.style.background='white'; this.nextSibling.style.transform='skewX(-45deg) scale(1, 1)';\"\n" +
                "           onmouseleave=\"this.style.color='white'; this.style.background='black'; this.nextSibling.style.transform='skewX(-45deg) scale(0, 1)';\"/>\n" +
                "</html>\n";
    }
}
