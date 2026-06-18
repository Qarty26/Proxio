package com.proxio.discoveryservice.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DiscoveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registersAndResolvesServiceInstances() throws Exception {
        mockMvc.perform(post("/api/discovery/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "serviceId": "backend",
                                  "instanceId": "backend-1",
                                  "baseUrl": "http://backend:8080"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serviceId").value("backend"));

        mockMvc.perform(get("/api/discovery/services/backend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].baseUrl").value("http://backend:8080"));
    }
}
