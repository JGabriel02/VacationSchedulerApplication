package com.JoaoGabriel.vacation_scheduler.vacation;

import com.JoaoGabriel.vacation_scheduler.employee.Employee;
import com.JoaoGabriel.vacation_scheduler.employee.EmployeeRepository;
import com.JoaoGabriel.vacation_scheduler.employee.EmployeeRole;
import com.JoaoGabriel.vacation_scheduler.vacation.dto.VacationRequest;
import com.JoaoGabriel.vacation_scheduler.vacation.exception.InvalidVacationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class VacationConcurrencyIntegrationTest {

    @Autowired
    private VacationService vacationService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private VacationRepository vacationRepository;

    @Test
    void concurrentRequestsCannotExceedThirtyDaysInTheSameCycle() throws Exception {
        Employee manager = new Employee();
        manager.setNome("Gestor Concorrência");
        manager.setEmail("gestor.concorrencia@empresa.com");
        manager.setPassword("x");
        manager.setAdmissionDate(LocalDate.now().minusYears(5));
        manager.setRole(EmployeeRole.MANAGER);
        manager.setManagerCode("CONC2345");
        manager = employeeRepository.save(manager);

        Employee employee = new Employee();
        employee.setNome("Funcionário Concorrência");
        employee.setEmail("func.concorrencia@empresa.com");
        employee.setPassword("x");
        employee.setAdmissionDate(LocalDate.now().minusYears(2));
        employee.setRole(EmployeeRole.EMPLOYEE);
        employee.setManager(manager);
        Employee savedEmployee = employeeRepository.save(employee);

        LocalDate first = LocalDate.now().plusMonths(1);
        LocalDate second = LocalDate.now().plusMonths(4);

        // Dois pedidos de 20 dias sem sobreposição: juntos passariam de 30.
        List<VacationRequest> requests = List.of(
                new VacationRequest(first, first.plusDays(19)),
                new VacationRequest(second, second.plusDays(19))
        );

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Future<Boolean>> results = new ArrayList<>();

        try {
            for (VacationRequest request : requests) {
                Callable<Boolean> task = () -> {
                    start.await();
                    try {
                        vacationService.create(request, savedEmployee.getId());
                        return true;
                    } catch (InvalidVacationException exception) {
                        return false;
                    }
                };
                results.add(executor.submit(task));
            }

            start.countDown();

            int successes = 0;
            for (Future<Boolean> result : results) {
                if (result.get()) {
                    successes++;
                }
            }

            assertEquals(1, successes);
            assertEquals(
                    1,
                    vacationRepository
                            .findByEmployeeIdOrderByStartDateAsc(savedEmployee.getId())
                            .size()
            );
        } finally {
            executor.shutdownNow();
        }
    }
}
