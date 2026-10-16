package com.energymonitor.recommendation.adapter.in.web;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.energymonitor.recommendation.api.RecommendationStatus;
import com.energymonitor.recommendation.api.RecommendationType;
import com.energymonitor.recommendation.application.exception.RecommendationNotFoundException;
import com.energymonitor.recommendation.application.port.in.RecommendationInbox;
import com.energymonitor.recommendation.application.port.in.RecommendationView;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web layer of the recommendation inbox: only members of a home see, read or delete its
 * recommendations (the rule lives in the use case, here mocked); a stranger gets a 404 and an
 * unread recommendation cannot be deleted (409).
 *
 * <p>Filters are disabled and {@code Clock} is mocked for the same reasons as in the other web
 * slices.
 */
@WebMvcTest(RecommendationController.class)
@AutoConfigureMockMvc(addFilters = false)
class RecommendationControllerTest {

    private static final String USER_ID = "use0000001";
    private static final String HOME_ID = "hom0000001";
    private static final String RECOMMENDATION_ID = "rec0000001";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecommendationInbox inbox;

    @MockitoBean
    private CurrentUserResolver currentUser;

    @MockitoBean
    private Clock clock;

    @BeforeEach
    void callerIsAuthenticated() {
        when(currentUser.resolveCurrentUserId()).thenReturn(USER_ID);
    }

    private static RecommendationView sample(RecommendationStatus status) {
        return new RecommendationView(RECOMMENDATION_ID, HOME_ID, "DEV0000001", "Fridge",
                RecommendationType.PEAK_HOURS, "recommendation.peak_hours", Instant.parse("2026-10-13T20:00:00Z"),
                status);
    }

    @Test
    @DisplayName("list answers 200 with the recommendations of the home")
    void listReturnsRecommendations() throws Exception {
        // Arrange
        when(inbox.list(USER_ID, HOME_ID)).thenReturn(List.of(sample(RecommendationStatus.UNREAD)));

        // Act + Assert
        mockMvc.perform(get("/api/v1/recommendations").param("homeId", HOME_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idRecommendation").value(RECOMMENDATION_ID))
                .andExpect(jsonPath("$[0].type").value("PEAK_HOURS"))
                .andExpect(jsonPath("$[0].status").value("UNREAD"));
    }

    @Test
    @DisplayName("list without homeId answers 400")
    void listRequiresHomeId() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/api/v1/recommendations"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("list answers 404 when the caller does not belong to the home")
    void listRefusesStranger() throws Exception {
        // Arrange
        when(inbox.list(USER_ID, HOME_ID)).thenThrow(new RecommendationNotFoundException("home not found"));

        // Act + Assert
        mockMvc.perform(get("/api/v1/recommendations").param("homeId", HOME_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("home not found"));
    }

    @Test
    @DisplayName("mark read answers 200 with the read recommendation")
    void markReadReturnsRecommendation() throws Exception {
        // Arrange
        when(inbox.markRead(USER_ID, RECOMMENDATION_ID)).thenReturn(sample(RecommendationStatus.READ));

        // Act + Assert
        mockMvc.perform(put("/api/v1/recommendations/" + RECOMMENDATION_ID + "/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READ"));
    }

    @Test
    @DisplayName("delete answers 204 for a read recommendation")
    void deleteReturnsNoContent() throws Exception {
        // Act + Assert
        mockMvc.perform(delete("/api/v1/recommendations/" + RECOMMENDATION_ID))
                .andExpect(status().isNoContent());
        verify(inbox, times(1)).delete(USER_ID, RECOMMENDATION_ID);
    }

    @Test
    @DisplayName("delete answers 409 when the recommendation is still unread")
    void deleteRefusesUnread() throws Exception {
        // Arrange
        doThrow(new IllegalStateException("only a read recommendation can be deleted"))
                .when(inbox).delete(USER_ID, RECOMMENDATION_ID);

        // Act + Assert
        mockMvc.perform(delete("/api/v1/recommendations/" + RECOMMENDATION_ID))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("delete read answers 200 with how many were deleted")
    void deleteReadReturnsCount() throws Exception {
        // Arrange
        when(inbox.deleteRead(USER_ID, HOME_ID)).thenReturn(3);

        // Act + Assert
        mockMvc.perform(delete("/api/v1/recommendations").param("homeId", HOME_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(3));
    }
}
