package com.JoaoGabriel.vacation_scheduler.employee;

import com.JoaoGabriel.vacation_scheduler.employee.dto.EmployeeRequest;
import com.JoaoGabriel.vacation_scheduler.employee.exception.EmailAlreadyExistsException;
import com.JoaoGabriel.vacation_scheduler.employee.exception.ManagerCodeNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private EmployeeService employeeService;

    private Employee manager;

    @BeforeEach
    void setUp() {
        employeeService = new EmployeeService(
                employeeRepository,
                passwordEncoder
        );

        manager = new Employee();
        manager.setId(1L);
        manager.setRole(EmployeeRole.MANAGER);
        manager.setManagerCode("ABCD2345");
    }

    @Test
    void shouldRejectUnknownManagerCode() {
        when(employeeRepository.findByManagerCode("XXXX"))
                .thenReturn(Optional.empty());

        assertThrows(
                ManagerCodeNotFoundException.class,
                () -> employeeService.create(request("XXXX"))
        );

        verify(employeeRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldTranslateConcurrentDuplicateEmailIntoEmailAlreadyExists() {
        EmployeeRequest request = request("ABCD2345");

        when(employeeRepository.findByManagerCode("ABCD2345"))
                .thenReturn(Optional.of(manager));

        // Primeira checagem passa; outra requisição grava o mesmo e-mail antes do flush.
        when(employeeRepository.existsByEmail(request.email()))
                .thenReturn(false, true);

        when(employeeRepository.saveAndFlush(any(Employee.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThrows(
                EmailAlreadyExistsException.class,
                () -> employeeService.create(request)
        );
    }

    @Test
    void shouldRethrowIntegrityViolationNotCausedByEmail() {
        EmployeeRequest request = request("ABCD2345");

        when(employeeRepository.findByManagerCode("ABCD2345"))
                .thenReturn(Optional.of(manager));
        when(employeeRepository.existsByEmail(request.email()))
                .thenReturn(false, false);
        when(employeeRepository.saveAndFlush(any(Employee.class)))
                .thenThrow(new DataIntegrityViolationException("other"));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> employeeService.create(request)
        );
    }

    private EmployeeRequest request(String managerCode) {
        return new EmployeeRequest(
                "Maria",
                "maria@empresa.com",
                "senha123",
                LocalDate.now().minusYears(1),
                managerCode
        );
    }
}
