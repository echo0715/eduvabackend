package com.eduva.eduva.service;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.polling.SyncPoller;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Arrays;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class AzureDocIntelligenceService {

    // set `<your-endpoint>` and `<your-key>` variables with the values from the Azure portal
    @Value("${azure.document.intelligence.endpoint}")
    private String endpoint;

    @Value("${azure.document.intelligence.key}")
    private String key;

    @Value("${azure.document.intelligence.version}")
    private String api_version;



//    public List<String> getFormatFigurePosition(String documentUrl){
    public String getFormatFigurePosition(String documentUrl){
        String modelId = "prebuilt-layout";
        try {
            String resultId = analyzeDocument(documentUrl, modelId);
            if (resultId == null) {
                System.out.println("Failed to get result ID");
            }

            // Step 2: Poll for results
            String results = getResults(resultId, modelId);
            System.out.println("Analysis Results:");
            System.out.println(results);
            return results;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private String analyzeDocument(String documentUrl, String modelId) throws Exception {
        // Prepare the curl command for analysis
        String[] command = {
                "curl",
                "-v",
                "-i",
                "POST",
                String.format("%s/documentintelligence/documentModels/%s:analyze?api-version=%s", endpoint, modelId, api_version),
                "-H", "Content-Type: application/json",
                "-H", String.format("Ocp-Apim-Subscription-Key: %s", key),
                "--data-ascii", String.format("{'urlSource': '%s'}", documentUrl)
        };

        // Execute the command
        Process process = Runtime.getRuntime().exec(command);
        process.waitFor(30, TimeUnit.SECONDS);

        // Print the command being executed
        System.out.println("Executing command: " + String.join(" ", command));

        // Read and print the standard output (response body)
        BufferedReader stdReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        System.out.println("\nResponse body:");
        String stdLine;
        while ((stdLine = stdReader.readLine()) != null) {
            System.out.println(stdLine);
            // Check for operation-location in the response body
            if (stdLine.trim().startsWith("operation-location:")) {
                String fullUrl = stdLine.substring(stdLine.indexOf(":") + 1).trim();
                // Extract the analyzeResults ID from the full URL
                String[] parts = fullUrl.split("/");
                for (int i = 0; i < parts.length; i++) {
                    if (parts[i].equals("analyzeResults")) {
                        return parts[i + 1].split("\\?")[0]; // Get the ID and remove query parameters
                    }
                }
            }
        }

        // Read and print the error stream (headers and verbose output)
        BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        System.out.println("\nResponse headers and verbose output:");
        String line;
        String operationLocation = null;

        while ((line = errReader.readLine()) != null) {
            System.out.println(line);
            // Backup check in error stream for operation-location
            if (line.contains("operation-location:")) {
                String fullUrl = line.substring(line.indexOf(":") + 1).trim();
                String[] parts = fullUrl.split("/");
                for (int i = 0; i < parts.length; i++) {
                    if (parts[i].equals("analyzeResults")) {
                        operationLocation = parts[i + 1].split("\\?")[0];
                        break;
                    }
                }
            }
        }

        // Print the exit code
        System.out.println("\nProcess exit code: " + process.exitValue());

        return operationLocation;
    }

    private String getResults(String resultId, String modelId) throws Exception {
        // Prepare the curl command for getting results
        String[] command = {
                "curl",
                "-v",
                "-X", "GET",
                String.format("%s/documentintelligence/documentModels/%s/analyzeResults/%s?api-version=%s",
                        endpoint, modelId, resultId, api_version),
                "-H", String.format("Ocp-Apim-Subscription-Key: %s", key)
        };

        // Keep polling until we get a success status or hit max retries
        int maxRetries = 5;
        int retryCount = 0;
        String results = null;

        while (retryCount < maxRetries) {
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor(30, TimeUnit.SECONDS);

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            results = response.toString();

            // Check if analysis is complete
            if (results.contains("\"status\":\"succeeded\"")) {
                break;
            }

            // Wait before polling again
            Thread.sleep(1000);
            retryCount++;
            System.out.println("Retrieving results:" + results);
        }

        return results;
    }
}