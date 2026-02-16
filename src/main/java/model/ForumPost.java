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
    private String category;
    // Moderation and behavior flags managed from admin/user flows.
    private String status;
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

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isPinned() { return pinned; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }

    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
