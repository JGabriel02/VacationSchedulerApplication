package com.JoaoGabriel.vacation_scheduler.auth;

import com.JoaoGabriel.vacation_scheduler.auth.exception.TooManyLoginAttemptsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginRateLimiterTest {

    private MutableClock clock;
    private LoginRateLimiter limiter;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-03-10T12:00:00Z"));
        limiter = new LoginRateLimiter(
                clock,
                new LoginRateLimitProperties(
                        5, Duration.ofMinutes(1),
                        3, Duration.ofMinutes(15)
                )
        );
    }

    @Test
    void blocksEmailAfterTooManyFailuresEvenFromAnotherIp() {
        for (int i = 0; i < 3; i++) {
            limiter.checkAllowed("10.0.0." + i, "ana@empresa.com");
            limiter.recordFailure("ana@empresa.com");
        }

        TooManyLoginAttemptsException exception = assertThrows(
                TooManyLoginAttemptsException.class,
                () -> limiter.checkAllowed("10.0.0.99", "ANA@empresa.com ")
        );

        assertEquals(15 * 60, exception.getRetryAfterSeconds());
    }

    @Test
    void successfulLoginResetsTheEmailCounter() {
        limiter.checkAllowed("10.0.0.1", "ana@empresa.com");
        limiter.recordFailure("ana@empresa.com");
        limiter.recordFailure("ana@empresa.com");
        limiter.recordSuccess("ana@empresa.com");
        limiter.recordFailure("ana@empresa.com");
        limiter.recordFailure("ana@empresa.com");

        assertDoesNotThrow(() -> limiter.checkAllowed("10.0.0.1", "ana@empresa.com"));
    }

    @Test
    void blocksIpThatTriesManyDifferentEmails() {
        for (int i = 0; i < 5; i++) {
            limiter.checkAllowed("10.0.0.1", "user" + i + "@empresa.com");
        }

        assertThrows(
                TooManyLoginAttemptsException.class,
                () -> limiter.checkAllowed("10.0.0.1", "outro@empresa.com")
        );
        assertDoesNotThrow(() -> limiter.checkAllowed("10.0.0.2", "outro@empresa.com"));
    }

    @Test
    void limitIsLiftedWhenTheWindowEnds() {
        for (int i = 0; i < 3; i++) {
            limiter.recordFailure("ana@empresa.com");
        }

        clock.advance(Duration.ofMinutes(15));

        assertDoesNotThrow(() -> limiter.checkAllowed("10.0.0.1", "ana@empresa.com"));
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
