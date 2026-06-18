package com.proxio.apigateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RateLimitingFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void actuatorEndpointBypassesRateLimit() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void normalRequestGetsRateLimitHeaders() throws Exception {
        mockMvc.perform(get("/gateway/ping"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-RateLimit-Limit"));
    }
}
