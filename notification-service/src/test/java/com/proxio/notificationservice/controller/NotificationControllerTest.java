package com.proxio.notificationservice.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void templatesReturnsSeedData() throws Exception {
        mockMvc.perform(get("/api/notifications/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("welcome"));
    }

    @Test
    void sendTestQueuesNotification() throws Exception {
        mockMvc.perform(post("/api/notifications/send-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipient": "demo@proxio.test",
                                  "channel": "email",
                                  "message": "Fresh apples are back in stock."
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("queued"));
    }
}
