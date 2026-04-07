<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;
use App\Repository\OnboardingtaskRepository;
use Symfony\Component\Validator\Constraints as Assert;

#[ORM\Entity(repositoryClass: OnboardingtaskRepository::class)]
#[ORM\Table(name: 'onboardingtask')]
class Onboardingtask
{
    public const STATUS_NOT_STARTED = 'not_started';
    public const STATUS_IN_PROGRESS = 'in_progress';
    public const STATUS_COMPLETED = 'completed';
    public const STATUS_BLOCKED = 'blocked';
    public const STATUS_ON_HOLD = 'on_hold';

    public const STATUS_VALUES = [
        self::STATUS_NOT_STARTED,
        self::STATUS_IN_PROGRESS,
        self::STATUS_COMPLETED,
        self::STATUS_BLOCKED,
        self::STATUS_ON_HOLD,
    ];

    public const STATUS_CHOICES = [
        'Not Started' => self::STATUS_NOT_STARTED,
        'In Progress' => self::STATUS_IN_PROGRESS,
        'Completed' => self::STATUS_COMPLETED,
        'Blocked' => self::STATUS_BLOCKED,
        'On Hold' => self::STATUS_ON_HOLD,
    ];

    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(name: 'taskId', type: 'integer')]
    private ?int $taskId = null;

    #[ORM\ManyToOne(targetEntity: Onboardingplan::class, inversedBy: 'onboardingtasks')]
    #[ORM\JoinColumn(name: 'planId', referencedColumnName: 'planId', nullable: false)]
    #[Assert\NotNull(message: 'This task must belong to an onboarding plan.')]
    private ?Onboardingplan $plan = null;

    #[ORM\Column(type: 'string', nullable: true)]
    #[Assert\NotBlank(message: 'Please enter a task title.')]
    #[Assert\Length(
        max: 255,
        maxMessage: 'The task title cannot be longer than {{ limit }} characters.'
    )]
    private ?string $title = null;

    #[ORM\Column(type: 'text', nullable: true)]
    #[Assert\NotBlank(message: 'Please enter a task description.')]
    #[Assert\Length(
        min: 10,
        minMessage: 'The task description must be at least {{ limit }} characters long.',
        max: 5000,
        maxMessage: 'The task description cannot be longer than {{ limit }} characters.'
    )]
    private ?string $description = null;

    #[ORM\Column(type: 'string', nullable: false)]
    #[Assert\NotBlank(message: 'Please choose a task status.')]
    #[Assert\Choice(choices: self::STATUS_VALUES, message: 'Please choose a valid task status.')]
    private ?string $status = self::STATUS_NOT_STARTED;

    #[ORM\Column(type: 'date', nullable: true)]
    #[Assert\GreaterThanOrEqual(
        value: 'today',
        message: 'The deadline cannot be in the past.'
    )]
    private ?\DateTimeInterface $deadline = null;

    #[ORM\Column(name: 'filePath', type: 'string', nullable: true)]
    #[Assert\Length(
        max: 255,
        maxMessage: 'The file path cannot be longer than {{ limit }} characters.'
    )]
    private ?string $filePath = null;

    #[ORM\Column(type: 'string', nullable: true)]
    #[Assert\Length(
        max: 255,
        maxMessage: 'The Cloudinary public ID cannot be longer than {{ limit }} characters.'
    )]
    private ?string $cloudinary_public_id = null;

    #[ORM\Column(type: 'string', nullable: true)]
    #[Assert\Length(
        max: 255,
        maxMessage: 'The original file name cannot be longer than {{ limit }} characters.'
    )]
    private ?string $original_file_name = null;

    #[ORM\Column(type: 'string', nullable: true)]
    #[Assert\Length(
        max: 120,
        maxMessage: 'The content type cannot be longer than {{ limit }} characters.'
    )]
    private ?string $content_type = null;

    public function getTaskId(): ?int
    {
        return $this->taskId;
    }

    public function setTaskId(int $taskId): self
    {
        $this->taskId = $taskId;
        return $this;
    }

    public function getPlan(): ?Onboardingplan
    {
        return $this->plan;
    }

    public function setPlan(?Onboardingplan $plan): self
    {
        $this->plan = $plan;
        return $this;
    }

    public function getTitle(): ?string
    {
        return $this->title;
    }

    public function setTitle(?string $title): self
    {
        $this->title = null !== $title ? trim($title) : null;
        return $this;
    }

    public function getDescription(): ?string
    {
        return $this->description;
    }

    public function setDescription(?string $description): self
    {
        $this->description = null !== $description ? trim($description) : null;
        return $this;
    }

    public function getStatus(): ?string
    {
        return $this->status;
    }

    public function setStatus(?string $status): self
    {
        $this->status = null !== $status ? trim($status) : null;
        return $this;
    }

    public function getDeadline(): ?\DateTimeInterface
    {
        return $this->deadline;
    }

    public function setDeadline(?\DateTimeInterface $deadline): self
    {
        $this->deadline = $deadline;
        return $this;
    }

    public function getFilePath(): ?string
    {
        return $this->filePath;
    }

    public function setFilePath(?string $filePath): self
    {
        $this->filePath = null !== $filePath ? trim($filePath) : null;
        return $this;
    }

    public function getCloudinaryPublicId(): ?string
    {
        return $this->cloudinary_public_id;
    }

    public function setCloudinaryPublicId(?string $cloudinary_public_id): self
    {
        $this->cloudinary_public_id = null !== $cloudinary_public_id ? trim($cloudinary_public_id) : null;
        return $this;
    }

    public function getOriginalFileName(): ?string
    {
        return $this->original_file_name;
    }

    public function setOriginalFileName(?string $original_file_name): self
    {
        $this->original_file_name = null !== $original_file_name ? trim($original_file_name) : null;
        return $this;
    }

    public function getContentType(): ?string
    {
        return $this->content_type;
    }

    public function setContentType(?string $content_type): self
    {
        $this->content_type = null !== $content_type ? trim($content_type) : null;
        return $this;
    }

    public function clearAttachment(): self
    {
        $this->filePath = null;
        $this->cloudinary_public_id = null;
        $this->original_file_name = null;
        $this->content_type = null;

        return $this;
    }

    public static function getStatusChoices(): array
    {
        return self::STATUS_CHOICES;
    }
}
