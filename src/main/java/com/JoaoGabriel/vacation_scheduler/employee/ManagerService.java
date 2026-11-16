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

/*
 * O papel MANAGER é exigido pelo SecurityConfig (/managers/** -> ROLE_MANAGER).
 * Aqui só garantimos que a solicitação pertence à equipe do gestor.
 */
@Service
@RequiredArgsConstructor
public class ManagerService {

    private final VacationRepository vacationRepository;

    @Transactional(readOnly = true)
    public List<VacationResponse> listEmployeeVacations(
            Long managerId
    ) {
        return vacationRepository
                .findByEmployeeManagerIdAndArchivedFalseOrderByStartDateAsc(
                        managerId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public VacationResponse approveVacation(
            Long vacationId,
            Long managerId
    ) {
        Vacation vacation = findTeamVacation(
                vacationId,
                managerId
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
            Long managerId
    ) {
        Vacation vacation = findTeamVacation(
                vacationId,
                managerId
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