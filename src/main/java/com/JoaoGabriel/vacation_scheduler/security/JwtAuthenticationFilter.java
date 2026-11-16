package com.JoaoGabriel.vacation_scheduler.security;

import com.JoaoGabriel.vacation_scheduler.employee.EmployeeRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication
        .UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final EmployeeRepository employeeRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorizationHeader =
                request.getHeader("Authorization");

        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")
                || SecurityContextHolder.getContext().getAuthentication() != null) {

            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader
                .substring(7)
                .trim();

        try {
            String email = jwtService.extractEmail(token);

            /*
             * O banco continua sendo a fonte da verdade: conta removida ou
             * papel alterado vale na hora, sem esperar o token expirar.
             */
            employeeRepository
                    .findByEmail(email)
                    .map(AuthenticatedUser::from)
                    .ifPresent(user -> SecurityContextHolder
                            .getContext()
                            .setAuthentication(
                                    new UsernamePasswordAuthenticationToken(
                                            user,
                                            null,
                                            user.authorities()
                                    )
                            ));

        } catch (JwtException | IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
