package com.JoaoGabriel.vacation_scheduler.vacation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class VacationArchiveJob {

    private final VacationRepository vacationRepository;

    @Value(
            "${app.vacation-archive.rejected-after-days:90}"
    )
    private long rejectedAfterDays;

    @Value(
            "${app.vacation-archive.completed-after-days:365}"
    )
    private long completedAfterDays;

    @Scheduled(
            cron = "${app.vacation-archive.cron:0 0 2 * * *}",
            zone = "${app.vacation-archive.zone:America/Sao_Paulo}"
    )
    @Transactional
    public void archiveOldVacations() {
        LocalDateTime rejectedLimit =
                LocalDateTime.now()
                        .minusDays(rejectedAfterDays);

        LocalDate completedLimit =
                LocalDate.now()
                        .minusDays(completedAfterDays);

        List<Vacation> rejectedVacations =
                vacationRepository
                        .findByArchivedFalseAndApprovalStatusAndUpdatedAtBefore(
                                VacationApprovalStatus.REJECTED,
                                rejectedLimit
                        );

        List<Vacation> completedVacations =
                vacationRepository
                        .findByArchivedFalseAndApprovalStatusAndEndDateBefore(
                                VacationApprovalStatus.APPROVED,
                                completedLimit
                        );

        List<Vacation> vacationsToArchive =
                new ArrayList<>();

        vacationsToArchive.addAll(
                rejectedVacations
        );

        vacationsToArchive.addAll(
                completedVacations
        );

        if (vacationsToArchive.isEmpty()) {
            log.info(
                    "Job de arquivamento finalizado: nenhuma férias para arquivar"
            );

            return;
        }

        vacationsToArchive.forEach(
                vacation ->
                        vacation.setArchived(true)
        );

        vacationRepository.saveAll(
                vacationsToArchive
        );

        log.info(
                "Job de arquivamento finalizado: {} férias arquivadas",
                vacationsToArchive.size()
        );
    }
}