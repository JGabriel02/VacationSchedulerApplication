package com.JoaoGabriel.vacation_scheduler.vacation;

public enum VacationApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED;

    /*
     * Solicitações ativas ocupam o período e contam no saldo do ciclo.
     * Rejeitadas e canceladas são apenas histórico.
     */
    public boolean isActive() {
        return this == PENDING || this == APPROVED;
    }
}
