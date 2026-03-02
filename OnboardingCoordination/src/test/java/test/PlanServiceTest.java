package test;

import static org.junit.jupiter.api.Assertions.*;

import Models.OnboardingPlan;
import Services.PlanService;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PlanServiceTest {

    private static PlanService planService;
    private static int testPlanId;
    private static int testUserId = 1; // Assuming user_id 1 exists in User table

    @BeforeAll
    /**
     * Sets the up value.
     */
    public static void setUp() {
        planService = new PlanService();
        System.out.println("=== Starting PlanService Tests ===");
    }

    @Test
    @Order(1)
    @DisplayName("Test 1: Add OnboardingPlan")
    /**
     * Executes this operation.
     */
    public void testAddOnboardingPlan() {
        try {
            // Create a future date
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, 30);
            Date futureDeadline = calendar.getTime();

            // Create a new plan
            OnboardingPlan plan = new OnboardingPlan(0, testUserId, "Pending", futureDeadline);

            // Add the plan and get generated ID
            testPlanId = planService.addOnboardingPlan(plan);

            // Verify the ID was generated
            assertTrue(testPlanId > 0, "Plan ID should be greater than 0");
            System.out.println("✓ Test 1 Passed: Plan added with ID: " + testPlanId);

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @Test
    @Order(2)
    @DisplayName("Test 2: Get OnboardingPlan by ID")
    /**
     * Executes this operation.
     */
    public void testGetOnboardingPlanById() {
        try {
            OnboardingPlan plan = planService.getOnboardingPlanById(testPlanId);

            // Verify plan was retrieved
            assertNotNull(plan, "Plan should not be null");
            assertEquals(testPlanId, plan.getPlanid(), "Plan ID should match");
            assertEquals(testUserId, plan.getUser_id(), "User ID should match");
            assertEquals("Pending", plan.getStatus(), "Status should be 'Pending'");
            System.out.println("✓ Test 2 Passed: Retrieved plan: " + plan);

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @Test
    @Order(3)
    @DisplayName("Test 3: Get All OnboardingPlans")
    /**
     * Executes this operation.
     */
    public void testGetAllOnboardingPlans() {
        try {
            List<OnboardingPlan> plans = planService.getAllOnboardingPlans();

            // Verify list is not empty
            assertNotNull(plans, "Plans list should not be null");
            assertTrue(plans.size() > 0, "Plans list should contain at least one plan");
            System.out.println("✓ Test 3 Passed: Retrieved " + plans.size() + " plans");

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @Test
    @Order(4)
    @DisplayName("Test 4: Get OnboardingPlans by User ID")
    /**
     * Executes this operation.
     */
    public void testGetOnboardingPlansByUserId() {
        try {
            List<OnboardingPlan> plans = planService.getOnboardingPlansByUserId(testUserId);

            // Verify list contains plans for the user
            assertNotNull(plans, "Plans list should not be null");
            assertTrue(plans.size() > 0, "Should have at least one plan for user");

            // Verify all plans belong to the correct user
            for (OnboardingPlan plan : plans) {
                assertEquals(testUserId, plan.getUser_id(), "All plans should belong to user " + testUserId);
            }
            System.out.println("✓ Test 4 Passed: Retrieved " + plans.size() + " plans for user " + testUserId);

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @Test
    @Order(5)
    @DisplayName("Test 5: Get OnboardingPlans by Status")
    /**
     * Executes this operation.
     */
    public void testGetOnboardingPlansByStatus() {
        try {
            List<OnboardingPlan> plans = planService.getOnboardingPlansByStatus("Pending");

            // Verify list contains plans with correct status
            assertNotNull(plans, "Plans list should not be null");

            // Verify all plans have the correct status
            for (OnboardingPlan plan : plans) {
                assertEquals("Pending", plan.getStatus(), "All plans should have status 'Pending'");
            }
            System.out.println("✓ Test 5 Passed: Retrieved " + plans.size() + " plans with status 'Pending'");

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @Test
    @Order(6)
    @DisplayName("Test 6: Update OnboardingPlan")
    /**
     * Executes this operation.
     */
    public void testUpdateOnboardingPlan() {
        try {
            // Get the current plan
            OnboardingPlan plan = planService.getOnboardingPlanById(testPlanId);
            assertNotNull(plan, "Plan should exist before update");

            // Update the plan
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, 60);
            Date newDeadline = calendar.getTime();

            plan.setStatus("In Progress");
            plan.setDeadline(newDeadline);

            // Perform update
            planService.updateOnboardingPlan(testPlanId, plan);

            // Verify update
            OnboardingPlan updatedPlan = planService.getOnboardingPlanById(testPlanId);
            assertEquals("In Progress", updatedPlan.getStatus(), "Status should be updated to 'In Progress'");
            System.out.println("✓ Test 6 Passed: Plan updated successfully");

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @Test
    @Order(7)
    @DisplayName("Test 7: Delete OnboardingPlan")
    /**
     * Executes this operation.
     */
    public void testDeleteOnboardingPlan() {
        try {
            // Delete the plan
            planService.deleteOnboardingPlan(testPlanId);

            // Verify deletion
            OnboardingPlan deletedPlan = planService.getOnboardingPlanById(testPlanId);
            assertNull(deletedPlan, "Plan should be null after deletion");
            System.out.println("✓ Test 7 Passed: Plan deleted successfully");

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @AfterAll
    /**
     * Executes this operation.
     */
    public static void tearDown() {
        System.out.println("=== PlanService Tests Completed ===\n");
    }
}
