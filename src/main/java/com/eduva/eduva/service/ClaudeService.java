package com.eduva.eduva.service;

import com.eduva.eduva.dto.ClaudeResponse.ClaudeContentItem;
import com.eduva.eduva.dto.ClaudeResponse.ClaudeQuestionItem;
import com.eduva.eduva.dto.ClaudeResponse.ClaudeResponseBody;
import com.eduva.eduva.dto.ClaudeResponse.ClaudeUsage;
import com.eduva.eduva.model.AnthropicUsage;
import com.eduva.eduva.model.QuestionGrading;
import com.eduva.eduva.model.SubmissionData;
import com.eduva.eduva.repository.AntropicUsageRepository;
import com.eduva.eduva.repository.QuestionGradingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.*;

@Service
@Slf4j
public class ClaudeService {

    @Autowired
    private QuestionGradingRepository questionGradingRepository;

    @Value("${anthropic.api.key}")
    private String apiKey;

    @Value("${anthropic.api.url}")
    private String apiUrl;

    private RestTemplate restTemplate;

    private ObjectMapper objectMapper;
    @Autowired
    private AntropicUsageRepository antropicUsageRepository;

    public ClaudeService(RestTemplate restTemplate, ObjectMapper objectMapper, QuestionGradingRepository questionGradingRepository) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.questionGradingRepository = questionGradingRepository;
    }

    /**
     * Makes an API call to Claude with a file attachment
     */
    public List<ClaudeQuestionItem> callWithFile(List<String> fileUrls, String prompt, SubmissionData submissionData) throws IOException {

        List<Map<String, Object>> content = new ArrayList<>();
        // Download and encode file
        // Process each file
        for (String fileUrl : fileUrls) {
            // Download and encode file
            byte[] fileBytes = downloadFile(fileUrl);
            String base64File = Base64.getEncoder().encodeToString(fileBytes);

            // Add file content
            Map<String, Object> fileContent = new HashMap<>();
            fileContent.put("type", "document");

            Map<String, Object> source = new HashMap<>();
            source.put("type", "base64");
            source.put("media_type", determineMediaType(fileUrl));
            source.put("data", base64File);

            // Add cache control
            Map<String, Object> cacheControl = new HashMap<>();
            cacheControl.put("type", "ephemeral");
            fileContent.put("cache_control", cacheControl);

            fileContent.put("source", source);
            content.add(fileContent);
        }

        // Add text prompt
        Map<String, Object> textContent = new HashMap<>();
        textContent.put("type", "text");
        textContent.put("text", prompt);
        content.add(textContent);

        // Create request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "claude-3-5-sonnet-20241022");
        requestBody.put("max_tokens", 1024);

        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", content);
        messages.add(message);

        requestBody.put("messages", messages);


        List<Map<String, Object>> tools = new ArrayList<>();

        // Example tool definition (you can customize the schema as needed):
        Map<String, Object> tool = new HashMap<>();
        tool.put("name", "grading_tool");
        tool.put("description", "For each question, grading the student response according to the rubric and your understanding, return the question_id, grade, and justification for your grading.");

        // The input_schema requires an object with a "questions" array
        Map<String, Object> inputSchema = new HashMap<>();
        inputSchema.put("type", "object");

        // The "questions" array items
        Map<String, Object> questionsProps = new HashMap<>();
        questionsProps.put("type", "array");

        // Each item in the questions array is an object
        Map<String, Object> questionItem = new HashMap<>();
        questionItem.put("type", "object");

        // question_id, grade, explanation properties
        Map<String, Object> questionItemProperties = new HashMap<>();

        // question_id
        Map<String, Object> questionIdDef = new HashMap<>();
        questionIdDef.put("type", "string");
        questionIdDef.put("description", "A unique question id, e.g. '1', '2(a)', '3(1)' etc.");
        questionItemProperties.put("question_id", questionIdDef);

        // grade
        Map<String, Object> gradeDef = new HashMap<>();
        gradeDef.put("type", "string");
        gradeDef.put("description", "A grade for this question, e.g. '10', '6.5', 'A', 'B-' etc.");
        questionItemProperties.put("grade", gradeDef);

        // explanation
        Map<String, Object> justificationDef = new HashMap<>();
        justificationDef.put("type", "string");
        justificationDef.put("description", "An justification for how the grade was assigned according to the rubric.");
        questionItemProperties.put("explanation", justificationDef);

        // Define the required fields inside each question object
        questionItem.put("properties", questionItemProperties);
        questionItem.put("required", List.of("question_id", "grade", "explanation"));

        // The questions array itself
        questionsProps.put("items", questionItem);

        // "questions" top-level property
        Map<String, Object> topLevelProperties = new HashMap<>();
        topLevelProperties.put("questions", questionsProps);

        // Add them to inputSchema
        inputSchema.put("properties", topLevelProperties);
        inputSchema.put("required", List.of("questions"));
        tool.put("input_schema", inputSchema);

        // Add the tool to the tools array
        tools.add(tool);

        requestBody.put("tools", tools);
        return makeApiCall(requestBody, submissionData);
    }

    public String formatQuestions(List<String> fileUrls, String prompt) throws IOException {

        List<Map<String, Object>> content = new ArrayList<>();

        // Download and encode file
        for (String fileUrl : fileUrls) {
            byte[] fileBytes = downloadFile(fileUrl);
            String base64File = Base64.getEncoder().encodeToString(fileBytes);

            Map<String, Object> fileContent = new HashMap<>();
            fileContent.put("type", "document");

            Map<String, Object> source = new HashMap<>();
            source.put("type", "base64");
            source.put("media_type", determineMediaType(fileUrl));
            source.put("data", base64File);

            fileContent.put("source", source);
            content.add(fileContent);
        }

        // Add text prompt
        Map<String, Object> textContent = new HashMap<>();
        textContent.put("type", "text");
        textContent.put("text", prompt);
        content.add(textContent);

        // Create request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "claude-3-5-sonnet-20241022");
        requestBody.put("max_tokens", 1024);

        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", content);
        messages.add(message);

        requestBody.put("messages", messages);

        List<Map<String, Object>> tools = new ArrayList<>();

        // Tool definition (grading tool for the new functionality)
        Map<String, Object> tool = new HashMap<>();
        tool.put("name", "grading_tool");
        tool.put("description", "For each question, grading the student response according to the rubric, returning the question_id, grade, and justification.");

        // Define input schema for grading tool
        Map<String, Object> inputSchema = new HashMap<>();
        inputSchema.put("type", "object");

        Map<String, Object> questionsProps = new HashMap<>();
        questionsProps.put("type", "array");

        // Each question is represented as an object
        Map<String, Object> questionItem = new HashMap<>();
        questionItem.put("type", "object");

        // Add properties for question_id, content, and rubric
        Map<String, Object> questionItemProperties = new HashMap<>();

        // question_id
        Map<String, Object> questionIdDef = new HashMap<>();
        questionIdDef.put("type", "string");
        questionIdDef.put("description", "A unique question id, e.g. '1', '2(a)', '3(1)' etc.");
        questionItemProperties.put("question_id", questionIdDef);

        // content
        Map<String, Object> contentDef = new HashMap<>();
        contentDef.put("type", "string");
        contentDef.put("description", "The content or the text of the question.");
        questionItemProperties.put("content", contentDef);

        // rubric
        Map<String, Object> rubricDef = new HashMap<>();
        rubricDef.put("type", "string");
        rubricDef.put("description", "The grading rubric for the question, outlining how grades are assigned.");
        questionItemProperties.put("rubric", rubricDef);

        // Define the required fields inside each question object
        questionItem.put("properties", questionItemProperties);
        questionItem.put("required", List.of("question_id", "content", "rubric"));

        // The questions array itself
        questionsProps.put("items", questionItem);

        // "questions" top-level property
        Map<String, Object> topLevelProperties = new HashMap<>();
        topLevelProperties.put("questions", questionsProps);

        // Add to inputSchema
        inputSchema.put("properties", topLevelProperties);
        inputSchema.put("required", List.of("questions"));
        tool.put("input_schema", inputSchema);

        tools.add(tool);
        requestBody.put("tools", tools);

        // Return response from API call
        return makeFileReformatApiCall(requestBody);
    }


    private List<ClaudeQuestionItem> makeApiCall(Map<String, Object> requestBody, SubmissionData submissionData) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        HttpEntity<String> entity = new HttpEntity<>(
                objectMapper.writeValueAsString(requestBody),
                headers
        );

        ResponseEntity<String> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                entity,
                String.class
        );
        String responseBody = response.getBody();
        // Store the questions and usage
        ClaudeResponseBody claudeResponseBody = objectMapper.readValue(responseBody, ClaudeResponseBody.class);
        List<ClaudeContentItem> contentItems = claudeResponseBody.getContent();
        List<QuestionGrading> questionGradings = new ArrayList<>();

        List<ClaudeQuestionItem> questions = List.of();
        for (ClaudeContentItem item : contentItems) {
            if ("tool_use".equals(item.getType()) && item.getInput() != null) {
                questions = item.getInput().getQuestions();
                for (ClaudeQuestionItem question : questions) {
                    QuestionGrading questionGrading = new QuestionGrading();
                    questionGrading.setSubmission(submissionData);
                    questionGrading.setAssignmentId(submissionData.getAssignment().getId());
                    questionGrading.setGrade(question.getGrade());
                    questionGrading.setExplanation(question.getExplanation());
                    questionGrading.setQuestionId(question.getQuestionId());
                    questionGradings.add(questionGrading);
                }
            }
        }
        questionGradingRepository.saveAll(questionGradings);

        ClaudeUsage claudeUsage = claudeResponseBody.getUsage();
        AnthropicUsage anthropicUsage = new AnthropicUsage();
        anthropicUsage.setCourseId(submissionData.getAssignment().getCourse().getId());
        anthropicUsage.setCacheReadInputTokens(claudeUsage.getCache_read_input_tokens());
        anthropicUsage.setInputTokens(claudeUsage.getInput_tokens());
        anthropicUsage.setOutputTokens(claudeUsage.getOutput_tokens());
        anthropicUsage.setCacheCreationInputTokens(claudeUsage.getCache_creation_input_tokens());
        antropicUsageRepository.save(anthropicUsage);

        return questions;
    }

    private String makeFileReformatApiCall(Map<String, Object> requestBody) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        HttpEntity<String> entity = new HttpEntity<>(
                objectMapper.writeValueAsString(requestBody),
                headers
        );

        ResponseEntity<String> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                entity,
                String.class
        );

        System.out.println(response.getBody());
        return response.getBody();
    }

    private byte[] downloadFile(String fileUrl) throws IOException {
//        ResponseEntity<byte[]> response = restTemplate.getForEntity(fileUrl, byte[].class);
        URI uri = UriComponentsBuilder.fromHttpUrl(fileUrl)
                .build(true)  // 'true' = don't re-encode the parameters
                .toUri();

        System.out.println("Download URI: " + uri);

        ResponseEntity<byte[]> response = restTemplate.getForEntity(uri, byte[].class);
        return response.getBody();
    }

    private String determineMediaType(String fileUrl) {
        String strippedUrl = fileUrl.split("\\?")[0];  // Everything before '?'
        String extension = strippedUrl.substring(strippedUrl.lastIndexOf(".") + 1).toLowerCase();
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "txt" -> "text/plain";
            case "json" -> "application/json";
            default -> throw new UnsupportedOperationException("Unsupported file type: " + extension);
        };
    }
}
