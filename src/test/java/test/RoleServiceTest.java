package test;

import static org.junit.jupiter.api.Assertions.*;

import Models.Role;
import Services.RoleService;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RoleServiceTest {

    private static RoleService roleService;
    private static int testRoleId;

    @BeforeAll
    public static void setUp() {
        roleService = new RoleService();
        System.out.println("=== Starting RoleService Tests ===");
    }

    @Test
    @Order(1)
    @DisplayName("Test 1: Add Role")
    public void testAddRole() {
        try {
            Role r = new Role(
                    0,
                    "TestRole",
                    "active",
                    "dashboard_test",
                    "This is a test role"
            );
            testRoleId = roleService.addRole(r);
            assertTrue(testRoleId > 0, "Generated role_id should be greater than 0");
            System.out.println("✓ Test 1 Passed: Role added successfully with ID " + testRoleId);

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @Test
    @Order(2)
    @DisplayName("Test 2: Get All Roles")
    public void testGetAllRoles() {
        try {
            List<Role> roles = roleService.getAllRoles();
            assertNotNull(roles, "Roles list should not be null");
            assertTrue(roles.size() > 0, "Roles list should contain at least one role");
            System.out.println("✓ Test 2 Passed: Retrieved " + roles.size() + " roles");

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @Test
    @Order(3)
    @DisplayName("Test 3: Get Role ID by Name")
    public void testGetRoleIdByName() {
        try {
            Integer roleId = roleService.getRoleIdByName("TestRole");
            assertNotNull(roleId, "Role ID should not be null");
            assertEquals(testRoleId, roleId);
            System.out.println("✓ Test 3 Passed: Retrieved role ID by name");

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @Test
    @Order(4)
    @DisplayName("Test 4: Update Role")
    public void testUpdateRole() {
        try {
            Role r = new Role(
                    testRoleId,
                    "UpdatedTestRole",
                    "inactive",
                    "dashboard_updated",
                    "Updated test role description"
            );

            roleService.updateRole(testRoleId, r);

            Integer roleId = roleService.getRoleIdByName("UpdatedTestRole");
            assertNotNull(roleId, "Role ID after update should not be null");
            assertEquals(testRoleId, roleId);
            System.out.println("✓ Test 4 Passed: Role updated successfully");

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @Test
    @Order(5)
    @DisplayName("Test 5: Delete Role")
    public void testDeleteRole() {
        try {
            roleService.deleteRole(testRoleId);

            Integer roleId = roleService.getRoleIdByName("UpdatedTestRole");
            assertNull(roleId, "Role should be null after deletion");
            System.out.println("✓ Test 5 Passed: Role deleted successfully");

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @AfterAll
    public static void tearDown() {
        System.out.println("=== RoleService Tests Completed ===\n");
    }
}