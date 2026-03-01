package Services;

import Models.OnboardingTask;
import Models.TaskRecommendation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class TaskDecisionGuideService {

    public List<TaskRecommendation> buildRecommendations(int roleId, List<OnboardingTask> tasks) {
        List<TaskRecommendation> recs = new ArrayList<>();

        if (tasks == null || tasks.isEmpty()) {
            recs.add(new TaskRecommendation(
                    TaskRecommendation.Priority.LOW,
                    "NO_TASKS",
                    "No tasks found for this plan yet.",
                    "The current plan has no task records loaded.",
                    null,
                    "Refresh",
                    TaskRecommendation.ActionType.REFRESH,
                    5
            ));
            return recs;
        }

        int blockedCount = 0;
        int inProgressCount = 0;
        int completedCount = 0;
        int notStartedCount = 0;
        int onHoldCount = 0;
        int completedMissingAttachmentCount = 0;

        OnboardingTask firstBlocked = null;
        OnboardingTask firstInProgress = null;
        OnboardingTask firstNotStarted = null;
        OnboardingTask firstOnHold = null;
        OnboardingTask firstCompletedMissingAttachment = null;

        for (OnboardingTask t : tasks) {
            String s = norm(t.getStatus());

            switch (s) {
                case "blocked" -> {
                    blockedCount++;
                    if (firstBlocked == null) firstBlocked = t;
                }
                case "in progress" -> {
                    inProgressCount++;
                    if (firstInProgress == null) firstInProgress = t;
                }
                case "completed" -> {
                    completedCount++;
                    if (!hasAttachment(t)) {
                        completedMissingAttachmentCount++;
                        if (firstCompletedMissingAttachment == null) firstCompletedMissingAttachment = t;
                    }
                }
                case "not started" -> {
                    notStartedCount++;
                    if (firstNotStarted == null) firstNotStarted = t;
                }
                case "on hold" -> {
                    onHoldCount++;
                    if (firstOnHold == null) firstOnHold = t;
                }
                default -> { }
            }
        }

        if (roleId == 1) {
            // Candidate
            if (firstBlocked != null) {
                recs.add(new TaskRecommendation(
                        TaskRecommendation.Priority.HIGH,
                        "REVIEW_BLOCKED_TASK",
                        "A blocked task needs your attention: Task #" + firstBlocked.getTaskId(),
                        "Blocked tasks prevent progress and should be reviewed first.",
                        firstBlocked.getTaskId(),
                        "Open Update",
                        TaskRecommendation.ActionType.OPEN_UPDATE,
                        100
                ));
            }

            if (firstCompletedMissingAttachment != null) {
                recs.add(new TaskRecommendation(
                        TaskRecommendation.Priority.HIGH,
                        "UPLOAD_MISSING_ATTACHMENT",
                        "Completed task is missing attachment: Task #" + firstCompletedMissingAttachment.getTaskId(),
                        "Completed work should include its supporting file/path for validation.",
                        firstCompletedMissingAttachment.getTaskId(),
                        "Open Update",
                        TaskRecommendation.ActionType.OPEN_UPDATE,
                        95
                ));
            }

            if (firstInProgress != null) {
                recs.add(new TaskRecommendation(
                        TaskRecommendation.Priority.MEDIUM,
                        "CONTINUE_TASK",
                        "Continue your in-progress task: Task #" + firstInProgress.getTaskId(),
                        "You already started this task, so continuing it is usually the fastest progress.",
                        firstInProgress.getTaskId(),
                        "Open Task",
                        TaskRecommendation.ActionType.SELECT_TASK,
                        75
                ));
            }

            if (firstNotStarted != null) {
                recs.add(new TaskRecommendation(
                        TaskRecommendation.Priority.MEDIUM,
                        "START_NEXT_TASK",
                        "Start your next task: Task #" + firstNotStarted.getTaskId(),
                        "No blocker detected on this not-started task.",
                        firstNotStarted.getTaskId(),
                        "Open Task",
                        TaskRecommendation.ActionType.SELECT_TASK,
                        70
                ));
            }

            if (firstOnHold != null) {
                recs.add(new TaskRecommendation(
                        TaskRecommendation.Priority.LOW,
                        "RESUME_ON_HOLD",
                        "You have a task on hold: Task #" + firstOnHold.getTaskId(),
                        "On-hold tasks may need a follow-up once dependencies are resolved.",
                        firstOnHold.getTaskId(),
                        "Review",
                        TaskRecommendation.ActionType.SELECT_TASK,
                        40
                ));
            }

        } else if (roleId == 2) {
            // Recruiter
            if (firstBlocked != null) {
                recs.add(new TaskRecommendation(
                        TaskRecommendation.Priority.HIGH,
                        "REVIEW_BLOCKED_TASK",
                        "Review blocked task: Task #" + firstBlocked.getTaskId(),
                        "A blocked task may require recruiter clarification or follow-up.",
                        firstBlocked.getTaskId(),
                        "Open Task",
                        TaskRecommendation.ActionType.SELECT_TASK,
                        100
                ));
            }

            if (notStartedCount >= 2 && firstNotStarted != null) {
                recs.add(new TaskRecommendation(
                        TaskRecommendation.Priority.HIGH,
                        "FOLLOW_UP_PROGRESS",
                        "Multiple tasks are still not started (" + notStartedCount + "). Follow up with candidate.",
                        "Several tasks remain untouched, which may indicate onboarding delay.",
                        firstNotStarted.getTaskId(),
                        "Open Task",
                        TaskRecommendation.ActionType.SELECT_TASK,
                        90
                ));
            }

            if (firstCompletedMissingAttachment != null) {
                recs.add(new TaskRecommendation(
                        TaskRecommendation.Priority.MEDIUM,
                        "REQUEST_ATTACHMENT",
                        "Completed task missing file: Task #" + firstCompletedMissingAttachment.getTaskId(),
                        "Recruiter review is easier when completed tasks include proof/attachment.",
                        firstCompletedMissingAttachment.getTaskId(),
                        "Open Update",
                        TaskRecommendation.ActionType.OPEN_UPDATE,
                        70
                ));
            }

            if (completedCount == 0 && inProgressCount == 0) {
                recs.add(new TaskRecommendation(
                        TaskRecommendation.Priority.MEDIUM,
                        "KICKOFF_ACTIVITY",
                        "No progress yet in this plan. Consider initiating task work.",
                        "No task is in progress or completed at the moment.",
                        null,
                        "Review Tasks",
                        TaskRecommendation.ActionType.REFRESH,
                        65
                ));
            }

            if (onHoldCount > 0 && firstOnHold != null) {
                recs.add(new TaskRecommendation(
                        TaskRecommendation.Priority.LOW,
                        "CHECK_ON_HOLD",
                        "There are " + onHoldCount + " task(s) on hold. Check blockers.",
                        "On-hold tasks often need dependency resolution or clarification.",
                        firstOnHold.getTaskId(),
                        "Open Task",
                        TaskRecommendation.ActionType.SELECT_TASK,
                        35
                ));
            }

        } else {
            // Admin
            if (blockedCount > 1 && firstBlocked != null) {
                recs.add(new TaskRecommendation(
                        TaskRecommendation.Priority.HIGH,
                        "PLAN_NEEDS_INTERVENTION",
                        "Multiple blocked tasks detected (" + blockedCount + "). Plan may need intervention.",
                        "Multiple blockers can indicate systemic issue or coordination problem.",
                        firstBlocked.getTaskId(),
                        "Open Task",
                        TaskRecommendation.ActionType.SELECT_TASK,
                        100
                ));
            } else if (firstBlocked != null) {
                recs.add(new TaskRecommendation(
                        TaskRecommendation.Priority.HIGH,
                        "REVIEW_BLOCKED_TASK",
                        "Blocked task detected: Task #" + firstBlocked.getTaskId(),
                        "Single blocker detected and should be reviewed before it spreads.",
                        firstBlocked.getTaskId(),
                        "Open Task",
                        TaskRecommendation.ActionType.SELECT_TASK,
                        92
                ));
            }

            if (firstCompletedMissingAttachment != null) {
                recs.add(new TaskRecommendation(
                        TaskRecommendation.Priority.MEDIUM,
                        "MISSING_COMPLETION_ARTIFACT",
                        "Completed task missing attachment: Task #" + firstCompletedMissingAttachment.getTaskId(),
                        "Completion quality checks are stronger when attachments are present.",
                        firstCompletedMissingAttachment.getTaskId(),
                        "Open Update",
                        TaskRecommendation.ActionType.OPEN_UPDATE,
                        72
                ));
            }

            if (notStartedCount == tasks.size() && firstNotStarted != null) {
                recs.add(new TaskRecommendation(
                        TaskRecommendation.Priority.MEDIUM,
                        "NO_PROGRESS",
                        "All tasks are still not started. Review execution progress.",
                        "No tasks have started yet across the loaded task list.",
                        firstNotStarted.getTaskId(),
                        "Open Task",
                        TaskRecommendation.ActionType.SELECT_TASK,
                        68
                ));
            }

            if (onHoldCount > 0 && firstOnHold != null) {
                recs.add(new TaskRecommendation(
                        TaskRecommendation.Priority.LOW,
                        "ON_HOLD_OVERSIGHT",
                        "There are " + onHoldCount + " on-hold task(s). Monitor risk.",
                        "On-hold tasks can impact delivery timelines if they stay idle too long.",
                        firstOnHold.getTaskId(),
                        "Open Task",
                        TaskRecommendation.ActionType.SELECT_TASK,
                        30
                ));
            }
        }

        if (recs.isEmpty()) {
            recs.add(new TaskRecommendation(
                    TaskRecommendation.Priority.LOW,
                    "NO_ACTION_NEEDED",
                    "No urgent action detected. Tasks look healthy.",
                    "No high-risk pattern detected from current status distribution.",
                    null,
                    "Refresh",
                    TaskRecommendation.ActionType.REFRESH,
                    10
            ));
        }

        recs.sort(Comparator.comparingInt(TaskRecommendation::getScore).reversed());
        return recs;
    }

    private boolean hasAttachment(OnboardingTask task) {
        return task != null && task.getFilepath() != null && !task.getFilepath().trim().isEmpty();
    }

    private String norm(String status) {
        if (status == null) return "";
        String s = status.trim().toLowerCase(Locale.ROOT);
        if (s.equals("in_progress")) return "in progress";
        if (s.equals("not_started")) return "not started";
        if (s.equals("on_hold")) return "on hold";
        return s;
    }
}