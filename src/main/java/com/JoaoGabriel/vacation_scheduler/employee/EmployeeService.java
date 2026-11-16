package com.JoaoGabriel.vacation_scheduler.employee;

import com.JoaoGabriel.vacation_scheduler.employee.dto.EmployeeRequest;
import com.JoaoGabriel.vacation_scheduler.employee.dto.EmployeeResponse;
import com.JoaoGabriel.vacation_scheduler.employee.dto.ManagerRequest;
import com.JoaoGabriel.vacation_scheduler.employee.dto.ManagerResponse;
import com.JoaoGabriel.vacation_scheduler.employee.exception.EmailAlreadyExistsException;
import com.JoaoGabriel.vacation_scheduler.employee.exception.ManagerCodeNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private static final String CODE_CHARACTERS =
            "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private static final int MANAGER_CODE_LENGTH = 8;

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    private final SecureRandom secureRandom = new SecureRandom();

    public EmployeeResponse create(EmployeeRequest request) {

        validateEmail(request.email());

        Employee manager = employeeRepository
                .findByManagerCode(request.managerCode())
                .orElseThrow(() -> new ManagerCodeNotFoundException(
                        "Código do gestor inválido"
                ));

        if (manager.getRole() != EmployeeRole.MANAGER) {
            throw new ManagerCodeNotFoundException(
                    "O código informado não pertence a um gestor"
            );
        }

        Employee employee = new Employee();

        employee.setNome(request.nome());
        employee.setEmail(request.email());
        employee.setPassword(
                passwordEncoder.encode(request.password())
        );
        employee.setAdmissionDate(request.admissionDate());

        employee.setRole(EmployeeRole.EMPLOYEE);
        employee.setManagerCode(null);
        employee.setManager(manager);

        Employee savedEmployee = saveNewAccount(employee);

        return new EmployeeResponse(
                savedEmployee.getId(),
                savedEmployee.getNome(),
                savedEmployee.getEmail()
        );
    }

    public ManagerResponse createManager(ManagerRequest request) {

        validateEmail(request.email());

        Employee manager = new Employee();

        manager.setNome(request.nome());
        manager.setEmail(request.email());
        manager.setPassword(
                passwordEncoder.encode(request.password())
        );
        manager.setAdmissionDate(request.admissionDate());


        manager.setRole(EmployeeRole.MANAGER);

        manager.setManager(null);


        manager.setManagerCode(
                generateUniqueManagerCode()
        );

        Employee savedManager = saveNewAccount(manager);

        return new ManagerResponse(
                savedManager.getId(),
                savedManager.getNome(),
                savedManager.getEmail(),
                savedManager.getRole(),
                savedManager.getManagerCode()
        );
    }

    /*
     * O validateEmail cobre o caso comum, mas duas requisições simultâneas
     * com o mesmo e-mail podem passar pela verificação ao mesmo tempo.
     * Nesse caso a constraint unique do banco barra a segunda inserção,
     * e convertemos o erro para a mesma resposta 409 de e-mail duplicado.
     */
    private Employee saveNewAccount(Employee employee) {
        try {
            return employeeRepository.saveAndFlush(employee);
        } catch (DataIntegrityViolationException exception) {
            if (employeeRepository.existsByEmail(employee.getEmail())) {
                throw new EmailAlreadyExistsException(
                        "E-mail já cadastrado"
                );
            }

            throw exception;
        }
    }

    private void validateEmail(String email) {

        if (employeeRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(
                    "E-mail já cadastrado"
            );
        }
    }

    private String generateUniqueManagerCode() {

        String code;

        do {
            code = generateManagerCode();
        } while (employeeRepository.existsByManagerCode(code));

        return code;
    }

    private String generateManagerCode() {

        StringBuilder code =
                new StringBuilder(MANAGER_CODE_LENGTH);

        for (int i = 0; i < MANAGER_CODE_LENGTH; i++) {

            int randomIndex = secureRandom.nextInt(
                    CODE_CHARACTERS.length()
            );

            char randomCharacter =
                    CODE_CHARACTERS.charAt(randomIndex);

            code.append(randomCharacter);
        }

        return code.toString();
    }
}

