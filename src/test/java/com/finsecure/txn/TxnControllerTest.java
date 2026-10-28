package com.finsecure.txn;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "txn.api-key=test")
@AutoConfigureMockMvc
class TxnControllerTest {

    @Autowired
    private MockMvc mvc;

    private static final String BODY = "{\"id\":\"T1\",\"fromAccount\":\"111\",\"toAccount\":\"222\",\"amount\":%s,\"currency\":\"SGD\"}";

    @Test
    void health() throws Exception {
        mvc.perform(get("/health")).andExpect(status().isOk());
    }

    @Test
    void validReturns200() throws Exception {
        mvc.perform(post("/validate").header("X-API-Key", "test").contentType(MediaType.APPLICATION_JSON)
                .content(BODY.formatted("100"))).andExpect(status().isOk());
    }

    @Test
    void invalidReturns422() throws Exception {
        mvc.perform(post("/validate").header("X-API-Key", "test").contentType(MediaType.APPLICATION_JSON)
                .content(BODY.formatted("9999"))).andExpect(status().isUnprocessableContent());
    }
}
