package Models;

import java.sql.Date;

public class JobOffer {

    private int jobOfferId;
    private String title;
    private String description;
    private String contractType;
    private double salary;
    private String location;
    private int experienceRequired;
    private Date publicationDate;
    private String status;
    private int user_id;  // new field

    // ======== DEFAULT CONSTRUCTOR ========
    public JobOffer() {
    }

    // ======== PARAMETERIZED CONSTRUCTOR ========
    public JobOffer(String title, String description, String contractType,
                    double salary, String location, int experienceRequired,
                    Date publicationDate, String status, int user_id) {
        this.title = title;
        this.description = description;
        this.contractType = contractType;
        this.salary = salary;
        this.location = location;
        this.experienceRequired = experienceRequired;
        this.publicationDate = publicationDate;
        this.status = status;
        this.user_id = user_id;
    }

    // ======== GETTERS & SETTERS ========
    public int getJobOfferId() {
        return jobOfferId;
    }

    public void setJobOfferId(int jobOfferId) {
        this.jobOfferId = jobOfferId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContractType() {
        return contractType;
    }

    public void setContractType(String contractType) {
        this.contractType = contractType;
    }

    public double getSalary() {
        return salary;
    }

    public void setSalary(double salary) {
        this.salary = salary;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getExperienceRequired() {
        return experienceRequired;
    }

    public void setExperienceRequired(int experienceRequired) {
        this.experienceRequired = experienceRequired;
    }

    public Date getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(Date publicationDate) {
        this.publicationDate = publicationDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }
}