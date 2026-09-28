package com.microchip.lambda_core.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.microchip.lambda_core.IntegrationTest;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class ShareControllerTest extends IntegrationTest {

    @Autowired MockMvc mvc;

    @Test
    void textNoteRoundTrip() throws Exception {
        String code = create(900);
        byte[] note = "привет\nстрока два".getBytes(StandardCharsets.UTF_8);

        mvc.perform(put("/api/share/" + code + "/files/message.txt").contentType("text/plain;charset=utf-8").content(note))
                .andExpect(status().isCreated());

        String manifest = mvc.perform(get("/api/share/" + code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.files.length()").value(1))
                .andExpect(jsonPath("$.files[0].name").value("message.txt"))
                .andExpect(jsonPath("$.files[0].size").value(note.length))
                .andReturn().getResponse().getContentAsString();
        String fileId = JsonPath.read(manifest, "$.files[0].id");

        mvc.perform(get("/api/share/" + code + "/files/" + fileId))
                .andExpect(status().isOk())
                .andExpect(content().bytes(note));
    }

    @Test
    void failedUploadLeavesNothing() throws Exception {
        String code = create(3600);
        mvc.perform(put("/api/share/" + code + "/files/big.bin").content(new byte[501]));

        mvc.perform(get("/api/share/" + code)).andExpect(jsonPath("$.files.length()").value(0));
    }

    @Test
    void sameNameTwiceIs409() throws Exception {
        String code = create(3600);
        mvc.perform(put("/api/share/" + code + "/files/a.txt").content(new byte[] {1})).andExpect(status().isCreated());
        mvc.perform(put("/api/share/" + code + "/files/A.txt").content(new byte[] {1})).andExpect(status().isConflict());
    }

    @Test
    void tooLargeIs413() throws Exception {
        String code = create(3600);
        mvc.perform(put("/api/share/" + code + "/files/big.bin").content(new byte[501]))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test
    void onlyOfferedLifetimesAreAccepted() throws Exception {
        mvc.perform(post("/api/share").contentType(MediaType.APPLICATION_JSON).content("{\"ttlSeconds\":7200}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownCodeIs404() throws Exception {
        mvc.perform(get("/api/share/ZZZZZZ")).andExpect(status().isNotFound());
    }

    private String create(int ttl) throws Exception {
        return JsonPath.read(mvc.perform(post("/api/share").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ttlSeconds\":%d}".formatted(ttl)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.code");
    }
}
