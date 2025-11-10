package com.adam_stegienko.campaign_controller_api_rest.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "plannerbooks")
public class PlannerBook {

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "campaign")
    private String campaign;

    @Column(name = "action")
    private Integer action;

    @Column(name = "execution_date")
    private LocalDateTime executionDate;

    public PlannerBook() {
    }

    public PlannerBook(UUID id, String campaign, Integer action, LocalDateTime executionDate) {
        this.id = id;
        this.campaign = campaign;
        this.action = action;
        this.executionDate = executionDate;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCampaign() {
        return campaign;
    }

    public void setCampaign(String campaign) {
        this.campaign = campaign;
    }

    public Integer getAction() {
        return action;
    }

    public void setAction(Integer action) {
        this.action = action;
    }

    public LocalDateTime getExecutionDate() {
        return executionDate;
    }

    public void setExecutionDate(LocalDateTime executionDate) {
        this.executionDate = executionDate;
    }
    
}