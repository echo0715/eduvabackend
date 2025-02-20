package com.eduva.eduva.service;

import com.eduva.eduva.dto.ClaudeResponse.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
public class RateLimitHandler {

    private static final int DEFAULT_RETRY_AFTER_SECONDS = 60;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * Executes a request with rate limit handling
     *
     * @param requestSupplier The supplier that executes the API request
     * @param <T> The return type of the request
     * @return The result of the request
     * @throws IOException If an error occurs during the request
     */
    public <T> T executeWithRateLimitHandling(Supplier<T> requestSupplier) throws IOException {
        return executeWithRateLimitHandling(requestSupplier, 0);
    }

    /**
     * Executes a request with rate limit handling and retry logic
     *
     * @param requestSupplier The supplier that executes the API request
     * @param retryCount Current retry count
     * @param <T> The return type of the request
     * @return The result of the request
     * @throws IOException If an error occurs during the request or max retries exceeded
     */
    private <T> T executeWithRateLimitHandling(Supplier<T> requestSupplier, int retryCount) throws IOException {
        try {
            return requestSupplier.get();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                if (retryCount >= MAX_RETRY_ATTEMPTS) {
                    log.error("Maximum retry attempts reached for rate-limited request");
                    throw new IOException("Rate limit exceeded and maximum retries attempted", e);
                }

                // Extract retry-after header or use default
                int retryAfterSeconds = getRetryAfterSeconds(e);
                log.warn("Rate limit hit. Waiting for {} seconds before retry. Attempt: {}/{}",
                        retryAfterSeconds, retryCount + 1, MAX_RETRY_ATTEMPTS);

                try {
                    // Simple synchronous wait
                    Thread.sleep(retryAfterSeconds * 1000L);
                    return executeWithRateLimitHandling(requestSupplier, retryCount + 1);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while waiting for rate limit reset", ie);
                }
            } else {
                throw new IOException("API request failed: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            throw new IOException("API request failed: " + e.getMessage(), e);
        }
    }

    /**
     * Executes a batch of requests with rate limiting awareness
     *
     * @param requests The list of request suppliers to execute
     * @param <T> The return type of each request
     * @return List of results from all requests
     * @throws IOException If an error occurs that can't be handled
     */
    public <T> List<T> executeBatchWithRateLimiting(List<Supplier<T>> requests) throws IOException {
        if (requests.isEmpty()) {
            return Collections.emptyList();
        }

        List<T> results = new ArrayList<>(requests.size());

        // Execute first request
        T firstResult = executeWithRateLimitHandling(requests.get(0));
        results.add(firstResult);

        if (requests.size() == 1) {
            return results;
        }

        // Execute remaining requests with rate limit awareness
        for (int i = 1; i < requests.size(); i++) {
            try {
                T result = executeWithRateLimitHandling(requests.get(i));
                results.add(result);
            } catch (IOException e) {
                log.error("Failed to execute request at index {}: {}", i, e.getMessage());
                throw e;
            }
        }

        return results;
    }

    /**
     * Executes a batch of requests asynchronously with rate limiting awareness
     *
     * @param requests The list of request suppliers to execute
     * @param <T> The return type of each request
     * @return List of CompletableFuture results from all requests
     */
    public <T> List<CompletableFuture<T>> executeBatchAsyncWithRateLimiting(List<Supplier<T>> requests) {
        if (requests.isEmpty()) {
            return Collections.emptyList();
        }

        List<CompletableFuture<T>> futures = new ArrayList<>(requests.size());

        // Create a queue for requests waiting to be executed
        Queue<Integer> requestQueue = new LinkedList<>();
        for (int i = 0; i < requests.size(); i++) {
            requestQueue.add(i);
        }

        // Create CompletableFutures for all requests
        for (int i = 0; i < requests.size(); i++) {
            futures.add(new CompletableFuture<>());
        }

        // Start processing the queue
        processNextInQueue(requests, futures, requestQueue, 0);

        return futures;
    }

    /**
     * Recursively processes the request queue, handling rate limits
     */
    private <T> void processNextInQueue(
            List<Supplier<T>> requests,
            List<CompletableFuture<T>> futures,
            Queue<Integer> requestQueue,
            int delayMs) {

        if (requestQueue.isEmpty()) {
            return;
        }

        int index = requestQueue.poll();

        scheduler.schedule(() -> {
            try {
                T result = executeWithRateLimitHandling(requests.get(index));
                futures.get(index).complete(result);
                // Process next request immediately
                processNextInQueue(requests, futures, requestQueue, 0);
            } catch (Exception e) {
                if (e.getCause() instanceof HttpClientErrorException &&
                        ((HttpClientErrorException) e.getCause()).getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {

                    // Put the request back in the queue
                    requestQueue.add(index);

                    int retryAfterMs = getRetryAfterSeconds((HttpClientErrorException) e.getCause()) * 1000;
                    log.warn("Rate limit hit. Delaying next request by {}ms", retryAfterMs);

                    // Process next request after delay
                    processNextInQueue(requests, futures, requestQueue, retryAfterMs);
                } else {
                    // Complete exceptionally for non-rate-limit errors
                    futures.get(index).completeExceptionally(e);
                    // Continue with next request
                    processNextInQueue(requests, futures, requestQueue, 0);
                }
            }
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Extract the Retry-After header value from the exception
     */
    private int getRetryAfterSeconds(HttpClientErrorException e) {
        try {
            String retryAfterHeader = e.getResponseHeaders().getFirst("Retry-After");
            if (retryAfterHeader != null) {
                try {
                    return Integer.parseInt(retryAfterHeader);
                } catch (NumberFormatException nfe) {
                    // Could be an HTTP-date format, which we'll handle as a timestamp
                    // For simplicity in this example, we're returning the default
                    log.warn("Could not parse Retry-After header: {}", retryAfterHeader);
                }
            }
        } catch (Exception ex) {
            log.warn("Error extracting Retry-After header", ex);
        }
        return DEFAULT_RETRY_AFTER_SECONDS;
    }

    /**
     * Shutdown the scheduler
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}