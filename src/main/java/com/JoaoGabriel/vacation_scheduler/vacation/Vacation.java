package com.JoaoGabriel.vacation_scheduler.vacation;

import com.JoaoGabriel.vacation_scheduler.employee.Employee;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "vacations")
@Getter
@Setter
public class Vacation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "total_days", nullable = false)
    private Integer totalDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false)
    private VacationApprovalStatus approvalStatus;

    @ManyToOne(optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    public Vacation() {
    }

    @PrePersist
    public void prePersist() {
        if (approvalStatus == null) {
            approvalStatus =
                    VacationApprovalStatus.PENDING;
        }
    }
}