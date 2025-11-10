package com.adam_stegienko.campaign_controller_api_rest.controller;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.adam_stegienko.campaign_controller_api_rest.model.PlannerBook;
import com.adam_stegienko.campaign_controller_api_rest.repositories.PlannerBookRepository;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@RestController
@RequestMapping("v1/api/plannerbooks")
public class PlannerBookController {

    private final PlannerBookRepository plannerBookRepository;
    private static final Logger LOGGER = LoggerFactory.getLogger(PlannerBookController.class);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Autowired
    public PlannerBookController(PlannerBookRepository plannerBookRepository) {
        this.plannerBookRepository = plannerBookRepository;
    }

    @PostConstruct
    public void init() {}

    @PreDestroy
    public void cleanUp() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            LOGGER.error("Executor shutdown interrupted", e);
            Thread.currentThread().interrupt(); // Reset the interrupt flag
        }
    }

    // GET /v1/api/plannerbooks - all planner books
    @GetMapping
    public List<PlannerBook> getAllPlannerBooks() {
        return plannerBookRepository.findAll();
    }

    @GetMapping("/{id}")
    public PlannerBook getPlannerBook(@PathVariable("id") UUID id) {
        return plannerBookRepository.findById(id).orElse(null);
    }

    // POST /v1/api/plannerbooks - create new planner book
    @PostMapping
    public PlannerBook createPlannerBook(@RequestBody PlannerBook newPlannerBook) {
        return plannerBookRepository.save(newPlannerBook);
    }

    // PUT /v1/api/plannerbooks/{id} - update planner book by id
    @PutMapping(path = "/{id}")
    public PlannerBook updatePlannerBook(@PathVariable UUID id, @RequestBody PlannerBook updatedPlannerBook) {
        return plannerBookRepository.findById(id)
            .map(plannerBook -> {
                plannerBook.setCampaign(updatedPlannerBook.getCampaign());
                plannerBook.setAction(updatedPlannerBook.getAction());
                plannerBook.setExecutionDate(updatedPlannerBook.getExecutionDate());
                return plannerBookRepository.save(plannerBook);
            })
            .orElseGet(() -> {
                updatedPlannerBook.setId(id);
                return plannerBookRepository.save(updatedPlannerBook);
            });
    }

    @DeleteMapping("/{id}")
    public void deletePlannerBook(@PathVariable("id") UUID id) {
        plannerBookRepository.deleteById(id);
    }
}
