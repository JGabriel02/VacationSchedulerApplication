package com.JoaoGabriel.vacation_scheduler.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("app.login-rate-limit")
public record LoginRateLimitProperties(
        Integer maxAttemptsPerIp,
        Duration ipWindow,
        Integer maxFailuresPerEmail,
        Duration emailWindow
) {

    public LoginRateLimitProperties {
        if (maxAttemptsPerIp == null) maxAttemptsPerIp = 20;
        if (ipWindow == null) ipWindow = Duration.ofMinutes(1);
        if (maxFailuresPerEmail == null) maxFailuresPerEmail = 5;
        if (emailWindow == null) emailWindow = Duration.ofMinutes(15);
    }
}
