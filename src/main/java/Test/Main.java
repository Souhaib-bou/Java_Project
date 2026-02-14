package Test;

import Models.OnboardingPlan;
import Models.OnboardingTask;
import Services.PlanService;
import Services.TaskService;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        PlanService planService = new PlanService();
        TaskService taskService = new TaskService();

        try {

            // Create future deadline (30 days from now)
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, 30);
            Date futureDeadline = calendar.getTime();

            int userId = 1; // Make sure this exists in Users table

            System.out.println("=== Creating Onboarding Plan ===");

            OnboardingPlan plan = new OnboardingPlan(
                    0,
                    userId,
                    "Pending",
                    futureDeadline
            );

            int generatedPlanId = planService.addOnboardingPlan(plan);
            System.out.println("✔ Plan created with ID: " + generatedPlanId);


            System.out.println("\n=== Adding Tasks ===");

            OnboardingTask task1 = new OnboardingTask(
                    0,
                    generatedPlanId,
                    "Complete Documentation",
                    "Review and sign all onboarding documents",
                    "Not Started",
                    "/documents/onboarding_docs.pdf"
            );

            taskService.addOnboardingTask(task1);
            System.out.println("✔ Task 1 added");

            OnboardingTask task2 = new OnboardingTask(
                    0,
                    generatedPlanId,
                    "IT Setup",
                    "Set up laptop, email and system access",
                    "Not Started",
                    "/documents/it_setup.pdf"
            );

            taskService.addOnboardingTask(task2);
            System.out.println("✔ Task 2 added");


            System.out.println("\n=== All Plans ===");
            List<OnboardingPlan> plans = planService.getAllOnboardingPlans();
            plans.forEach(System.out::println);


            System.out.println("\n=== All Tasks ===");
            List<OnboardingTask> tasks = taskService.getAllOnboardingTasks();
            tasks.forEach(System.out::println);


        } catch (SQLException e) {
            System.err.println("Database Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
