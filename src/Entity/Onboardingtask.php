<?php

namespace App\Entity;

use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\Mapping as ORM;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;

use App\Repository\OnboardingtaskRepository;

#[ORM\Entity(repositoryClass: OnboardingtaskRepository::class)]
#[ORM\Table(name: 'onboardingtask')]
class Onboardingtask
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(type: 'integer')]
    private ?int $taskId = null;

    public function getTaskId(): ?int
    {
        return $this->taskId;
    }

    public function setTaskId(int $taskId): self
    {
        $this->taskId = $taskId;
        return $this;
    }

    #[ORM\Column(type: 'integer', nullable: false)]
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

    #[ORM\Column(type: 'string', nullable: true)]
    private ?string $title = null;

    public function getTitle(): ?string
    {
        return $this->title;
    }

    public function setTitle(?string $title): self
    {
        $this->title = $title;
        return $this;
    }

    #[ORM\Column(type: 'text', nullable: true)]
    private ?string $description = null;

    public function getDescription(): ?string
    {
        return $this->description;
    }

    public function setDescription(?string $description): self
    {
        $this->description = $description;
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
    private ?string $filePath = null;

    public function getFilePath(): ?string
    {
        return $this->filePath;
    }

    public function setFilePath(?string $filePath): self
    {
        $this->filePath = $filePath;
        return $this;
    }

    #[ORM\Column(type: 'string', nullable: true)]
    private ?string $cloudinary_public_id = null;

    public function getCloudinary_public_id(): ?string
    {
        return $this->cloudinary_public_id;
    }

    public function setCloudinary_public_id(?string $cloudinary_public_id): self
    {
        $this->cloudinary_public_id = $cloudinary_public_id;
        return $this;
    }

    #[ORM\Column(type: 'string', nullable: true)]
    private ?string $original_file_name = null;

    public function getOriginal_file_name(): ?string
    {
        return $this->original_file_name;
    }

    public function setOriginal_file_name(?string $original_file_name): self
    {
        $this->original_file_name = $original_file_name;
        return $this;
    }

    #[ORM\Column(type: 'string', nullable: true)]
    private ?string $content_type = null;

    public function getContent_type(): ?string
    {
        return $this->content_type;
    }

    public function setContent_type(?string $content_type): self
    {
        $this->content_type = $content_type;
        return $this;
    }

    public function getCloudinaryPublicId(): ?string
    {
        return $this->cloudinary_public_id;
    }

    public function setCloudinaryPublicId(?string $cloudinary_public_id): static
    {
        $this->cloudinary_public_id = $cloudinary_public_id;

        return $this;
    }

    public function getOriginalFileName(): ?string
    {
        return $this->original_file_name;
    }

    public function setOriginalFileName(?string $original_file_name): static
    {
        $this->original_file_name = $original_file_name;

        return $this;
    }

    public function getContentType(): ?string
    {
        return $this->content_type;
    }

    public function setContentType(?string $content_type): static
    {
        $this->content_type = $content_type;

        return $this;
    }

}
