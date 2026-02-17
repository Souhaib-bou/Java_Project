package Services;

import Utils.MyDB;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ApplicationService {

    private Connection connection;

    /**
     * Creates a new ApplicationService instance.
     */
    public ApplicationService() {
        connection = MyDB.getInstance().getConnection();
    }

    // ✅ ADD APPLICATION (Candidate applies)
    /**
     * Creates a new record and updates the UI.
     */
    public void add(Application app) throws SQLException {

        String sql = "INSERT INTO Application " +
                "(applicationDate, coverLetter, resumePath, user_id, jobOfferId) " +
                "VALUES (?, ?, ?, ?, ?)";

        PreparedStatement ps = connection.prepareStatement(sql);

        ps.setDate(1, Date.valueOf(app.getApplicationDate()));
        ps.setString(2, app.getCoverLetter());
        ps.setString(3, app.getResumePath());
        ps.setInt(4, app.getuser_id());      // changed
        ps.setInt(5, app.getJobOfferId());

        ps.executeUpdate();
        System.out.println("Application added successfully!");
    }

    // ✅ UPDATE STATUS (Recruiter action)
    /**
     * Updates the selected record and refreshes the UI.
     */
    public void updateStatus(int applicationId, String status) throws SQLException {

        String sql = "UPDATE Application SET currentStatus = ? WHERE applicationId = ?";

        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, status);
        ps.setInt(2, applicationId);

        ps.executeUpdate();
        System.out.println("Application status updated!");
    }

    // ✅ DELETE APPLICATION
    /**
     * Deletes the selected record and refreshes the UI.
     */
    public void delete(int applicationId) throws SQLException {

        String sql = "DELETE FROM Application WHERE applicationId = ?";

        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, applicationId);

        ps.executeUpdate();
        System.out.println("Application deleted!");
    }

    // ✅ GET ALL APPLICATIONS
    /**
     * Returns the all value.
     */
    public List<Application> getAll() throws SQLException {

        List<Application> applications = new ArrayList<>();

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
                    rs.getInt("user_id"),       // changed
                    rs.getInt("jobOfferId")
            );

            applications.add(app);
        }

        return applications;
    }
    /**
     * Updates the selected record and refreshes the UI.
     */
    public void update(Application app) throws SQLException {
        String sql = "UPDATE Application SET applicationDate=?, coverLetter=?, resumePath=?, user_id=?, jobOfferId=? WHERE applicationId=?";
        PreparedStatement stmt = connection.prepareStatement(sql);

        stmt.setDate(1, Date.valueOf(app.getApplicationDate()));
        stmt.setString(2, app.getCoverLetter());
        stmt.setString(3, app.getResumePath());
        stmt.setInt(4, app.getuser_id());
        stmt.setInt(5, app.getJobOfferId());
        stmt.setInt(6, app.getApplicationId());

        stmt.executeUpdate();
    }


    // ✅ GET APPLICATIONS BY USER (Candidate view)
    /**
     * Returns the byuser value.
     */
    public List<Application> getByUser(int user_id) throws SQLException {

        List<Application> applications = new ArrayList<>();

        String sql = "SELECT * FROM Application WHERE user_id = ?";

        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, user_id);

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {

            Application app = new Application(
                    rs.getInt("applicationId"),
                    rs.getDate("applicationDate").toLocalDate(),
                    rs.getString("coverLetter"),
                    rs.getString("currentStatus"),
                    rs.getString("resumePath"),
                    rs.getTimestamp("lastUpdateDate").toLocalDateTime(),
                    rs.getInt("user_id"),        // changed
                    rs.getInt("jobOfferId")
            );

            applications.add(app);
        }

        return applications;
    }

    // ✅ GET APPLICATIONS BY JOB (Recruiter view)
    /**
     * Returns the byjob value.
     */
    public List<Application> getByJob(int jobId) throws SQLException {

        List<Application> applications = new ArrayList<>();

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
                    rs.getInt("user_id"),        // changed
                    rs.getInt("jobOfferId")
            );

            applications.add(app);
        }

        return applications;
    }
}
