package com.JoaoGabriel.vacation_scheduler.auth;

import com.JoaoGabriel.vacation_scheduler.auth.dto.LoginRequest;
import com.JoaoGabriel.vacation_scheduler.auth.dto.LoginResponse;
import com.JoaoGabriel.vacation_scheduler.auth.exception.InvalidCredentialsException;
import com.JoaoGabriel.vacation_scheduler.employee.Employee;
import com.JoaoGabriel.vacation_scheduler.employee.EmployeeRepository;
import com.JoaoGabriel.vacation_scheduler.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS =
            "E-mail ou senha inválidos";

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginRateLimiter loginRateLimiter;

    /*
     * Hash usado quando o e-mail não existe. Assim o BCrypt roda nos dois
     * casos e o tempo de resposta não revela quais e-mails estão cadastrados.
     */
    private final String dummyPasswordHash;

    public AuthService(
            EmployeeRepository employeeRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            LoginRateLimiter loginRateLimiter
    ) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginRateLimiter = loginRateLimiter;
        this.dummyPasswordHash = passwordEncoder.encode("login-timing-protection");
    }

    public LoginResponse login(LoginRequest request, String clientIp) {

        loginRateLimiter.checkAllowed(clientIp, request.email());

        Optional<Employee> found =
                employeeRepository.findByEmail(request.email());

        boolean passwordMatches = passwordEncoder.matches(
                request.password(),
                found.map(Employee::getPassword).orElse(dummyPasswordHash)
        );

        if (found.isEmpty() || !passwordMatches) {
            loginRateLimiter.recordFailure(request.email());

            throw new InvalidCredentialsException(INVALID_CREDENTIALS);
        }

        loginRateLimiter.recordSuccess(request.email());

        Employee employee = found.get();

        String token = jwtService.generateToken(
                employee.getEmail(),
                employee.getRole()
        );

        return new LoginResponse(
                employee.getId(),
                employee.getNome(),
                employee.getEmail(),
                employee.getRole(),
                employee.getManagerCode(),
                token
        );
    }
}
