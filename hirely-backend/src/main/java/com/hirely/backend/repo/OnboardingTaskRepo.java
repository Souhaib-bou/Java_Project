package com.hirely.backend.repo;

import com.hirely.backend.model.OnboardingTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OnboardingTaskRepo extends JpaRepository<OnboardingTask, Integer> {

    List<OnboardingTask> findByPlanId(Integer planId);
}