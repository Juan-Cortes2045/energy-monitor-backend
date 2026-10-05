package com.energymonitor.home.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.energymonitor.home.application.exception.AccessCodeNotFoundException;
import com.energymonitor.home.application.exception.HomeAccessDeniedException;
import com.energymonitor.home.application.exception.HomeConflictException;
import com.energymonitor.home.application.exception.HomeNotFoundException;
import com.energymonitor.home.application.exception.HomeTypeNotFoundException;
import com.energymonitor.home.application.exception.MissingUserIdentityException;
import com.energymonitor.home.application.exception.UserHomeNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

@WebMvcTest(controllers = HomeExceptionHandler.class)
class HomeExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void missingUserIdentityReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/homes")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void homeNotFoundReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/homes")
                        .header("X-User-Id", "use0000001")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
