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
    /**
     * Creates a new JobOffer instance.
     */
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
    public JobOffer(int jobOfferId, String title, String description, String contractType,
                    double salary, String location, int experienceRequired,
                    Date publicationDate, String status, int user_id) {

        this.jobOfferId = jobOfferId;
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
    /**
     * Returns the jobofferid value.
     */
    public int getJobOfferId() {
        return jobOfferId;
    }

    /**
     * Sets the jobofferid value.
     */
    public void setJobOfferId(int jobOfferId) {
        this.jobOfferId = jobOfferId;
    }

    /**
     * Returns the title value.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title value.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Returns the description value.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description value.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the contracttype value.
     */
    public String getContractType() {
        return contractType;
    }

    /**
     * Sets the contracttype value.
     */
    public void setContractType(String contractType) {
        this.contractType = contractType;
    }

    /**
     * Returns the salary value.
     */
    public double getSalary() {
        return salary;
    }

    /**
     * Sets the salary value.
     */
    public void setSalary(double salary) {
        this.salary = salary;
    }

    /**
     * Returns the location value.
     */
    public String getLocation() {
        return location;
    }

    /**
     * Sets the location value.
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Returns the experiencerequired value.
     */
    public int getExperienceRequired() {
        return experienceRequired;
    }

    /**
     * Sets the experiencerequired value.
     */
    public void setExperienceRequired(int experienceRequired) {
        this.experienceRequired = experienceRequired;
    }

    /**
     * Returns the publicationdate value.
     */
    public Date getPublicationDate() {
        return publicationDate;
    }

    /**
     * Sets the publicationdate value.
     */
    public void setPublicationDate(Date publicationDate) {
        this.publicationDate = publicationDate;
    }

    /**
     * Returns the status value.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the status value.
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Returns the user_id value.
     */
    public int getUser_id() {
        return user_id;
    }

    /**
     * Sets the user_id value.
     */
    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }
}
