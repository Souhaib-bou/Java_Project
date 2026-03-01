package Services;

import Models.OnboardingTask;
import Utils.MyDB;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TaskService {

    private final Connection cnx;

    /**
     * Creates a new TaskService instance.
     */
    public TaskService() {
        cnx = MyDB.getInstance().getConnection();
    }

    // ADD TASK
    /**
     * Creates a new record and updates the UI.
     */
    public void addOnboardingTask(OnboardingTask task) throws SQLException {
        String sql = "INSERT INTO onboardingtask (Planid, Title, Description, Status, Filepath) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql);

        ps.setInt(1, task.getPlanId());
        ps.setString(2, task.getTitle());
        ps.setString(3, task.getDescription());
        ps.setString(4, toDbStatus(task.getStatus()));

        if (task.getFilepath() == null || task.getFilepath().trim().isEmpty()) {
            ps.setNull(5, Types.VARCHAR);
        } else {
            ps.setString(5, task.getFilepath());
        }

        ps.executeUpdate();
    }

    // UPDATE TASK
    /**
     * Updates the selected record and refreshes the UI.
     */
    public void updateOnboardingTask(int taskId, OnboardingTask task) throws SQLException {
        String sql = "UPDATE onboardingtask SET Planid = ?, Title = ?, Description = ?, Status = ?, Filepath = ? WHERE Taskid = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);

        ps.setInt(1, task.getPlanId());
        ps.setString(2, task.getTitle());
        ps.setString(3, task.getDescription());
        ps.setString(4, toDbStatus(task.getStatus()));
        ps.setString(5, task.getFilepath());
        ps.setInt(6, taskId);

        ps.executeUpdate();
    }

    // DELETE TASK
    /**
     * Deletes the selected record and refreshes the UI.
     */
    public void deleteOnboardingTask(int taskId) throws SQLException {
        String sql = "DELETE FROM onboardingtask WHERE Taskid = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, taskId);
        ps.executeUpdate();
    }

    // GET ALL TASKS
    /**
     * Returns the allonboardingtasks value.
     */
    public List<OnboardingTask> getAllOnboardingTasks() throws SQLException {
        List<OnboardingTask> list = new ArrayList<>();
        String sql = "SELECT * FROM onboardingtask";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            OnboardingTask t = new OnboardingTask();
            t.setTaskId(rs.getInt("Taskid"));
            t.setPlanId(rs.getInt("Planid"));
            t.setTitle(rs.getString("Title"));
            t.setDescription(rs.getString("Description"));
            t.setStatus(rs.getString("Status"));
            t.setFilepath(rs.getString("Filepath"));
            list.add(t);
        }

        return list;
    }

    // GET TASK BY ID
    /**
     * Returns the onboardingtaskbyid value.
     */
    public OnboardingTask getOnboardingTaskById(int taskId) throws SQLException {
        String sql = "SELECT * FROM onboardingtask WHERE Taskid = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, taskId);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            OnboardingTask t = new OnboardingTask();
            t.setTaskId(rs.getInt("Taskid"));
            t.setPlanId(rs.getInt("Planid"));
            t.setTitle(rs.getString("Title"));
            t.setDescription(rs.getString("Description"));
            t.setStatus(rs.getString("Status"));
            t.setFilepath(rs.getString("Filepath"));
            return t;
        }

        return null;
    }

    // GET TASKS BY PLAN
    /**
     * Returns the tasksbyplanid value.
     */
    public List<OnboardingTask> getTasksByPlanId(int planId) throws SQLException {
        List<OnboardingTask> list = new ArrayList<>();
        String sql = "SELECT * FROM onboardingtask WHERE Planid = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, planId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            OnboardingTask t = new OnboardingTask();
            t.setTaskId(rs.getInt("Taskid"));
            t.setPlanId(rs.getInt("Planid"));
            t.setTitle(rs.getString("Title"));
            t.setDescription(rs.getString("Description"));
            t.setStatus(rs.getString("Status"));
            t.setFilepath(rs.getString("Filepath"));
            list.add(t);
        }

        return list;
    }
    private String toDbStatus(String uiStatus) {
        if (uiStatus == null) return "not_started";

        String s = uiStatus.trim().toLowerCase();

        return switch (s) {
            case "not started", "not_started" -> "not_started";
            case "in progress", "in_progress" -> "in_progress";
            case "completed" -> "completed";
            case "blocked" -> "blocked";
            case "on hold", "on_hold" -> "on_hold";
            default -> "not_started";
        };
    }

    // GET TASKS BY STATUS
    /**
     * Returns the tasksbystatus value.
     */
    public List<OnboardingTask> getTasksByStatus(String status) throws SQLException {
        List<OnboardingTask> list = new ArrayList<>();
        String sql = "SELECT * FROM onboardingtask WHERE Status = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, status);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            OnboardingTask t = new OnboardingTask();
            t.setTaskId(rs.getInt("Taskid"));
            t.setPlanId(rs.getInt("Planid"));
            t.setTitle(rs.getString("Title"));
            t.setDescription(rs.getString("Description"));
            t.setStatus(rs.getString("Status"));
            t.setFilepath(rs.getString("Filepath"));
            list.add(t);
        }

        return list;
    }

    // DELETE ALL TASKS FOR PLAN
    /**
     * Deletes the selected record and refreshes the UI.
     */
    public void deleteTasksByPlanId(int planId) throws SQLException {
        String sql = "DELETE FROM onboardingtask WHERE Planid = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, planId);
        ps.executeUpdate();
    }
    /**
     * Executes this operation.
     */
    public int countTasksByPlanId(int planId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM onboardingtask WHERE Planid = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, planId);
        ResultSet rs = ps.executeQuery();
        rs.next();
        return rs.getInt(1);
    }

    /**
     * Executes this operation.
     */
    public int countCompletedTasksByPlanId(int planId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM onboardingtask WHERE Planid = ? AND Status = 'Completed'";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, planId);
        ResultSet rs = ps.executeQuery();
        rs.next();
        return rs.getInt(1);
    }

}
