package com.JoaoGabriel.vacation_scheduler.vacation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VacationArchiveJobTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    @Mock
    private VacationRepository vacationRepository;

    @Test
    void shouldArchiveRejectedAndCancelledUsingTheConfiguredClock() {
        Instant now = LocalDate.of(2026, 6, 1).atStartOfDay(ZONE).toInstant();
        Clock clock = Clock.fixed(now, ZONE);

        VacationArchiveJob job = new VacationArchiveJob(vacationRepository, clock);
        ReflectionTestUtils.setField(job, "rejectedAfterDays", 90L);
        ReflectionTestUtils.setField(job, "completedAfterDays", 365L);

        Vacation cancelled = new Vacation();
        cancelled.setApprovalStatus(VacationApprovalStatus.CANCELLED);

        List<VacationApprovalStatus> historyStatuses = List.of(
                VacationApprovalStatus.REJECTED,
                VacationApprovalStatus.CANCELLED
        );
        LocalDateTime rejectedLimit = LocalDateTime.of(2026, 3, 3, 0, 0);

        when(vacationRepository
                .findByArchivedFalseAndApprovalStatusInAndUpdatedAtBefore(
                        historyStatuses,
                        rejectedLimit
                ))
                .thenReturn(List.of(cancelled));

        when(vacationRepository
                .findByArchivedFalseAndApprovalStatusAndEndDateBefore(
                        VacationApprovalStatus.APPROVED,
                        LocalDate.of(2025, 6, 1)
                ))
                .thenReturn(List.of());

        job.archiveOldVacations();

        assertTrue(cancelled.isArchived());
        verify(vacationRepository).saveAll(anyList());
    }
}
