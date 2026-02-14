package Models;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Application {

    private int applicationId;
    private LocalDate applicationDate;
    private String coverLetter;
    private String currentStatus;
    private String resumePath;
    private LocalDateTime lastUpdateDate;
    private int user_id;      // ✅ keep as user_id
    private int jobOfferId;

    public Application() {}

    // Constructor for adding new application (without ID / status / lastUpdate)
    public Application(LocalDate applicationDate, String coverLetter,
                       String resumePath, int user_id, int jobOfferId) {
        this.applicationDate = applicationDate;
        this.coverLetter = coverLetter;
        this.resumePath = resumePath;
        this.user_id = user_id;    // ✅ consistent
        this.jobOfferId = jobOfferId;
    }

    // Constructor for reading from DB
    public Application(int applicationId, LocalDate applicationDate,
                       String coverLetter, String currentStatus,
                       String resumePath, LocalDateTime lastUpdateDate,
                       int user_id, int jobOfferId) {
        this.applicationId = applicationId;
        this.applicationDate = applicationDate;
        this.coverLetter = coverLetter;
        this.currentStatus = currentStatus;
        this.resumePath = resumePath;
        this.lastUpdateDate = lastUpdateDate;
        this.user_id = user_id;    // ✅ consistent
        this.jobOfferId = jobOfferId;
    }

    // Getters & Setters

    public int getApplicationId() { return applicationId; }
    public void setApplicationId(int applicationId) { this.applicationId = applicationId; }

    public LocalDate getApplicationDate() { return applicationDate; }
    public void setApplicationDate(LocalDate applicationDate) { this.applicationDate = applicationDate; }

    public String getCoverLetter() { return coverLetter; }
    public void setCoverLetter(String coverLetter) { this.coverLetter = coverLetter; }

    public String getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(String currentStatus) { this.currentStatus = currentStatus; }

    public String getResumePath() { return resumePath; }
    public void setResumePath(String resumePath) { this.resumePath = resumePath; }

    public LocalDateTime getLastUpdateDate() { return lastUpdateDate; }
    public void setLastUpdateDate(LocalDateTime lastUpdateDate) { this.lastUpdateDate = lastUpdateDate; }

    public int getuser_id() { return user_id; }         // ✅ updated
    public void setUser_id(int user_id) { this.user_id = user_id; } // ✅ updated

    public int getJobOfferId() { return jobOfferId; }
    public void setJobOfferId(int jobOfferId) { this.jobOfferId = jobOfferId; }
}