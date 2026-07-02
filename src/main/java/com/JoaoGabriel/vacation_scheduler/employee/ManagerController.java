package com.JoaoGabriel.vacation_scheduler.employee;

import com.JoaoGabriel.vacation_scheduler.employee.dto.ManagerRequest;
import com.JoaoGabriel.vacation_scheduler.employee.dto.ManagerResponse;
import com.JoaoGabriel.vacation_scheduler.vacation.dto.VacationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/managers")
@RequiredArgsConstructor
public class ManagerController {

    private final EmployeeService employeeService;
    private final ManagerService managerService;

    @PostMapping
    public ResponseEntity<ManagerResponse> create(
            @Valid @RequestBody ManagerRequest request
    ) {
        ManagerResponse response =
                employeeService.createManager(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/vacations")
    public ResponseEntity<List<VacationResponse>>
    listEmployeeVacations(
            Authentication authentication
    ) {
        Employee authenticatedEmployee =
                getAuthenticatedEmployee(authentication);

        List<VacationResponse> vacations =
                managerService.listEmployeeVacations(
                        authenticatedEmployee
                );

        return ResponseEntity.ok(vacations);
    }

    @PatchMapping("/vacations/{id}/approve")
    public ResponseEntity<VacationResponse>
    approveVacation(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Employee authenticatedEmployee =
                getAuthenticatedEmployee(authentication);

        VacationResponse response =
                managerService.approveVacation(
                        id,
                        authenticatedEmployee
                );

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/vacations/{id}/reject")
    public ResponseEntity<VacationResponse>
    rejectVacation(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Employee authenticatedEmployee =
                getAuthenticatedEmployee(authentication);

        VacationResponse response =
                managerService.rejectVacation(
                        id,
                        authenticatedEmployee
                );

        return ResponseEntity.ok(response);
    }

    private Employee getAuthenticatedEmployee(
            Authentication authentication
    ) {
        if (authentication == null
                || !(authentication.getPrincipal()
                instanceof Employee employee)) {

            return null;
        }

        return employee;
    }
}