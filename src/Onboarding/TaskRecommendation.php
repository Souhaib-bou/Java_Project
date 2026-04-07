<?php

namespace App\Onboarding;

final class TaskRecommendation
{
    public const PRIORITY_HIGH = 'high';
    public const PRIORITY_MEDIUM = 'medium';
    public const PRIORITY_LOW = 'low';

    public const ACTION_SELECT_TASK = 'select_task';
    public const ACTION_OPEN_UPDATE = 'open_update';
    public const ACTION_REFRESH = 'refresh';

    public function __construct(
        private readonly string $priority,
        private readonly string $type,
        private readonly string $message,
        private readonly string $reason,
        private readonly ?int $taskId,
        private readonly string $actionLabel,
        private readonly string $actionType,
        private readonly int $score,
    ) {
    }

    public function getPriority(): string
    {
        return $this->priority;
    }

    public function getType(): string
    {
        return $this->type;
    }

    public function getMessage(): string
    {
        return $this->message;
    }

    public function getReason(): string
    {
        return $this->reason;
    }

    public function getTaskId(): ?int
    {
        return $this->taskId;
    }

    public function getActionLabel(): string
    {
        return $this->actionLabel;
    }

    public function getActionType(): string
    {
        return $this->actionType;
    }

    public function getScore(): int
    {
        return $this->score;
    }
}
