package model;

import java.time.LocalDateTime;

/**
 * Data model for a forum post row.
 * Used by repositories and JavaFX controllers.
 */
public class ForumPost {
    // Core post identity and ownership.
    private long id;
    private long authorId;
    // Main content fields entered from JavaFX post editor dialogs.
    private String title;
    private String content;
    private String tag;
    // Moderation and behavior flags managed from admin/user flows.
    private String status;
    private double duplicateScore;
    private Long duplicateOfPostId;
    // Transient engagement fields for UI/feed operations.
    private int likeCount;
    private int shareCount;
    private boolean likedByCurrentUser;
    private boolean pinned;
    private boolean locked;
    // Audit timestamp from database.
    private LocalDateTime createdAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getAuthorId() { return authorId; }
    public void setAuthorId(long authorId) { this.authorId = authorId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }

    /** @deprecated use {@link #getTag()} */
    @Deprecated
    public String getCategory() { return tag; }

    /** @deprecated use {@link #setTag(String)} */
    @Deprecated
    public void setCategory(String category) { this.tag = category; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getDuplicateScore() { return duplicateScore; }
    public void setDuplicateScore(double duplicateScore) { this.duplicateScore = duplicateScore; }

    public Long getDuplicateOfPostId() { return duplicateOfPostId; }
    public void setDuplicateOfPostId(Long duplicateOfPostId) { this.duplicateOfPostId = duplicateOfPostId; }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = Math.max(0, likeCount); }

    public int getShareCount() { return shareCount; }
    public void setShareCount(int shareCount) { this.shareCount = Math.max(0, shareCount); }

    public boolean isLikedByCurrentUser() { return likedByCurrentUser; }
    public void setLikedByCurrentUser(boolean likedByCurrentUser) { this.likedByCurrentUser = likedByCurrentUser; }

    public boolean isPinned() { return pinned; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }

    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
