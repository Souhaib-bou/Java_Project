package test;

import static org.junit.jupiter.api.Assertions.*;

import Models.JobOffer;
import Services.JobOfferService;
import java.sql.Date;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.List;
import org.junit.jupiter.api.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JobOfferServiceTest {

    private static JobOfferService jobService;
    private static int testJobId;
    private static int testUserId = 1; // change if needed

    @BeforeAll
    public static void setUp() {
        jobService = new JobOfferService();
        System.out.println("=== Starting JobOfferService Tests ===");
    }

    @Test
    @Order(1)
    @DisplayName("Test 1: Add JobOffer")
    public void testAddJobOffer() {
        try {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, 1);
            Date pubDate = new Date(cal.getTimeInMillis());

            JobOffer job = new JobOffer(
                    "Test Job",
                    "Job description for testing",
                    "CDI",
                    50000.0,
                    "Remote",
                    2,
                    pubDate,
                    "Open",
                    testUserId
            );

            jobService.add(job);

            List<JobOffer> list = jobService.getAll();
            assertFalse(list.isEmpty(), "Job list should not be empty");

            // Save testJobId for later tests
            testJobId = list.get(list.size() - 1).getJobOfferId();

            System.out.println("✓ Test 1 Passed: Job added successfully with ID " + testJobId);

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @Test
    @Order(2)
    @DisplayName("Test 2: Get JobOffer by ID")
    public void testGetJobOfferById() {
        try {
            List<JobOffer> list = jobService.getAll();
            JobOffer job = list.stream()
                    .filter(j -> j.getJobOfferId() == testJobId)
                    .findFirst()
                    .orElse(null);

            assertNotNull(job, "JobOffer should not be null");
            assertEquals("Test Job", job.getTitle(), "Title should match");
            System.out.println("✓ Test 2 Passed: Retrieved JobOffer: " + job.getTitle());

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @Test
    @Order(3)
    @DisplayName("Test 3: Update JobOffer")
    public void testUpdateJobOffer() {
        try {
            List<JobOffer> list = jobService.getAll();
            JobOffer job = list.stream()
                    .filter(j -> j.getJobOfferId() == testJobId)
                    .findFirst()
                    .orElse(null);

            assertNotNull(job, "JobOffer should exist before update");

            job.setTitle("Updated Test Job");
            job.setStatus("Closed");
            jobService.update(job);

            // Verify update
            List<JobOffer> updatedList = jobService.getAll();
            JobOffer updatedJob = updatedList.stream()
                    .filter(j -> j.getJobOfferId() == testJobId)
                    .findFirst()
                    .orElse(null);

            assertEquals("Updated Test Job", updatedJob.getTitle(), "Title should be updated");
            assertEquals("Closed", updatedJob.getStatus(), "Status should be updated");
            System.out.println("✓ Test 3 Passed: JobOffer updated successfully");

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @Test
    @Order(4)
    @DisplayName("Test 4: Get All JobOffers")
    public void testGetAllJobOffers() {
        try {
            List<JobOffer> list = jobService.getAll();
            assertNotNull(list, "JobOffer list should not be null");
            assertTrue(list.size() > 0, "JobOffer list should contain at least one job");
            System.out.println("✓ Test 4 Passed: Retrieved " + list.size() + " job offers");

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @Test
    @Order(5)
    @DisplayName("Test 5: Delete JobOffer")
    public void testDeleteJobOffer() {
        try {
            jobService.delete(testJobId);

            List<JobOffer> list = jobService.getAll();
            boolean exists = list.stream().anyMatch(j -> j.getJobOfferId() == testJobId);
            assertFalse(exists, "JobOffer should be deleted");
            System.out.println("✓ Test 5 Passed: JobOffer deleted successfully");

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @AfterAll
    public static void tearDown() {
        System.out.println("=== JobOfferService Tests Completed ===\n");
    }
}