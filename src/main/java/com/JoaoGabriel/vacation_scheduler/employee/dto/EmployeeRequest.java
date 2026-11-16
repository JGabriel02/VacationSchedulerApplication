package com.JoaoGabriel.vacation_scheduler.employee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import com.JoaoGabriel.vacation_scheduler.employee.validation.StrongPassword;

import java.time.LocalDate;

public record EmployeeRequest(

        @NotBlank(message = "O nome é obrigatório")
        String nome,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Informe um e-mail válido")
        String email,

        @StrongPassword
        String password,

        @NotNull(message = "A data de admissão é obrigatória")
        @PastOrPresent(message = "A data de admissão não pode estar no futuro")
        LocalDate admissionDate,

        @NotBlank(message = "O código do gestor é obrigatório")
        String managerCode

) {
}