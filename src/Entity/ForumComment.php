<?php

namespace App\Entity;

use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\Mapping as ORM;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;

use App\Repository\ForumCommentRepository;

#[ORM\Entity(repositoryClass: ForumCommentRepository::class)]
#[ORM\Table(name: 'forum_comment')]
class ForumComment
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

    #[ORM\ManyToOne(targetEntity: ForumPost::class, inversedBy: 'forumComments')]
    #[ORM\JoinColumn(name: 'post_id', referencedColumnName: 'id')]
    private ?ForumPost $forumPost = null;

    public function getForumPost(): ?ForumPost
    {
        return $this->forumPost;
    }

    public function setForumPost(?ForumPost $forumPost): self
    {
        $this->forumPost = $forumPost;
        return $this;
    }

    #[ORM\ManyToOne(targetEntity: User::class, inversedBy: 'forumComments')]
    #[ORM\JoinColumn(name: 'author_id', referencedColumnName: 'user_id')]
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

    #[ORM\Column(type: 'text', nullable: false)]
    private ?string $content = null;

    public function getContent(): ?string
    {
        return $this->content;
    }

    public function setContent(string $content): self
    {
        $this->content = $content;
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

    #[ORM\Column(type: 'text', nullable: true)]
    private ?string $moderation_note = null;

    public function getModeration_note(): ?string
    {
        return $this->moderation_note;
    }

    public function setModeration_note(?string $moderation_note): self
    {
        $this->moderation_note = $moderation_note;
        return $this;
    }

    #[ORM\Column(type: 'datetime', nullable: true)]
    private ?\DateTimeInterface $edited_at = null;

    public function getEdited_at(): ?\DateTimeInterface
    {
        return $this->edited_at;
    }

    public function setEdited_at(?\DateTimeInterface $edited_at): self
    {
        $this->edited_at = $edited_at;
        return $this;
    }

    #[ORM\ManyToOne(targetEntity: User::class)]
    #[ORM\JoinColumn(name: 'edited_by', referencedColumnName: 'user_id')]
    private ?User $editedByUser = null;

    public function getEditedByUser(): ?User
    {
        return $this->editedByUser;
    }

    public function setEditedByUser(?User $editedByUser): self
    {
        $this->editedByUser = $editedByUser;
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

    #[ORM\Column(type: 'datetime', nullable: false)]
    private ?\DateTimeInterface $updated_at = null;

    public function getUpdated_at(): ?\DateTimeInterface
    {
        return $this->updated_at;
    }

    public function setUpdated_at(\DateTimeInterface $updated_at): self
    {
        $this->updated_at = $updated_at;
        return $this;
    }

    #[ORM\Column(type: 'boolean', nullable: false)]
    private ?bool $is_pinned = null;

    public function is_pinned(): ?bool
    {
        return $this->is_pinned;
    }

    public function setIs_pinned(bool $is_pinned): self
    {
        $this->is_pinned = $is_pinned;
        return $this;
    }

    #[ORM\OneToMany(targetEntity: ForumNotification::class, mappedBy: 'forumComment')]
    private Collection $forumNotifications;

    public function __construct()
    {
        $this->forumNotifications = new ArrayCollection();
    }

    /**
     * @return Collection<int, ForumNotification>
     */
    public function getForumNotifications(): Collection
    {
        if (!$this->forumNotifications instanceof Collection) {
            $this->forumNotifications = new ArrayCollection();
        }
        return $this->forumNotifications;
    }

    public function addForumNotification(ForumNotification $forumNotification): self
    {
        if (!$this->getForumNotifications()->contains($forumNotification)) {
            $this->getForumNotifications()->add($forumNotification);
        }
        return $this;
    }

    public function removeForumNotification(ForumNotification $forumNotification): self
    {
        $this->getForumNotifications()->removeElement($forumNotification);
        return $this;
    }

    public function getModerationNote(): ?string
    {
        return $this->moderation_note;
    }

    public function setModerationNote(?string $moderation_note): static
    {
        $this->moderation_note = $moderation_note;

        return $this;
    }

    public function getEditedAt(): ?\DateTime
    {
        return $this->edited_at;
    }

    public function setEditedAt(?\DateTime $edited_at): static
    {
        $this->edited_at = $edited_at;

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

    public function getUpdatedAt(): ?\DateTime
    {
        return $this->updated_at;
    }

    public function setUpdatedAt(\DateTime $updated_at): static
    {
        $this->updated_at = $updated_at;

        return $this;
    }

    public function isPinned(): ?bool
    {
        return $this->is_pinned;
    }

    public function setIsPinned(bool $is_pinned): static
    {
        $this->is_pinned = $is_pinned;

        return $this;
    }

}
