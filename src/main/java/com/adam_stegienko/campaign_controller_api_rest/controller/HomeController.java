package com.adam_stegienko.campaign_controller_api_rest.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

  @GetMapping("/")
  public String campaignControllerMessage() {
    return "Campaign Controller App";
  }
}