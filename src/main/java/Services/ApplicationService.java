package Services;

import Models.Application;
import Utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ApplicationService {

    private Connection connection;

    public ApplicationService() {
        connection = MyDB.getInstance().getConnection();
    }
    public List<Application> getByUser(int userId) throws SQLException {
        List<Application> list = new ArrayList<>();

        String sql = "SELECT * FROM Application WHERE user_id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, userId);

        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Application app = new Application(
                    rs.getInt("applicationId"),
                    rs.getDate("applicationDate").toLocalDate(),
                    rs.getString("coverLetter"),
                    rs.getString("currentStatus"),
                    rs.getString("resumePath"),
                    rs.getTimestamp("lastUpdateDate").toLocalDateTime(),
                    rs.getInt("user_id"),
                    rs.getInt("jobOfferId"),
                    rs.getDouble("expectedSalary"),
                    rs.getDate("availabilityDate").toLocalDate(),
                    rs.getString("phone"),
                    rs.getString("email"),
                    rs.getInt("experienceYears"),
                    rs.getString("portfolioUrl"),
                    (Double) rs.getObject("score"),
                    rs.getString("reviewNote")
            );
            list.add(app);
        }
        return list;
    }
    // ================= ADD APPLICATION =================
    public void add(Application app) throws SQLException {

        String sql = "INSERT INTO Application " +
                "(applicationDate, coverLetter, resumePath, user_id, jobOfferId,\n" +
                " expectedSalary, availabilityDate, phone, email, experienceYears, portfolioUrl) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = connection.prepareStatement(sql);

        ps.setDate(1, Date.valueOf(app.getApplicationDate()));
        ps.setString(2, app.getCoverLetter());
        ps.setString(3, app.getResumePath());
        ps.setInt(4, app.getuser_id());
        ps.setInt(5, app.getJobOfferId());
        ps.setDouble(6, app.getExpectedSalary());
        ps.setDate(7, Date.valueOf(app.getAvailabilityDate()));
        ps.setString(8, app.getPhone());
        ps.setString(9, app.getEmail());
        ps.setInt(10, app.getExperienceYears());
        ps.setString(11, app.getPortfolioUrl());

        ps.executeUpdate();
    }

    /**
     * Returns the recruiter's email for a given job offer.
     * JobOffer.user_id → User.user_id → User.email
     */
    public String getRecruiterEmailByJob(int jobOfferId) throws SQLException {
        String sql = "SELECT u.email " +
                "FROM users u " +
                "JOIN joboffer j ON j.user_id = u.user_id " +
                "WHERE j.jobOfferId = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, jobOfferId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getString("email");
        return null;
    }
    public boolean hasAlreadyApplied(int userId, int jobOfferId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM application WHERE user_id = ? AND jobOfferId = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, userId);
        ps.setInt(2, jobOfferId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getInt(1) > 0;
        return false;
    }
    /**
     * Returns the job title for a given job offer.
     */
    public String getJobTitle(int jobOfferId) throws SQLException {
        String sql = "SELECT title FROM joboffer WHERE jobOfferId = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, jobOfferId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getString("title");
        return "Unknown Position";
    }



    public List<Application> getByJob(int jobId) throws SQLException {

        List<Application> list = new ArrayList<>();

        String sql = "SELECT * FROM Application WHERE jobOfferId = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, jobId);

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            Application app = new Application(
                    rs.getInt("applicationId"),
                    rs.getDate("applicationDate").toLocalDate(),
                    rs.getString("coverLetter"),
                    rs.getString("currentStatus"),
                    rs.getString("resumePath"),
                    rs.getTimestamp("lastUpdateDate").toLocalDateTime(),
                    rs.getInt("user_id"),
                    rs.getInt("jobOfferId"),
                    rs.getDouble("expectedSalary"),
                    rs.getDate("availabilityDate").toLocalDate(),
                    rs.getString("phone"),
                    rs.getString("email"),
                    rs.getInt("experienceYears"),
                    rs.getString("portfolioUrl"),
                    (Double) rs.getObject("score"),
                    rs.getString("reviewNote")
            );

            list.add(app);
        }

        return list;
    }

    // ================= UPDATE (Recruiter) =================
    public void update(Application app) throws SQLException {

        String sql = "UPDATE Application SET " +
                "applicationDate=?, coverLetter=?, resumePath=?, expectedSalary=?, availabilityDate=?, phone=?, email=?, experienceYears=?, portfolioUrl=?, score=?, reviewNote=?, currentStatus=? " +
                "WHERE applicationId=?";

        PreparedStatement ps = connection.prepareStatement(sql);

        ps.setDate(1, Date.valueOf(app.getApplicationDate()));
        ps.setString(2, app.getCoverLetter());
        ps.setString(3, app.getResumePath());
        ps.setDouble(4, app.getExpectedSalary());
        ps.setDate(5, Date.valueOf(app.getAvailabilityDate()));
        ps.setString(6, app.getPhone());
        ps.setString(7, app.getEmail());
        ps.setInt(8, app.getExperienceYears());
        ps.setString(9, app.getPortfolioUrl());
        ps.setObject(10, app.getScore());
        ps.setString(11, app.getReviewNote());
        ps.setString(12, app.getCurrentStatus());
        ps.setInt(13, app.getApplicationId());

        ps.executeUpdate();
    }

    // ================= DELETE =================
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM Application WHERE applicationId=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    // ================= GET ALL =================
    public List<Application> getAll() throws SQLException {

        List<Application> list = new ArrayList<>();
        String sql = "SELECT * FROM Application";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            Application app = new Application(
                    rs.getInt("applicationId"),
                    rs.getDate("applicationDate").toLocalDate(),
                    rs.getString("coverLetter"),
                    rs.getString("currentStatus"),
                    rs.getString("resumePath"),
                    rs.getTimestamp("lastUpdateDate").toLocalDateTime(),
                    rs.getInt("user_id"),
                    rs.getInt("jobOfferId"),
                    rs.getDouble("expectedSalary"),
                    rs.getDate("availabilityDate").toLocalDate(),
                    rs.getString("phone"),
                    rs.getString("email"),
                    rs.getInt("experienceYears"),
                    rs.getString("portfolioUrl"),
                    (Double) rs.getObject("score"),
                    rs.getString("reviewNote")
            );

            list.add(app);
        }
        return list;
    }
}