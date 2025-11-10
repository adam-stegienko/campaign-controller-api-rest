package com.adam_stegienko.campaign_controller_api_rest;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.adam_stegienko.campaign_controller_api_rest.repositories.PlannerBookRepository;
import com.adam_stegienko.campaign_controller_api_rest.services.GoogleAdsApiService;
import com.google.ads.googleads.lib.GoogleAdsClient;
import com.google.ads.googleads.v21.services.CampaignServiceClient;
import com.google.ads.googleads.v21.services.GoogleAdsServiceClient;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.yaml")
class CampaignControllerApiTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlannerBookRepository plannerBookRepository;

    @MockBean
    private GoogleAdsClient googleAdsClient;

    @MockBean
    private GoogleAdsServiceClient googleAdsServiceClient;

    @MockBean
    private CampaignServiceClient campaignServiceClient;

    @MockBean
    private GoogleAdsApiService googleAdsApiService;

    @Test
    void shouldReturnCampaignControllerApp() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk());
    }

    @Test
    void shouldReturnPlannerBooks() throws Exception {
        mockMvc.perform(get("/v1/api/plannerbooks"))
            .andExpect(status().isOk());
    }

    @Test
    void shouldReturnPlannerBookById() throws Exception {
        UUID uuid = UUID.randomUUID();
        mockMvc.perform(get("/v1/api/plannerbooks/" + uuid.toString()))
            .andExpect(status().isOk());
    }

    @Test
    void shouldCreatePlannerBook() throws Exception {
        String newPlannerBookJson = "{"
            + "\"campaign\":\"Test Campaign\","
            + "\"action\":1,"
            + "\"execution_date\":\"2022-01-01 00:00:00\""
            + "}";

        mockMvc.perform(post("/v1/api/plannerbooks")
            .contentType(MediaType.APPLICATION_JSON)
            .content(newPlannerBookJson))
            .andExpect(status().isOk());
    }

    @Test
    void shouldUpdatePlannerBook() throws Exception {
        UUID uuid = UUID.randomUUID();
        String updatedPlannerBookJson = "{"
            + "\"campaign\":\"Updated Campaign\","
            + "\"action\":0,"
            + "\"execution_date\":\"2022-01-02 00:00:00\""
            + "}";

        mockMvc.perform(put("/v1/api/plannerbooks/" + uuid.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(updatedPlannerBookJson))
            .andExpect(status().isOk());
    }

    @Test
    void shouldDeletePlannerBook() throws Exception {
        UUID uuid = UUID.randomUUID();
        mockMvc.perform(delete("/v1/api/plannerbooks/" + uuid.toString()))
            .andExpect(status().isOk());
    }
}
