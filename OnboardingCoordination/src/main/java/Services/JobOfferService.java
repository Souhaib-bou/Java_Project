package Services;

import Models.JobOffer;
import Utils.MyDB;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JobOfferService {

    private Connection cnx = MyDB.getInstance().getConnection();

    // ================= CREATE =================
    /**
     * Creates a new record and updates the UI.
     */
    public void add(JobOffer j) throws SQLException {
        String sql = "INSERT INTO JobOffer (title, description, contractType, salary, location, experienceRequired, publicationDate, status, user_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql);

        ps.setString(1, j.getTitle());
        ps.setString(2, j.getDescription());
        ps.setString(3, j.getContractType());
        ps.setDouble(4, j.getSalary());
        ps.setString(5, j.getLocation());
        ps.setInt(6, j.getExperienceRequired());
        ps.setDate(7, j.getPublicationDate());
        ps.setString(8, j.getStatus());
        ps.setInt(9, j.getUser_id()); // new field

        ps.executeUpdate();
    }

    // ================= READ =================
    /**
     * Returns the all value.
     */
    public List<JobOffer> getAll() throws SQLException {
        List<JobOffer> list = new ArrayList<>();
        String sql = "SELECT * FROM JobOffer";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            JobOffer j = new JobOffer();
            j.setJobOfferId(rs.getInt("jobOfferId"));
            j.setTitle(rs.getString("title"));
            j.setDescription(rs.getString("description"));
            j.setContractType(rs.getString("contractType"));
            j.setSalary(rs.getDouble("salary"));
            j.setLocation(rs.getString("location"));
            j.setExperienceRequired(rs.getInt("experienceRequired"));
            j.setPublicationDate(rs.getDate("publicationDate"));
            j.setStatus(rs.getString("status"));
            j.setUser_id(rs.getInt("user_id")); // new field
            list.add(j);
        }
        return list;
    }

    // ================= UPDATE =================
    /**
     * Updates the selected record and refreshes the UI.
     */
    public void update(JobOffer j) throws SQLException {
        String sql = "UPDATE JobOffer SET title=?, description=?, contractType=?, salary=?, location=?, experienceRequired=?, publicationDate=?, status=?, user_id=? WHERE jobOfferId=?";
        PreparedStatement ps = cnx.prepareStatement(sql);

        ps.setString(1, j.getTitle());
        ps.setString(2, j.getDescription());
        ps.setString(3, j.getContractType());
        ps.setDouble(4, j.getSalary());
        ps.setString(5, j.getLocation());
        ps.setInt(6, j.getExperienceRequired());
        ps.setDate(7, j.getPublicationDate());
        ps.setString(8, j.getStatus());
        ps.setInt(9, j.getUser_id()); // new field
        ps.setInt(10, j.getJobOfferId());

        ps.executeUpdate();
    }

    // ================= DELETE =================
    /**
     * Deletes the selected record and refreshes the UI.
     */
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM JobOffer WHERE jobOfferId=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }
}
