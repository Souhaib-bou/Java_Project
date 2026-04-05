package test;

import static org.junit.jupiter.api.Assertions.*;

import Models.Application;
import Services.ApplicationService;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ApplicationServiceTest {

    private static ApplicationService appService;
    private static int testApplicationId;
    private static int testUserId = 1;   // adjust for your DB
    private static int testJobId = 1;    // adjust for your DB

    @BeforeAll
    public static void setUp() {
        appService = new ApplicationService();
        System.out.println("=== Starting ApplicationService Tests ===");
    }

    @Test
    @Order(1)
    @DisplayName("Test 1: Add Application")
    public void testAddApplication() {
        try {
            Application app = new Application(
                    0,
                    LocalDate.now(),
                    "This is a test cover letter",
                    "Pending",
                    "/resumes/test_resume.pdf",
                    LocalDateTime.now(),
                    testUserId,
                    testJobId
            );

            appService.add(app);

            List<Application> apps = appService.getByUser(testUserId);
            assertFalse(apps.isEmpty(), "Applications for user should not be empty");

            testApplicationId = apps.get(apps.size() - 1).getApplicationId();
            System.out.println("✓ Test 1 Passed: Application added successfully with ID " + testApplicationId);

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @Test
    @Order(2)
    @DisplayName("Test 2: Get Application by User")
    public void testGetByUser() {
        try {
            List<Application> apps = appService.getByUser(testUserId);
            assertTrue(apps.stream().anyMatch(a -> a.getApplicationId() == testApplicationId),
                    "Should find the test application by user");
            System.out.println("✓ Test 2 Passed: Application retrieved by user");

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @Test
    @Order(3)
    @DisplayName("Test 3: Get Application by Job")
    public void testGetByJob() {
        try {
            List<Application> apps = appService.getByJob(testJobId);
            assertTrue(apps.stream().anyMatch(a -> a.getApplicationId() == testApplicationId),
                    "Should find the test application by job");
            System.out.println("✓ Test 3 Passed: Application retrieved by job");

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @Test
    @Order(4)
    @DisplayName("Test 4: Update Application")
    public void testUpdateApplication() {
        try {
            Application app = appService.getByUser(testUserId).stream()
                    .filter(a -> a.getApplicationId() == testApplicationId)
                    .findFirst()
                    .orElse(null);
            assertNotNull(app, "Application should exist before update");

            app.setCoverLetter("Updated cover letter");
            appService.update(app);

            Application updatedApp = appService.getByUser(testUserId).stream()
                    .filter(a -> a.getApplicationId() == testApplicationId)
                    .findFirst()
                    .orElse(null);

            assertEquals("Updated cover letter", updatedApp.getCoverLetter(), "Cover letter should be updated");
            System.out.println("✓ Test 4 Passed: Application updated successfully");

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @Test
    @Order(5)
    @DisplayName("Test 5: Update Application Status")
    public void testUpdateStatus() {
        try {
            appService.updateStatus(testApplicationId, "Reviewed");

            Application app = appService.getByUser(testUserId).stream()
                    .filter(a -> a.getApplicationId() == testApplicationId)
                    .findFirst()
                    .orElse(null);

            assertEquals("Reviewed", app.getCurrentStatus(), "Status should be updated");
            System.out.println("✓ Test 5 Passed: Application status updated successfully");

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @Test
    @Order(6)
    @DisplayName("Test 6: Get All Applications")
    public void testGetAllApplications() {
        try {
            List<Application> apps = appService.getAll();
            assertNotNull(apps, "Applications list should not be null");
            assertTrue(apps.size() > 0, "Applications list should contain at least one application");
            System.out.println("✓ Test 6 Passed: Retrieved " + apps.size() + " applications");

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @Test
    @Order(7)
    @DisplayName("Test 7: Delete Application")
    public void testDeleteApplication() {
        try {
            appService.delete(testApplicationId);

            List<Application> apps = appService.getByUser(testUserId);
            assertTrue(apps.stream().noneMatch(a -> a.getApplicationId() == testApplicationId),
                    "Application should be deleted");
            System.out.println("✓ Test 7 Passed: Application deleted successfully");

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @AfterAll
    public static void tearDown() {
        System.out.println("=== ApplicationService Tests Completed ===\n");
    }
}