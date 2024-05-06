

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
public class HTMLPreserver {
    private static final List<String> PRESERVED_TAGS = List.of("h1", "h2", "h3", "h4", "h5", "h6", "p", "title","li","a");

    public static void main(String[] args) {
        String inputFilePath = "1.html";
        String outputFilePath = "preserved_output1.html";
        try {
            Document doc = Jsoup.parse(new File(inputFilePath), "UTF-8");
            Document preservedDoc = preserveElements(doc);
            writeDocumentToFile(preservedDoc, inputFilePath);
            System.out.println("Preservation completed. Output saved to: " + outputFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    public static Document preserveElements(Document doc) {
//        Document preservedDoc = Jsoup.parse("<html></html>");
//        for (String tagName : PRESERVED_TAGS) {
//            Elements elements = doc.select(tagName);
//            for (Element element : elements) {
//                preservedDoc.body().appendChild(element.clone());
//            }
//        }
//        return preservedDoc;
//    }
public static Document preserveElements(Document doc) {
    Document preservedDoc = Jsoup.parse("<html></html>");

    for (String tagName : PRESERVED_TAGS) {
        Elements elements = doc.select(tagName);
        for (Element element : elements) {
            // Clone the element
            Element clonedElement = element.clone();
            // If the cloned element is an anchor (<a>) element
            if (clonedElement.tagName().equalsIgnoreCase("a")) {
                // Get the original href attribute value
                String originalHref = element.attr("href");

                // Set the href attribute in the cloned element
                clonedElement.attr("href", originalHref);
            }

            // Append the cloned element to the body of the preserved document
            preservedDoc.body().appendChild(clonedElement);
        }
    }

    return preservedDoc;
}
//    public static void writeDocumentToFile(Document doc, String filePath) {
//        if (doc == null) {
//            System.err.println("Document is null. Cannot write to file.");
//            return;
//        }
//        try (FileWriter writer = new FileWriter(filePath)) {
//            // Get all elements in the document
//            Elements elements = doc.getAllElements();
//
//            // Iterate through all elements in the document
//            for (Element element : elements) {
//                String tagName = element.tagName().toLowerCase();
//
//                // Check if the element's tag should be preserved
//                if (PRESERVED_TAGS.contains(tagName)) {
//                    // Write the tag with proper spacing around its content
//                    writer.write(" <" + tagName + "> ");
//                    writer.write(element.text().trim());
//                    writer.write(" </" + tagName + "> ");
//                    writer.write("\n"); // Add a new line
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
public static void writeDocumentToFile(Document doc, String filePath) {
    if (doc == null) {
        System.err.println("Document is null. Cannot write to file.");
        return;
    }

    try (FileWriter writer = new FileWriter(filePath)) {
        // Get all elements in the document
        Elements elements = doc.getAllElements();

        // Iterate through all elements in the document
        for (Element element : elements) {
            String tagName = element.tagName().toLowerCase();

            // Check if the element's tag should be preserved
            if (PRESERVED_TAGS.contains(tagName)) {
                // Write the opening tag
                writer.write("<" + tagName);

                // Write attributes if the element is an anchor (<a>) tag
                if (tagName.equals("a")) {
                    String href = element.attr("href");
                    writer.write(" href=\"" + href + "\"");
                }

                writer.write("> ");

                // Write the element's text content with proper spacing
                writer.write(element.text().trim());

                // Write the closing tag
                writer.write(" </" + tagName + ">");
                writer.write("\n"); // Add a new line
            }
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
}
}
