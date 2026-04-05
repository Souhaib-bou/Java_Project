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
    private int user_id;
    private int jobOfferId;

    // New realistic fields
    private double expectedSalary;
    private LocalDate availabilityDate;
    private String phone;
    private String email;
    private int experienceYears;
    private String portfolioUrl;
    private Double score;      // recruiter evaluation (nullable)
    private String reviewNote; // recruiter note

    public Application() {}

    // Constructor for adding new application (candidate)
    public Application(LocalDate applicationDate, String coverLetter, String resumePath,
                       int user_id, int jobOfferId,
                       double expectedSalary, LocalDate availabilityDate,
                       String phone, String email, int experienceYears, String portfolioUrl) {

        this.applicationDate = applicationDate;
        this.coverLetter = coverLetter;
        this.resumePath = resumePath;
        this.user_id = user_id;
        this.jobOfferId = jobOfferId;
        this.expectedSalary = expectedSalary;
        this.availabilityDate = availabilityDate;
        this.phone = phone;
        this.email = email;
        this.experienceYears = experienceYears;
        this.portfolioUrl = portfolioUrl;
    }

    // Constructor for reading from DB
    public Application(int applicationId, LocalDate applicationDate,
                       String coverLetter, String currentStatus,
                       String resumePath, LocalDateTime lastUpdateDate,
                       int user_id, int jobOfferId,
                       double expectedSalary, LocalDate availabilityDate,
                       String phone, String email, int experienceYears,
                       String portfolioUrl, Double score, String reviewNote) {

        this.applicationId = applicationId;
        this.applicationDate = applicationDate;
        this.coverLetter = coverLetter;
        this.currentStatus = currentStatus;
        this.resumePath = resumePath;
        this.lastUpdateDate = lastUpdateDate;
        this.user_id = user_id;
        this.jobOfferId = jobOfferId;
        this.expectedSalary = expectedSalary;
        this.availabilityDate = availabilityDate;
        this.phone = phone;
        this.email = email;
        this.experienceYears = experienceYears;
        this.portfolioUrl = portfolioUrl;
        this.score = score;
        this.reviewNote = reviewNote;
    }

    // ===== Getters & Setters =====

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

    public int getuser_id() { return user_id; }
    public void setUser_id(int user_id) { this.user_id = user_id; }

    public int getJobOfferId() { return jobOfferId; }
    public void setJobOfferId(int jobOfferId) { this.jobOfferId = jobOfferId; }

    public double getExpectedSalary() { return expectedSalary; }
    public void setExpectedSalary(double expectedSalary) { this.expectedSalary = expectedSalary; }

    public LocalDate getAvailabilityDate() { return availabilityDate; }
    public void setAvailabilityDate(LocalDate availabilityDate) { this.availabilityDate = availabilityDate; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public int getExperienceYears() { return experienceYears; }
    public void setExperienceYears(int experienceYears) { this.experienceYears = experienceYears; }

    public String getPortfolioUrl() { return portfolioUrl; }
    public void setPortfolioUrl(String portfolioUrl) { this.portfolioUrl = portfolioUrl; }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }

    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }
}