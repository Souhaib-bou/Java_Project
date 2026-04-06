<?php

namespace App\Entity;

use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\Mapping as ORM;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;

use App\Repository\OnboardingplanRepository;

#[ORM\Entity(repositoryClass: OnboardingplanRepository::class)]
#[ORM\Table(name: 'onboardingplan')]
class Onboardingplan
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(name: 'planId', type: 'integer')]
    private ?int $planId = null;

    public function getPlanId(): ?int
    {
        return $this->planId;
    }

    public function setPlanId(int $planId): self
    {
        $this->planId = $planId;
        return $this;
    }

    #[ORM\ManyToOne(targetEntity: User::class)]
    #[ORM\JoinColumn(name: 'user_id', referencedColumnName: 'user_id', nullable: false)]
    private ?User $user = null;

    public function getUser(): ?User
    {
        return $this->user;
    }

    public function setUser(?User $user): self
    {
        $this->user = $user;
        return $this;
    }

    #[ORM\Column(type: 'string', nullable: false)]
    private ?string $status = null;

    public function getStatus(): ?string
    {
        return $this->status;
    }

    public function setStatus(string $status): self
    {
        $this->status = $status;
        return $this;
    }

    #[ORM\Column(type: 'date', nullable: true)]
    private ?\DateTimeInterface $deadline = null;

    public function getDeadline(): ?\DateTimeInterface
    {
        return $this->deadline;
    }

    public function setDeadline(?\DateTimeInterface $deadline): self
    {
        $this->deadline = $deadline;
        return $this;
    }

    #[ORM\Column(type: 'string', nullable: true)]
    private ?string $qr_token = null;

    public function getQr_token(): ?string
    {
        return $this->qr_token;
    }

    public function setQr_token(?string $qr_token): self
    {
        $this->qr_token = $qr_token;
        return $this;
    }

    public function getQrToken(): ?string
    {
        return $this->qr_token;
    }

    public function setQrToken(?string $qr_token): static
    {
        $this->qr_token = $qr_token;

        return $this;
    }

}
