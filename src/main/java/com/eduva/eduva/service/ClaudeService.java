package com.eduva.eduva.service;

import com.eduva.eduva.dto.ClaudeResponse.*;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

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
//    public List<ClaudeQuestionInfo> callWithFile(List<String> fileUrls, SubmissionData submissionData, String rubricContent) throws IOException {
//        List<ClaudeQuestionInfo> questions = objectMapper.readValue(
//                rubricContent,
//                objectMapper.getTypeFactory().constructCollectionType(List.class, ClaudeQuestionInfo.class)
//        );
//
//        // Process files once
//        List<Map<String, Object>> fileContents = processFiles(fileUrls);
//
//        // Create CompletableFuture for each question
//        List<CompletableFuture<ClaudeQuestionInfo>> futures = questions.stream()
//                .map(question -> CompletableFuture.supplyAsync(() -> {
//                    try {
//                        return makeApiCallForQuestion(fileContents, question, submissionData);
//                    } catch (IOException e) {
//                        throw new CompletionException(e);
//                    }
//                }))
//                .toList();
//
//        // Wait for all futures to complete and collect results in order
//        return futures.stream()
//                .map(CompletableFuture::join)
//                .collect(Collectors.toList());
//    }

    public List<ClaudeQuestionInfo> autoGrade(List<String> fileUrls, SubmissionData submissionData, String rubricContent) throws IOException {
        List<ClaudeQuestionInfo> questions = objectMapper.readValue(
                rubricContent,
                objectMapper.getTypeFactory().constructCollectionType(List.class, ClaudeQuestionInfo.class)
        );

        if (questions.isEmpty()) {
            return Collections.emptyList();
        }

        // Process files once
        List<Map<String, Object>> fileContents = processFiles(fileUrls);

        // Process first question synchronously
        ClaudeQuestionInfo firstQuestion = makeApiCallForQuestion(fileContents, questions.get(0), submissionData);

        // If there's only one question, return the result
        if (questions.size() == 1) {
            return List.of(firstQuestion);
        }

        // Process remaining questions in parallel
        List<CompletableFuture<ClaudeQuestionInfo>> futures = questions.subList(1, questions.size()).stream()
                .map(question -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return makeApiCallForQuestion(fileContents, question, submissionData);
                    } catch (IOException e) {
                        throw new CompletionException(e);
                    }
                }))
                .toList();

        // Combine first question with parallel results
        List<ClaudeQuestionInfo> results = new ArrayList<>();
        results.add(firstQuestion);
        results.addAll(futures.stream()
                .map(CompletableFuture::join)
                .toList());

        return results;
    }

    public List<String> extractQuestionIds(List<String> fileUrls, String prompt) throws IOException {
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
        requestBody.put("max_tokens", 4096);

        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", content);
        messages.add(message);

        requestBody.put("messages", messages);

        List<Map<String, Object>> tools = new ArrayList<>();

        // Modified tool definition for question ID objects
        Map<String, Object> tool = new HashMap<>();
        tool.put("name", "question_id_extract");
        tool.put("description", "Extract all question IDs from the document as objects");

        // Define modified input schema for objects
        Map<String, Object> inputSchema = new HashMap<>();
        inputSchema.put("type", "object");

        Map<String, Object> properties = new HashMap<>();

        // Define question_ids array of objects
        Map<String, Object> questionIdsProps = new HashMap<>();
        questionIdsProps.put("type", "array");

        // Define the object structure
        Map<String, Object> itemProperties = new HashMap<>();
        Map<String, Object> questionIdProp = new HashMap<>();
        questionIdProp.put("type", "string");
        questionIdProp.put("description", "A unique question id, e.g. '1', '2(a)', '3(1)' etc.");

        itemProperties.put("question_id", questionIdProp);

        Map<String, Object> itemSchema = new HashMap<>();
        itemSchema.put("type", "object");
        itemSchema.put("properties", itemProperties);
        itemSchema.put("required", List.of("question_id"));

        questionIdsProps.put("items", itemSchema);

        properties.put("questions", questionIdsProps);
        inputSchema.put("properties", properties);
        inputSchema.put("required", List.of("questions"));

        tool.put("input_schema", inputSchema);
        tools.add(tool);
        requestBody.put("tools", tools);

        // Return response from API call
        return makeQuestionIdApiCall(requestBody);
    }

    public List<ClaudeQuestionInfo> formatQuestions(List<String> fileUrls, String prompt) throws IOException {

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
        requestBody.put("max_tokens", 4096);

        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", content);
        messages.add(message);

        requestBody.put("messages", messages);
        List<Map<String, Object>> tools = new ArrayList<>();

        // Tool definition (grading tool for the new functionality)
        Map<String, Object> tool = new HashMap<>();
        tool.put("name", "question_format");
        tool.put("description", "For each question, format the question in a clear way including the question id, the question content, the question rubric");

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

        Map<String, Object> maxGrade = new HashMap<>();
        rubricDef.put("type", "string");
        rubricDef.put("description", "The best grade or the full points that this question can give, e.g. 'A', 'B+', '3 points', '1 points', etc.");
        questionItemProperties.put("maxGrade", maxGrade);

        Map<String, Object> preContext = new HashMap<>();
        preContext.put("type", "string");
        preContext.put("description", "The context for the question.");

        questionItemProperties.put("preContext", preContext);


        // Define the required fields inside each question object
        questionItem.put("properties", questionItemProperties);
        questionItem.put("required", List.of("question_id", "content", "rubric", "maxGrade", "preContext"));

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

    public List<ClaudeQuestionInfo> formatQuestionsByAutoGenerateRubrics(List<String> fileUrls, List<String> questionIds) throws IOException {
        if (questionIds.isEmpty()) {
            return Collections.emptyList();
        }


        List<ClaudeQuestionInfo> formattedQuestions = new ArrayList<>();

        List<ClaudeQuestionInfo> questionResult = sendRequestForRubricGeneration(fileUrls, questionIds.get(0));
        if (!questionResult.isEmpty()) {
            formattedQuestions.add(questionResult.get(0));
        }

        // Create a list of CompletableFutures for the remaining questions
        List<CompletableFuture<ClaudeQuestionInfo>> futures = questionIds.subList(1, questionIds.size()).stream()
                .map(questionId -> CompletableFuture.supplyAsync(() -> {
                    try {
                        // Send request asynchronously for each question
                        List<ClaudeQuestionInfo> result = sendRequestForRubricGeneration(fileUrls, questionId);
                        return result.isEmpty() ? null : result.get(0); // Return the first result or null
                    } catch (IOException e) {
                        throw new CompletionException(e); // Wrap exception in CompletionException
                    }
                }))
                .toList();

        // Wait for all futures to complete and collect results
        futures.stream()
                .map(CompletableFuture::join) // Join each future to get the result
                .filter(Objects::nonNull) // Filter out any null results
                .forEach(formattedQuestions::add);

        return formattedQuestions;
    }

    private List<ClaudeQuestionInfo> sendRequestForRubricGeneration(List<String> fileUrls, String questionId) throws IOException {

        String prompt = String.format("I will give you a file of problem set and an question id. Please generate a grading rubric for question %s. Here are the steps you need to do:\n" +
                "1. First read this problem and come up with a solution for yourself for this question id.\n" +
                "2. Based on what you understand about this question, create a grading rubric that specifies the grading criteria, point allocation, and any other relevant notes or considerations for this question.\n" +
                "3. Return a json that contain the\n" +
                    "(1). question id, " +
                    "(2). question content" +
                        "(4). The max grade possible for this question, like ‘2 points’, ‘3 points’ or ‘A’, ‘B+’ "+
                    "(3). The rubric content, which contain the main grading criteria like how the grade should be assigned to each question",questionId);
        List<Map<String, Object>> content = new ArrayList<>();

        // Process files once and prepare them for API call
        List<Map<String, Object>> fileContents = processFiles(fileUrls);
        content.addAll(fileContents);

        // Add the rubric generation prompt
        Map<String, Object> textContent = new HashMap<>();
        textContent.put("type", "text");
        textContent.put("text", prompt);
        content.add(textContent);

        // Create request body for the API call
        Map<String, Object> requestBody = createRubricGeneratingRequestBody(content);

        // Make the API call to generate the rubric for the given question
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);

        // Send POST request to the API
        ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);

        // Process the API response
        String responseBody = response.getBody();
        System.out.println(responseBody);
        ClaudeResponseBody claudeResponseBody = objectMapper.readValue(responseBody, ClaudeResponseBody.class);

        List<ClaudeQuestionInfo> questions = new ArrayList<>();

        // Process content items and extract generated rubrics
        for (ClaudeContentItem item : claudeResponseBody.getContent()) {
            if ("tool_use".equals(item.getType()) && item.getInput() != null) {
                questions = item.getInput().getQuestions();
            }
        }

        return questions;
    }


    private List<ClaudeQuestionInfo> makeFileReformatApiCall(Map<String, Object> requestBody) throws IOException {
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

        ClaudeResponseBody claudeResponseBody = objectMapper.readValue(responseBody, ClaudeResponseBody.class);
        System.out.println(responseBody);

        List<ClaudeQuestionInfo> questions = List.of();
        // Process content items and extract questions
        for (ClaudeContentItem item : claudeResponseBody.getContent()) {
            if ("tool_use".equals(item.getType()) && item.getInput() != null) {
                // Convert and filter questions to grading items
                questions = item.getInput().getQuestions();
            }
        }
        return questions;
    }

    // Modified API call method
    private List<String> makeQuestionIdApiCall(Map<String, Object> requestBody) throws IOException {
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

        ClaudeResponseBody claudeResponseBody = objectMapper.readValue(responseBody, ClaudeResponseBody.class);
        System.out.println(responseBody);

        List<String> questionIds = new ArrayList<>();

        // Process content items and extract question IDs
        for (ClaudeContentItem item : claudeResponseBody.getContent()) {
            if ("tool_use".equals(item.getType()) && item.getInput() != null) {
                List<ClaudeQuestionInfo> questions = item.getInput().getQuestions();
                questionIds = questions.stream()
                        .map(ClaudeQuestionInfo::getQuestionId)
                        .toList();
            }
        }
        System.out.println(questionIds);
        System.out.println(questionIds.size());
        System.out.println("hhh");
        return questionIds;
    }

    private List<Map<String, Object>> processFiles(List<String> fileUrls) throws IOException {
        List<Map<String, Object>> fileContents = new ArrayList<>();
        for (String fileUrl : fileUrls) {
            byte[] fileBytes = downloadFile(fileUrl);
            String base64File = Base64.getEncoder().encodeToString(fileBytes);

            Map<String, Object> fileContent = new HashMap<>();
            fileContent.put("type", "document");

            Map<String, Object> source = new HashMap<>();
            source.put("type", "base64");
            source.put("media_type", determineMediaType(fileUrl));
            source.put("data", base64File);

            Map<String, Object> cacheControl = new HashMap<>();
            cacheControl.put("type", "ephemeral");
            fileContent.put("cache_control", cacheControl);

            fileContent.put("source", source);
            fileContents.add(fileContent);
        }
        return fileContents;
    }

    private ClaudeQuestionInfo makeApiCallForQuestion(List<Map<String, Object>> fileContents,
                                                      ClaudeQuestionInfo question, SubmissionData submissionData) throws IOException {

        List<Map<String, Object>> content = new ArrayList<>(fileContents);

        // Add text prompt with question-specific information
        Map<String, Object> textContent = new HashMap<>();
        textContent.put("type", "text");
        String questionPrompt = String.format("""
            I want you to grade for the question %s from the student. Here are the steps you need to do:
            1. First try to come up with a solution for this question for yourself
            2. Find the corresponding rubric for question %s, read and understand the rubric for this question first and understand the grading and grading notes if any.
            3. Grade the student's response by your understanding and checking the rubric and then grade, please think critically and don't give the point too easy!! For your final grading, please provide with a grade and valid explanation for your grade based on the rubric.
           
            Here is the rubric:
            %s
            
            """,
                question.getQuestionId(),
                question.getQuestionId(),
                question.getRubric());
        textContent.put("text", questionPrompt);
        content.add(textContent);
        // Create request body
        Map<String, Object> requestBody = createRequestBody(content);

        // Make API call
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

        // Process response
        String responseBody = response.getBody();
        System.out.println("1");
        System.out.println(responseBody);
        ClaudeResponseBody claudeResponseBody = objectMapper.readValue(responseBody, ClaudeResponseBody.class);

        // Process content items and extract question info
        for (ClaudeContentItem item : claudeResponseBody.getContent()) {
            if ("tool_use".equals(item.getType()) && item.getInput() != null) {
                ClaudeQuestionInfo gradedQuestion = item.getInput().getQuestions().get(0);

                // Save question grading
                QuestionGrading grading = new QuestionGrading();
                grading.setSubmission(submissionData);
                grading.setAssignmentId(submissionData.getAssignment().getId());
                grading.setGrade(gradedQuestion.getGrade());
                grading.setExplanation(gradedQuestion.getExplanation());
                grading.setQuestionId(gradedQuestion.getQuestionId());
                questionGradingRepository.save(grading);
                // Save usage data
                saveUsageData(claudeResponseBody.getUsage(), submissionData);

                return gradedQuestion;
            }
        }

        throw new IOException("No valid response received for question: " + question.getQuestionId());
    }

    private List<Map<String, Object>> createRubricGeneratingTools() {
        List<Map<String, Object>> tools = new ArrayList<>();
        Map<String, Object> tool = new HashMap<>();
        tool.put("name", "rubric_generating_tool");
        tool.put("description", "Generate a rubric for a specific question");

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

        Map<String, Object> maxGrade = new HashMap<>();
        rubricDef.put("type", "string");
        rubricDef.put("description", "The best grade or the full points that this question can give, e.g. 'A', 'B+', '3 points', '1 points', etc.");
        questionItemProperties.put("maxGrade", maxGrade);

        Map<String, Object> preContext = new HashMap<>();
        preContext.put("type", "string");
        preContext.put("description", "The context for the question.");

        questionItemProperties.put("preContext", preContext);

        // Define the required fields inside each question object
        questionItem.put("properties", questionItemProperties);
        questionItem.put("required", List.of("question_id", "content", "rubric", "maxGrade", "preContext"));

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
        return tools;
    }
    private List<Map<String, Object>> createTools() {
        List<Map<String, Object>> tools = new ArrayList<>();
        Map<String, Object> tool = new HashMap<>();
        tool.put("name", "grading_tool");
        tool.put("description", "Grading the student response according to the rubric and your understanding, return the question_id, grade, and justification for your grading.");

        Map<String, Object> inputSchema = new HashMap<>();
        inputSchema.put("type", "object");

        Map<String, Object> questionsProps = new HashMap<>();
        questionsProps.put("type", "array");

        Map<String, Object> questionItem = new HashMap<>();
        questionItem.put("type", "object");

        Map<String, Object> questionItemProperties = new HashMap<>();

        Map<String, Object> questionIdDef = new HashMap<>();
        questionIdDef.put("type", "string");
        questionIdDef.put("description", "A unique question id, e.g. '1', '2(a)', '3(1)' etc.");
        questionItemProperties.put("question_id", questionIdDef);

        Map<String, Object> gradeDef = new HashMap<>();
        gradeDef.put("type", "string");
        gradeDef.put("description", "A grade for this question, e.g. '10', '6.5', 'A', 'B-' etc.");
        questionItemProperties.put("grade", gradeDef);

        Map<String, Object> justificationDef = new HashMap<>();
        justificationDef.put("type", "string");
        justificationDef.put("description", "An justification for how the grade was assigned according to the rubric.");
        questionItemProperties.put("explanation", justificationDef);

        questionItem.put("properties", questionItemProperties);
        questionItem.put("required", List.of("question_id", "grade", "explanation"));

        questionsProps.put("items", questionItem);

        Map<String, Object> topLevelProperties = new HashMap<>();
        topLevelProperties.put("questions", questionsProps);

        inputSchema.put("properties", topLevelProperties);
        inputSchema.put("required", List.of("questions"));
        tool.put("input_schema", inputSchema);

        tools.add(tool);
        return tools;
    }

    private void saveUsageData(ClaudeUsage claudeUsage, SubmissionData submissionData) {
        AnthropicUsage anthropicUsage = new AnthropicUsage();
        anthropicUsage.setCourseId(submissionData.getAssignment().getCourse().getId());
        anthropicUsage.setCacheReadInputTokens(claudeUsage.getCache_read_input_tokens());
        anthropicUsage.setInputTokens(claudeUsage.getInput_tokens());
        anthropicUsage.setOutputTokens(claudeUsage.getOutput_tokens());
        anthropicUsage.setCacheCreationInputTokens(claudeUsage.getCache_creation_input_tokens());
        antropicUsageRepository.save(anthropicUsage);
    }

    private Map<String, Object> createRequestBody(List<Map<String, Object>> content) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "claude-3-5-sonnet-20241022");
        requestBody.put("max_tokens", 1024);

        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", content);
        messages.add(message);
        requestBody.put("messages", messages);

        requestBody.put("tools", createTools());

        return requestBody;
    }

    private Map<String, Object> createRubricGeneratingRequestBody(List<Map<String, Object>> content) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "claude-3-5-sonnet-20241022");
        requestBody.put("max_tokens", 1024);

        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", content);
        messages.add(message);
        requestBody.put("messages", messages);

        requestBody.put("tools", createRubricGeneratingTools());
        return requestBody;
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
