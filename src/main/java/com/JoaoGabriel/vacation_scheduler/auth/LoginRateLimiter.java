package com.JoaoGabriel.vacation_scheduler.auth;

import com.JoaoGabriel.vacation_scheduler.auth.exception.TooManyLoginAttemptsException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*
 * Limite de tentativas de login em memória, com janela fixa:
 *  - por IP: conta todas as tentativas (freia varredura de muitos e-mails);
 *  - por e-mail: conta só as falhas e zera no login bem-sucedido
 *    (freia força bruta em uma conta, mesmo trocando de IP).
 *
 * Por ser em memória, vale por instância. Com mais de uma réplica, o ideal
 * é mover os contadores para um store compartilhado (ex.: Redis).
 */
@Component
public class LoginRateLimiter {

    private final Clock clock;
    private final LoginRateLimitProperties properties;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public LoginRateLimiter(Clock clock, LoginRateLimitProperties properties) {
        this.clock = clock;
        this.properties = properties;
    }

    public void checkAllowed(String clientIp, String email) {
        Instant now = clock.instant();

        Window ipWindow = windows.compute(
                ipKey(clientIp),
                (key, current) -> increment(current, now, properties.ipWindow())
        );

        if (ipWindow.count() > properties.maxAttemptsPerIp()) {
            throw new TooManyLoginAttemptsException(
                    ipWindow.secondsUntilReset(now, properties.ipWindow())
            );
        }

        Window emailWindow = windows.get(emailKey(email));

        if (emailWindow != null
                && !emailWindow.isExpired(now, properties.emailWindow())
                && emailWindow.count() >= properties.maxFailuresPerEmail()) {

            throw new TooManyLoginAttemptsException(
                    emailWindow.secondsUntilReset(now, properties.emailWindow())
            );
        }
    }

    public void recordFailure(String email) {
        Instant now = clock.instant();

        windows.compute(
                emailKey(email),
                (key, current) -> increment(current, now, properties.emailWindow())
        );
    }

    public void recordSuccess(String email) {
        windows.remove(emailKey(email));
    }

    @Scheduled(fixedDelayString = "PT10M")
    void removeExpiredWindows() {
        Instant now = clock.instant();
        Duration longest = properties.ipWindow().compareTo(properties.emailWindow()) > 0
                ? properties.ipWindow()
                : properties.emailWindow();

        windows.values().removeIf(window -> window.isExpired(now, longest));
    }

    private static Window increment(Window current, Instant now, Duration length) {
        if (current == null || current.isExpired(now, length)) {
            return new Window(now, 1);
        }

        return new Window(current.start(), current.count() + 1);
    }

    private static String ipKey(String clientIp) {
        return "ip:" + (clientIp == null ? "unknown" : clientIp);
    }

    private static String emailKey(String email) {
        return "email:" + (email == null ? "" : email.trim().toLowerCase(Locale.ROOT));
    }

    private record Window(Instant start, int count) {

        boolean isExpired(Instant now, Duration length) {
            return !now.isBefore(start.plus(length));
        }

        long secondsUntilReset(Instant now, Duration length) {
            long seconds = Duration.between(now, start.plus(length)).toSeconds();
            return Math.max(1, seconds);
        }
    }
}
