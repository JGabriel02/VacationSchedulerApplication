package com.JoaoGabriel.vacation_scheduler.employee.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/*
 * Política de senha dos cadastros: 8 a 72 caracteres (72 é o limite do
 * BCrypt), com pelo menos uma letra e um número.
 */
@Documented
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@ReportAsSingleViolation
@NotBlank
@Size(min = 8, max = 72)
@Pattern(regexp = "^(?=.*\\p{L})(?=.*\\d).*$")
public @interface StrongPassword {

    String message() default
            "A senha deve ter de 8 a 72 caracteres, com pelo menos uma letra e um número";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
