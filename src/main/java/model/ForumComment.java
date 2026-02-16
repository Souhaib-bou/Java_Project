package model;

import java.time.LocalDateTime;

/**
 * Data model for a forum comment row.
 * Used in comment lists and repository mapping.
 */
public class ForumComment {
    // Identity + relations.
    private long id;
    private long postId;
    private long authorId;
    // Moderated comment payload.
    private String content;
    private String status;
    // Audit timestamp from database.
    private LocalDateTime createdAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getPostId() { return postId; }
    public void setPostId(long postId) { this.postId = postId; }

    public long getAuthorId() { return authorId; }
    public void setAuthorId(long authorId) { this.authorId = authorId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
