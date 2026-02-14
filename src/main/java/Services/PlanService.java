package Services;

import Models.OnboardingPlan;
import Utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PlanService {

    private final Connection cnx;

    public PlanService() {
        cnx = MyDB.getInstance().getConnection();
    }

    // ADD PLAN
    public int addOnboardingPlan(OnboardingPlan plan) throws SQLException {
        String sql = "INSERT INTO OnboardingPlan (user_id, Status, Deadline) VALUES (?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

        ps.setInt(1, plan.getUserId());
        ps.setString(2, plan.getStatus());
        ps.setDate(3, new java.sql.Date(plan.getDeadline().getTime()));

        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            return rs.getInt(1);
        } else {
            throw new SQLException("Failed to retrieve generated Plan ID.");
        }
    }

    // UPDATE PLAN
    public void updateOnboardingPlan(int planId, OnboardingPlan plan) throws SQLException {
        String sql = "UPDATE OnboardingPlan SET user_id = ?, Status = ?, Deadline = ? WHERE Planid = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);

        ps.setInt(1, plan.getUserId());
        ps.setString(2, plan.getStatus());
        ps.setDate(3, new java.sql.Date(plan.getDeadline().getTime()));
        ps.setInt(4, planId);

        ps.executeUpdate();
    }

    // DELETE PLAN
    public void deleteOnboardingPlan(int planId) throws SQLException {
        String sql = "DELETE FROM OnboardingPlan WHERE Planid = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, planId);
        ps.executeUpdate();
    }

    // GET ALL PLANS
    public List<OnboardingPlan> getAllOnboardingPlans() throws SQLException {
        List<OnboardingPlan> list = new ArrayList<>();
        String sql = "SELECT * FROM OnboardingPlan";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            OnboardingPlan p = new OnboardingPlan();
            p.setPlanId(rs.getInt("Planid"));
            p.setUserId(rs.getInt("user_id"));
            p.setStatus(rs.getString("Status"));
            p.setDeadline(rs.getDate("Deadline"));
            list.add(p);
        }

        return list;
    }

    // GET PLAN BY ID
    public OnboardingPlan getOnboardingPlanById(int planId) throws SQLException {
        String sql = "SELECT * FROM OnboardingPlan WHERE Planid = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, planId);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            OnboardingPlan p = new OnboardingPlan();
            p.setPlanId(rs.getInt("Planid"));
            p.setUserId(rs.getInt("user_id"));
            p.setStatus(rs.getString("Status"));
            p.setDeadline(rs.getDate("Deadline"));
            return p;
        }

        return null;
    }

    // GET PLANS BY USER
    public List<OnboardingPlan> getOnboardingPlansByUserId(int userId) throws SQLException {
        List<OnboardingPlan> list = new ArrayList<>();
        String sql = "SELECT * FROM OnboardingPlan WHERE user_id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            OnboardingPlan p = new OnboardingPlan();
            p.setPlanId(rs.getInt("Planid"));
            p.setUserId(rs.getInt("user_id"));
            p.setStatus(rs.getString("Status"));
            p.setDeadline(rs.getDate("Deadline"));
            list.add(p);
        }

        return list;
    }

    // GET PLANS BY STATUS
    public List<OnboardingPlan> getOnboardingPlansByStatus(String status) throws SQLException {
        List<OnboardingPlan> list = new ArrayList<>();
        String sql = "SELECT * FROM OnboardingPlan WHERE Status = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, status);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            OnboardingPlan p = new OnboardingPlan();
            p.setPlanId(rs.getInt("Planid"));
            p.setUserId(rs.getInt("user_id"));
            p.setStatus(rs.getString("Status"));
            p.setDeadline(rs.getDate("Deadline"));
            list.add(p);
        }

        return list;
    }
}
