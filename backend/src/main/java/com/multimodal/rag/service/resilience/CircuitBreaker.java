package com.multimodal.rag.service.resilience;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Simple circuit breaker with three states: CLOSED, OPEN, HALF_OPEN.
 *
 * <p>State transitions:</p>
 * <ul>
 *   <li>CLOSED → OPEN: when consecutive failures reach {@code failureThreshold}</li>
 *   <li>OPEN → HALF_OPEN: after {@code recoveryTimeoutSec} seconds elapse</li>
 *   <li>HALF_OPEN → CLOSED: when a test request succeeds</li>
 *   <li>HALF_OPEN → OPEN: when a test request fails</li>
 * </ul>
 *
 * <p>Thread-safe via atomic primitives. No external locking required.</p>
 */
@Slf4j
public class CircuitBreaker {

    public enum State { CLOSED, OPEN, HALF_OPEN }

    private final String name;
    private final int failureThreshold;
    private final long recoveryTimeoutMs;
    private final int halfOpenTestRequests;

    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong openedAt = new AtomicLong(0);
    private final AtomicInteger halfOpenSuccessCount = new AtomicInteger(0);
    private final AtomicInteger halfOpenTestCount = new AtomicInteger(0);

    public CircuitBreaker(String name, int failureThreshold, long recoveryTimeoutMs, int halfOpenTestRequests) {
        this.name = name;
        this.failureThreshold = failureThreshold;
        this.recoveryTimeoutMs = recoveryTimeoutMs;
        this.halfOpenTestRequests = halfOpenTestRequests;
        log.info("[CircuitBreaker] '{}' initialized: threshold={}, recoveryTimeout={}ms, halfOpenTests={}",
                name, failureThreshold, recoveryTimeoutMs, halfOpenTestRequests);
    }

    /**
     * @return true if requests should be allowed through
     */
    public boolean allowRequest() {
        State current = state.get();

        if (current == State.CLOSED) {
            return true;
        }

        if (current == State.OPEN) {
            long elapsed = System.currentTimeMillis() - openedAt.get();
            if (elapsed >= recoveryTimeoutMs) {
                if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                    halfOpenTestCount.set(0);
                    halfOpenSuccessCount.set(0);
                    log.info("[CircuitBreaker] '{}' transitioned OPEN → HALF_OPEN after {}ms", name, elapsed);
                }
                return true;
            }
            log.debug("[CircuitBreaker] '{}' is OPEN, rejecting request ({}ms until HALF_OPEN)",
                    name, recoveryTimeoutMs - elapsed);
            return false;
        }

        // HALF_OPEN: allow limited test requests
        int tests = halfOpenTestCount.incrementAndGet();
        if (tests <= halfOpenTestRequests) {
            return true;
        }
        halfOpenTestCount.decrementAndGet();
        log.debug("[CircuitBreaker] '{}' is HALF_OPEN, rate-limiting test requests", name);
        return false;
    }

    /**
     * Record a successful call. May transition HALF_OPEN → CLOSED.
     */
    public void recordSuccess() {
        consecutiveFailures.set(0);

        State current = state.get();
        if (current == State.HALF_OPEN) {
            int successes = halfOpenSuccessCount.incrementAndGet();
            if (successes >= halfOpenTestRequests) {
                if (state.compareAndSet(State.HALF_OPEN, State.CLOSED)) {
                    log.info("[CircuitBreaker] '{}' transitioned HALF_OPEN → CLOSED (service recovered)", name);
                }
            }
        }
    }

    /**
     * Record a failed call. May transition CLOSED → OPEN or HALF_OPEN → OPEN.
     */
    public void recordFailure() {
        int failures = consecutiveFailures.incrementAndGet();

        State current = state.get();
        if (current == State.CLOSED && failures >= failureThreshold) {
            if (state.compareAndSet(State.CLOSED, State.OPEN)) {
                openedAt.set(System.currentTimeMillis());
                log.warn("[CircuitBreaker] '{}' transitioned CLOSED → OPEN after {} consecutive failures", name, failures);
            }
        } else if (current == State.HALF_OPEN) {
            if (state.compareAndSet(State.HALF_OPEN, State.OPEN)) {
                openedAt.set(System.currentTimeMillis());
                log.warn("[CircuitBreaker] '{}' transitioned HALF_OPEN → OPEN (test request failed, service still unhealthy)", name);
            }
        } else {
            log.debug("[CircuitBreaker] '{}' recorded failure {}/{}", name, failures, failureThreshold);
        }
    }

    public State getState() {
        if (state.get() == State.OPEN) {
            allowRequest();
        }
        return state.get();
    }

    public String getName() {
        return name;
    }

    public int getConsecutiveFailures() {
        return consecutiveFailures.get();
    }

    /**
     * Manually reset the breaker to CLOSED. Useful for admin/health endpoints.
     */
    public void reset() {
        consecutiveFailures.set(0);
        halfOpenTestCount.set(0);
        halfOpenSuccessCount.set(0);
        if (state.getAndSet(State.CLOSED) != State.CLOSED) {
            log.info("[CircuitBreaker] '{}' manually reset to CLOSED", name);
        }
    }
}