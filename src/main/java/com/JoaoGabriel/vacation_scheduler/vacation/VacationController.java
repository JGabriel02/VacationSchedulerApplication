package com.JoaoGabriel.vacation_scheduler.vacation;

import com.JoaoGabriel.vacation_scheduler.security.AuthenticatedUser;
import com.JoaoGabriel.vacation_scheduler.vacation.dto.VacationRequest;
import com.JoaoGabriel.vacation_scheduler.vacation.dto.VacationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return vacationService.create(request, user.id());
    }

    @GetMapping("/me")
    public List<VacationResponse> findMyVacations(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return vacationService.findByEmployee(user.id());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        vacationService.cancel(id, user.id());
    }
}
