<<<<<<< HEAD
package Models;
=======
    package Models;
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67

import java.util.Date;

    public class OnboardingPlan {

        private int planId;
        private int userId;
        private String status;
        private Date deadline;

        public OnboardingPlan() {}

        /**
         * Creates a new OnboardingPlan instance.
         */
        public OnboardingPlan(int planId, int userId, String status, Date deadline) {
            this.planId = planId;
            this.userId = userId;
            this.status = status;
            this.deadline = deadline;
        }

        /**
         * Returns the planid value.
         */
        public int getPlanId() {
            return planId;
        }

        /**
         * Sets the planid value.
         */
        public void setPlanId(int planId) {
            this.planId = planId;
        }

        /**
         * Returns the userid value.
         */
        public int getUserId() {
            return userId;
        }

        /**
         * Sets the userid value.
         */
        public void setUserId(int userId) {
            this.userId = userId;
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
         * Returns the deadline value.
         */
        public Date getDeadline() {
            return deadline;
        }

        /**
         * Sets the deadline value.
         */
        public void setDeadline(Date deadline) {
            this.deadline = deadline;
        }

        @Override
        /**
         * Executes this operation.
         */
        public String toString() {
            return "OnboardingPlan{" +
                    "planId=" + planId +
                    ", userId=" + userId +
                    ", status='" + status + '\'' +
                    ", deadline=" + deadline +
                    '}';
        }
    }
