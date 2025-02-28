//package com.eduva.eduva.service;
//
//import com.eduva.eduva.dto.ClaudeResponse.ClaudeQuestionInfo;
//import com.eduva.eduva.model.SubmissionData;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.*;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.*;
//
//@Service
//@Slf4j
//public class OpenRouterService {
//
//    @Value("${openrouter.api.key}")
//    private String apiKey;
//
//    @Value("${openrouter.api.url}")
//    private String apiUrl;
//
//    @Autowired
//    private RestTemplate restTemplate;
//
//    public ClaudeQuestionInfo openRouterDeepSeekV3QuestionGradingRequest(String studentResponse,
//                                                           ClaudeQuestionInfo question, SubmissionData submissionData, Integer questionIndex) {
//        try {
//            String questionPrompt = String.format("""
//            I want you to grade for the question %s from the student. Here are the steps you need to do:
//            1. First read the question context and question content and try to come up with a solution for this question for yourself
//            2. Find the corresponding rubric for question %s, read and understand the rubric for this question first and understand the grading and grading notes if any.
//            3. Grade the student's response by your understanding and checking the rubric and then grade, please think critically and don't give the point too easy!! For your final grading, please provide with a grade and very concise explanation consists for your grading.
//            Here is the context you should know:
//            %s
//            Here is the question content:
//            %s,
//            Here is the rubric:
//            %s, and the max grade/ best level you can give for this question is %s,
//
//            The student response is this: %s
//            If student didn't do this question, give none to the grade and say "no response" in explanation.
//            """,
//                    question.getQuestionId(),
//                    question.getQuestionId(),
//                    question.getPreContext(),
//                    question.getContent(),
//                    question.getRubric(),
//                    question.getMaxGrade(),
//                    studentResponse);
//            // Set up headers
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_JSON);
//            headers.set("Authorization", "Bearer " + apiKey);
//            headers.set("X-Title", "EDUVA");
//
//            // Create request body
//            Map<String, Object> requestBody = new HashMap<>();
//            requestBody.put("model", "deepseek/deepseek-chat");
//
//            Map<String, String> message = new HashMap<>();
//            message.put("role", "user");
//            message.put("content", questionPrompt);
//
//            List<Map<String, Object>> tools = createGradeTools();
//            requestBody.put("tools", tools);
//
//            requestBody.put("messages", Collections.singletonList(message));
//            requestBody.put("temperature", 0.7);
//            requestBody.put("max_tokens", 1000);
//
//            // Create http entity
//            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
//
//            // Send request and get response
//            ResponseEntity<String> response = restTemplate.exchange(
//                    apiUrl + "/chat/completions",
//                    HttpMethod.POST,
//                    entity,
//                    String.class
//            );
//
//            // Print raw response
//            log.info("Response status: {}", response.getStatusCode());
//            log.info("Response body: {}", response.getBody());
//
//        } catch (Exception e) {
//            log.error("Error calling OpenRouter API", e);
//        }
//        return null;
//
//    }
//
//    private List<Map<String, Object>> createGradeTools() {
//        List<Map<String, Object>> tools = new ArrayList<>();
//        Map<String, Object> tool = new HashMap<>();
//        tool.put("type", "function");
//
//        // Create function object
//        Map<String, Object> function = new HashMap<>();
//        function.put("name", "grading_tool");
//        function.put("description", "Grading the student response according to the rubric and your understanding, return the question_id, grade, and justification for your grading.");
//
//        // Create parameters object
//        Map<String, Object> parameters = new HashMap<>();
//        parameters.put("type", "object");
//
//        Map<String, Object> properties = new HashMap<>();
//
//        // Create questions property
//        Map<String, Object> questionsProps = new HashMap<>();
//        questionsProps.put("type", "array");
//
//        Map<String, Object> items = new HashMap<>();
//        items.put("type", "object");
//
//        Map<String, Object> itemProperties = new HashMap<>();
//
//        Map<String, Object> questionIdDef = new HashMap<>();
//        questionIdDef.put("type", "string");
//        questionIdDef.put("description", "A unique question id, e.g. '1', '2(a)', '3(1)' etc.");
//        itemProperties.put("question_id", questionIdDef);
//
//        Map<String, Object> gradeDef = new HashMap<>();
//        gradeDef.put("type", "string");
//        gradeDef.put("description", "A grade for this question, e.g. '10', '6.5', 'A', 'B-' etc.");
//        itemProperties.put("grade", gradeDef);
//
//        Map<String, Object> explanationDef = new HashMap<>();
//        explanationDef.put("type", "string");
//        explanationDef.put("description", "An justification for how the grade was assigned according to the rubric.");
//        itemProperties.put("explanation", explanationDef);
//
//        items.put("properties", itemProperties);
//        items.put("required", Arrays.asList("question_id", "grade", "explanation"));
//
//        questionsProps.put("items", items);
//
//        properties.put("questions", questionsProps);
//
//        parameters.put("properties", properties);
//        parameters.put("required", Arrays.asList("questions"));
//
//        // Add parameters to function
//        function.put("parameters", parameters);
//
//        // Add function to tool
//        tool.put("function", function);
//
//        tools.add(tool);
//        return tools;
//    }
//}

package com.eduva.eduva.service;

import com.eduva.eduva.dto.ClaudeResponse.ClaudeQuestionInfo;
import com.eduva.eduva.model.QuestionGrading;
import com.eduva.eduva.repository.QuestionGradingRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.eduva.eduva.model.SubmissionData;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.io.IOException;
import java.util.*;

@Service
@Slf4j
public class OpenRouterService {

    @Value("${openrouter.api.key}")
    private String apiKey;

    @Value("${openrouter.api.url}")
    private String apiUrl;

    @Value("${openrouter.gemini.api.url}")
    private String geminiApiUrl;


    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private QuestionGradingRepository questionGradingRepository;

    public static class UsageData {
        private int promptTokens;
        private int completionTokens;
        private int totalTokens;

        // Getters and setters
        public int getPromptTokens() { return promptTokens; }
        public void setPromptTokens(int promptTokens) { this.promptTokens = promptTokens; }

        public int getCompletionTokens() { return completionTokens; }
        public void setCompletionTokens(int completionTokens) { this.completionTokens = completionTokens; }

        public int getTotalTokens() { return totalTokens; }
        public void setTotalTokens(int totalTokens) { this.totalTokens = totalTokens; }

        @Override
        public String toString() {
            return "UsageData{" +
                    "promptTokens=" + promptTokens +
                    ", completionTokens=" + completionTokens +
                    ", totalTokens=" + totalTokens +
                    '}';
        }
    }

    // Add a class to represent the grading response
    public static class GradingResponse {
        private String question_id;
        private String grade;
        private String explanation;

        // Getters and setters
        public String getQuestion_id() { return question_id; }
        public void setQuestion_id(String question_id) { this.question_id = question_id; }

        public String getGrade() { return grade; }
        public void setGrade(String grade) { this.grade = grade; }

        public String getExplanation() { return explanation; }
        public void setExplanation(String explanation) { this.explanation = explanation; }

        @Override
        public String toString() {
            return "GradingResponse{" +
                    "question_id='" + question_id + '\'' +
                    ", grade='" + grade + '\'' +
                    ", explanation='" + explanation + '\'' +
                    '}';
        }
    }

    // Add a response wrapper class that includes both grading and usage data
    public static class DeepSeekResponse {
        private GradingResponse gradingResponse;
        private UsageData usageData;

        public GradingResponse getGradingResponse() { return gradingResponse; }
        public void setGradingResponse(GradingResponse gradingResponse) { this.gradingResponse = gradingResponse; }

        public UsageData getUsageData() { return usageData; }
        public void setUsageData(UsageData usageData) { this.usageData = usageData; }
    }

    public ClaudeQuestionInfo openRouterDeepSeekV3QuestionGradingRequest(String studentResponse,
                                                                         ClaudeQuestionInfo question, SubmissionData submissionData, Integer questionIndex) {
        try {
            String questionPrompt = String.format("""
            I want you to grade for the question %s from the student. Here are the steps you need to do:
            1. First read the question context and question content and try to come up with a solution for this question for yourself
            2. Find the corresponding rubric for question %s, read and understand the rubric for this question first and understand the grading and grading notes if any.
            3. Grade the student's response by your understanding and checking the rubric and then grade, please think critically and don't give the point too easy!! For your final grading, please provide with a grade and very concise explanation consists for your grading.
            
            Here is the context you should know:
            %s
            
            Here is the question content:
            %s,
            
            Here is the rubric:
            %s, and the max grade/ best level you can give for this question is %s,
            
            The student response is this: %s
            
            If student didn't do this question, give none to the grade and say "no response" in explanation.
            
            Please output your grading in this json format: 
            
            {
              "question_id": "the question id",
              "grade": "the grade you're giving",
              "explanation": "your justification for the grade based on the rubric"
            }
            """,
                    question.getQuestionId(),
                    question.getQuestionId(),
                    question.getPreContext(),
                    question.getContent(),
                    question.getRubric(),
                    question.getMaxGrade(),
                    studentResponse);

            // Set up headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("X-Title", "EDUVA");

            // Create request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "deepseek/deepseek-chat");


            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", questionPrompt);

            requestBody.put("messages", Collections.singletonList(message));
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 1000);

//            Map<String, Object> provider = new HashMap<>();
//            provider.put("order", List.of("Fireworks"));
//            provider.put("allow_fallbacks", false);
//            requestBody.put("provider", provider);


            // Add response_format parameter for JSON output
            Map<String, String> responseFormat = new HashMap<>();
            responseFormat.put("type", "json_object");
            requestBody.put("response_format", responseFormat);

            // Create http entity
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // Send request and get response
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl + "/chat/completions",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            // Print raw response
            log.info("Response status: {}", response.getStatusCode());
            log.info("Response body: {}", response.getBody());

            DeepSeekResponse deepSeekResponse = parseDeepSeekResponse(response.getBody());

            if (deepSeekResponse != null && deepSeekResponse.getGradingResponse() != null) {
                GradingResponse gradingResponse = deepSeekResponse.getGradingResponse();

                question.setGrade(gradingResponse.getGrade());
                question.setExplanation(gradingResponse.getExplanation());

                QuestionGrading grading = new QuestionGrading();
                grading.setSubmission(submissionData);
                grading.setAssignmentId(submissionData.getAssignment().getId());
                grading.setGrade(gradingResponse.getGrade());
                grading.setExplanation(gradingResponse.getExplanation());
                grading.setQuestionId(question.getQuestionId());
                grading.setQuestion_order(questionIndex);
                questionGradingRepository.save(grading);

                // Log the usage data
                if (deepSeekResponse.getUsageData() != null) {
                    log.info("Token usage: {}", deepSeekResponse.getUsageData());
                }
            }

        } catch (Exception e) {
            log.error("Error calling OpenRouter API", e);
        }

        return question;
    }
    private DeepSeekResponse parseDeepSeekResponse(String responseBody) {
        DeepSeekResponse deepSeekResponse = new DeepSeekResponse();

        try {
            // Parse the JSON response
            JsonNode rootNode = objectMapper.readTree(responseBody);

            // Extract usage data
            if (rootNode.has("usage")) {
                JsonNode usageNode = rootNode.get("usage");
                UsageData usageData = new UsageData();
                usageData.setPromptTokens(usageNode.get("prompt_tokens").asInt());
                usageData.setCompletionTokens(usageNode.get("completion_tokens").asInt());
                usageData.setTotalTokens(usageNode.get("total_tokens").asInt());
                deepSeekResponse.setUsageData(usageData);
            }

            // Extract grading response
            if (rootNode.has("choices") && rootNode.get("choices").isArray() && rootNode.get("choices").size() > 0) {
                JsonNode messageNode = rootNode.get("choices").get(0).get("message");
                if (messageNode != null && messageNode.has("content")) {
                    // Get the raw content as a string
                    String content = messageNode.get("content").asText();

                    // Check if content is wrapped in a code block (```json ... ```)
                    if (content.startsWith("```json")) {
                        content = content.replace("```json", "").replace("```", "").trim(); // Clean the JSON string
                    }

                    // If the content is a valid JSON string, parse it into GradingResponse
                    if (isValidJson(content)) {
                        GradingResponse gradingResponse = objectMapper.readValue(content, GradingResponse.class);
                        deepSeekResponse.setGradingResponse(gradingResponse);
                    } else {
                        // If the content is not valid JSON, store the raw string
                        GradingResponse gradingResponse = new GradingResponse();
                        gradingResponse.setExplanation(content); // Set the raw string as explanation
                        deepSeekResponse.setGradingResponse(gradingResponse);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error parsing DeepSeek response", e);
        }

        return deepSeekResponse;
    }

    // Helper method to check if a string is valid JSON
    private boolean isValidJson(String contentJson) {
        try {
            objectMapper.readTree(contentJson); // Try parsing it
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public List<ClaudeQuestionInfo> makeApiCallForFormatAnswer(List<Map<String, Object>> fileContents,
                                                               List<String> questionIds, SubmissionData submissionData) throws JsonProcessingException {
        String joinedQuestionIds = String.join(",", questionIds);

        // Create request body for OpenRouter/Gemini
        Map<String, Object> requestBody = createGeminiFormatAnswerRequestBody(fileContents, joinedQuestionIds);

        // Make API call to OpenRouter
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        HttpEntity<String> entity = new HttpEntity<>(
                objectMapper.writeValueAsString(requestBody),
                headers
        );

        System.out.println("ready to send gemini format");
        ResponseEntity<String> response = restTemplate.exchange(
                apiUrl + "/chat/completions",
                HttpMethod.POST,
                entity,
                String.class
        );
        System.out.println("get gemini format");

        // Process response from OpenRouter/Gemini
        String responseBody = response.getBody();
        System.out.println(responseBody);

//        OpenRouterResponseBody geminiResponseBody = objectMapper.readValue(responseBody, OpenRouterResponseBody.class);
//
//        // Save usage data if needed
//        if (geminiResponseBody.getUsage() != null) {
//            saveUsageData(geminiResponseBody.getUsage(), submissionData.getAssignment().getCourse().getTeacher().getId());
//        }

        // Extract and parse the JSON response containing question info
        return null;
    }

    private Map<String, Object> createGeminiFormatAnswerRequestBody(List<Map<String, Object>> fileContents, String joinedQuestionIds) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "google/gemini-2.0-flash-001");
        requestBody.put("max_tokens", 4096);

        List<Map<String, Object>> messages = new ArrayList<>();

        // System message to define the task
        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", "You are a helpful assistant that parses student responses into a structured format.");
        messages.add(systemMessage);

        // User message with file contents and instructions
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");

        // Create text content with instructions
        String prompt = String.format("""
        I will give you a file of student response and a list of question ids.
        You should return a json that contain each question id and the student response correspond to that specific question.
        You should decide whether the student response is pure text, or it contain images, figures or tables. If it is pure text, give true, otherwise give false.
        
        Note:
        Check the json output should contain all the question ids: %s
        Make sure that you include everything that the student written for each question id.
        Here is the list of question ids: %s
        """, joinedQuestionIds, joinedQuestionIds);

        // Create combined content with files and instructions
        List<Object> content = new ArrayList<>(fileContents);
        Map<String, Object> textContent = new HashMap<>();
        textContent.put("type", "text");
        textContent.put("text", prompt);
        content.add(textContent);

        userMessage.put("content", content);
        messages.add(userMessage);

        requestBody.put("messages", messages);

        // Add function calling capability
        requestBody.put("tools", createGeminiFormatAnswerTools());
        requestBody.put("tool_choice", createToolChoice());

        return requestBody;
    }

    private List<Map<String, Object>> createGeminiFormatAnswerTools() {
        List<Map<String, Object>> tools = new ArrayList<>();

        Map<String, Object> functionTool = new HashMap<>();
        functionTool.put("type", "function");

        Map<String, Object> function = new HashMap<>();
        function.put("name", "response_parsing_tool");
        function.put("description", "Parse the student responses and return a mapping of question IDs to their corresponding responses content");

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");

        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> questions = new HashMap<>();
        questions.put("type", "array");
        questions.put("description", "Array of question objects with ID, content, and type information");

        Map<String, Object> items = new HashMap<>();
        items.put("type", "object");

        Map<String, Object> itemProperties = new HashMap<>();

        Map<String, Object> questionId = new HashMap<>();
        questionId.put("type", "string");
        questionId.put("description", "A unique question id, e.g. '1', '2(a)', '3(1)' etc.");
        itemProperties.put("question_id", questionId);

        Map<String, Object> content = new HashMap<>();
        content.put("type", "string");
        content.put("description", "The student's written response for this question.");
        itemProperties.put("content", content);

        Map<String, Object> isPureText = new HashMap<>();
        isPureText.put("type", "boolean");
        isPureText.put("description", "Whether the student response is pure text (true) or it contains drawings, figures, or tables (false).");
        itemProperties.put("isPureText", isPureText);

        items.put("properties", itemProperties);
        items.put("required", Arrays.asList("question_id", "content", "isPureText"));

        questions.put("items", items);
        properties.put("questions", questions);

        parameters.put("properties", properties);
        parameters.put("required", Collections.singletonList("questions"));

        function.put("parameters", parameters);
        functionTool.put("function", function);

        tools.add(functionTool);
        return tools;
    }

    private Map<String, Object> createToolChoice() {
        Map<String, Object> toolChoice = new HashMap<>();
        toolChoice.put("type", "function");

        Map<String, Object> function = new HashMap<>();
        function.put("name", "response_parsing_tool");

        toolChoice.put("function", function);
        return toolChoice;
    }



}