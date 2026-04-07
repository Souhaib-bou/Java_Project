<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;
use App\Repository\OnboardingplanRepository;
use Symfony\Component\Validator\Constraints as Assert;

#[ORM\Entity(repositoryClass: OnboardingplanRepository::class)]
#[ORM\Table(name: 'onboardingplan')]
class Onboardingplan
{
    public const STATUS_PENDING = 'pending';
    public const STATUS_IN_PROGRESS = 'in_progress';
    public const STATUS_COMPLETED = 'completed';
    public const STATUS_ON_HOLD = 'on_hold';

    public const STATUS_VALUES = [
        self::STATUS_PENDING,
        self::STATUS_IN_PROGRESS,
        self::STATUS_COMPLETED,
        self::STATUS_ON_HOLD,
    ];

    public const STATUS_CHOICES = [
        'Pending' => self::STATUS_PENDING,
        'In Progress' => self::STATUS_IN_PROGRESS,
        'Completed' => self::STATUS_COMPLETED,
        'On Hold' => self::STATUS_ON_HOLD,
    ];

    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(name: 'planId', type: 'integer')]
    private ?int $planId = null;

    #[ORM\ManyToOne(targetEntity: User::class, inversedBy: 'onboardingplans')]
    #[ORM\JoinColumn(name: 'user_id', referencedColumnName: 'user_id', nullable: false)]
    #[Assert\NotNull(message: 'Please select a user for this onboarding plan.')]
    private ?User $user = null;

    #[ORM\Column(type: 'string', nullable: false)]
    #[Assert\NotBlank(message: 'Please choose a plan status.')]
    #[Assert\Choice(choices: self::STATUS_VALUES, message: 'Please choose a valid plan status.')]
    private ?string $status = self::STATUS_PENDING;

    #[ORM\Column(type: 'date', nullable: true)]
    #[Assert\GreaterThanOrEqual(
        value: 'today',
        message: 'The deadline cannot be in the past.'
    )]
    private ?\DateTimeInterface $deadline = null;

    #[ORM\Column(type: 'string', nullable: true)]
    #[Assert\Length(
        max: 80,
        maxMessage: 'The QR token cannot be longer than {{ limit }} characters.'
    )]
    private ?string $qr_token = null;

    #[ORM\OneToMany(targetEntity: Onboardingtask::class, mappedBy: 'plan', orphanRemoval: true)]
    private Collection $onboardingtasks;

    public function __construct()
    {
        $this->onboardingtasks = new ArrayCollection();
    }

    public function getPlanId(): ?int
    {
        return $this->planId;
    }

    public function setPlanId(int $planId): self
    {
        $this->planId = $planId;
        return $this;
    }

    public function getUser(): ?User
    {
        return $this->user;
    }

    public function setUser(?User $user): self
    {
        $this->user = $user;
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

    public function getQr_token(): ?string
    {
        return $this->qr_token;
    }

    public function setQr_token(?string $qr_token): self
    {
        $this->qr_token = null !== $qr_token ? trim($qr_token) : null;
        return $this;
    }

    public function getQrToken(): ?string
    {
        return $this->qr_token;
    }

    public function setQrToken(?string $qr_token): static
    {
        $this->qr_token = null !== $qr_token ? trim($qr_token) : null;
        return $this;
    }

    public function getOnboardingtasks(): Collection
    {
        return $this->onboardingtasks;
    }

    public function addOnboardingtask(Onboardingtask $onboardingtask): self
    {
        if (!$this->onboardingtasks->contains($onboardingtask)) {
            $this->onboardingtasks->add($onboardingtask);
            $onboardingtask->setPlan($this);
        }

        return $this;
    }

    public function removeOnboardingtask(Onboardingtask $onboardingtask): self
    {
        if ($this->onboardingtasks->removeElement($onboardingtask)) {
            if ($onboardingtask->getPlan() === $this) {
                $onboardingtask->setPlan(null);
            }
        }

        return $this;
    }

    public static function getStatusChoices(): array
    {
        return self::STATUS_CHOICES;
    }
}
