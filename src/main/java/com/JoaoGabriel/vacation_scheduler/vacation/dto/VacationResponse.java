package com.JoaoGabriel.vacation_scheduler.vacation.dto;

import com.JoaoGabriel.vacation_scheduler.vacation.VacationApprovalStatus;

import java.time.LocalDate;

public record VacationResponse(
        Long id,
        LocalDate startDate,
        LocalDate endDate,
        Integer totalDays,
        Long employeeId,
        String employeeName,
        VacationApprovalStatus approvalStatus

) {
}