package com.JoaoGabriel.vacation_scheduler.security;

import com.JoaoGabriel.vacation_scheduler.employee.Employee;
import com.JoaoGabriel.vacation_scheduler.employee.EmployeeRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

/*
 * Principal guardado no SecurityContext. Contém só o necessário para
 * identificar o usuário e autorizar a requisição (sem hash de senha nem
 * relacionamentos JPA).
 */
public record AuthenticatedUser(
        Long id,
        String nome,
        String email,
        EmployeeRole role
) {

    public static AuthenticatedUser from(Employee employee) {
        return new AuthenticatedUser(
                employee.getId(),
                employee.getNome(),
                employee.getEmail(),
                employee.getRole()
        );
    }

    public List<GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}
