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

        $blockedTasks = [];
        $inProgressTasks = [];
        $notStartedTasks = [];
        $onHoldTasks = [];
        $completedMissingAttachmentTasks = [];

        foreach ($tasks as $task) {
            $status = $task->getStatus() ?? '';

            switch ($status) {
                case Onboardingtask::STATUS_BLOCKED:
                    $blockedTasks[] = $task;
                    break;

                case Onboardingtask::STATUS_IN_PROGRESS:
                    $inProgressTasks[] = $task;
                    break;

                case Onboardingtask::STATUS_COMPLETED:
                    if (!$task->hasAttachment()) {
                        $completedMissingAttachmentTasks[] = $task;
                    }
                    break;

                case Onboardingtask::STATUS_NOT_STARTED:
                    $notStartedTasks[] = $task;
                    break;

                case Onboardingtask::STATUS_ON_HOLD:
                    $onHoldTasks[] = $task;
                    break;
            }
        }

        $blockedCount = \count($blockedTasks);
        $notStartedCount = \count($notStartedTasks);
        $onHoldCount = \count($onHoldTasks);

        if (ViewerContext::ROLE_CANDIDATE === $roleId) {
            foreach ($this->sliceTasks($blockedTasks, 2) as $index => $task) {
                $recommendations[] = $this->buildRecommendation(
                    TaskRecommendation::PRIORITY_HIGH,
                    'review_blocked_task_' . $task->getTaskId(),
                    'A blocked task needs your attention: Task #' . $task->getTaskId(),
                    'Blocked tasks prevent progress and should be reviewed first.',
                    $task,
                    'Open Update',
                    TaskRecommendation::ACTION_OPEN_UPDATE,
                    100 - ($index * 4)
                );
            }

            foreach ($this->sliceTasks($completedMissingAttachmentTasks, 2) as $index => $task) {
                $recommendations[] = $this->buildRecommendation(
                    TaskRecommendation::PRIORITY_HIGH,
                    'upload_missing_attachment_' . $task->getTaskId(),
                    'Completed task is missing proof: Task #' . $task->getTaskId(),
                    'Completed work should include its supporting file or proof link.',
                    $task,
                    'Add Attachment',
                    TaskRecommendation::ACTION_OPEN_UPDATE,
                    95 - ($index * 3)
                );
            }

            foreach ($this->sliceTasks($inProgressTasks, 2) as $index => $task) {
                $recommendations[] = $this->buildRecommendation(
                    TaskRecommendation::PRIORITY_MEDIUM,
                    'continue_task_' . $task->getTaskId(),
                    'Continue your in-progress task: Task #' . $task->getTaskId(),
                    'You already started this task, so continuing it is usually the fastest progress.',
                    $task,
                    'Open Task',
                    TaskRecommendation::ACTION_SELECT_TASK,
                    75 - ($index * 4)
                );
            }

            foreach ($this->sliceTasks($notStartedTasks, 2) as $index => $task) {
                $recommendations[] = $this->buildRecommendation(
                    TaskRecommendation::PRIORITY_MEDIUM,
                    'start_next_task_' . $task->getTaskId(),
                    'Start your next task: Task #' . $task->getTaskId(),
                    'No blocker is currently visible on this not-started task.',
                    $task,
                    'Open Task',
                    TaskRecommendation::ACTION_SELECT_TASK,
                    70 - ($index * 5)
                );
            }
        } elseif (ViewerContext::ROLE_RECRUITER === $roleId) {
            foreach ($this->sliceTasks($blockedTasks, 2) as $index => $task) {
                $recommendations[] = $this->buildRecommendation(
                    TaskRecommendation::PRIORITY_HIGH,
                    'review_blocked_task_' . $task->getTaskId(),
                    'Review blocked task: Task #' . $task->getTaskId(),
                    'A blocked task may require recruiter clarification or follow-up.',
                    $task,
                    'Open Task',
                    TaskRecommendation::ACTION_SELECT_TASK,
                    100 - ($index * 5)
                );
            }

            if ($notStartedCount >= 2 && isset($notStartedTasks[0])) {
                $recommendations[] = new TaskRecommendation(
                    TaskRecommendation::PRIORITY_HIGH,
                    'follow_up_progress',
                    'Multiple tasks are still not started (' . $notStartedCount . '). Follow up with the candidate.',
                    'Several tasks remain untouched, which may indicate onboarding delay.',
                    $notStartedTasks[0]->getTaskId(),
                    'Open Task',
                    TaskRecommendation::ACTION_SELECT_TASK,
                    90
                );
            }

            foreach ($this->sliceTasks($completedMissingAttachmentTasks, 2) as $index => $task) {
                $recommendations[] = $this->buildRecommendation(
                    TaskRecommendation::PRIORITY_MEDIUM,
                    'request_attachment_' . $task->getTaskId(),
                    'Completed task missing file: Task #' . $task->getTaskId(),
                    'Recruiter review is easier when completed tasks include proof or an attachment.',
                    $task,
                    'Open Update',
                    TaskRecommendation::ACTION_OPEN_UPDATE,
                    70 - ($index * 4)
                );
            }
        } else {
            if ($blockedCount > 1 && isset($blockedTasks[0])) {
                $recommendations[] = new TaskRecommendation(
                    TaskRecommendation::PRIORITY_HIGH,
                    'plan_needs_intervention',
                    'Multiple blocked tasks detected (' . $blockedCount . '). This plan may need intervention.',
                    'Multiple blockers can indicate a systemic issue or coordination problem.',
                    $blockedTasks[0]->getTaskId(),
                    'Open Task',
                    TaskRecommendation::ACTION_SELECT_TASK,
                    100
                );
            } else {
                foreach ($this->sliceTasks($blockedTasks, 2) as $index => $task) {
                    $recommendations[] = $this->buildRecommendation(
                        TaskRecommendation::PRIORITY_HIGH,
                        'review_blocked_task_' . $task->getTaskId(),
                        'Blocked task detected: Task #' . $task->getTaskId(),
                        'A blocker is visible and should be reviewed before it spreads.',
                        $task,
                        'Open Task',
                        TaskRecommendation::ACTION_SELECT_TASK,
                        92 - ($index * 5)
                    );
                }
            }

            foreach ($this->sliceTasks($completedMissingAttachmentTasks, 2) as $index => $task) {
                $recommendations[] = $this->buildRecommendation(
                    TaskRecommendation::PRIORITY_MEDIUM,
                    'missing_completion_artifact_' . $task->getTaskId(),
                    'Completed task missing attachment: Task #' . $task->getTaskId(),
                    'Completion checks are stronger when the proof link or file is present.',
                    $task,
                    'Open Update',
                    TaskRecommendation::ACTION_OPEN_UPDATE,
                    72 - ($index * 4)
                );
            }

            if ($notStartedCount === \count($tasks) && isset($notStartedTasks[0])) {
                $recommendations[] = new TaskRecommendation(
                    TaskRecommendation::PRIORITY_MEDIUM,
                    'no_progress',
                    'All tasks are still not started. Review execution progress.',
                    'No tasks have started yet across the loaded task list.',
                    $notStartedTasks[0]->getTaskId(),
                    'Open Task',
                    TaskRecommendation::ACTION_SELECT_TASK,
                    68
                );
            }
        }

        foreach ($this->sliceTasks($onHoldTasks, 1) as $index => $task) {
            $recommendations[] = $this->buildRecommendation(
                TaskRecommendation::PRIORITY_LOW,
                'on_hold_follow_up_' . $task->getTaskId(),
                'There are ' . $onHoldCount . ' on-hold task(s). Review their blockers.',
                'On-hold tasks often need dependency resolution or clarification.',
                $task,
                'Review',
                TaskRecommendation::ACTION_SELECT_TASK,
                35 - ($index * 3)
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

    /**
     * @param Onboardingtask[] $tasks
     * @return Onboardingtask[]
     */
    private function sliceTasks(array $tasks, int $limit): array
    {
        return \array_slice($tasks, 0, $limit);
    }

    private function buildRecommendation(
        string $priority,
        string $type,
        string $message,
        string $reason,
        Onboardingtask $task,
        string $actionLabel,
        string $actionType,
        int $score,
    ): TaskRecommendation {
        return new TaskRecommendation(
            $priority,
            $type,
            $message,
            $reason,
            $task->getTaskId(),
            $actionLabel,
            $actionType,
            $score
        );
    }
}
