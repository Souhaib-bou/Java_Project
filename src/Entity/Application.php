<?php

namespace App\Entity;

use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\Mapping as ORM;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;

use App\Repository\ApplicationRepository;

#[ORM\Entity(repositoryClass: ApplicationRepository::class)]
#[ORM\Table(name: 'application')]
class Application
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(type: 'integer')]
    private ?int $applicationId = null;

    public function getApplicationId(): ?int
    {
        return $this->applicationId;
    }

    public function setApplicationId(int $applicationId): self
    {
        $this->applicationId = $applicationId;
        return $this;
    }

    #[ORM\Column(type: 'date', nullable: true)]
    private ?\DateTimeInterface $applicationDate = null;

    public function getApplicationDate(): ?\DateTimeInterface
    {
        return $this->applicationDate;
    }

    public function setApplicationDate(?\DateTimeInterface $applicationDate): self
    {
        $this->applicationDate = $applicationDate;
        return $this;
    }

    #[ORM\Column(type: 'text', nullable: true)]
    private ?string $coverLetter = null;

    public function getCoverLetter(): ?string
    {
        return $this->coverLetter;
    }

    public function setCoverLetter(?string $coverLetter): self
    {
        $this->coverLetter = $coverLetter;
        return $this;
    }

    #[ORM\Column(type: 'string', nullable: true)]
    private ?string $currentStatus = null;

    public function getCurrentStatus(): ?string
    {
        return $this->currentStatus;
    }

    public function setCurrentStatus(?string $currentStatus): self
    {
        $this->currentStatus = $currentStatus;
        return $this;
    }

    #[ORM\Column(type: 'string', nullable: true)]
    private ?string $resumePath = null;

    public function getResumePath(): ?string
    {
        return $this->resumePath;
    }

    public function setResumePath(?string $resumePath): self
    {
        $this->resumePath = $resumePath;
        return $this;
    }

    #[ORM\Column(type: 'datetime', nullable: true)]
    private ?\DateTimeInterface $lastUpdateDate = null;

    public function getLastUpdateDate(): ?\DateTimeInterface
    {
        return $this->lastUpdateDate;
    }

    public function setLastUpdateDate(?\DateTimeInterface $lastUpdateDate): self
    {
        $this->lastUpdateDate = $lastUpdateDate;
        return $this;
    }

    #[ORM\ManyToOne(targetEntity: User::class, inversedBy: 'applications')]
    #[ORM\JoinColumn(name: 'user_id', referencedColumnName: 'user_id')]
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

    #[ORM\ManyToOne(targetEntity: Joboffer::class, inversedBy: 'applications')]
    #[ORM\JoinColumn(name: 'jobOfferId', referencedColumnName: 'jobOfferId')]
    private ?Joboffer $joboffer = null;

    public function getJoboffer(): ?Joboffer
    {
        return $this->joboffer;
    }

    public function setJoboffer(?Joboffer $joboffer): self
    {
        $this->joboffer = $joboffer;
        return $this;
    }

    #[ORM\Column(type: 'decimal', nullable: true)]
    private ?float $expectedSalary = null;

    public function getExpectedSalary(): ?float
    {
        return $this->expectedSalary;
    }

    public function setExpectedSalary(?float $expectedSalary): self
    {
        $this->expectedSalary = $expectedSalary;
        return $this;
    }

    #[ORM\Column(type: 'date', nullable: true)]
    private ?\DateTimeInterface $availabilityDate = null;

    public function getAvailabilityDate(): ?\DateTimeInterface
    {
        return $this->availabilityDate;
    }

    public function setAvailabilityDate(?\DateTimeInterface $availabilityDate): self
    {
        $this->availabilityDate = $availabilityDate;
        return $this;
    }

    #[ORM\Column(type: 'string', nullable: true)]
    private ?string $phone = null;

    public function getPhone(): ?string
    {
        return $this->phone;
    }

    public function setPhone(?string $phone): self
    {
        $this->phone = $phone;
        return $this;
    }

    #[ORM\Column(type: 'string', nullable: true)]
    private ?string $email = null;

    public function getEmail(): ?string
    {
        return $this->email;
    }

    public function setEmail(?string $email): self
    {
        $this->email = $email;
        return $this;
    }

    #[ORM\Column(type: 'integer', nullable: true)]
    private ?int $experienceYears = null;

    public function getExperienceYears(): ?int
    {
        return $this->experienceYears;
    }

    public function setExperienceYears(?int $experienceYears): self
    {
        $this->experienceYears = $experienceYears;
        return $this;
    }

    #[ORM\Column(type: 'string', nullable: true)]
    private ?string $portfolioUrl = null;

    public function getPortfolioUrl(): ?string
    {
        return $this->portfolioUrl;
    }

    public function setPortfolioUrl(?string $portfolioUrl): self
    {
        $this->portfolioUrl = $portfolioUrl;
        return $this;
    }

    #[ORM\Column(type: 'decimal', nullable: true)]
    private ?float $score = null;

    public function getScore(): ?float
    {
        return $this->score;
    }

    public function setScore(?float $score): self
    {
        $this->score = $score;
        return $this;
    }

    #[ORM\Column(type: 'text', nullable: true)]
    private ?string $reviewNote = null;

    public function getReviewNote(): ?string
    {
        return $this->reviewNote;
    }

    public function setReviewNote(?string $reviewNote): self
    {
        $this->reviewNote = $reviewNote;
        return $this;
    }

}
