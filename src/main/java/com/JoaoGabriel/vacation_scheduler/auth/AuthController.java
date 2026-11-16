package com.JoaoGabriel.vacation_scheduler.auth;

import com.JoaoGabriel.vacation_scheduler.auth.dto.LoginRequest;
import com.JoaoGabriel.vacation_scheduler.auth.dto.LoginResponse;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        return authService.login(
                request,
                httpRequest.getRemoteAddr()
        );
    }
}