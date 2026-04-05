<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;

use App\Repository\ForumInteractionRepository;

#[ORM\Entity(repositoryClass: ForumInteractionRepository::class)]
#[ORM\Table(name: 'forum_interaction')]
class ForumInteraction
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(type: 'integer')]
    private ?int $id = null;

    public function getId(): ?int
    {
        return $this->id;
    }

    public function setId(int $id): self
    {
        $this->id = $id;
        return $this;
    }

    #[ORM\Column(type: 'string', nullable: false)]
    private ?string $target_type = null;

    public function getTarget_type(): ?string
    {
        return $this->target_type;
    }

    public function setTarget_type(string $target_type): self
    {
        $this->target_type = $target_type;
        return $this;
    }

    #[ORM\Column(type: 'integer', nullable: false)]
    private ?int $target_id = null;

    public function getTarget_id(): ?int
    {
        return $this->target_id;
    }

    public function setTarget_id(int $target_id): self
    {
        $this->target_id = $target_id;
        return $this;
    }

    #[ORM\OneToOne(targetEntity: User::class, inversedBy: 'forumInteraction')]
    #[ORM\JoinColumn(name: 'user_id', referencedColumnName: 'user_id', unique: true)]
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
    private ?string $interaction_type = null;

    public function getInteraction_type(): ?string
    {
        return $this->interaction_type;
    }

    public function setInteraction_type(string $interaction_type): self
    {
        $this->interaction_type = $interaction_type;
        return $this;
    }

    #[ORM\Column(type: 'datetime', nullable: false)]
    private ?\DateTimeInterface $created_at = null;

    public function getCreated_at(): ?\DateTimeInterface
    {
        return $this->created_at;
    }

    public function setCreated_at(\DateTimeInterface $created_at): self
    {
        $this->created_at = $created_at;
        return $this;
    }

    public function getTargetType(): ?string
    {
        return $this->target_type;
    }

    public function setTargetType(string $target_type): static
    {
        $this->target_type = $target_type;

        return $this;
    }

    public function getTargetId(): ?int
    {
        return $this->target_id;
    }

    public function setTargetId(int $target_id): static
    {
        $this->target_id = $target_id;

        return $this;
    }

    public function getInteractionType(): ?string
    {
        return $this->interaction_type;
    }

    public function setInteractionType(string $interaction_type): static
    {
        $this->interaction_type = $interaction_type;

        return $this;
    }

    public function getCreatedAt(): ?\DateTime
    {
        return $this->created_at;
    }

    public function setCreatedAt(\DateTime $created_at): static
    {
        $this->created_at = $created_at;

        return $this;
    }

}
