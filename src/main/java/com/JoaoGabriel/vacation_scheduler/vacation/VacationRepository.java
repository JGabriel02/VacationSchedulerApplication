package com.JoaoGabriel.vacation_scheduler.vacation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VacationRepository
        extends JpaRepository<Vacation, Long> {

    List<Vacation> findByEmployeeIdOrderByStartDateAsc(
            Long employeeId
    );

    List<Vacation> findByEmployeeIdAndStartDateBetween(
            Long employeeId,
            LocalDate cycleStart,
            LocalDate cycleEnd
    );

    Optional<Vacation> findByIdAndEmployeeId(
            Long id,
            Long employeeId
    );

    List<Vacation>
    findByEmployeeManagerIdAndArchivedFalseOrderByStartDateAsc(
            Long managerId
    );

    Optional<Vacation> findByIdAndEmployeeManagerId(
            Long vacationId,
            Long managerId
    );

    List<Vacation>
    findByArchivedFalseAndApprovalStatusAndUpdatedAtBefore(
            VacationApprovalStatus approvalStatus,
            LocalDateTime updatedBefore
    );

    List<Vacation>
    findByArchivedFalseAndApprovalStatusAndEndDateBefore(
            VacationApprovalStatus approvalStatus,
            LocalDate endDate
    );
}