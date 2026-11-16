package com.JoaoGabriel.vacation_scheduler.employee;

import com.JoaoGabriel.vacation_scheduler.employee.dto.EmployeeRequest;
import com.JoaoGabriel.vacation_scheduler.employee.dto.EmployeeResponse;
import com.JoaoGabriel.vacation_scheduler.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    public EmployeeResponse create(@Valid @RequestBody EmployeeRequest request) {
        return employeeService.create(request);
    }

    @GetMapping("/me")
    public EmployeeResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
        return new EmployeeResponse(
                user.id(),
                user.nome(),
                user.email()
        );
    }
}
