package com.JoaoGabriel.vacation_scheduler.employee;

import com.JoaoGabriel.vacation_scheduler.vacation.Vacation;
import com.JoaoGabriel.vacation_scheduler.vacation.VacationApprovalStatus;
import com.JoaoGabriel.vacation_scheduler.vacation.VacationRepository;
import com.JoaoGabriel.vacation_scheduler.vacation.dto.VacationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ManagerService {

    private final VacationRepository vacationRepository;

    @Transactional(readOnly = true)
    public List<VacationResponse> listEmployeeVacations(
            Employee authenticatedEmployee
    ) {
        validateManager(authenticatedEmployee);

        return vacationRepository
                .findByEmployeeManagerIdAndArchivedFalseOrderByStartDateAsc(
                        authenticatedEmployee.getId()
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public VacationResponse approveVacation(
            Long vacationId,
            Employee authenticatedEmployee
    ) {
        validateManager(authenticatedEmployee);

        Vacation vacation = findTeamVacation(
                vacationId,
                authenticatedEmployee.getId()
        );

        validateNotArchived(vacation);
        validatePendingStatus(vacation);

        vacation.setApprovalStatus(
                VacationApprovalStatus.APPROVED
        );

        Vacation savedVacation =
                vacationRepository.save(vacation);

        return toResponse(savedVacation);
    }

    @Transactional
    public VacationResponse rejectVacation(
            Long vacationId,
            Employee authenticatedEmployee
    ) {
        validateManager(authenticatedEmployee);

        Vacation vacation = findTeamVacation(
                vacationId,
                authenticatedEmployee.getId()
        );

        validateNotArchived(vacation);
        validatePendingStatus(vacation);

        vacation.setApprovalStatus(
                VacationApprovalStatus.REJECTED
        );

        Vacation savedVacation =
                vacationRepository.save(vacation);

        return toResponse(savedVacation);
    }

    private Vacation findTeamVacation(
            Long vacationId,
            Long managerId
    ) {
        return vacationRepository
                .findByIdAndEmployeeManagerId(
                        vacationId,
                        managerId
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Solicitação de férias não encontrada para esta equipe"
                        )
                );
    }

    private void validateManager(
            Employee authenticatedEmployee
    ) {
        if (authenticatedEmployee == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Autenticação necessária"
            );
        }

        if (authenticatedEmployee.getRole()
                != EmployeeRole.MANAGER) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Apenas gestores podem realizar esta operação"
            );
        }
    }

    private void validatePendingStatus(
            Vacation vacation
    ) {
        if (vacation.getApprovalStatus()
                != VacationApprovalStatus.PENDING) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Esta solicitação já foi analisada"
            );
        }
    }

    private void validateNotArchived(
            Vacation vacation
    ) {
        if (vacation.isArchived()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Esta solicitação está arquivada"
            );
        }
    }

    private VacationResponse toResponse(
            Vacation vacation
    ) {
        return new VacationResponse(
                vacation.getId(),
                vacation.getStartDate(),
                vacation.getEndDate(),
                vacation.getTotalDays(),
                vacation.getEmployee().getId(),
                vacation.getEmployee().getNome(),
                vacation.getApprovalStatus()
        );
    }
}