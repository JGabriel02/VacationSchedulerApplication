package com.JoaoGabriel.vacation_scheduler.auth;

import com.JoaoGabriel.vacation_scheduler.auth.dto.LoginRequest;
import com.JoaoGabriel.vacation_scheduler.auth.dto.LoginResponse;
import com.JoaoGabriel.vacation_scheduler.auth.exception.InvalidCredentialsException;
import com.JoaoGabriel.vacation_scheduler.employee.Employee;
import com.JoaoGabriel.vacation_scheduler.employee.EmployeeRepository;
import com.JoaoGabriel.vacation_scheduler.employee.EmployeeRole;
import com.JoaoGabriel.vacation_scheduler.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private LoginRateLimiter loginRateLimiter;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode("login-timing-protection")).thenReturn("dummy-hash");

        authService = new AuthService(
                employeeRepository,
                passwordEncoder,
                jwtService,
                loginRateLimiter
        );
    }

    @Test
    void unknownEmailStillRunsThePasswordCheckAndCountsAsFailure() {
        LoginRequest request = new LoginRequest("ninguem@empresa.com", "senha1234");

        when(employeeRepository.findByEmail(request.email())).thenReturn(Optional.empty());

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(request, "10.0.0.1")
        );

        assertEquals("E-mail ou senha inválidos", exception.getMessage());
        verify(loginRateLimiter).checkAllowed("10.0.0.1", request.email());
        verify(passwordEncoder).matches("senha1234", "dummy-hash");
        verify(loginRateLimiter).recordFailure(request.email());
    }

    @Test
    void wrongPasswordCountsAsFailure() {
        Employee employee = employee();
        LoginRequest request = new LoginRequest(employee.getEmail(), "errada123");

        when(employeeRepository.findByEmail(employee.getEmail())).thenReturn(Optional.of(employee));
        when(passwordEncoder.matches("errada123", "real-hash")).thenReturn(false);

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(request, "10.0.0.1")
        );

        verify(loginRateLimiter).recordFailure(employee.getEmail());
        verify(jwtService, never()).generateToken(employee.getEmail(), EmployeeRole.EMPLOYEE);
    }

    @Test
    void successfulLoginResetsCounterAndPutsRoleInToken() {
        Employee employee = employee();
        LoginRequest request = new LoginRequest(employee.getEmail(), "certa1234");

        when(employeeRepository.findByEmail(employee.getEmail())).thenReturn(Optional.of(employee));
        when(passwordEncoder.matches("certa1234", "real-hash")).thenReturn(true);
        when(jwtService.generateToken(employee.getEmail(), EmployeeRole.EMPLOYEE)).thenReturn("token");

        LoginResponse response = authService.login(request, "10.0.0.1");

        assertEquals("token", response.token());
        verify(loginRateLimiter).recordSuccess(employee.getEmail());
    }

    private Employee employee() {
        Employee employee = new Employee();
        employee.setId(1L);
        employee.setNome("Ana");
        employee.setEmail("ana@empresa.com");
        employee.setPassword("real-hash");
        employee.setRole(EmployeeRole.EMPLOYEE);
        return employee;
    }
}
