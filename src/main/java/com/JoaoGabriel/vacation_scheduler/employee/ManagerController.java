package com.JoaoGabriel.vacation_scheduler.employee;

import com.JoaoGabriel.vacation_scheduler.employee.dto.ManagerRequest;
import com.JoaoGabriel.vacation_scheduler.employee.dto.ManagerResponse;
import com.JoaoGabriel.vacation_scheduler.security.AuthenticatedUser;
import com.JoaoGabriel.vacation_scheduler.vacation.dto.VacationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
 * POST /managers é público de propósito: o gestor se cadastra e repassa o
 * código gerado para a equipe. As demais rotas exigem ROLE_MANAGER
 * (ver SecurityConfig).
 */
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
    public List<VacationResponse> listEmployeeVacations(
            @AuthenticationPrincipal AuthenticatedUser manager
    ) {
        return managerService.listEmployeeVacations(manager.id());
    }

    @PatchMapping("/vacations/{id}/approve")
    public VacationResponse approveVacation(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser manager
    ) {
        return managerService.approveVacation(id, manager.id());
    }

    @PatchMapping("/vacations/{id}/reject")
    public VacationResponse rejectVacation(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser manager
    ) {
        return managerService.rejectVacation(id, manager.id());
    }
}
