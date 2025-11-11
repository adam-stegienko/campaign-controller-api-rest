package com.adam_stegienko.campaign_controller_api_rest.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.adam_stegienko.campaign_controller_api_rest.services.GoogleAdsApiService;

@RestController
@RequestMapping("/v1/api/google-ads")
@ConditionalOnProperty(name = "api.googleads.enabled", havingValue = "true", matchIfMissing = false)
public class GoogleAdsApiController {

    private final GoogleAdsApiService googleAdsService;

    @Autowired
    public GoogleAdsApiController(GoogleAdsApiService googleAdsService) {
        this.googleAdsService = googleAdsService;
    }

    @GetMapping("/campaigns/status/{name}")
    public String getCampaignStatusByName(@PathVariable String name, @RequestParam String customerId) {
        return googleAdsService.getCampaignStatusByName(customerId, name);
    }

    @GetMapping("/campaigns/status")
    public String getCampaignStatusByNamesList(@RequestParam List<String> campaignNames, @RequestParam String customerId) {
        return googleAdsService.getCampaignStatusByNamesList(campaignNames, customerId);
    }

    @PutMapping("/campaigns/status/{name}")
    public String updateCampaignStatusByName(@PathVariable String name, @RequestParam String customerId, @RequestParam String status) {
        return googleAdsService.updateCampaignStatusByName(customerId, name, status);
    }
}
