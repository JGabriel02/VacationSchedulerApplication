package com.JoaoGabriel.vacation_scheduler;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.cors.allowed-origins=http://localhost:5173, https://front.example.com")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiErrorsAndCorsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void invalidManagerCodeReturnsBadRequestInsteadOfServerError() throws Exception {
        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Ana",
                                  "email": "ana@empresa.com",
                                  "password": "senha123",
                                  "admissionDate": "2024-01-10",
                                  "managerCode": "NAOEXISTE"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Código do gestor inválido"));
    }

    @Test
    void duplicatedEmailReturnsConflict() throws Exception {
        String body = """
                {
                  "nome": "Gestor",
                  "email": "gestor@empresa.com",
                  "password": "senha123",
                  "admissionDate": "2020-01-10"
                }
                """;

        mockMvc.perform(post("/managers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/managers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("E-mail já cadastrado"));
    }

    @Test
    void preflightFromConfiguredProductionOriginIsAllowed() throws Exception {
        mockMvc.perform(options("/auth/login")
                        .header("Origin", "https://front.example.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Access-Control-Allow-Origin",
                        "https://front.example.com"
                ));
    }

    @Test
    void preflightFromUnknownOriginIsRejected() throws Exception {
        mockMvc.perform(options("/auth/login")
                        .header("Origin", "https://malicioso.example.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}
