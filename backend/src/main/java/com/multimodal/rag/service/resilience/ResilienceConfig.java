package com.multimodal.rag.service.resilience;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for the resilience module (retry, backoff, circuit breaker).
 * All parameters are configurable via application.yml under "resilience.*".
 */
@Data
@Component
@ConfigurationProperties(prefix = "resilience")
public class ResilienceConfig {

    /** Maximum number of retry attempts for transient failures. */
    private int maxRetries = 3;

    /** Base delay in milliseconds for exponential backoff (1st retry = baseDelay, 2nd = baseDelay*2, etc.). */
    private long baseDelayMs = 1000;

    /** Maximum backoff delay in milliseconds (caps exponential growth). */
    private long maxDelayMs = 8000;

    /** Random jitter factor (0.0-1.0) added to backoff to avoid thundering herd. */
    private double jitterFactor = 0.2;

    /** Number of consecutive failures before the circuit breaker opens. */
    private int circuitBreakerFailureThreshold = 5;

    /** Time in seconds the circuit breaker stays open before transitioning to half-open. */
    private long circuitBreakerRecoveryTimeoutSec = 60;

    /** Number of test requests allowed in half-open state to probe recovery. */
    private int circuitBreakerHalfOpenTestRequests = 1;

    /** Maximum number of keyword results returned in FULLTEXT fallback mode. */
    private int fallbackKeywordLimit = 10;
}