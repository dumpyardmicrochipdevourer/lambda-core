package com.microchip.lambda_core.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.microchip.lambda_core.IntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@AutoConfigureMockMvc
class FeedbackControllerTest extends IntegrationTest {

    @Autowired MockMvc mvc;

    @Test
    void anonymousWritesAdminReads() throws Exception {
        String marker = UUID.randomUUID().toString();
        mvc.perform(send(ip(), marker, "@anna")).andExpect(status().isNoContent());

        mvc.perform(get("/api/feedback").header(HttpHeaders.AUTHORIZATION, bearer(UUID.randomUUID(), "admin", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].body").value(marker + "\n\n-- \n@anna"))
                .andExpect(jsonPath("$[0].readAt").doesNotExist());
    }

    @Test
    void readingIsForAdminsOnly() throws Exception {
        mvc.perform(get("/api/feedback")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/feedback").header(HttpHeaders.AUTHORIZATION, bearer(UUID.randomUUID(), "u", "USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void sixthMessageWithinAnHourIs429() throws Exception {
        String ip = ip();
        for (int i = 0; i < 5; i++) {
            mvc.perform(send(ip, "hi " + i, null)).andExpect(status().isNoContent());
        }
        mvc.perform(send(ip, "hi again", null)).andExpect(status().isTooManyRequests());
        mvc.perform(send(ip(), "someone else", null)).andExpect(status().isNoContent());
    }

    @Test
    void emptyAndHugeMessagesAre400() throws Exception {
        mvc.perform(send(ip(), " ", null)).andExpect(status().isBadRequest());
        mvc.perform(send(ip(), "x".repeat(9000), null)).andExpect(status().isBadRequest());
    }

    private static MockHttpServletRequestBuilder send(String ip, String message, String contact) {
        String body = contact == null
                ? "{\"message\":\"%s\"}".formatted(message)
                : "{\"message\":\"%s\",\"contact\":\"%s\"}".formatted(message, contact);
        return post("/api/feedback").header("X-Real-IP", ip).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static String ip() {
        return "10.0.0." + UUID.randomUUID().toString().substring(0, 6);
    }
}
