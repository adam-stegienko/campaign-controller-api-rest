package com.adam_stegienko.campaign_controller_api_rest.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.adam_stegienko.campaign_controller_api_rest.model.PlannerBook;
import com.adam_stegienko.campaign_controller_api_rest.repositories.PlannerBookRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.annotation.PostConstruct;

@Controller
public class EventController {
    private final PlannerBookRepository plannerBookRepository;
    private static final Logger LOGGER = LoggerFactory.getLogger(EventController.class);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Autowired
    public EventController(PlannerBookRepository plannerBookRepository) {
        this.plannerBookRepository = plannerBookRepository;
    }

    @PostConstruct
    public void init() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            executor.shutdown();
            try {
                executor.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                LOGGER.error(e.toString());
            }
        }));
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @CrossOrigin
    public SseEmitter streamPlannerBookEvents() {
        final SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        emitter.onCompletion(() -> LOGGER.info("SseEmitter is completed"));
        emitter.onTimeout(() -> LOGGER.info("SseEmitter is timed out"));
        emitter.onError((ex) -> LOGGER.info("SseEmitter got error:", ex));
        
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        executor.execute(() -> {
            try {
                while (true) {
                    // Fetch planner books from /v1/api/plannerbooks
                    List<PlannerBook> plannerBooksList = plannerBookRepository.findAll();

                    // filter out plannerbooks that have executionDate in the past or equal to now
                    LocalDateTime now = LocalDateTime.now();
                    Optional<PlannerBook> oldestPlannerBook = plannerBooksList.stream()
                        .filter(book -> book.getExecutionDate() != null && !book.getExecutionDate().isAfter(now))
                        .min((book1, book2) -> book1.getExecutionDate().compareTo(book2.getExecutionDate()));

                    if (oldestPlannerBook.isPresent()) {
                        String json = objectMapper.writeValueAsString(oldestPlannerBook.get());
                        emitter.send(SseEmitter.event().data(json));
                    }

                    TimeUnit.SECONDS.sleep(5); // Sleep for 5 seconds before fetching again
                }
            } catch (IOException | IllegalStateException | InterruptedException e) {
                // Log the error or handle it as needed
                System.out.println("Client disconnected or error sending event: " + e.getMessage());
                emitter.completeWithError(e); // Complete the emitter with error if you want to log client disconnection as an error
            } finally {
                emitter.complete(); // Complete the emitter when done
            }
        });

        return emitter;
    }
}
