package com.adam_stegienko.campaign_controller_api_rest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.adam_stegienko.campaign_controller_api_rest"})
@EntityScan("com.adam_stegienko.campaign_controller_api_rest.model")
@EnableJpaRepositories("com.adam_stegienko.campaign_controller_api_rest.repositories")
@ComponentScan(basePackages = {"com.adam_stegienko.campaign_controller_api_rest.controller"})
@ComponentScan(basePackages = {"com.adam_stegienko.campaign_controller_api_rest.services"})
@ComponentScan(basePackages = {"com.adam_stegienko.campaign_controller_api_rest.config"})
@ComponentScan(basePackages = {"com.adam_stegienko.campaign_controller_api_rest.dto"})
@EnableScheduling
public class CampaignControllerApi {

  public static void main(String[] args) {
    SpringApplication.run(CampaignControllerApi.class, args);
  }

}
