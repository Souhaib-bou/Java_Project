package test;

import static org.junit.jupiter.api.Assertions.*;

import Models.User;
import Services.UserService;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserServiceTest {

    private static UserService userService;
    private static int testUserId;

    @BeforeAll
    public static void setUp() {
        userService = new UserService();
        System.out.println("=== Starting UserService Tests ===");
    }

    @Test
    @Order(1)
    @DisplayName("Test 1: Add User")
    public void testAddUser() {
        try {
            User u = new User(
                    0,
                    "Test",
                    "User",
                    "test.user@example.com",
                    "password123",
                    null,        // roleId
                    "active"
            );
            u.setProfilePic("/images/test_profile.png");

            testUserId = userService.addUser(u);
            assertTrue(testUserId > 0, "Generated user_id should be greater than 0");
            System.out.println("✓ Test 1 Passed: User added successfully with ID " + testUserId);

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @Test
    @Order(2)
    @DisplayName("Test 2: Get User by ID")
    public void testGetUserById() {
        try {
            User u = userService.getUserById(testUserId);
            assertNotNull(u, "User should exist");
            assertEquals("Test", u.getFirstName());
            assertEquals("User", u.getLastName());
            assertEquals("test.user@example.com", u.getEmail());
            System.out.println("✓ Test 2 Passed: User retrieved by ID");

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @Test
    @Order(3)
    @DisplayName("Test 3: Find User by Email")
    public void testFindByEmail() {
        try {
            User u = userService.findByEmail("test.user@example.com");
            assertNotNull(u, "User should be found by email");
            assertEquals(testUserId, u.getUserId());
            System.out.println("✓ Test 3 Passed: User retrieved by email");

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @Test
    @Order(4)
    @DisplayName("Test 4: Update User")
    public void testUpdateUser() {
        try {
            User u = userService.getUserById(testUserId);
            assertNotNull(u, "User should exist before update");

            u.setFirstName("Updated");
            u.setLastName("Tester");
            u.setProfilePic("/images/updated_profile.png");
            userService.updateUser(testUserId, u);

            User updated = userService.getUserById(testUserId);
            assertEquals("Updated", updated.getFirstName());
            assertEquals("Tester", updated.getLastName());
            assertEquals("/images/updated_profile.png", updated.getProfilePic());
            System.out.println("✓ Test 4 Passed: User updated successfully");

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @Test
    @Order(5)
    @DisplayName("Test 5: Set User Status")
    public void testSetUserStatus() {
        try {
            userService.setUserStatus(testUserId, "inactive");

            User u = userService.getUserById(testUserId);
            assertEquals("inactive", u.getStatus());
            System.out.println("✓ Test 5 Passed: User status updated successfully");

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @Test
    @Order(6)
    @DisplayName("Test 6: Get All Users")
    public void testGetAllUsers() {
        try {
            List<User> users = userService.getAllUsers();
            assertNotNull(users, "Users list should not be null");
            assertTrue(users.size() > 0, "Users list should contain at least one user");
            System.out.println("✓ Test 6 Passed: Retrieved " + users.size() + " users");

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @Test
    @Order(7)
    @DisplayName("Test 7: Delete User")
    public void testDeleteUser() {
        try {
            userService.deleteUser(testUserId);

            User u = userService.getUserById(testUserId);
            assertNull(u, "User should be null after deletion");
            System.out.println("✓ Test 7 Passed: User deleted successfully");

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @AfterAll
    public static void tearDown() {
        System.out.println("=== UserService Tests Completed ===\n");
    }
}