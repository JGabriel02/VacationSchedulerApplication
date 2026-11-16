package com.JoaoGabriel.vacation_scheduler.vacation;

import com.JoaoGabriel.vacation_scheduler.employee.Employee;
import com.JoaoGabriel.vacation_scheduler.employee.EmployeeRepository;
import com.JoaoGabriel.vacation_scheduler.vacation.dto.VacationRequest;
import com.JoaoGabriel.vacation_scheduler.vacation.dto.VacationResponse;
import com.JoaoGabriel.vacation_scheduler.vacation.exception.InvalidVacationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VacationService {

    private final VacationRepository vacationRepository;
    private final EmployeeRepository employeeRepository;
    private final Clock clock;

    @Transactional
    public VacationResponse create(
            VacationRequest request,
            Long employeeId
    ) {
        /*
         * Trava a linha do funcionário até o fim da transação. Sem isso, duas
         * solicitações simultâneas leem o mesmo saldo e ambas passam no limite
         * de 30 dias.
         */
        Employee employee = employeeRepository
                .findByIdForUpdate(employeeId)
                .orElseThrow(() -> new InvalidVacationException(
                        "Funcionário autenticado não encontrado"
                ));

        validateEmployee(employee);
        validateHasApprover(employee);
        validateDates(request);

        LocalDate eligibilityDate =
                employee.getAdmissionDate().plusYears(1);

        if (request.startDate().isBefore(eligibilityDate)) {
            throw new InvalidVacationException(
                    "As férias só podem começar a partir de "
                            + eligibilityDate
            );
        }

        LocalDate cycleStart = calculateCycleStart(
                employee.getAdmissionDate(),
                request.startDate()
        );

        LocalDate cycleEnd =
                calculateCycleEnd(cycleStart);

        if (today().isBefore(cycleStart)) {
            throw new InvalidVacationException(
                    "Este ciclo de férias só estará disponível a partir de "
                            + cycleStart
            );
        }

        if (request.endDate().isAfter(cycleEnd)) {
            throw new InvalidVacationException(
                    "O período de férias deve terminar até "
                            + cycleEnd
            );
        }

        int totalDays = calculateTotalDays(
                request.startDate(),
                request.endDate()
        );

        validateTotalDays(totalDays);

        List<Vacation> allEmployeeVacations =
                vacationRepository
                        .findByEmployeeIdOrderByStartDateAsc(
                                employee.getId()
                        );

        validateOverlappingVacation(
                allEmployeeVacations,
                request.startDate(),
                request.endDate()
        );

        List<Vacation> vacationsInCycle =
                allEmployeeVacations.stream()
                        .filter(vacation ->
                                vacation.getApprovalStatus().isActive()
                        )
                        .filter(vacation ->
                                !vacation.getStartDate()
                                        .isBefore(cycleStart)
                        )
                        .filter(vacation ->
                                !vacation.getStartDate()
                                        .isAfter(cycleEnd)
                        )
                        .toList();

        int usedDays = vacationsInCycle.stream()
                .mapToInt(Vacation::getTotalDays)
                .sum();

        if (usedDays + totalDays > 30) {
            throw new InvalidVacationException(
                    "O funcionário não pode ultrapassar 30 dias de férias no mesmo ciclo"
            );
        }

        if (!isValidDivision(usedDays, totalDays)) {
            throw new InvalidVacationException(
                    "As férias devem ser tiradas em 30 dias ou divididas em 20 e 10 dias"
            );
        }

        Vacation vacation = new Vacation();
        vacation.setStartDate(request.startDate());
        vacation.setEndDate(request.endDate());
        vacation.setTotalDays(totalDays);
        vacation.setEmployee(employee);
        vacation.setApprovalStatus(
                VacationApprovalStatus.PENDING
        );

        Vacation savedVacation =
                vacationRepository.save(vacation);

        return toResponse(savedVacation);
    }

    @Transactional(readOnly = true)
    public List<VacationResponse> findByEmployee(
            Long employeeId
    ) {
        return vacationRepository
                .findByEmployeeIdAndArchivedFalseOrderByStartDateAsc(
                        employeeId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /*
     * Cancelamento lógico: o registro continua no banco com status CANCELLED,
     * para o gestor e o histórico enxergarem o que aconteceu.
     */
    @Transactional
    public void cancel(
            Long vacationId,
            Long employeeId
    ) {
        Vacation vacation = vacationRepository
                .findByIdAndEmployeeId(
                        vacationId,
                        employeeId
                )
                .orElseThrow(() ->
                        new InvalidVacationException(
                                "Férias não encontradas para este funcionário"
                        )
                );

        if (vacation.isArchived()
                || !vacation.getApprovalStatus().isActive()) {

            throw new InvalidVacationException(
                    "Apenas solicitações pendentes ou aprovadas podem ser canceladas"
            );
        }

        if (!vacation.getStartDate()
                .isAfter(today())) {

            throw new InvalidVacationException(
                    "Não é possível cancelar férias que já começaram ou terminaram"
            );
        }

        vacation.setApprovalStatus(
                VacationApprovalStatus.CANCELLED
        );

        vacationRepository.save(vacation);
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private void validateEmployee(
            Employee employee
    ) {
        if (employee.getAdmissionDate() == null) {
            throw new InvalidVacationException(
                    "A data de admissão do funcionário não foi informada"
            );
        }
    }

    /*
     * Toda solicitação nasce PENDING e só o gestor vinculado pode aprová-la.
     * Sem gestor (caso de quem tem perfil MANAGER), o pedido ficaria pendente para sempre.
     */
    private void validateHasApprover(
            Employee employee
    ) {
        if (employee.getManager() == null) {
            throw new InvalidVacationException(
                    "Não há um gestor vinculado para aprovar esta solicitação"
            );
        }
    }

    private void validateDates(
            VacationRequest request
    ) {
        if (!request.startDate()
                .isAfter(today())) {

            throw new InvalidVacationException(
                    "A data de início das férias deve ser futura"
            );
        }

        if (request.endDate()
                .isBefore(request.startDate())) {

            throw new InvalidVacationException(
                    "A data final não pode ser anterior à data inicial"
            );
        }
    }

    private int calculateTotalDays(
            LocalDate startDate,
            LocalDate endDate
    ) {
        return (int) ChronoUnit.DAYS.between(
                startDate,
                endDate
        ) + 1;
    }

    private void validateTotalDays(
            int totalDays
    ) {
        if (totalDays != 10
                && totalDays != 20
                && totalDays != 30) {

            throw new InvalidVacationException(
                    "O período deve ter 10, 20 ou 30 dias"
            );
        }
    }

    private void validateOverlappingVacation(
            List<Vacation> employeeVacations,
            LocalDate requestedStartDate,
            LocalDate requestedEndDate
    ) {
        boolean overlapping =
                employeeVacations.stream()
                        .filter(vacation ->
                                vacation.getApprovalStatus().isActive()
                        )
                        .anyMatch(vacation ->
                                !vacation.getStartDate()
                                        .isAfter(requestedEndDate)
                                        &&
                                        !vacation.getEndDate()
                                                .isBefore(requestedStartDate)
                        );

        if (overlapping) {
            throw new InvalidVacationException(
                    "Você já possui uma solicitação de férias neste período"
            );
        }
    }

    private boolean isValidDivision(
            int usedDays,
            int requestedDays
    ) {
        if (usedDays == 0) {
            return requestedDays == 10
                    || requestedDays == 20
                    || requestedDays == 30;
        }

        if (usedDays == 10) {
            return requestedDays == 20;
        }

        if (usedDays == 20) {
            return requestedDays == 10;
        }

        return false;
    }

    private LocalDate calculateCycleStart(
            LocalDate admissionDate,
            LocalDate vacationStartDate
    ) {
        LocalDate cycleStart =
                admissionDate.plusYears(1);

        while (!vacationStartDate.isBefore(
                cycleStart.plusYears(1)
        )) {
            cycleStart =
                    cycleStart.plusYears(1);
        }

        return cycleStart;
    }

    private LocalDate calculateCycleEnd(
            LocalDate cycleStart
    ) {
        return cycleStart
                .plusYears(1)
                .minusDays(1);
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