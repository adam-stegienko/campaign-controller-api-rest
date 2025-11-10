package com.adam_stegienko.campaign_controller_api_rest.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.adam_stegienko.campaign_controller_api_rest.model.PlannerBook;

@Repository
public interface PlannerBookRepository extends JpaRepository<PlannerBook, UUID> {

}
