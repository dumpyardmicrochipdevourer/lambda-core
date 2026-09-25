package com.microchip.lambda_core.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.microchip.lambda_core.IntegrationTest;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class UserFileControllerTest extends IntegrationTest {

    @Autowired MockMvc mvc;

    @Test
    void newUserGetsEmptyStorageWithDefaultQuota() throws Exception {
        mvc.perform(get("/api/files").header(HttpHeaders.AUTHORIZATION, user()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quota").value(1000))
                .andExpect(jsonPath("$.used").value(0))
                .andExpect(jsonPath("$.files.length()").value(0));
    }

    @Test
    void uploadDownloadAndDelete() throws Exception {
        UUID id = UUID.randomUUID();
        String auth = bearer(id, "anna", "USER");
        String fileId = start(auth, "hello.txt", 5);

        mvc.perform(put("/api/files/" + fileId + "/content").header(HttpHeaders.AUTHORIZATION, auth)
                        .content("hello".getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETE"));

        mvc.perform(get("/api/files").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(jsonPath("$.used").value(5))
                .andExpect(jsonPath("$.reserved").value(0))
                .andExpect(jsonPath("$.files[0].name").value("hello.txt"));

        mvc.perform(get("/api/files/" + fileId + "/content").param("access_token", token(id, "anna", "USER")))
                .andExpect(status().isOk())
                .andExpect(content().string("hello"));

        mvc.perform(delete("/api/files/" + fileId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/files").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(jsonPath("$.used").value(0))
                .andExpect(jsonPath("$.files.length()").value(0));
    }

    @Test
    void interruptedUploadResumesFromWhatReachedDisk() throws Exception {
        String auth = user();
        String fileId = start(auth, "big.bin", 10);

        mvc.perform(put("/api/files/" + fileId + "/content").header(HttpHeaders.AUTHORIZATION, auth)
                        .content("01234".getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UPLOADING"))
                .andExpect(jsonPath("$.received").value(5));

        mvc.perform(put("/api/files/" + fileId + "/content").header(HttpHeaders.AUTHORIZATION, auth)
                        .header("Upload-Offset", "0").content("0123456789".getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isConflict())
                .andExpect(header().string("Upload-Offset", "5"));

        mvc.perform(put("/api/files/" + fileId + "/content").header(HttpHeaders.AUTHORIZATION, auth)
                        .header("Upload-Offset", "5").content("56789".getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETE"));

        mvc.perform(get("/api/files/" + fileId + "/content").header(HttpHeaders.AUTHORIZATION, auth)
                        .header(HttpHeaders.RANGE, "bytes=3-6"))
                .andExpect(status().isPartialContent())
                .andExpect(content().string("3456"));
    }

    @Test
    void reservationCountsAgainstQuota() throws Exception {
        String auth = user();
        start(auth, "a.bin", 600);

        mvc.perform(startRequest(auth, "b.bin", 500)).andExpect(status().isInsufficientStorage());
        mvc.perform(startRequest(auth, "b.bin", 400)).andExpect(status().isCreated());
    }

    @Test
    void takenNameGetsSuffix() throws Exception {
        String auth = user();
        start(auth, "report.pdf", 1);

        mvc.perform(startRequest(auth, "REPORT.pdf", 1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("REPORT (1).pdf"));
    }

    @Test
    void badNamesAreRejected() throws Exception {
        String auth = user();
        for (String name : new String[] {"", "..", "a/b", "a\\\\b"}) {
            mvc.perform(startRequest(auth, name, 1)).andExpect(status().isBadRequest());
        }
    }

    @Test
    void foreignFilesAreInvisible() throws Exception {
        String fileId = start(user(), "secret.txt", 3);

        mvc.perform(delete("/api/files/" + fileId).header(HttpHeaders.AUTHORIZATION, user()))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/files/" + fileId + "/content").header(HttpHeaders.AUTHORIZATION, user())
                        .content(new byte[] {1, 2, 3}))
                .andExpect(status().isNotFound());
    }

    @Test
    void withoutTokenIs401() throws Exception {
        mvc.perform(get("/api/files")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/files").header(HttpHeaders.AUTHORIZATION, "Bearer garbage"))
                .andExpect(status().isUnauthorized());
    }

    private String start(String auth, String name, long size) throws Exception {
        return JsonPath.read(mvc.perform(startRequest(auth, name, size))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder startRequest(
            String auth, String name, long size) {
        return post("/api/files").header(HttpHeaders.AUTHORIZATION, auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"%s\",\"size\":%d}".formatted(name, size));
    }

    private static String user() {
        UUID id = UUID.randomUUID();
        return bearer(id, "u" + id.toString().substring(0, 8), "USER");
    }
}
