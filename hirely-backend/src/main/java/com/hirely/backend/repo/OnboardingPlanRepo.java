package com.hirely.backend.repo;

import com.hirely.backend.model.OnboardingPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface OnboardingPlanRepo extends JpaRepository<OnboardingPlan, Integer> {

    // Candidate: only their plans
    List<OnboardingPlan> findByUserId(Integer userId);
    Optional<OnboardingPlan> findByQrToken(String qrToken);
}