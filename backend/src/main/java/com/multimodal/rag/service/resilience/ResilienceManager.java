package com.multimodal.rag.service.resilience;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Central resilience manager that provides:
 * <ol>
 *   <li><b>Automatic retry</b> with exponential backoff + jitter for transient failures</li>
 *   <li><b>Circuit breaker</b> per named service to prevent cascading failures</li>
 *   <li><b>Graceful degradation</b> to MySQL FULLTEXT search when vector DB is unavailable</li>
 * </ol>
 *
 * <p>Usage example:</p>
 * <pre>
 *   ResilienceManager.ResilientSearchResult result = resilienceManager.executeVectorSearch(
 *       query,
 *       () -&gt; vectorStore.search(query, topK),
 *       () -&gt; fulltextSearch(query, userId)
 *   );
 *   // result.fallback() == true means FULLTEXT was used
 *   List&lt;Document&gt; docs = result.documents();
 * </pre>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ResilienceManager {

    private final ResilienceConfig config;

    /** Named circuit breakers — one per downstream service. */
    private final Map<String, CircuitBreaker> breakers = new ConcurrentHashMap<>();

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Execute a callable with automatic retry and circuit-breaker protection.
     * If the circuit is open and no fallback is provided, throws immediately.
     *
     * @param serviceName  name for the circuit breaker (e.g. "vector-search", "llm-chat")
     * @param action       the primary operation to attempt
     * @param <T>          return type
     * @return result of the action on success
     * @throws RuntimeException if all retries exhausted and no fallback available
     */
    public <T> T executeWithRetry(String serviceName, Supplier<T> action) {
        return executeWithRetry(serviceName, action, null);
    }

    /**
     * Execute a callable with retry + circuit-breaker + optional fallback.
     *
     * <p>When the circuit breaker is OPEN and a fallback is supplied,
     * the fallback is called immediately without attempting the primary action.
     * When the circuit is CLOSED or HALF_OPEN, the primary action is attempted
     * with retries; if all retries fail and a fallback exists, the fallback is used.</p>
     *
     * @param serviceName  name for the circuit breaker
     * @param action       the primary operation
     * @param fallback     optional fallback to invoke when all retries fail or circuit is open
     * @param <T>          return type
     * @return result from action (preferred) or fallback
     */
    public <T> T executeWithRetry(String serviceName, Supplier<T> action, Supplier<T> fallback) {
        CircuitBreaker breaker = getOrCreateBreaker(serviceName);

        if (!breaker.allowRequest()) {
            log.warn("[Resilience] Circuit '{}' is OPEN — skipping primary action", serviceName);
            if (fallback != null) {
                log.info("[Resilience] Executing fallback for '{}'", serviceName);
                return fallback.get();
            }
            throw new RuntimeException("Service '" + serviceName + "' is unavailable (circuit open) and no fallback configured");
        }

        int maxRetries = config.getMaxRetries();
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                T result = action.get();
                breaker.recordSuccess();
                if (attempt > 1) {
                    log.info("[Resilience] '{}' succeeded on attempt {}/{} after transient failures", serviceName, attempt, maxRetries);
                }
                return result;
            } catch (Exception e) {
                lastException = e;

                if (!isRetryable(e)) {
                    log.error("[Resilience] '{}' encountered non-retryable error on attempt {}/{}: {}",
                            serviceName, attempt, maxRetries, e.getMessage());
                    breaker.recordFailure();
                    break;
                }

                if (attempt < maxRetries) {
                    long delay = calculateBackoff(attempt);
                    log.warn("[Resilience] '{}' failed on attempt {}/{}: {}. Retrying in {}ms...",
                            serviceName, attempt, maxRetries, e.getMessage(), delay);
                    sleep(delay);
                } else {
                    log.error("[Resilience] '{}' failed after {} attempts: {}",
                            serviceName, maxRetries, e.getMessage());
                    breaker.recordFailure();
                }
            }
        }

        // All retries exhausted — try fallback
        if (fallback != null) {
            log.warn("[Resilience] '{}' exhausted all retries, falling back to alternative", serviceName);
            try {
                return fallback.get();
            } catch (Exception fbEx) {
                log.error("[Resilience] Fallback for '{}' also failed: {}", serviceName, fbEx.getMessage());
                throw new RuntimeException("Both primary action and fallback failed for '" + serviceName + "'", fbEx);
            }
        }

        throw new RuntimeException("Service '" + serviceName + "' failed after " + maxRetries + " attempts: "
                + (lastException != null ? lastException.getMessage() : "unknown error"), lastException);
    }

    /**
     * Execute a vector search with full resilience: retry + circuit breaker + FULLTEXT fallback.
     *
     * @param query               search query
     * @param vectorSearchAction  primary vector search (returns List&lt;Document&gt;)
     * @param fulltextFallback    MySQL FULLTEXT fallback search
     * @return search results, tagged with metadata indicating fallback usage
     */
    public ResilientSearchResult executeVectorSearch(
            String query,
            Supplier<List<Document>> vectorSearchAction,
            Supplier<List<Document>> fulltextFallback
    ) {
        CircuitBreaker breaker = getOrCreateBreaker("vector-search");

        // If circuit is open, go straight to fallback
        if (!breaker.allowRequest()) {
            log.warn("[Resilience] Vector search circuit OPEN — using FULLTEXT fallback for query: '{}'", query);
            List<Document> results = fulltextFallback.get();
            return new ResilientSearchResult(results, true, breaker.getState().name());
        }

        // Try vector search with retries
        int maxRetries = config.getMaxRetries();
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                List<Document> results = vectorSearchAction.get();
                breaker.recordSuccess();
                if (attempt > 1) {
                    log.info("[Resilience] Vector search succeeded on attempt {}/{}", attempt, maxRetries);
                }
                return new ResilientSearchResult(results, false, breaker.getState().name());
            } catch (Exception e) {
                lastException = e;

                if (!isRetryable(e)) {
                    log.error("[Resilience] Vector search non-retryable error on attempt {}/{}: {}",
                            attempt, maxRetries, e.getMessage());
                    breaker.recordFailure();
                    break;
                }

                if (attempt < maxRetries) {
                    long delay = calculateBackoff(attempt);
                    log.warn("[Resilience] Vector search failed on attempt {}/{}: {}. Retrying in {}ms...",
                            attempt, maxRetries, e.getMessage(), delay);
                    sleep(delay);
                } else {
                    log.error("[Resilience] Vector search failed after {} attempts", maxRetries);
                    breaker.recordFailure();
                }
            }
        }

        // Vector search exhausted — degrade to FULLTEXT
        log.warn("[Resilience] Vector search fully degraded to FULLTEXT for query: '{}'", query);
        try {
            List<Document> fallbackResults = fulltextFallback.get();
            return new ResilientSearchResult(fallbackResults, true, breaker.getState().name());
        } catch (Exception fbEx) {
            log.error("[Resilience] FULLTEXT fallback also failed: {}", fbEx.getMessage());
            throw new RuntimeException("Both vector and FULLTEXT search failed", fbEx);
        }
    }

    // ── Health / Status ────────────────────────────────────────────────────

    /**
     * Get the current state of a named circuit breaker.
     */
    public CircuitBreaker.State getCircuitState(String serviceName) {
        CircuitBreaker breaker = breakers.get(serviceName);
        return breaker != null ? breaker.getState() : CircuitBreaker.State.CLOSED;
    }

    /**
     * Get a summary of all circuit breakers for health endpoints.
     */
    public Map<String, Map<String, Object>> getHealthSummary() {
        Map<String, Map<String, Object>> summary = new ConcurrentHashMap<>();
        for (Map.Entry<String, CircuitBreaker> entry : breakers.entrySet()) {
            CircuitBreaker b = entry.getValue();
            Map<String, Object> info = new ConcurrentHashMap<>();
            info.put("state", b.getState().name());
            info.put("consecutiveFailures", b.getConsecutiveFailures());
            summary.put(entry.getKey(), info);
        }
        return summary;
    }

    /**
     * Manually reset a circuit breaker (admin use).
     */
    public void resetCircuit(String serviceName) {
        CircuitBreaker breaker = breakers.get(serviceName);
        if (breaker != null) {
            breaker.reset();
        }
    }

    // ── Internal ───────────────────────────────────────────────────────────

    private CircuitBreaker getOrCreateBreaker(String serviceName) {
        return breakers.computeIfAbsent(serviceName, name ->
                new CircuitBreaker(
                        name,
                        config.getCircuitBreakerFailureThreshold(),
                        config.getCircuitBreakerRecoveryTimeoutSec() * 1000,
                        config.getCircuitBreakerHalfOpenTestRequests()
                )
        );
    }

    /**
     * Determine if an exception is transient and worth retrying.
     */
    private boolean isRetryable(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage() : "";
        // Network / connection transient errors
        if (msg.contains("Connection reset")
                || msg.contains("I/O error")
                || msg.contains("Connection refused")
                || msg.contains("Connection timed out")
                || msg.contains("Read timed out")
                || msg.contains("SocketException")
                || msg.contains("No route to host")
                || msg.contains("Broken pipe")) {
            return true;
        }
        // Java SQL transient errors
        if (msg.contains("Communications link failure")
                || msg.contains("Connection is closed")
                || msg.contains("SSL handshake")) {
            return true;
        }
        // Spring AI / HTTP 5xx
        if (msg.contains("500")
                || msg.contains("502")
                || msg.contains("503")
                || msg.contains("504")) {
            return true;
        }
        // Known retryable exception types
        Throwable cause = e;
        while (cause != null) {
            String className = cause.getClass().getSimpleName();
            if (className.equals("ConnectException")
                    || className.equals("SocketTimeoutException")
                    || className.equals("UnknownHostException")
                    || className.equals("NoRouteToHostException")
                    || className.equals("SSLHandshakeException")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    /**
     * Exponential backoff with jitter: min(base * 2^(attempt-1), max) * (1 +/- jitter)
     */
    private long calculateBackoff(int attempt) {
        long baseDelay = config.getBaseDelayMs();
        long maxDelay = config.getMaxDelayMs();
        double jitter = config.getJitterFactor();

        long exponentialDelay = baseDelay * (1L << (attempt - 1));
        long cappedDelay = Math.min(exponentialDelay, maxDelay);

        double jitterRange = cappedDelay * jitter;
        double randomizedDelay = cappedDelay + (ThreadLocalRandom.current().nextDouble(-1, 1) * jitterRange);

        return Math.max(100, Math.round(randomizedDelay));
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Retry sleep interrupted", e);
        }
    }

    // ── Result wrapper ─────────────────────────────────────────────────────

    /**
     * Wraps search results with metadata about whether fallback was used.
     */
    public record ResilientSearchResult(
            List<Document> documents,
            boolean fallback,
            String circuitState
    ) {
        /**
         * Build a response map with the fallback flag for API consumers.
         */
        public Map<String, Object> toResponseMap() {
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("fallback", fallback);
            map.put("circuitState", circuitState);
            map.put("resultCount", documents != null ? documents.size() : 0);
            return map;
        }
    }
}