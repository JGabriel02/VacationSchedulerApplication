package com.JoaoGabriel.vacation_scheduler;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void routesAreProtectedByRole() throws Exception {
        String managerCode = createManager("gestor.seg@empresa.com");
        createEmployee("func.seg@empresa.com", managerCode);

        String managerToken = login("gestor.seg@empresa.com");
        String employeeToken = login("func.seg@empresa.com");

        mockMvc.perform(get("/managers/vacations"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/managers/vacations")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Você não tem permissão para esta operação"));

        mockMvc.perform(get("/managers/vacations")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/vacations/me")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/vacations/me")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/employees/me")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("func.seg@empresa.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void weakPasswordIsRejectedOnSignUp() throws Exception {
        mockMvc.perform(post("/managers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Gestor",
                                  "email": "fraca@empresa.com",
                                  "password": "123456",
                                  "admissionDate": "2020-01-10"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void repeatedFailedLoginsAreThrottled() throws Exception {
        createManager("alvo@empresa.com");

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(loginRequest("alvo@empresa.com", "errada123"))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(loginRequest("alvo@empresa.com", "senha1234"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    private String createManager(String email) throws Exception {
        String body = mockMvc.perform(post("/managers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Gestor",
                                  "email": "%s",
                                  "password": "senha1234",
                                  "admissionDate": "2020-01-10"
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return field(body, "managerCode");
    }

    private void createEmployee(String email, String managerCode) throws Exception {
        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Funcionário",
                                  "email": "%s",
                                  "password": "senha1234",
                                  "admissionDate": "2023-01-10",
                                  "managerCode": "%s"
                                }
                                """.formatted(email, managerCode)))
                .andExpect(status().isOk());
    }

    private String login(String email) throws Exception {
        String body = mockMvc.perform(loginRequest(email, "senha1234"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return field(body, "token");
    }

    private org.springframework.test.web.servlet.RequestBuilder loginRequest(
            String email,
            String password
    ) {
        return post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "email": "%s", "password": "%s" }
                        """.formatted(email, password));
    }

    private static String field(String json, String name) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\"" + name + "\"\\s*:\\s*\"([^\"]+)\"")
                .matcher(json);

        if (!matcher.find()) {
            throw new IllegalStateException(name + " não encontrado em " + json);
        }

        return matcher.group(1);
    }
}
