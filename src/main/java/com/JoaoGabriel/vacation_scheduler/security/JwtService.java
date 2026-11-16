package com.JoaoGabriel.vacation_scheduler.security;

import com.JoaoGabriel.vacation_scheduler.employee.EmployeeRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    static final String ROLE_CLAIM = "role";

    private final SecretKey signingKey;
    private final long expiration;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration
    ) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expiration = expiration;
    }

    public String generateToken(String email, EmployeeRole role) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(email)
                .claim(ROLE_CLAIM, role.name())
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(signingKey)
                .compact();
    }

    /*
     * Valida assinatura e expiração em um único parse. O jjwt já lança
     * ExpiredJwtException (subclasse de JwtException) para tokens vencidos,
     * então não é preciso checar a data manualmente.
     */
    public String extractEmail(String token) {
        String email = parse(token).getSubject();

        if (email == null || email.isBlank()) {
            throw new JwtException("Token sem subject");
        }

        return email;
    }

    Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
