package com.JoaoGabriel.vacation_scheduler.vacation;

import com.JoaoGabriel.vacation_scheduler.employee.Employee;
import com.JoaoGabriel.vacation_scheduler.vacation.dto.VacationRequest;
import com.JoaoGabriel.vacation_scheduler.vacation.dto.VacationResponse;
import com.JoaoGabriel.vacation_scheduler.vacation.exception.InvalidVacationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VacationServiceTest {

    @Mock
    private VacationRepository vacationRepository;

    private VacationService vacationService;

    private Employee employee;

    private LocalDate today;
    private LocalDate firstPeriodStart;
    private LocalDate secondPeriodStart;

    @BeforeEach
    void setUp() {
        vacationService =
                new VacationService(vacationRepository);

        today = LocalDate.now();

        /*
         * Como a admissão ocorreu há dois anos,
         * o funcionário já está elegível para férias.
         */
        employee = new Employee();
        employee.setId(1L);
        employee.setNome("João");
        employee.setAdmissionDate(
                today.minusYears(2)
        );

        /*
         * Datas futuras e dentro do ciclo atual.
         */
        firstPeriodStart =
                today.plusMonths(2);

        secondPeriodStart =
                today.plusMonths(5);
    }

    @Test
    void shouldCreateThirtyDayVacationSuccessfully() {
        VacationRequest request =
                requestWithDays(
                        firstPeriodStart,
                        30
                );

        when(
                vacationRepository
                        .findByEmployeeIdOrderByStartDateAsc(
                                employee.getId()
                        )
        ).thenReturn(List.of());

        when(
                vacationRepository.save(
                        any(Vacation.class)
                )
        ).thenAnswer(invocation -> {
            Vacation vacation =
                    invocation.getArgument(0);

            vacation.setId(10L);

            return vacation;
        });

        VacationResponse response =
                vacationService.create(
                        request,
                        employee
                );

        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals(
                request.startDate(),
                response.startDate()
        );
        assertEquals(
                request.endDate(),
                response.endDate()
        );
        assertEquals(30, response.totalDays());
        assertEquals(
                employee.getId(),
                response.employeeId()
        );
        assertEquals(
                employee.getNome(),
                response.employeeName()
        );
        assertEquals(
                VacationApprovalStatus.PENDING,
                response.approvalStatus()
        );

        ArgumentCaptor<Vacation> captor =
                ArgumentCaptor.forClass(
                        Vacation.class
                );

        verify(vacationRepository)
                .save(captor.capture());

        Vacation savedVacation =
                captor.getValue();

        assertEquals(
                VacationApprovalStatus.PENDING,
                savedVacation.getApprovalStatus()
        );
        assertEquals(
                employee,
                savedVacation.getEmployee()
        );
    }

    @Test
    void shouldCreateTenDayVacationSuccessfully() {
        VacationRequest request =
                requestWithDays(
                        firstPeriodStart,
                        10
                );

        when(
                vacationRepository
                        .findByEmployeeIdOrderByStartDateAsc(
                                employee.getId()
                        )
        ).thenReturn(List.of());

        configureSuccessfulSave();

        VacationResponse response =
                vacationService.create(
                        request,
                        employee
                );

        assertEquals(10, response.totalDays());

        assertEquals(
                VacationApprovalStatus.PENDING,
                response.approvalStatus()
        );
    }

    @Test
    void shouldRejectVacationWithInvalidNumberOfDays() {
        VacationRequest request =
                requestWithDays(
                        firstPeriodStart,
                        15
                );

        InvalidVacationException exception =
                assertThrows(
                        InvalidVacationException.class,
                        () -> vacationService.create(
                                request,
                                employee
                        )
                );

        assertEquals(
                "O período deve ter 10, 20 ou 30 dias",
                exception.getMessage()
        );

        verify(
                vacationRepository,
                never()
        ).save(any());
    }

    @Test
    void shouldRejectVacationStartingToday() {
        VacationRequest request =
                requestWithDays(
                        today,
                        10
                );

        InvalidVacationException exception =
                assertThrows(
                        InvalidVacationException.class,
                        () -> vacationService.create(
                                request,
                                employee
                        )
                );

        assertEquals(
                "A data de início das férias deve ser futura",
                exception.getMessage()
        );

        verifyNoInteractions(vacationRepository);
    }

    @Test
    void shouldRejectVacationStartingInThePast() {
        VacationRequest request =
                requestWithDays(
                        today.minusDays(1),
                        10
                );

        InvalidVacationException exception =
                assertThrows(
                        InvalidVacationException.class,
                        () -> vacationService.create(
                                request,
                                employee
                        )
                );

        assertEquals(
                "A data de início das férias deve ser futura",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectEndDateBeforeStartDate() {
        VacationRequest request =
                new VacationRequest(
                        firstPeriodStart,
                        firstPeriodStart.minusDays(1)
                );

        InvalidVacationException exception =
                assertThrows(
                        InvalidVacationException.class,
                        () -> vacationService.create(
                                request,
                                employee
                        )
                );

        assertEquals(
                "A data final não pode ser anterior à data inicial",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectEmployeeWithoutOneYearOfAdmission() {
        employee.setAdmissionDate(
                today.minusMonths(6)
        );

        VacationRequest request =
                requestWithDays(
                        firstPeriodStart,
                        10
                );

        InvalidVacationException exception =
                assertThrows(
                        InvalidVacationException.class,
                        () -> vacationService.create(
                                request,
                                employee
                        )
                );

        assertTrue(
                exception.getMessage().startsWith(
                        "As férias só podem começar a partir de"
                )
        );
    }

    @Test
    void shouldRejectNullEmployee() {
        VacationRequest request =
                requestWithDays(
                        firstPeriodStart,
                        10
                );

        InvalidVacationException exception =
                assertThrows(
                        InvalidVacationException.class,
                        () -> vacationService.create(
                                request,
                                null
                        )
                );

        assertEquals(
                "Funcionário autenticado não encontrado",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectEmployeeWithoutAdmissionDate() {
        employee.setAdmissionDate(null);

        VacationRequest request =
                requestWithDays(
                        firstPeriodStart,
                        10
                );

        InvalidVacationException exception =
                assertThrows(
                        InvalidVacationException.class,
                        () -> vacationService.create(
                                request,
                                employee
                        )
                );

        assertEquals(
                "A data de admissão do funcionário não foi informada",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectOverlappingApprovedVacation() {
        Vacation existingVacation =
                createVacation(
                        firstPeriodStart,
                        10,
                        VacationApprovalStatus.APPROVED
                );

        VacationRequest request =
                requestWithDays(
                        firstPeriodStart.plusDays(5),
                        10
                );

        when(
                vacationRepository
                        .findByEmployeeIdOrderByStartDateAsc(
                                employee.getId()
                        )
        ).thenReturn(
                List.of(existingVacation)
        );

        InvalidVacationException exception =
                assertThrows(
                        InvalidVacationException.class,
                        () -> vacationService.create(
                                request,
                                employee
                        )
                );

        assertEquals(
                "Você já possui uma solicitação de férias neste período",
                exception.getMessage()
        );

        verify(
                vacationRepository,
                never()
        ).save(any());
    }

    @Test
    void shouldRejectOverlappingPendingVacation() {
        Vacation existingVacation =
                createVacation(
                        firstPeriodStart,
                        10,
                        VacationApprovalStatus.PENDING
                );

        VacationRequest request =
                requestWithDays(
                        firstPeriodStart.plusDays(3),
                        10
                );

        when(
                vacationRepository
                        .findByEmployeeIdOrderByStartDateAsc(
                                employee.getId()
                        )
        ).thenReturn(
                List.of(existingVacation)
        );

        assertThrows(
                InvalidVacationException.class,
                () -> vacationService.create(
                        request,
                        employee
                )
        );

        verify(
                vacationRepository,
                never()
        ).save(any());
    }

    @Test
    void shouldIgnoreRejectedVacationWhenCheckingOverlap() {
        Vacation rejectedVacation =
                createVacation(
                        firstPeriodStart,
                        10,
                        VacationApprovalStatus.REJECTED
                );

        VacationRequest request =
                requestWithDays(
                        firstPeriodStart,
                        10
                );

        when(
                vacationRepository
                        .findByEmployeeIdOrderByStartDateAsc(
                                employee.getId()
                        )
        ).thenReturn(
                List.of(rejectedVacation)
        );

        configureSuccessfulSave();

        VacationResponse response =
                vacationService.create(
                        request,
                        employee
                );

        assertNotNull(response);

        assertEquals(
                VacationApprovalStatus.PENDING,
                response.approvalStatus()
        );
    }

    @Test
    void shouldRejectVacationWhenEmployeeExceedsThirtyDays() {
        Vacation existingVacation =
                createVacation(
                        firstPeriodStart,
                        20,
                        VacationApprovalStatus.APPROVED
                );

        VacationRequest request =
                requestWithDays(
                        secondPeriodStart,
                        20
                );

        when(
                vacationRepository
                        .findByEmployeeIdOrderByStartDateAsc(
                                employee.getId()
                        )
        ).thenReturn(
                List.of(existingVacation)
        );

        InvalidVacationException exception =
                assertThrows(
                        InvalidVacationException.class,
                        () -> vacationService.create(
                                request,
                                employee
                        )
                );

        assertEquals(
                "O funcionário não pode ultrapassar 30 dias de férias no mesmo ciclo",
                exception.getMessage()
        );

        verify(
                vacationRepository,
                never()
        ).save(any());
    }

    @Test
    void shouldCountPendingVacationInThirtyDayLimit() {
        Vacation pendingVacation =
                createVacation(
                        firstPeriodStart,
                        20,
                        VacationApprovalStatus.PENDING
                );

        VacationRequest request =
                requestWithDays(
                        secondPeriodStart,
                        20
                );

        when(
                vacationRepository
                        .findByEmployeeIdOrderByStartDateAsc(
                                employee.getId()
                        )
        ).thenReturn(
                List.of(pendingVacation)
        );

        assertThrows(
                InvalidVacationException.class,
                () -> vacationService.create(
                        request,
                        employee
                )
        );
    }

    @Test
    void shouldIgnoreRejectedVacationInThirtyDayLimit() {
        Vacation rejectedVacation =
                createVacation(
                        firstPeriodStart,
                        30,
                        VacationApprovalStatus.REJECTED
                );

        VacationRequest request =
                requestWithDays(
                        secondPeriodStart,
                        30
                );

        when(
                vacationRepository
                        .findByEmployeeIdOrderByStartDateAsc(
                                employee.getId()
                        )
        ).thenReturn(
                List.of(rejectedVacation)
        );

        configureSuccessfulSave();

        VacationResponse response =
                vacationService.create(
                        request,
                        employee
                );

        assertEquals(30, response.totalDays());
    }

    @Test
    void shouldRejectTenPlusTenDivision() {
        Vacation existingVacation =
                createVacation(
                        firstPeriodStart,
                        10,
                        VacationApprovalStatus.APPROVED
                );

        VacationRequest request =
                requestWithDays(
                        secondPeriodStart,
                        10
                );

        when(
                vacationRepository
                        .findByEmployeeIdOrderByStartDateAsc(
                                employee.getId()
                        )
        ).thenReturn(
                List.of(existingVacation)
        );

        InvalidVacationException exception =
                assertThrows(
                        InvalidVacationException.class,
                        () -> vacationService.create(
                                request,
                                employee
                        )
                );

        assertEquals(
                "As férias devem ser tiradas em 30 dias ou divididas em 20 e 10 dias",
                exception.getMessage()
        );

        verify(
                vacationRepository,
                never()
        ).save(any());
    }

    @Test
    void shouldAllowTwentyDaysAfterTenDays() {
        Vacation existingVacation =
                createVacation(
                        firstPeriodStart,
                        10,
                        VacationApprovalStatus.APPROVED
                );

        VacationRequest request =
                requestWithDays(
                        secondPeriodStart,
                        20
                );

        when(
                vacationRepository
                        .findByEmployeeIdOrderByStartDateAsc(
                                employee.getId()
                        )
        ).thenReturn(
                List.of(existingVacation)
        );

        configureSuccessfulSave();

        VacationResponse response =
                vacationService.create(
                        request,
                        employee
                );

        assertEquals(20, response.totalDays());

        assertEquals(
                VacationApprovalStatus.PENDING,
                response.approvalStatus()
        );

        verify(vacationRepository)
                .save(any(Vacation.class));
    }

    @Test
    void shouldAllowTenDaysAfterTwentyDays() {
        Vacation existingVacation =
                createVacation(
                        firstPeriodStart,
                        20,
                        VacationApprovalStatus.APPROVED
                );

        VacationRequest request =
                requestWithDays(
                        secondPeriodStart,
                        10
                );

        when(
                vacationRepository
                        .findByEmployeeIdOrderByStartDateAsc(
                                employee.getId()
                        )
        ).thenReturn(
                List.of(existingVacation)
        );

        configureSuccessfulSave();

        VacationResponse response =
                vacationService.create(
                        request,
                        employee
                );

        assertEquals(10, response.totalDays());

        assertEquals(
                VacationApprovalStatus.PENDING,
                response.approvalStatus()
        );
    }

    @Test
    void shouldListEmployeeVacationsWithApprovalStatus() {
        Vacation pendingVacation =
                createVacation(
                        firstPeriodStart,
                        10,
                        VacationApprovalStatus.PENDING
                );

        pendingVacation.setId(1L);

        Vacation approvedVacation =
                createVacation(
                        secondPeriodStart,
                        20,
                        VacationApprovalStatus.APPROVED
                );

        approvedVacation.setId(2L);

        when(
                vacationRepository
                        .findByEmployeeIdOrderByStartDateAsc(
                                employee.getId()
                        )
        ).thenReturn(
                List.of(
                        pendingVacation,
                        approvedVacation
                )
        );

        List<VacationResponse> responses =
                vacationService.findByEmployee(
                        employee
                );

        assertEquals(2, responses.size());

        assertEquals(
                VacationApprovalStatus.PENDING,
                responses.get(0).approvalStatus()
        );

        assertEquals(
                VacationApprovalStatus.APPROVED,
                responses.get(1).approvalStatus()
        );
    }

    @Test
    void shouldDeleteFutureVacation() {
        Vacation vacation =
                createVacation(
                        firstPeriodStart,
                        10,
                        VacationApprovalStatus.PENDING
                );

        vacation.setId(20L);

        when(
                vacationRepository
                        .findByIdAndEmployeeId(
                                vacation.getId(),
                                employee.getId()
                        )
        ).thenReturn(Optional.of(vacation));

        vacationService.delete(
                vacation.getId(),
                employee
        );

        verify(vacationRepository)
                .delete(vacation);
    }

    @Test
    void shouldRejectDeletingVacationStartingToday() {
        Vacation vacation =
                createVacation(
                        today,
                        10,
                        VacationApprovalStatus.APPROVED
                );

        vacation.setId(20L);

        when(
                vacationRepository
                        .findByIdAndEmployeeId(
                                vacation.getId(),
                                employee.getId()
                        )
        ).thenReturn(Optional.of(vacation));

        InvalidVacationException exception =
                assertThrows(
                        InvalidVacationException.class,
                        () -> vacationService.delete(
                                vacation.getId(),
                                employee
                        )
                );

        assertEquals(
                "Não é possível cancelar férias que já começaram ou terminaram",
                exception.getMessage()
        );

        verify(
                vacationRepository,
                never()
        ).delete(any());
    }

    @Test
    void shouldRejectDeletingPastVacation() {
        Vacation vacation =
                createVacation(
                        today.minusMonths(1),
                        10,
                        VacationApprovalStatus.APPROVED
                );

        vacation.setId(20L);

        when(
                vacationRepository
                        .findByIdAndEmployeeId(
                                vacation.getId(),
                                employee.getId()
                        )
        ).thenReturn(Optional.of(vacation));

        assertThrows(
                InvalidVacationException.class,
                () -> vacationService.delete(
                        vacation.getId(),
                        employee
                )
        );

        verify(
                vacationRepository,
                never()
        ).delete(any());
    }

    @Test
    void shouldRejectDeletingVacationFromAnotherEmployee() {
        when(
                vacationRepository
                        .findByIdAndEmployeeId(
                                99L,
                                employee.getId()
                        )
        ).thenReturn(Optional.empty());

        InvalidVacationException exception =
                assertThrows(
                        InvalidVacationException.class,
                        () -> vacationService.delete(
                                99L,
                                employee
                        )
                );

        assertEquals(
                "Férias não encontradas para este funcionário",
                exception.getMessage()
        );

        verify(
                vacationRepository,
                never()
        ).delete(any());
    }

    private VacationRequest requestWithDays(
            LocalDate startDate,
            int totalDays
    ) {
        return new VacationRequest(
                startDate,
                startDate.plusDays(totalDays - 1L)
        );
    }

    private Vacation createVacation(
            LocalDate startDate,
            int totalDays,
            VacationApprovalStatus status
    ) {
        Vacation vacation = new Vacation();

        vacation.setStartDate(startDate);
        vacation.setEndDate(
                startDate.plusDays(totalDays - 1L)
        );
        vacation.setTotalDays(totalDays);
        vacation.setEmployee(employee);
        vacation.setApprovalStatus(status);

        return vacation;
    }

    private void configureSuccessfulSave() {
        when(
                vacationRepository.save(
                        any(Vacation.class)
                )
        ).thenAnswer(invocation -> {
            Vacation vacation =
                    invocation.getArgument(0);

            vacation.setId(100L);

            return vacation;
        });
    }
}
