package com.JoaoGabriel.vacation_scheduler.vacation;

import com.JoaoGabriel.vacation_scheduler.employee.Employee;
import com.JoaoGabriel.vacation_scheduler.vacation.dto.VacationRequest;
import com.JoaoGabriel.vacation_scheduler.vacation.dto.VacationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/vacations")
@RequiredArgsConstructor
public class VacationController {

    private final VacationService vacationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VacationResponse create(
            @Valid @RequestBody VacationRequest request,
            Authentication authentication
    ) {

        Employee employee =
                (Employee) authentication.getPrincipal();

        return vacationService.create(
                request,
                employee
        );
    }

    @GetMapping("/me")
    public List<VacationResponse> findMyVacations(
            Authentication authentication
    ) {
        Employee employee =
                (Employee) authentication.getPrincipal();

        return vacationService.findByEmployee(employee);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Employee employee =
                (Employee) authentication.getPrincipal();

        vacationService.cancel(
                id,
                employee
        );
    }
}