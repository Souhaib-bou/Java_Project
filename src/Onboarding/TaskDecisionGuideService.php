<?php

namespace App\Onboarding;

use App\Entity\Onboardingtask;

final class TaskDecisionGuideService
{
    /**
     * @param Onboardingtask[] $tasks
     * @return TaskRecommendation[]
     */
    public function buildRecommendations(int $roleId, array $tasks): array
    {
        $recommendations = [];

        if ([] === $tasks) {
            return [
                new TaskRecommendation(
                    TaskRecommendation::PRIORITY_LOW,
                    'no_tasks',
                    'No tasks found for this plan yet.',
                    'The current plan has no task records loaded.',
                    null,
                    'Refresh',
                    TaskRecommendation::ACTION_REFRESH,
                    5
                ),
            ];
        }

        $blockedCount = 0;
        $inProgressCount = 0;
        $completedCount = 0;
        $notStartedCount = 0;
        $onHoldCount = 0;
        $completedMissingAttachmentCount = 0;

        $firstBlocked = null;
        $firstInProgress = null;
        $firstNotStarted = null;
        $firstOnHold = null;
        $firstCompletedMissingAttachment = null;

        foreach ($tasks as $task) {
            $status = $task->getStatus() ?? '';

            switch ($status) {
                case Onboardingtask::STATUS_BLOCKED:
                    ++$blockedCount;
                    $firstBlocked ??= $task;
                    break;

                case Onboardingtask::STATUS_IN_PROGRESS:
                    ++$inProgressCount;
                    $firstInProgress ??= $task;
                    break;

                case Onboardingtask::STATUS_COMPLETED:
                    ++$completedCount;
                    if (!$task->hasAttachment()) {
                        ++$completedMissingAttachmentCount;
                        $firstCompletedMissingAttachment ??= $task;
                    }
                    break;

                case Onboardingtask::STATUS_NOT_STARTED:
                    ++$notStartedCount;
                    $firstNotStarted ??= $task;
                    break;

                case Onboardingtask::STATUS_ON_HOLD:
                    ++$onHoldCount;
                    $firstOnHold ??= $task;
                    break;
            }
        }

        if (ViewerContext::ROLE_CANDIDATE === $roleId) {
            if ($firstBlocked) {
                $recommendations[] = new TaskRecommendation(
                    TaskRecommendation::PRIORITY_HIGH,
                    'review_blocked_task',
                    'A blocked task needs your attention: Task #' . $firstBlocked->getTaskId(),
                    'Blocked tasks prevent progress and should be reviewed first.',
                    $firstBlocked->getTaskId(),
                    'Open Update',
                    TaskRecommendation::ACTION_OPEN_UPDATE,
                    100
                );
            }

            if ($firstCompletedMissingAttachment) {
                $recommendations[] = new TaskRecommendation(
                    TaskRecommendation::PRIORITY_HIGH,
                    'upload_missing_attachment',
                    'Completed task is missing attachment: Task #' . $firstCompletedMissingAttachment->getTaskId(),
                    'Completed work should include its supporting file or proof link.',
                    $firstCompletedMissingAttachment->getTaskId(),
                    'Add Attachment',
                    TaskRecommendation::ACTION_OPEN_UPDATE,
                    95
                );
            }

            if ($firstInProgress) {
                $recommendations[] = new TaskRecommendation(
                    TaskRecommendation::PRIORITY_MEDIUM,
                    'continue_task',
                    'Continue your in-progress task: Task #' . $firstInProgress->getTaskId(),
                    'You already started this task, so continuing it is usually the fastest progress.',
                    $firstInProgress->getTaskId(),
                    'Open Task',
                    TaskRecommendation::ACTION_SELECT_TASK,
                    75
                );
            }

            if ($firstNotStarted) {
                $recommendations[] = new TaskRecommendation(
                    TaskRecommendation::PRIORITY_MEDIUM,
                    'start_next_task',
                    'Start your next task: Task #' . $firstNotStarted->getTaskId(),
                    'No blocker is currently visible on this not-started task.',
                    $firstNotStarted->getTaskId(),
                    'Open Task',
                    TaskRecommendation::ACTION_SELECT_TASK,
                    70
                );
            }
        } elseif (ViewerContext::ROLE_RECRUITER === $roleId) {
            if ($firstBlocked) {
                $recommendations[] = new TaskRecommendation(
                    TaskRecommendation::PRIORITY_HIGH,
                    'review_blocked_task',
                    'Review blocked task: Task #' . $firstBlocked->getTaskId(),
                    'A blocked task may require recruiter clarification or follow-up.',
                    $firstBlocked->getTaskId(),
                    'Open Task',
                    TaskRecommendation::ACTION_SELECT_TASK,
                    100
                );
            }

            if ($notStartedCount >= 2 && $firstNotStarted) {
                $recommendations[] = new TaskRecommendation(
                    TaskRecommendation::PRIORITY_HIGH,
                    'follow_up_progress',
                    'Multiple tasks are still not started (' . $notStartedCount . '). Follow up with the candidate.',
                    'Several tasks remain untouched, which may indicate onboarding delay.',
                    $firstNotStarted->getTaskId(),
                    'Open Task',
                    TaskRecommendation::ACTION_SELECT_TASK,
                    90
                );
            }

            if ($firstCompletedMissingAttachment) {
                $recommendations[] = new TaskRecommendation(
                    TaskRecommendation::PRIORITY_MEDIUM,
                    'request_attachment',
                    'Completed task missing file: Task #' . $firstCompletedMissingAttachment->getTaskId(),
                    'Recruiter review is easier when completed tasks include proof or an attachment.',
                    $firstCompletedMissingAttachment->getTaskId(),
                    'Open Update',
                    TaskRecommendation::ACTION_OPEN_UPDATE,
                    70
                );
            }
        } else {
            if ($blockedCount > 1 && $firstBlocked) {
                $recommendations[] = new TaskRecommendation(
                    TaskRecommendation::PRIORITY_HIGH,
                    'plan_needs_intervention',
                    'Multiple blocked tasks detected (' . $blockedCount . '). This plan may need intervention.',
                    'Multiple blockers can indicate a systemic issue or coordination problem.',
                    $firstBlocked->getTaskId(),
                    'Open Task',
                    TaskRecommendation::ACTION_SELECT_TASK,
                    100
                );
            } elseif ($firstBlocked) {
                $recommendations[] = new TaskRecommendation(
                    TaskRecommendation::PRIORITY_HIGH,
                    'review_blocked_task',
                    'Blocked task detected: Task #' . $firstBlocked->getTaskId(),
                    'A blocker is visible and should be reviewed before it spreads.',
                    $firstBlocked->getTaskId(),
                    'Open Task',
                    TaskRecommendation::ACTION_SELECT_TASK,
                    92
                );
            }

            if ($firstCompletedMissingAttachment) {
                $recommendations[] = new TaskRecommendation(
                    TaskRecommendation::PRIORITY_MEDIUM,
                    'missing_completion_artifact',
                    'Completed task missing attachment: Task #' . $firstCompletedMissingAttachment->getTaskId(),
                    'Completion checks are stronger when the proof link or file is present.',
                    $firstCompletedMissingAttachment->getTaskId(),
                    'Open Update',
                    TaskRecommendation::ACTION_OPEN_UPDATE,
                    72
                );
            }

            if ($notStartedCount === \count($tasks) && $firstNotStarted) {
                $recommendations[] = new TaskRecommendation(
                    TaskRecommendation::PRIORITY_MEDIUM,
                    'no_progress',
                    'All tasks are still not started. Review execution progress.',
                    'No tasks have started yet across the loaded task list.',
                    $firstNotStarted->getTaskId(),
                    'Open Task',
                    TaskRecommendation::ACTION_SELECT_TASK,
                    68
                );
            }
        }

        if ($onHoldCount > 0 && $firstOnHold) {
            $recommendations[] = new TaskRecommendation(
                TaskRecommendation::PRIORITY_LOW,
                'on_hold_follow_up',
                'There are ' . $onHoldCount . ' on-hold task(s). Review their blockers.',
                'On-hold tasks often need dependency resolution or clarification.',
                $firstOnHold->getTaskId(),
                'Review',
                TaskRecommendation::ACTION_SELECT_TASK,
                35
            );
        }

        if ([] === $recommendations) {
            $recommendations[] = new TaskRecommendation(
                TaskRecommendation::PRIORITY_LOW,
                'no_action_needed',
                'No urgent action detected. Tasks look healthy.',
                'No high-risk pattern was detected from the current status distribution.',
                null,
                'Refresh',
                TaskRecommendation::ACTION_REFRESH,
                10
            );
        }

        usort($recommendations, static fn (TaskRecommendation $left, TaskRecommendation $right): int => $right->getScore() <=> $left->getScore());

        return $recommendations;
    }
}
