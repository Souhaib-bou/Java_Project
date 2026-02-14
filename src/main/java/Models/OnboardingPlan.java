    package Models;

    import java.util.Date;

    public class OnboardingPlan {

        private int planId;
        private int userId;
        private String status;
        private Date deadline;

        public OnboardingPlan() {}

        public OnboardingPlan(int planId, int userId, String status, Date deadline) {
            this.planId = planId;
            this.userId = userId;
            this.status = status;
            this.deadline = deadline;
        }

        public int getPlanId() {
            return planId;
        }

        public void setPlanId(int planId) {
            this.planId = planId;
        }

        public int getUserId() {
            return userId;
        }

        public void setUserId(int userId) {
            this.userId = userId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Date getDeadline() {
            return deadline;
        }

        public void setDeadline(Date deadline) {
            this.deadline = deadline;
        }

        @Override
        public String toString() {
            return "OnboardingPlan{" +
                    "planId=" + planId +
                    ", userId=" + userId +
                    ", status='" + status + '\'' +
                    ", deadline=" + deadline +
                    '}';
        }
    }
