package com.eduva.eduva.service;

import com.eduva.eduva.dto.ClaudeResponse.*;
import com.eduva.eduva.dto.QuestionIdsWithCacheFiles;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Slf4j
public class ClaudeService {

    @Autowired
    private QuestionGradingRepository questionGradingRepository;

    @Value("${anthropic.api.key}")
    private String apiKey;

    @Value("${anthropic.api.url}")
    private String apiUrl;
    
    @Value("${anthropic.model.name}")
    private String anthropicModel;

    private RestTemplate restTemplate;

    private ObjectMapper objectMapper;
    @Autowired
    private AntropicUsageRepository antropicUsageRepository;

    @Autowired
    private OpenRouterService openRouterService;
    @Autowired
    private FileStorageService fileStorageService;

    public ClaudeService(RestTemplate restTemplate, ObjectMapper objectMapper, QuestionGradingRepository questionGradingRepository) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.questionGradingRepository = questionGradingRepository;
    }



//    public List<ClaudeQuestionInfo> autoGrade(List<String> fileUrls, SubmissionData submissionData, String rubricContent) throws IOException {
//
//        questionGradingRepository.deleteBySubmission_Id(submissionData.getId());
//
//        List<ClaudeQuestionInfo> questions = objectMapper.readValue(
//                rubricContent,
//                objectMapper.getTypeFactory().constructCollectionType(List.class, ClaudeQuestionInfo.class)
//        );
//
//        List<String> questionIds = questions.stream()
//                .map(ClaudeQuestionInfo::getQuestionId)
//                .toList();
//
//        Map<String, Integer> questionIdToIndex = IntStream.range(0, questionIds.size())
//                .boxed()
//                .collect(Collectors.toMap(
//                        i -> questionIds.get(i),
//                        i -> i
//                ));
//
//        if (questions.isEmpty()) {
//            return Collections.emptyList();
//        }
//
//        // Process files once
//        List<Map<String, Object>> fileContents = processFiles(fileUrls);
//
//        // Create a map to hold results in the correct order
//        Map<String, ClaudeQuestionInfo> resultMap = new ConcurrentHashMap<>();
//
//        // Track processed questions
//        Set<String> processedQuestionIds = Collections.synchronizedSet(new HashSet<>());
//
//        // Process first question synchronously
//        try {
//            ClaudeQuestionInfo firstQuestion = makeApiCallForGradeQuestion(fileContents, questions.get(0), submissionData, questionIdToIndex.get(questions.get(0).getQuestionId()));
//            resultMap.put(questionIds.get(0), firstQuestion);
//            processedQuestionIds.add(questionIds.get(0));
//        } catch (HttpClientErrorException ex) {
//            handleRateLimit(ex);
//            // Retry after rate limit
//            ClaudeQuestionInfo firstQuestion = makeApiCallForGradeQuestion(fileContents, questions.get(0), submissionData, questionIdToIndex.get(questions.get(0).getQuestionId()));
//            resultMap.put(questionIds.get(0), firstQuestion);
//            processedQuestionIds.add(questionIds.get(0));
//        }
//
//        // If there's only one question, return the result
//        if (questions.size() == 1) {
//            return List.of(resultMap.get(questionIds.get(0)));
//        }
//
//        // Create a thread pool for remaining questions
//        ExecutorService executor = Executors.newFixedThreadPool(Math.min(10, questions.size()));
//
//        List<CompletableFuture<Void>> futures = new ArrayList<>();
//
//        for (ClaudeQuestionInfo question : questions.subList(1, questions.size())) {
//            futures.add(CompletableFuture.runAsync(() -> {
//                while (true) {
//                    try {
//                        String questionId = question.getQuestionId();
//                        if (!processedQuestionIds.contains(questionId)) {
//                            ClaudeQuestionInfo result = makeApiCallForGradeQuestion(fileContents, question, submissionData, questionIdToIndex.get(questionId));
//                            resultMap.put(questionId, result);
//                            processedQuestionIds.add(questionId);
//                            return;
//                        }
//                    } catch (HttpClientErrorException ex) {
//                        if (ex.getStatusCode().value() == 429) {
//                            try {
//                                handleRateLimit(ex);
//                            } catch (IOException e) {
//                                throw new RuntimeException(e);
//                            }
//                        } else {
//                            throw new CompletionException(ex);
//                        }
//                    } catch (IOException e) {
//                        throw new CompletionException(e);
//                    }
//                }
//            }, executor));
//        }
//
//        // Wait for all futures to complete
//        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
//
//        // Convert map to ordered list based on original questionIds order
//        List<ClaudeQuestionInfo> orderedResults = new ArrayList<>();
//        for (String questionId : questionIds) {
//            orderedResults.add(resultMap.get(questionId));
//        }
//
//        // Shutdown the executor
//        executor.shutdown();
//        try {
//            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
//                executor.shutdownNow();
//            }
//        } catch (InterruptedException e) {
//            executor.shutdownNow();
//            Thread.currentThread().interrupt();
//        }
//
//        return orderedResults;
//    }



    public List<ClaudeQuestionInfo> autoGrade(List<String> fileUrls, SubmissionData submissionData, String rubricContent) throws IOException, InterruptedException {

        questionGradingRepository.deleteBySubmission_Id(submissionData.getId());

        List<ClaudeQuestionInfo> questions = objectMapper.readValue(
                rubricContent,
                objectMapper.getTypeFactory().constructCollectionType(List.class, ClaudeQuestionInfo.class)
        );

        List<String> questionIds = questions.stream()
                .map(ClaudeQuestionInfo::getQuestionId)
                .toList();

        Map<String, Integer> questionIdToIndex = IntStream.range(0, questionIds.size())
                .boxed()
                .collect(Collectors.toMap(
                        i -> questionIds.get(i),
                        i -> i
                ));

        if (questions.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, ClaudeQuestionInfo> resultMap = new ConcurrentHashMap<>();


        List<Map<String, Object>> fileContentsWithCache = processFiles(fileUrls);
        List<Map<String, Object>> fileContentsWithoutCache = processFilesWithoutCache(fileUrls);
        List<ClaudeQuestionInfo> claudeAnswerFormatInfoList = makeApiCallForFormatAnswer(fileContentsWithCache, questionIds, submissionData);
//        List<ClaudeQuestionInfo> claudeAnswerFormatInfoList = openRouterService.makeApiCallForFormatAnswer(fileContentsWithoutCache, questionIds, submissionData);
        String jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(claudeAnswerFormatInfoList);
        System.out.println(jsonString);


        Map<String, ClaudeQuestionInfo> questionMap = questions.stream()
                .collect(Collectors.toMap(
                        ClaudeQuestionInfo::getQuestionId,  // key mapper
                        question -> question                 // value mapper
                ));

        Map<String, ClaudeQuestionInfo> answerMap = claudeAnswerFormatInfoList.stream()
                .collect(Collectors.toMap(
                ClaudeQuestionInfo::getQuestionId,  // key mapper
                question -> question                 // value mapper
        ));

        List<String> questionWithPureTextIds = new ArrayList<>();
        List<String> questionWithoutPureTextIds = new ArrayList<>();
        for (String questionId : questionIds) {
            ClaudeQuestionInfo question = questionMap.get(questionId);
            if (question.getIsPureText()) {
                questionWithPureTextIds.add(questionId);
            } else {
                questionWithoutPureTextIds.add(questionId);
            }
        }

        ExecutorService executorService = Executors.newFixedThreadPool(10);  // Choose thread pool size based on the expected load
        List<Callable<ClaudeQuestionInfo>> tasks = new ArrayList<>();

        // Handle questionWithPureTextIds concurrently, one task per question
        for (String questionId : questionWithPureTextIds) {
            tasks.add(() -> {
                ClaudeQuestionInfo question = questionMap.get(questionId);
                ClaudeQuestionInfo answer = answerMap.get(questionId);
                ClaudeQuestionInfo result = openRouterService.openRouterDeepSeekV3QuestionGradingRequest(answer.getContent(), question, submissionData, questionIdToIndex.get(questionId));
                resultMap.put(questionId, result);
                return result;
            });
        }

        // Handle questionWithoutPureTextIds concurrently, one task per question
        for (String questionId : questionWithoutPureTextIds) {
            tasks.add(() -> {
                ClaudeQuestionInfo question = questionMap.get(questionId);
                ClaudeQuestionInfo result = makeApiCallForGradeQuestion(fileContentsWithCache, question, submissionData, questionIdToIndex.get(questionId));
                resultMap.put(questionId, result);
                return result;
            });
        }

        // Execute all tasks concurrently
        List<Future<ClaudeQuestionInfo>> futures = executorService.invokeAll(tasks);
        executorService.shutdown();
        executorService.awaitTermination(180, TimeUnit.SECONDS);

        List<ClaudeQuestionInfo> orderedResults = new ArrayList<>();
        for (String questionId : questionIds) {
            orderedResults.add(resultMap.get(questionId));
        }


        return orderedResults;
    }



    private void handleRateLimit(HttpClientErrorException ex) throws IOException {
        String retryAfter = ex.getResponseHeaders().getFirst("Retry-After");
        long retryAfterMillis = retryAfter != null ? Long.parseLong(retryAfter) : 60;
        log.warn("Rate limit hit. Waiting for {} seconds", retryAfterMillis / 1000);
        try {
            Thread.sleep(retryAfterMillis); // Wait before retrying
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for rate limit to expire", e);
        }
    }




    public QuestionIdsWithCacheFiles extractQuestionIds(List<String> fileUrls, String prompt, Long teacherId, String rubricOption) throws IOException {
        List<Map<String, Object>> fileContents = processFiles(fileUrls);
        List<Map<String, Object>> content = new ArrayList<>(fileContents);

        // Add text prompt
        Map<String, Object> textContent = new HashMap<>();
        textContent.put("type", "text");
        textContent.put("text", prompt);
        content.add(textContent);

        // Create request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", anthropicModel);
        requestBody.put("max_tokens", 1024);

        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", content);
        messages.add(message);

//        List<Map<String, Object>> tools = createExtractIdTools();
        List<Map<String, Object>> tools;
        if (rubricOption.equals("handwriting")) {
            tools = createQuestionContentFormatAndExtractIdTools();
        } else {
            tools = createQuestionFormatExtractIdAndGenerateRubricTools();
        }




        requestBody.put("messages", messages);

        requestBody.put("tools", tools);

        // Return response from API call
        List<String> questionIds = makeQuestionIdApiCall(requestBody, teacherId);
        return new QuestionIdsWithCacheFiles(questionIds, fileContents);
    }

//    public List<ClaudeQuestionInfo> formatQuestions(List<Map<String, Object>> fileContents, List<String> questionIds, Long teacherId) throws IOException {
//        if (questionIds.isEmpty()) {
//            return Collections.emptyList();
//        }
//
//        // Process first question synchronously
//        ClaudeQuestionInfo firstQuestion = sendRequestForQuestionFormat(fileContents, questionIds.get(0), teacherId);
//
//        // If there's only one question, return the result
//        if (questionIds.size() == 1) {
//            return List.of(firstQuestion);
//        }
//
//        // Create a list to hold all formatted questions
//        List<ClaudeQuestionInfo> formattedQuestions = new ArrayList<>();
//        formattedQuestions.add(firstQuestion);
//
//        // Process remaining questions in parallel
//        List<CompletableFuture<ClaudeQuestionInfo>> futures = questionIds.subList(1, questionIds.size()).stream()
//                .map(questionId -> CompletableFuture.supplyAsync(() -> {
//                    try {
//                        return sendRequestForQuestionFormat(fileContents, questionId, teacherId);
//                    } catch (IOException e) {
//                        throw new CompletionException(e);
//                    }
//                }))
//                .toList();
//
//        // Wait for all futures to complete and collect results
//        formattedQuestions.addAll(futures.stream()
//                .map(CompletableFuture::join)
//                .filter(Objects::nonNull)
//                .toList());
//
//
//        return formattedQuestions;
//    }
    public List<ClaudeQuestionInfo> formatQuestions(List<Map<String, Object>> fileContents, List<String> questionIds, Long teacherId) throws IOException {
        if (questionIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> groupedIds = new ArrayList<>();
        for (int i = 0; i < questionIds.size(); i += 1) {
            int end = Math.min(i + 1, questionIds.size());
            String group = String.join(",", questionIds.subList(i, end));
            groupedIds.add(group);
        }

        // Create a map to hold results in the correct order
        Map<String, List<ClaudeQuestionInfo>> resultMap = new ConcurrentHashMap<>();

        // Process first question synchronously
        List<ClaudeQuestionInfo> firstBatch;
        try {
            firstBatch = sendRequestForQuestionFormat(fileContents, groupedIds.get(0), teacherId);
            resultMap.put(groupedIds.get(0), firstBatch);
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().value() == 429) {

                String retryAfter = ex.getResponseHeaders().getFirst("Retry-After");
                long secondsToWait = retryAfter != null ? Long.parseLong(retryAfter) : 60;
                log.warn("Rate limited on first question. Waiting for {} seconds", secondsToWait);
                try {
                    Thread.sleep(secondsToWait * 1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while waiting for rate limit to expire", ie);
                }
                // Try again after waiting
                firstBatch = sendRequestForQuestionFormat(fileContents, groupedIds.get(0), teacherId);
                resultMap.put(groupedIds.get(0), firstBatch);
            } else {
                throw new IOException("API error: " + ex.getMessage(), ex);
            }
        }

        // If there's only one question, return the result
        if (groupedIds.size() == 1) {
            return firstBatch;
        }

        // Keep track of which questions we've processed
//        Set<String> processedQuestionIds = new HashSet<>();

        Set<String> processedQuestionIds = Collections.synchronizedSet(new HashSet<>());
        processedQuestionIds.add(groupedIds.get(0));

        // Create a list of remaining question IDs to process
        List<String> remainingQuestionIds = new ArrayList<>(groupedIds.subList(1, groupedIds.size()));

        // Process remaining questions in batches until all are done
        while (!remainingQuestionIds.isEmpty()) {
            // Use a thread pool with a reasonable size
            ExecutorService executor = Executors.newFixedThreadPool(
                    Math.min(10, remainingQuestionIds.size())
            );

            // Track if we hit rate limits in this batch
            AtomicBoolean rateLimited = new AtomicBoolean(false);
            AtomicLong retryAfterMillis = new AtomicLong(0);

            // Process current batch
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (String qIds : remainingQuestionIds) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    if (rateLimited.get()) {
                        return; // Skip if we already hit rate limit
                    }

                    try {
                        List<ClaudeQuestionInfo> result = sendRequestForQuestionFormat(fileContents, qIds, teacherId);
                        if (result != null) {
                            resultMap.put(qIds, result);
                            processedQuestionIds.add(qIds);
                        }
                    } catch (HttpClientErrorException ex) {
                        if (ex.getStatusCode().value() == 429) {
                            String retryAfter = ex.getResponseHeaders().getFirst("Retry-After");
                            long secondsToWait = retryAfter != null ? Long.parseLong(retryAfter) : 60;
                            rateLimited.set(true);
                            retryAfterMillis.set(secondsToWait * 1000);
                            log.warn("Rate limit hit for question ID: {}. Retry after: {} seconds",
                                    qIds, secondsToWait);
                        } else {
                            log.error("API error for question ID {}: {} - {}",
                                    qIds, ex.getStatusCode(), ex.getResponseBodyAsString());
                        }
                    } catch (IOException e) {
                        log.error("Error processing question ID {}: {}", qIds, e.getMessage());
                    }
                }, executor);

                futures.add(future);
            }

            // Wait for all futures to complete or a rate limit to be hit
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .exceptionally(e -> null)
                    .join();

            // Create new list of remaining questions
//            remainingQuestionIds = remainingQuestionIds.stream()
//                    .filter(ids -> !processedQuestionIds.contains(ids))
//                    .collect(Collectors.toList());

            synchronized (processedQuestionIds) {
                remainingQuestionIds = remainingQuestionIds.stream()
                        .filter(ids -> !processedQuestionIds.contains(ids))
                        .collect(Collectors.toList());
            }

            // If we hit a rate limit and didn't make progress, wait
            if (rateLimited.get() && !remainingQuestionIds.isEmpty()) {
                long waitTime = retryAfterMillis.get();
                log.info("Waiting for rate limit to expire: {} ms before resuming batch processing", waitTime);
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while waiting for rate limit to expire", ie);
                }
            }

            // Shutdown executor
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException ie) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // Convert map to ordered list based on original questionIds order
        List<ClaudeQuestionInfo> orderedResults = new ArrayList<>();
        for (String qIds : groupedIds) {
            List<ClaudeQuestionInfo> info = resultMap.get(qIds);
            if (info != null) {
                orderedResults.addAll(info);
            }
        }

        return orderedResults;
    }

    public List<ClaudeQuestionInfo> formatOnlyContentQuestions(List<Map<String, Object>> fileContents, List<String> questionIds, Long teacherId) throws IOException {
        if (questionIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> groupedIds = new ArrayList<>();
        for (int i = 0; i < questionIds.size(); i += 1) {
            int end = Math.min(i + 1, questionIds.size());
            String group = String.join(",", questionIds.subList(i, end));
            groupedIds.add(group);
        }

        // Create a map to hold results in the correct order
        Map<String, List<ClaudeQuestionInfo>> resultMap = new ConcurrentHashMap<>();

        // Process first question synchronously
        List<ClaudeQuestionInfo> firstBatch;
        try {
            firstBatch = sendRequestForQuestionContentOnlyFormat(fileContents, groupedIds.get(0), teacherId);
            resultMap.put(groupedIds.get(0), firstBatch);
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().value() == 429) {

                String retryAfter = ex.getResponseHeaders().getFirst("Retry-After");
                long secondsToWait = retryAfter != null ? Long.parseLong(retryAfter) : 60;
                log.warn("Rate limited on first question. Waiting for {} seconds", secondsToWait);
                try {
                    Thread.sleep(secondsToWait * 1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while waiting for rate limit to expire", ie);
                }
                // Try again after waiting
                firstBatch = sendRequestForQuestionFormat(fileContents, groupedIds.get(0), teacherId);
                resultMap.put(groupedIds.get(0), firstBatch);
            } else {
                throw new IOException("API error: " + ex.getMessage(), ex);
            }
        }

        // If there's only one question, return the result
        if (groupedIds.size() == 1) {
            return firstBatch;
        }

        // Keep track of which questions we've processed
//        Set<String> processedQuestionIds = new HashSet<>();

        Set<String> processedQuestionIds = Collections.synchronizedSet(new HashSet<>());
        processedQuestionIds.add(groupedIds.get(0));

        // Create a list of remaining question IDs to process
        List<String> remainingQuestionIds = new ArrayList<>(groupedIds.subList(1, groupedIds.size()));

        // Process remaining questions in batches until all are done
        while (!remainingQuestionIds.isEmpty()) {
            // Use a thread pool with a reasonable size
            ExecutorService executor = Executors.newFixedThreadPool(
                    Math.min(10, remainingQuestionIds.size())
            );

            // Track if we hit rate limits in this batch
            AtomicBoolean rateLimited = new AtomicBoolean(false);
            AtomicLong retryAfterMillis = new AtomicLong(0);

            // Process current batch
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (String qIds : remainingQuestionIds) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    if (rateLimited.get()) {
                        return; // Skip if we already hit rate limit
                    }

                    try {
                        List<ClaudeQuestionInfo> result = sendRequestForQuestionContentOnlyFormat(fileContents, qIds, teacherId);
                        if (result != null) {
                            resultMap.put(qIds, result);
                            processedQuestionIds.add(qIds);
                        }
                    } catch (HttpClientErrorException ex) {
                        if (ex.getStatusCode().value() == 429) {
                            String retryAfter = ex.getResponseHeaders().getFirst("Retry-After");
                            long secondsToWait = retryAfter != null ? Long.parseLong(retryAfter) : 60;
                            rateLimited.set(true);
                            retryAfterMillis.set(secondsToWait * 1000);
                            log.warn("Rate limit hit for question ID: {}. Retry after: {} seconds",
                                    qIds, secondsToWait);
                        } else {
                            log.error("API error for question ID {}: {} - {}",
                                    qIds, ex.getStatusCode(), ex.getResponseBodyAsString());
                        }
                    } catch (IOException e) {
                        log.error("Error processing question ID {}: {}", qIds, e.getMessage());
                    }
                }, executor);

                futures.add(future);
            }

            // Wait for all futures to complete or a rate limit to be hit
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .exceptionally(e -> null)
                    .join();

            // Create new list of remaining questions
//            remainingQuestionIds = remainingQuestionIds.stream()
//                    .filter(ids -> !processedQuestionIds.contains(ids))
//                    .collect(Collectors.toList());

            synchronized (processedQuestionIds) {
                remainingQuestionIds = remainingQuestionIds.stream()
                        .filter(ids -> !processedQuestionIds.contains(ids))
                        .collect(Collectors.toList());
            }

            // If we hit a rate limit and didn't make progress, wait
            if (rateLimited.get() && !remainingQuestionIds.isEmpty()) {
                long waitTime = retryAfterMillis.get();
                log.info("Waiting for rate limit to expire: {} ms before resuming batch processing", waitTime);
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while waiting for rate limit to expire", ie);
                }
            }

            // Shutdown executor
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException ie) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // Convert map to ordered list based on original questionIds order
        List<ClaudeQuestionInfo> orderedResults = new ArrayList<>();
        for (String qIds : groupedIds) {
            List<ClaudeQuestionInfo> info = resultMap.get(qIds);
            if (info != null) {
                orderedResults.addAll(info);
            }
        }

        return orderedResults;
    }

    public List<ClaudeQuestionInfo> formatQuestionsByAutoGenerateRubrics(List<Map<String, Object>> fileContents, List<String> questionIds) throws IOException {
        if (questionIds.isEmpty()) {
            return Collections.emptyList();
        }


        List<ClaudeQuestionInfo> formattedQuestions = new ArrayList<>();

        List<ClaudeQuestionInfo> questionResult = sendRequestForRubricGeneration(fileContents, questionIds.get(0));
        if (!questionResult.isEmpty()) {
            formattedQuestions.add(questionResult.get(0));
        }

        // Create a list of CompletableFutures for the remaining questions
        List<CompletableFuture<ClaudeQuestionInfo>> futures = questionIds.subList(1, questionIds.size()).stream()
                .map(questionId -> CompletableFuture.supplyAsync(() -> {
                    try {
                        // Send request asynchronously for each question
                        List<ClaudeQuestionInfo> result = sendRequestForRubricGeneration(fileContents, questionId);
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

    private List<ClaudeQuestionInfo> sendRequestForRubricGeneration(List<Map<String, Object>> fileContents, String questionId) throws IOException {

        String prompt = String.format("I will give you a file of problem set and an question id. Please generate a grading rubric for question %s. Here are the steps you need to do:\n" +
                "1. First read this problem and come up with a solution for yourself for this question id.\n" +
                "2. Based on what you understand about this question, create a grading rubric that specifies the grading criteria, point allocation, and any other relevant notes or considerations for this question.\n" +
                "3. Return a json that contain the\n" +
                    "(1). question id, " +
                    "(2). question content" +
                        "(4). The max grade possible for this question, like ‘2 points’, ‘3 points’ or ‘A’, ‘B+’ "+
                    "(3). The rubric content, which contain the main grading criteria like how the grade should be assigned to each question",questionId);
        List<Map<String, Object>> content = new ArrayList<>();

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

        ClaudeResponseBody claudeResponseBody = objectMapper.readValue(responseBody, ClaudeResponseBody.class);
        System.out.println(claudeResponseBody);
        List<ClaudeQuestionInfo> questions = new ArrayList<>();

        // Process content items and extract generated rubrics
        for (ClaudeContentItem item : claudeResponseBody.getContent()) {
            if ("tool_use".equals(item.getType()) && item.getInput() != null) {
                questions = item.getInput().getQuestions();
            }
        }

        return questions;
    }

//    private ClaudeQuestionInfo sendRequestForQuestionFormat(List<Map<String, Object>> fileContents, String questionId, Long teacherId) throws IOException {
//        String prompt = String.format(
//                "I will give you some files about a specific problem set and a question id. Please extract the following information for question %s:\n" +
//                        "\n" +
//                        "(1) The question content\n" +
//                        "\n" +
//                        "(2) The pre-context that comes before the question but is related to this question. For example, question 2(a) and 2(b) may share the same context before them. If there is no context, just put an empty string.\n" +
//                        "\n" +
//                        "(3) The rubric details for this question\n" +
//                        "\n" +
//                        "(4) The max grade/best level for this question, like '2 points', '3 points' or 'A', 'B+'\n" +
//                        "\n" +
//                        "Note:\n" +
//                        "- Preserve all related details from the file.\n" +
//                        "- Ensure information is clearly formatted and structured according to the specifications.",
//                questionId);
//
//        List<Map<String, Object>> content = new ArrayList<>(fileContents);
//
//        // Add text prompt
//        Map<String, Object> textContent = new HashMap<>();
//        textContent.put("type", "text");
//        textContent.put("text", prompt);
//        content.add(textContent);
//
//        // Create request body
//        Map<String, Object> requestBody = new HashMap<>();
//        requestBody.put("model", anthropicModel);
//        requestBody.put("max_tokens", 4096);
//
//        List<Map<String, Object>> messages = new ArrayList<>();
//        Map<String, Object> message = new HashMap<>();
//        message.put("role", "user");
//        message.put("content", content);
//        messages.add(message);
//
//        requestBody.put("messages", messages);
//
//        // Create tools for question formatting
////        List<Map<String, Object>> tools = createQuestionFormatTools();
//        List<Map<String, Object>> tools = createQuestionFormatExtractIdAndGenerateRubricTools();
//        requestBody.put("tools", tools);
//
//        // Make API call
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        headers.set("x-api-key", apiKey);
//        headers.set("anthropic-version", "2023-06-01");
//
//        HttpEntity<String> entity = new HttpEntity<>(
//                objectMapper.writeValueAsString(requestBody),
//                headers
//        );
//
//        ResponseEntity<String> response = restTemplate.exchange(
//                apiUrl,
//                HttpMethod.POST,
//                entity,
//                String.class
//        );
//
//        String responseBody = response.getBody();
//        ClaudeResponseBody claudeResponseBody = objectMapper.readValue(responseBody, ClaudeResponseBody.class);
//
//        System.out.println(claudeResponseBody);
//
//        saveUsageData(claudeResponseBody.getUsage(), teacherId);
//
//        // Process content items and extract question info
//        for (ClaudeContentItem item : claudeResponseBody.getContent()) {
//            if ("tool_use".equals(item.getType()) && item.getInput() != null && !item.getInput().getQuestions().isEmpty()) {
//                return item.getInput().getQuestions().get(0);
//            }
//        }
//
//        return null;
//    }

//    private List<ClaudeQuestionInfo> sendRequestForQuestionFormat(List<Map<String, Object>> fileContents, String questionIds, Long teacherId) throws IOException {
//        String prompt = String.format(
//                "I will give you some files about a specific problem set, and here are some given question ids: %s. For each of the given question id, please format the question information according to the following:\n" +
//                        "\n" +
//                        "(1) The question content\n" +
//                        "\n" +
//                        "(2) The pre-context that comes before the question but is related to this question. For example, question 2(a) and 2(b) may share some same context before them. Remember the pre-context includes all information needed to solve this problem. If there is no context, just put an empty string.\n" +
//                        "\n" +
//                        "(3) The rubric details for this question\n" +
//                        "\n" +
//                        "(4) The max grade/best level for this question, like '2 points', '3 points' or 'A', 'B+'\n" +
//                        "\n" +
//                        "Here are the question ids: %s \n" +
//                        "Note:\n" +
//                        "- Preserve all related details from the file.\n" +
//                        "- Ensure information is clearly formatted and structured according to the question_format tool." +
//                        "- Ensure that all of the given question ids's information have been included in the final json according to the question_format tool.\n.",
//                questionIds, questionIds);
//
//        List<Map<String, Object>> content = new ArrayList<>(fileContents);
//
//        // Add text prompt
//        Map<String, Object> textContent = new HashMap<>();
//        textContent.put("type", "text");
//        textContent.put("text", prompt);
//        content.add(textContent);
//
//        // Create request body
//        Map<String, Object> requestBody = new HashMap<>();
//        requestBody.put("model", anthropicModel);
//        requestBody.put("max_tokens", 4096);
//
//        List<Map<String, Object>> messages = new ArrayList<>();
//        Map<String, Object> message = new HashMap<>();
//        message.put("role", "user");
//        message.put("content", content);
//        messages.add(message);
//
//        requestBody.put("messages", messages);
//
//        // Create tools for question formatting
//        List<Map<String, Object>> tools = createQuestionFormatExtractIdAndGenerateRubricTools();
//        requestBody.put("tools", tools);
//
//        // Make API call
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        headers.set("x-api-key", apiKey);
//        headers.set("anthropic-version", "2023-06-01");
//
//        HttpEntity<String> entity = new HttpEntity<>(
//                objectMapper.writeValueAsString(requestBody),
//                headers
//        );
//
//        try {
//            ResponseEntity<String> response = restTemplate.exchange(
//                    apiUrl,
//                    HttpMethod.POST,
//                    entity,
//                    String.class
//            );
//
//            String responseBody = response.getBody();
//            System.out.println(responseBody);
//            ClaudeResponseBody claudeResponseBody = objectMapper.readValue(responseBody, ClaudeResponseBody.class);
//
//            System.out.println("Response for question ID: " + questionIds +
//                    "    " + objectMapper.writeValueAsString(claudeResponseBody));
//
//            saveUsageData(claudeResponseBody.getUsage(), teacherId);
//
//            // Process content items and extract question info
//            for (ClaudeContentItem item : claudeResponseBody.getContent()) {
//                if ("tool_use".equals(item.getType()) && item.getInput() != null && !item.getInput().getQuestions().isEmpty()) {
//                    return item.getInput().getQuestions();
//                }
//            }
//
//            return null;
//        } catch (HttpClientErrorException ex) {
//            if (ex.getStatusCode().value() == 429) {
//                String retryAfter = ex.getResponseHeaders().getFirst("Retry-After");
//                log.warn("Rate limit reached for question ID {}. Retry after: {} seconds",
//                        questionIds, retryAfter != null ? retryAfter : "unknown");
//
//                // Let the exception propagate so it can be handled by the rate limiting logic
//                throw ex;
//            } else {
//                log.error("Error calling Anthropic API for question ID {}: {} - {}",
//                        questionIds, ex.getStatusCode(), ex.getResponseBodyAsString());
//                throw new IOException("API error: " + ex.getMessage(), ex);
//            }
//        } catch (Exception e) {
//            log.error("Unexpected error processing question ID {}: {}", questionIds, e.getMessage(), e);
//            throw new IOException("Error processing request: " + e.getMessage(), e);
//        }
//    }
    // Modified API call method

    private List<ClaudeQuestionInfo> sendRequestForQuestionFormat(List<Map<String, Object>> fileContents, String questionIds, Long teacherId) throws IOException {
        String prompt = String.format(
                "I will give you some files about a specific problem set. For the given question id %s, please format the question information according to the following:\n" +
                        "\n" +
                        "(1) The question content\n" +
                        "\n" +
                        "(2) The pre-context that comes before the question but is related to this question. Remember the pre-context includes all information needed to solve this problem. If there is no context, just put an empty string.\n" +
                        "\n" +
                        "(3) The rubric details for this question\n" +
                        "\n" +
                        "(4) The max grade/best level for this question, like '2 points', '3 points' or 'A', 'B+'\n" +
                        "\n" +
                        "(5) Whether the problem context, problem content, and anticipated student response for this question should contain pure text, or should contain figures, images, tables. If it is pure text, return true. If the problem context or problem content is not pure text, but the information of them is already fully extracted, also return true. Otherwise return false.  \n" +
                        "\n" +
                        "Here is the question id: %s \n" +
                        "Before providing the final response, verify these:\n" +
                        "- Preserve all related grading details from the files into the rubric part, if there are any other information that will help grading, please continue to include them.\n" +
                        "- If your given pre-context plus the question content is not enough for solving this problem %s, please continue to include more." +
                        "- Ensure information is clearly formatted and structured according to the question_format tool.\n" ,
                questionIds, questionIds, questionIds);

        List<Map<String, Object>> content = new ArrayList<>(fileContents);

        // Add text prompt
        Map<String, Object> textContent = new HashMap<>();
        textContent.put("type", "text");
        textContent.put("text", prompt);
        content.add(textContent);

        // Create request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", anthropicModel);
        requestBody.put("max_tokens", 4096);

        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", content);
        messages.add(message);

        requestBody.put("messages", messages);

        // Create tools for question formatting
        List<Map<String, Object>> tools = createQuestionFormatExtractIdAndGenerateRubricTools();
        requestBody.put("tools", tools);

        // Make API call
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        HttpEntity<String> entity = new HttpEntity<>(
                objectMapper.writeValueAsString(requestBody),
                headers
        );

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            String responseBody = response.getBody();
            System.out.println(responseBody);
            ClaudeResponseBody claudeResponseBody = objectMapper.readValue(responseBody, ClaudeResponseBody.class);

            System.out.println("Response for question ID: " + questionIds +
                    "    " + objectMapper.writeValueAsString(claudeResponseBody));

            saveUsageData(claudeResponseBody.getUsage(), teacherId);

            // Process content items and extract question info
            for (ClaudeContentItem item : claudeResponseBody.getContent()) {
                if ("tool_use".equals(item.getType()) && item.getInput() != null && !item.getInput().getQuestions().isEmpty()) {
                    return item.getInput().getQuestions();
                }
            }

            return null;
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().value() == 429) {
                String retryAfter = ex.getResponseHeaders().getFirst("Retry-After");
                log.warn("Rate limit reached for question ID {}. Retry after: {} seconds",
                        questionIds, retryAfter != null ? retryAfter : "unknown");

                // Let the exception propagate so it can be handled by the rate limiting logic
                throw ex;
            } else {
                log.error("Error calling Anthropic API for question ID {}: {} - {}",
                        questionIds, ex.getStatusCode(), ex.getResponseBodyAsString());
                throw new IOException("API error: " + ex.getMessage(), ex);
            }
        } catch (Exception e) {
            log.error("Unexpected error processing question ID {}: {}", questionIds, e.getMessage(), e);
            throw new IOException("Error processing request: " + e.getMessage(), e);
        }
    }

    private List<ClaudeQuestionInfo> sendRequestForQuestionContentOnlyFormat(List<Map<String, Object>> fileContents, String questionIds, Long teacherId) throws IOException {
        String prompt = String.format(
                "I will give you some files about a specific problem set. For the given question id %s, please format the question information according to the following:\n" +
                        "\n" +
                        "(1) The question content\n" +
                        "\n" +
                        "(2) The pre-context that comes before the question but is related to this question. Remember the pre-context includes all information needed to solve this problem. If there is no context, just put an empty string.\n" +
                        "\n" +
                        "(3) The max grade/best level for this question, like '2 points', '3 points' or 'A', 'B+'\n, if there is no such information, just give null" +
                        "\n" +
                        "(4) Whether the problem context, problem content, and anticipated student response for this question should contain pure text, or should contain figures, images, tables. If it is pure text, return true. If the problem context or problem content is not pure text, but the information of them is already fully extracted, also return true. Otherwise return false.  \n" +
                        "\n" +
                        "Here is the question id: %s \n" +
                        "Before providing the final response, verify these:\n" +
                        "- If your given pre-context plus the question content is not enough for solving this problem %s, please continue to include more.\n" +
                        "- Ensure information is clearly formatted and structured according to the question_content_format tool.\n" ,
                questionIds, questionIds, questionIds);

        List<Map<String, Object>> content = new ArrayList<>(fileContents);

        // Add text prompt
        Map<String, Object> textContent = new HashMap<>();
        textContent.put("type", "text");
        textContent.put("text", prompt);
        content.add(textContent);

        // Create request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", anthropicModel);
        requestBody.put("max_tokens", 4096);

        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", content);
        messages.add(message);

        requestBody.put("messages", messages);

        // Create tools for question formatting
        List<Map<String, Object>> tools = createQuestionContentFormatAndExtractIdTools();
        requestBody.put("tools", tools);

        // Make API call
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        HttpEntity<String> entity = new HttpEntity<>(
                objectMapper.writeValueAsString(requestBody),
                headers
        );

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            String responseBody = response.getBody();
            System.out.println(responseBody);
            ClaudeResponseBody claudeResponseBody = objectMapper.readValue(responseBody, ClaudeResponseBody.class);

            System.out.println("Response for question ID: " + questionIds +
                    "    " + objectMapper.writeValueAsString(claudeResponseBody));

            saveUsageData(claudeResponseBody.getUsage(), teacherId);

            // Process content items and extract question info
            for (ClaudeContentItem item : claudeResponseBody.getContent()) {
                if ("tool_use".equals(item.getType()) && item.getInput() != null && !item.getInput().getQuestions().isEmpty()) {
                    return item.getInput().getQuestions();
                }
            }

            return null;
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().value() == 429) {
                String retryAfter = ex.getResponseHeaders().getFirst("Retry-After");
                log.warn("Rate limit reached for question ID {}. Retry after: {} seconds",
                        questionIds, retryAfter != null ? retryAfter : "unknown");

                // Let the exception propagate so it can be handled by the rate limiting logic
                throw ex;
            } else {
                log.error("Error calling Anthropic API for question ID {}: {} - {}",
                        questionIds, ex.getStatusCode(), ex.getResponseBodyAsString());
                throw new IOException("API error: " + ex.getMessage(), ex);
            }
        } catch (Exception e) {
            log.error("Unexpected error processing question ID {}: {}", questionIds, e.getMessage(), e);
            throw new IOException("Error processing request: " + e.getMessage(), e);
        }
    }
    private List<String> makeQuestionIdApiCall(Map<String, Object> requestBody, Long teacherId) throws IOException {
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
        System.out.println(claudeResponseBody);

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

        saveUsageData(claudeResponseBody.getUsage(), teacherId);
        return questionIds;
    }

    private ClaudeQuestionInfo makeApiCallForGradeQuestion(List<Map<String, Object>> fileContents,
                                                      ClaudeQuestionInfo question, SubmissionData submissionData, Integer questionIndex) throws IOException {


        List<Map<String, Object>> content = new ArrayList<>(fileContents);

        // Handle image rubric(s)
        if (question.getRubricType().equals("image")) {
            // Split the rubric string by semicolons to get multiple image paths
            String[] imageIds = question.getRubric().split(";");

            for (String imageId : imageIds) {
                // Trim the imageId to remove any potential whitespace
                String trimmedImageId = imageId.trim();
                if (!trimmedImageId.isEmpty()) {
                    String imageUrl = fileStorageService.generateImagePresignedUrl(trimmedImageId);

                    // Create image content item
                    Map<String, Object> imageContent = new HashMap<>();
                    imageContent.put("type", "image");

                    Map<String, Object> source = new HashMap<>();
                    source.put("type", "url");
                    source.put("url", imageUrl);

                    imageContent.put("source", source);

                    // Add image content to the content list
                    content.add(imageContent);
                }
            }
        }

        // Add text prompt with question-specific information
        Map<String, Object> textContent = new HashMap<>();
        textContent.put("type", "text");

        // Modify prompt based on rubric type
        String rubricText;
        if (question.getRubricType().equals("image")) {
            int imageCount = question.getRubric().split(";").length;
            rubricText = String.format("Please refer to the %d image rubric%s I've attached",
                    imageCount,
                    imageCount > 1 ? "s" : "");
        } else {
            rubricText = question.getRubric();
        }

        String questionPrompt = String.format("""
        I want you to grade for the question %s from the student answer and output the json using the answer_grading_tool. Here are the steps you need to do:
        1. First read the question context and question content and try to come up with a solution for this question for yourself
        2. Find the corresponding rubric for question %s, read and understand the rubric for this question first and understand the grading and grading notes if any.
        3. Grade the student's answer by your understanding and checking the rubric and then grade, please think critically and don't give the point too easy!! For your final grading, please provide with a grade and very concise explanation consists for your grading.
        Here is the context you should know:
        %s
        Here is the question content:
        %s,
        Here is the rubric:
        %s, and the max grade/ best level you can give for this question is %s,
        
        The student's answer is in the attached file. If student didn't do this question, give none to the grade and say "No related answer" in explanation.
        """,
                question.getQuestionId(),
                question.getQuestionId(),
                question.getPreContext(),
                question.getContent(),
                rubricText,
                question.getMaxGrade());

        textContent.put("text", questionPrompt);
        content.add(textContent);

//        Map<String, Object> cacheControl = new HashMap<>();
//        cacheControl.put("type", "ephemeral");
//        textContent.put("cache_control", cacheControl);
        content.add(textContent);
        // Create request body
        Map<String, Object> requestBody = createGradeRequestBody(content);

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

        System.out.println(question.getQuestionId());
        System.out.println(responseBody);
        ClaudeResponseBody claudeResponseBody = objectMapper.readValue(responseBody, ClaudeResponseBody.class);
        System.out.println(objectMapper.writeValueAsString(claudeResponseBody));

        saveUsageData(claudeResponseBody.getUsage(), submissionData.getAssignment().getCourse().getTeacher().getId());

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
                grading.setQuestion_order(questionIndex);
                questionGradingRepository.save(grading);
                return gradedQuestion;
            }
        }

        throw new IOException("No valid response received for question: " + question.getQuestionId());
    }

    private List<ClaudeQuestionInfo> makeApiCallForFormatAnswer(List<Map<String, Object>> fileContents,
                                                           List<String> questionIds, SubmissionData submissionData) throws IOException {
        String joinedQuestionIds = String.join(",", questionIds);
        List<Map<String, Object>> content = new ArrayList<>(fileContents);

        // Add text prompt with question-specific information
        Map<String, Object> textContent = new HashMap<>();
        textContent.put("type", "text");
        String answerformatprompt = String.format("""
        I will give you a file of student response and a list of question ids.
        You should return a json that contain each question id and the student response correspond to that specific question.
        You should decide whether the student response is pure text, or it contain images, figures or tables. If it is pure text, give true, otherwise give false.
        
        Note:
        Check the json output should contain all the question ids: %s
        Make sure that you include everything that the student written for each question id.
        Here is the list of question ids: %s
        """, joinedQuestionIds,joinedQuestionIds);

        textContent.put("text", answerformatprompt);
        content.add(textContent);
        // Create request body
        Map<String, Object> requestBody = createFormatAnswerRequestBody(content);

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

        System.out.println(responseBody);
        ClaudeResponseBody claudeResponseBody = objectMapper.readValue(responseBody, ClaudeResponseBody.class);
        System.out.println(objectMapper.writeValueAsString(claudeResponseBody));

        saveUsageData(claudeResponseBody.getUsage(), submissionData.getAssignment().getCourse().getTeacher().getId());

        // Process content items and extract question info
        for (ClaudeContentItem item : claudeResponseBody.getContent()) {
            if ("tool_use".equals(item.getType()) && item.getInput() != null) {
                List<ClaudeQuestionInfo> formatAnswers = item.getInput().getQuestions();
                return formatAnswers;
            }
        }

        throw new IOException("No valid response received for format answer");
    }

    private List<Map<String, Object>> createRubricGeneratingTools() {
        List<Map<String, Object>> tools = new ArrayList<>();
        Map<String, Object> tool = new HashMap<>();
        tool.put("name", "rubric_generating_tool");
        tool.put("description", "Automatically generate a rubric for a specific question");

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
    private List<Map<String, Object>> createGradeTools() {
        List<Map<String, Object>> tools = new ArrayList<>();
        Map<String, Object> tool = new HashMap<>();
        tool.put("name", "answer_grading_tool");
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
    private List<Map<String, Object>> createQuestionFormatTools() {
        List<Map<String, Object>> tools = new ArrayList<>();
        Map<String, Object> tool = new HashMap<>();
        tool.put("name", "question_format");
        tool.put("description", "Format the question details including the question id, content, pre-context, rubric, and max grade");

        // Define input schema for the tool
        Map<String, Object> inputSchema = new HashMap<>();
        inputSchema.put("type", "object");

        // Define the questions array
        Map<String, Object> questionsProps = new HashMap<>();
        questionsProps.put("type", "array");

        // Define question item structure
        Map<String, Object> questionItem = new HashMap<>();
        questionItem.put("type", "object");

        // Define question properties
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

        // maxGrade
        Map<String, Object> maxGradeDef = new HashMap<>();
        maxGradeDef.put("type", "string");
        maxGradeDef.put("description", "The best grade or the full points that this question can give, e.g. 'A', 'B+', '3 points', '1 points', etc.");
        questionItemProperties.put("maxGrade", maxGradeDef);

        // preContext
        Map<String, Object> preContextDef = new HashMap<>();
        preContextDef.put("type", "string");
        preContextDef.put("description", "The context for the question.");
        questionItemProperties.put("preContext", preContextDef);

        Map<String, Object> isPureText = new HashMap<>();
        isPureText.put("type", "boolean");
        isPureText.put("description", "Whether the problem context, problem content and anticipated student response is pure text or not.");
        questionItemProperties.put("isPureText", isPureText);


        // Define required fields
        questionItem.put("properties", questionItemProperties);
        questionItem.put("required", List.of("question_id", "content", "rubric", "maxGrade", "preContext", "isPureText"));

        // Add question item to questions array
        questionsProps.put("items", questionItem);

        // Add questions to top-level properties
        Map<String, Object> topLevelProperties = new HashMap<>();
        topLevelProperties.put("questions", questionsProps);

        // Finalize input schema
        inputSchema.put("properties", topLevelProperties);
        inputSchema.put("required", List.of("questions"));
        tool.put("input_schema", inputSchema);

        tools.add(tool);
        return tools;
    }

    private List<Map<String, Object>> createQuestionOnlyContentFormatTools(){
        List<Map<String, Object>> tools = new ArrayList<>();
        Map<String, Object> tool = new HashMap<>();
        tool.put("name", "question_content_format");
        tool.put("description", "Format the question details including the question id, content, pre-context, and max grade");

        // Define input schema for the tool
        Map<String, Object> inputSchema = new HashMap<>();
        inputSchema.put("type", "object");

        // Define the questions array
        Map<String, Object> questionsProps = new HashMap<>();
        questionsProps.put("type", "array");

        // Define question item structure
        Map<String, Object> questionItem = new HashMap<>();
        questionItem.put("type", "object");

        // Define question properties
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

        // maxGrade
        Map<String, Object> maxGradeDef = new HashMap<>();
        maxGradeDef.put("type", "string");
        maxGradeDef.put("description", "The best grade or the full points that this question can give, e.g. 'A', 'B+', '3 points', '1 points', etc.");
        questionItemProperties.put("maxGrade", maxGradeDef);

        // preContext
        Map<String, Object> preContextDef = new HashMap<>();
        preContextDef.put("type", "string");
        preContextDef.put("description", "The context for the question.");
        questionItemProperties.put("preContext", preContextDef);

        Map<String, Object> isPureText = new HashMap<>();
        isPureText.put("type", "boolean");
        isPureText.put("description", "Whether the problem context, problem content and anticipated student response is pure text or not.");
        questionItemProperties.put("isPureText", isPureText);


        // Define required fields
        questionItem.put("properties", questionItemProperties);
        questionItem.put("required", List.of("question_id", "content", "maxGrade", "preContext", "isPureText"));

        // Add question item to questions array
        questionsProps.put("items", questionItem);

        // Add questions to top-level properties
        Map<String, Object> topLevelProperties = new HashMap<>();
        topLevelProperties.put("questions", questionsProps);

        // Finalize input schema
        inputSchema.put("properties", topLevelProperties);
        inputSchema.put("required", List.of("questions"));
        tool.put("input_schema", inputSchema);

        tools.add(tool);
        return tools;
    }

    private List<Map<String,Object>> createExtractIdTools(){
        List<Map<String, Object>> tools = new ArrayList<>();

        // Modified tool definition for question ID objects
        Map<String, Object> tool = new HashMap<>();
        tool.put("name", "question_id_extract");
        tool.put("description", "Extract all question IDs from the provided document");

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
        return tools;
    }

    private List<Map<String, Object>> createQuestionFormatExtractIdAndGenerateRubricTools(){
        List<Map<String, Object>> tools = new ArrayList<>();
        tools.add(createExtractIdTools().get(0));
        tools.add(createQuestionFormatTools().get(0));
        tools.add(createRubricGeneratingTools().get(0));
        return tools;
    }
    private List<Map<String, Object>> createQuestionContentFormatAndExtractIdTools(){
        List<Map<String, Object>> tools = new ArrayList<>();
        tools.add(createExtractIdTools().get(0));
        tools.add(createQuestionOnlyContentFormatTools().get(0));
        return tools;
    }

    private List<Map<String, Object>> createFormatAnswerAndGradeTools(){
        List<Map<String, Object>> tools = new ArrayList<>();
        tools.add(createGradeTools().get(0));
        tools.add(createFormatAnswerTools().get(0));
        return tools;
    }

    private List<Map<String, Object>> createFormatAnswerTools(){
        List<Map<String, Object>> tools = new ArrayList<>();
        Map<String, Object> tool = new HashMap<>();
        tool.put("name", "response_parsing_tool");
        tool.put("description", "Parse the student responses and return a mapping of question IDs to their corresponding responses content but not grade");

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

        Map<String, Object> responseDef = new HashMap<>();
        responseDef.put("type", "string");
        responseDef.put("description", "The student's written response for this question.");
        questionItemProperties.put("content", responseDef);

        Map<String, Object> isPureText = new HashMap<>();
        responseDef.put("type", "boolean");
        responseDef.put("description", "Whether the student response is pure text or it contain some drawings, figures, or tables.");
        questionItemProperties.put("isPureText", isPureText);


        questionItem.put("properties", questionItemProperties);
        questionItem.put("required", List.of("question_id", "content", "isPureText"));

        questionsProps.put("items", questionItem);

        Map<String, Object> topLevelProperties = new HashMap<>();
        topLevelProperties.put("questions", questionsProps);

        inputSchema.put("properties", topLevelProperties);
        inputSchema.put("required", List.of("questions"));
        tool.put("input_schema", inputSchema);

        tools.add(tool);
        return tools;
    }


    private Map<String, Object> createGradeRequestBody(List<Map<String, Object>> content) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", anthropicModel);
        requestBody.put("max_tokens", 1024);

        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", content);
        messages.add(message);
        requestBody.put("messages", messages);

        requestBody.put("tools", createFormatAnswerAndGradeTools());

        return requestBody;
    }

    private Map<String, Object> createFormatAnswerRequestBody(List<Map<String, Object>> content) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", anthropicModel);
        requestBody.put("max_tokens", 4096);

        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", content);
        messages.add(message);
        requestBody.put("messages", messages);
        requestBody.put("tools", createFormatAnswerAndGradeTools());
        return requestBody;
    }




    private Map<String, Object> createRubricGeneratingRequestBody(List<Map<String, Object>> content) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", anthropicModel);
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

    private List<Map<String, Object>> processFilesWithoutCache(List<String> fileUrls) throws IOException {
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

            fileContent.put("source", source);
            fileContents.add(fileContent);
        }
        return fileContents;
    }
    private void saveUsageData(ClaudeUsage claudeUsage, Long teacherId) {
        AnthropicUsage anthropicUsage = new AnthropicUsage();
        anthropicUsage.setUserId(teacherId);
        anthropicUsage.setCacheReadInputTokens(claudeUsage.getCache_read_input_tokens());
        anthropicUsage.setInputTokens(claudeUsage.getInput_tokens());
        anthropicUsage.setOutputTokens(claudeUsage.getOutput_tokens());
        anthropicUsage.setCacheCreationInputTokens(claudeUsage.getCache_creation_input_tokens());
        antropicUsageRepository.save(anthropicUsage);
    }


    public byte[] downloadFile(String fileUrl) throws IOException {
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
