package test;

import Models.OnboardingPlan;
import Models.OnboardingTask;
import Services.PlanService;
import Services.TaskService;
import org.junit.jupiter.api.*;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TaskServiceTest {

    private static TaskService taskService;
    private static PlanService planService;
    private static int testPlanId;
    private static int testTaskId;
    private static int testUserId = 1;

    @BeforeAll
    public static void setUp() {
        taskService = new TaskService();
        planService = new PlanService();
        System.out.println("=== Starting TaskService Tests ===");

        // Create a test plan first (tasks need a valid Planid)
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, 30);
            Date futureDeadline = calendar.getTime();

            OnboardingPlan plan = new OnboardingPlan(0, testUserId, "Pending", futureDeadline);
            testPlanId = planService.addOnboardingPlan(plan);
            System.out.println("Setup: Created test plan with ID: " + testPlanId);
        } catch (SQLException e) {
            fail("Failed to create test plan: " + e.getMessage());
        }
    }

    @Test
    @Order(1)
    @DisplayName("Test 1: Add OnboardingTask")
    public void testAddOnboardingTask() {
        try {
            // Create a new task
            OnboardingTask task = new OnboardingTask(
                    0,
                    testPlanId,
                    "Complete Documentation",
                    "Review and sign all onboarding documents",
                    "Not Started",
                    "/documents/onboarding_docs.pdf"
            );

            // Add the task
            taskService.addOnboardingTask(task);

            // Verify task was added by getting all tasks
            List<OnboardingTask> tasks = taskService.getTasksByPlanId(testPlanId);
            assertTrue(tasks.size() > 0, "At least one task should exist");

            // Store the first task ID for later tests
            testTaskId = tasks.get(0).getTaskid();

            System.out.println("✓ Test 1 Passed: Task added successfully");

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @Test
    @Order(2)
    @DisplayName("Test 2: Get OnboardingTask by ID")
    public void testGetOnboardingTaskById() {
        try {
            OnboardingTask task = taskService.getOnboardingTaskById(testTaskId);

            // Verify task was retrieved
            assertNotNull(task, "Task should not be null");
            assertEquals(testTaskId, task.getTaskid(), "Task ID should match");
            assertEquals(testPlanId, task.getPlanid(), "Plan ID should match");
            assertEquals("Complete Documentation", task.getTitle(), "Title should match");
            System.out.println("✓ Test 2 Passed: Retrieved task: " + task);

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @Test
    @Order(3)
    @DisplayName("Test 3: Get All OnboardingTasks")
    public void testGetAllOnboardingTasks() {
        try {
            List<OnboardingTask> tasks = taskService.getAllOnboardingTasks();

            // Verify list is not empty
            assertNotNull(tasks, "Tasks list should not be null");
            assertTrue(tasks.size() > 0, "Tasks list should contain at least one task");
            System.out.println("✓ Test 3 Passed: Retrieved " + tasks.size() + " tasks");

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @Test
    @Order(4)
    @DisplayName("Test 4: Get OnboardingTasks by Plan ID")
    public void testGetTasksByPlanId() {
        try {
            List<OnboardingTask> tasks = taskService.getTasksByPlanId(testPlanId);

            // Verify list contains tasks for the plan
            assertNotNull(tasks, "Tasks list should not be null");
            assertTrue(tasks.size() > 0, "Should have at least one task for the plan");

            // Verify all tasks belong to the correct plan
            for (OnboardingTask task : tasks) {
                assertEquals(testPlanId, task.getPlanid(), "All tasks should belong to plan " + testPlanId);
            }
            System.out.println("✓ Test 4 Passed: Retrieved " + tasks.size() + " tasks for plan " + testPlanId);

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @Test
    @Order(5)
    @DisplayName("Test 5: Add Multiple Tasks for Same Plan")
    public void testAddMultipleTasks() {
        try {
            // Add second task
            OnboardingTask task2 = new OnboardingTask(
                    0,
                    testPlanId,
                    "IT Setup",
                    "Set up laptop and email account",
                    "Not Started",
                    "/documents/it_setup.pdf"
            );
            taskService.addOnboardingTask(task2);

            // Add third task
            OnboardingTask task3 = new OnboardingTask(
                    0,
                    testPlanId,
                    "Training Modules",
                    "Complete mandatory training courses",
                    "Not Started",
                    "/documents/training.pdf"
            );
            taskService.addOnboardingTask(task3);

            // Verify multiple tasks exist
            List<OnboardingTask> tasks = taskService.getTasksByPlanId(testPlanId);
            assertTrue(tasks.size() >= 3, "Should have at least 3 tasks for the plan");
            System.out.println("✓ Test 5 Passed: Added multiple tasks, total: " + tasks.size());

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @Test
    @Order(6)
    @DisplayName("Test 6: Get OnboardingTasks by Status")
    public void testGetTasksByStatus() {
        try {
            List<OnboardingTask> tasks = taskService.getTasksByStatus("Not Started");

            // Verify list contains tasks with correct status
            assertNotNull(tasks, "Tasks list should not be null");

            // Verify all tasks have the correct status
            for (OnboardingTask task : tasks) {
                assertEquals("Not Started", task.getStatus(), "All tasks should have status 'Not Started'");
            }
            System.out.println("✓ Test 6 Passed: Retrieved " + tasks.size() + " tasks with status 'Not Started'");

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @Test
    @Order(7)
    @DisplayName("Test 7: Update OnboardingTask")
    public void testUpdateOnboardingTask() {
        try {
            // Get the current task
            OnboardingTask task = taskService.getOnboardingTaskById(testTaskId);
            assertNotNull(task, "Task should exist before update");

            // Update the task
            task.setStatus("In Progress");
            task.setDescription("Updated: Review and sign all onboarding documents");

            // Perform update
            taskService.updateOnboardingTask(testTaskId, task);

            // Verify update
            OnboardingTask updatedTask = taskService.getOnboardingTaskById(testTaskId);
            assertEquals("In Progress", updatedTask.getStatus(), "Status should be updated to 'In Progress'");
            assertTrue(updatedTask.getDescription().contains("Updated"), "Description should be updated");
            System.out.println("✓ Test 7 Passed: Task updated successfully");

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @Test
    @Order(8)
    @DisplayName("Test 8: Delete OnboardingTask")
    public void testDeleteOnboardingTask() {
        try {
            // Delete the task
            taskService.deleteOnboardingTask(testTaskId);

            // Verify deletion
            OnboardingTask deletedTask = taskService.getOnboardingTaskById(testTaskId);
            assertNull(deletedTask, "Task should be null after deletion");
            System.out.println("✓ Test 8 Passed: Task deleted successfully");

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @Test
    @Order(9)
    @DisplayName("Test 9: Delete All Tasks by Plan ID")
    public void testDeleteTasksByPlanId() {
        try {
            // Delete all tasks for the plan
            taskService.deleteTasksByPlanId(testPlanId);

            // Verify all tasks are deleted
            List<OnboardingTask> tasks = taskService.getTasksByPlanId(testPlanId);
            assertEquals(0, tasks.size(), "No tasks should remain for the plan");
            System.out.println("✓ Test 9 Passed: All tasks deleted for plan " + testPlanId);

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @AfterAll
    public static void tearDown() {
        // Clean up: Delete the test plan
        try {
            planService.deleteOnboardingPlan(testPlanId);
            System.out.println("Cleanup: Deleted test plan with ID: " + testPlanId);
        } catch (SQLException e) {
            System.err.println("Failed to delete test plan: " + e.getMessage());
        }
        System.out.println("=== TaskService Tests Completed ===\n");
    }
}