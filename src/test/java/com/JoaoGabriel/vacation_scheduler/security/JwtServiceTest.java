package com.JoaoGabriel.vacation_scheduler.security;

import com.JoaoGabriel.vacation_scheduler.employee.EmployeeRole;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private static final String SECRET =
            "dGVzdF9qd3Rfc2VjcmV0X2tleV9mb3JfdmFjYXRpb25fc2NoZWR1bGVyXzIwMjY=";

    @Test
    void tokenCarriesEmailAndRole() {
        JwtService jwtService = new JwtService(SECRET, 60_000);

        String token = jwtService.generateToken("ana@empresa.com", EmployeeRole.MANAGER);

        assertEquals("ana@empresa.com", jwtService.extractEmail(token));
        assertEquals("MANAGER", jwtService.parse(token).get(JwtService.ROLE_CLAIM, String.class));
    }

    @Test
    void expiredTokenIsRejectedByTheParser() {
        JwtService jwtService = new JwtService(SECRET, -1_000);

        String token = jwtService.generateToken("ana@empresa.com", EmployeeRole.EMPLOYEE);

        assertThrows(ExpiredJwtException.class, () -> jwtService.extractEmail(token));
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService jwtService = new JwtService(SECRET, 60_000);

        String token = jwtService.generateToken("ana@empresa.com", EmployeeRole.EMPLOYEE);
        String tampered = token.substring(0, token.length() - 2)
                + (token.endsWith("AA") ? "BB" : "AA");

        assertThrows(JwtException.class, () -> jwtService.extractEmail(tampered));
    }
}
