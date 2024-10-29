package org.example;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONObject;
import org.json.JSONArray;

public class SemanticSearchTool {

    private static final String API_URL = "https://api-inference.huggingface.co/models/sentence-transformers/all-MiniLM-L6-v2";
    private static final String API_TOKEN = "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"; // Replace with your actual token

    // Document class to hold metadata for each line in a file
    static class Document {
        String fileName;
        String text;
        int lineNumber;

        public Document(String fileName, String text, int lineNumber) {
            this.fileName = fileName;
            this.text = text;
            this.lineNumber = lineNumber;
        }
    }

    // Reads all lines from files in a folder and returns a list of Document objects with metadata
    public static List<Document> readDocumentsFromFolder(String folderPath) throws IOException {
        List<Document> documents = new ArrayList<>();
        Files.walk(Paths.get(folderPath))
                .filter(Files::isRegularFile)
                .forEach(filePath -> {
                    try {
                        List<String> lines = Files.readAllLines(filePath);
                        for (int i = 0; i < lines.size(); i++) {
                            documents.add(new Document(filePath.getFileName().toString(), lines.get(i), i + 1));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
        return documents;
    }

    // Sends request to the API with source sentence and list of document sentences
    public static List<Double> getSimilarityScores(String sourceSentence, List<String> sentences) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        // Create JSON payload
        JSONObject json = new JSONObject();
        json.put("source_sentence", sourceSentence);

        JSONArray sentencesArray = new JSONArray(sentences);
        json.put("sentences", sentencesArray);

        // Build the request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Authorization", "Bearer " + API_TOKEN)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();

        // Send the request and get the response
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Parse the response to extract similarity scores
        JSONArray scoresArray = new JSONArray(response.body());
        List<Double> scores = new ArrayList<>();
        for (int i = 0; i < scoresArray.length(); i++) {
            scores.add(scoresArray.getDouble(i));
        }

        return scores;
    }

    public static void main(String[] args) {
        try {
            String folderPath = "C:\\Users\\huy.nguyen\\Desktop\\personal-project\\hugging-face-practice\\sematic-search-tool\\document-test"; // Replace with the actual folder path
            String sourceSentence = "big cats";

            // Read documents and their metadata
            List<Document> documents = readDocumentsFromFolder(folderPath);

            // Extract text content for API input
            List<String> sentences = new ArrayList<>();
            for (Document doc : documents) {
                sentences.add(doc.text);
            }

            // Call the API and get similarity scores
            List<Double> scores = getSimilarityScores(sourceSentence, sentences);

            // Display results with file names, scores, and line numbers
            System.out.println("Similarity Scores:");
            for (int i = 0; i < scores.size(); i++) {
                Document doc = documents.get(i);
                System.out.printf("Score: %.4f | File: %s | Line: %d | Text: %s%n", scores.get(i), doc.fileName, doc.lineNumber, doc.text);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
