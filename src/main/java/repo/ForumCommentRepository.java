package repo;

import model.CommentSort;
import model.ForumComment;
import util.DB;
import util.InputValidator;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * JDBC data access for forum comments.
 * Provides list/filter operations plus comment CRUD.
 */
public class ForumCommentRepository {

    // READ: comments for a post (admin/internal view includes all statuses).
    public List<ForumComment> findByPostId(long postId) throws SQLException {
        String sql = """
                SELECT id, post_id, author_id, content, status, created_at, is_pinned
                FROM forum_comment
                WHERE post_id = ?
                ORDER BY is_pinned DESC, created_at DESC
                """;

        List<ForumComment> out = new ArrayList<>();

        // try-with-resources ensures DB connection/statement/result are always closed.
        try (Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, postId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ForumComment c = new ForumComment();
                    c.setId(rs.getLong("id"));
                    c.setPostId(rs.getLong("post_id"));
                    c.setAuthorId(rs.getLong("author_id"));
                    c.setContent(rs.getString("content"));
                    c.setStatus(rs.getString("status"));
                    c.setPinned(rs.getBoolean("is_pinned"));

                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null)
                        c.setCreatedAt(ts.toLocalDateTime());

                    out.add(c);
                }
            }
        }
        return out;
    }

    // CREATE: inserts comment row and returns generated id.
    public long insert(ForumComment c) throws SQLException {
        // Central validation to keep controller logic lightweight.
        List<String> errors = InputValidator.validateComment(c.getContent());
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(" | ", errors));
        }

        String sql = """
                INSERT INTO forum_comment (post_id, author_id, content, status)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, c.getPostId());
            ps.setLong(2, c.getAuthorId());
            ps.setString(3, c.getContent());
            ps.setString(4, normalizeModerationStatus(c.getStatus()));

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next())
                    return keys.getLong(1);
            }
            return 0;
        }
    }

    // UPDATE: edits comment content/status.
    public void update(ForumComment c) throws SQLException {
        // Same rules are enforced for updates.
        List<String> errors = InputValidator.validateComment(c.getContent());
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(" | ", errors));
        }

        String sql = """
                UPDATE forum_comment
                SET content=?, status=?, updated_at=CURRENT_TIMESTAMP
                WHERE id=?
                """;

        try (Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, c.getContent());
            ps.setString(2, normalizeModerationStatus(c.getStatus()));
            ps.setLong(3, c.getId());

            ps.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        // DELETE: remove one comment by id.
        String sql = "DELETE FROM forum_comment WHERE id = ?";
        try (Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public List<ForumComment> findApprovedByPostId(long postId) throws SQLException {
        return findApprovedByPostIdSorted(postId, CommentSort.NEWEST);
    }

    public List<ForumComment> findByAuthorId(long authorId) throws SQLException {
        // READ: profile query for comments written by one user.
        String sql = """
                SELECT id, post_id, author_id, content, status, created_at, is_pinned
                FROM forum_comment
                WHERE author_id = ?
                ORDER BY created_at DESC
                """;
        return fetchList(sql, authorId);
    }

    public List<ForumComment> findLatest(int limit) throws SQLException {
        int safeLimit = Math.max(1, limit);
        String sql = """
                SELECT id, post_id, author_id, content, status, created_at, is_pinned
                FROM forum_comment
                ORDER BY created_at DESC
                LIMIT ?
                """;
        List<ForumComment> out = new ArrayList<>();
        try (Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, safeLimit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(mapRow(rs));
                }
            }
        }
        return out;
    }

    public void updateStatus(long commentId, String status) throws SQLException {
        String normalized = normalizeModerationStatus(status);
        String sql = """
                UPDATE forum_comment
                SET status=?, updated_at=CURRENT_TIMESTAMP
                WHERE id=?
                """;
        try (Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, normalized);
            ps.setLong(2, commentId);
            ps.executeUpdate();
        }
    }

    public List<ForumComment> findByPostIdSorted(long postId, CommentSort sort) throws SQLException {
        return findByPostIdSortedInternal(postId, sort, "WHERE c.post_id=?");
    }

    public List<ForumComment> findApprovedByPostIdSorted(long postId, CommentSort sort) throws SQLException {
        return findByPostIdSortedInternal(
                postId,
                sort,
                "WHERE c.post_id=? AND UPPER(TRIM(COALESCE(c.status, 'PENDING'))) IN ('APPROVED','APPROVE')");
    }

    public List<ForumComment> findVisibleByPostIdSorted(long postId, CommentSort sort) throws SQLException {
        return findByPostIdSortedInternal(
                postId,
                sort,
                "WHERE c.post_id=? AND UPPER(TRIM(COALESCE(c.status, 'PENDING'))) <> 'REJECTED'");
    }

    public void setPinned(long commentId, boolean pinned) throws SQLException {
        String sql = """
                UPDATE forum_comment
                SET is_pinned=?, updated_at=CURRENT_TIMESTAMP
                WHERE id=?
                """;
        try (Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBoolean(1, pinned);
            ps.setLong(2, commentId);
            ps.executeUpdate();
        }
    }

    private List<ForumComment> fetchList(String sql, long idParam) throws SQLException {
        // Shared executor for queries that vary only by SQL and one id parameter.
        List<ForumComment> out = new ArrayList<>();
        try (Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, idParam);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(mapRow(rs));
                }
            }
        }
        return out;
    }

    private ForumComment mapRow(ResultSet rs) throws SQLException {
        ForumComment c = new ForumComment();
        c.setId(rs.getLong("id"));
        c.setPostId(rs.getLong("post_id"));
        c.setAuthorId(rs.getLong("author_id"));
        c.setContent(rs.getString("content"));
        c.setStatus(rs.getString("status"));
        c.setPinned(rs.getBoolean("is_pinned"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            c.setCreatedAt(ts.toLocalDateTime());
        }
        return c;
    }

    private List<ForumComment> findByPostIdSortedInternal(long postId, CommentSort sort, String whereClause)
            throws SQLException {
        CommentSort safeSort = sort == null ? CommentSort.NEWEST : sort;
        String sql;
        if (safeSort == CommentSort.TOP) {
            sql = """
                    SELECT c.id, c.post_id, c.author_id, c.content, c.status, c.created_at, c.is_pinned
                    FROM forum_comment c
                    LEFT JOIN forum_interaction i
                      ON i.target_type='COMMENT'
                     AND i.interaction_type='LIKE'
                     AND i.target_id=c.id
                    """
                    + whereClause
                    + """
                    GROUP BY c.id, c.post_id, c.author_id, c.content, c.status, c.created_at, c.is_pinned
                    ORDER BY c.is_pinned DESC, COUNT(i.id) DESC, c.created_at DESC
                    """;
        } else {
            String createdOrder = safeSort == CommentSort.OLDEST ? "ASC" : "DESC";
            sql = """
                    SELECT c.id, c.post_id, c.author_id, c.content, c.status, c.created_at, c.is_pinned
                    FROM forum_comment c
                    """
                    + whereClause
                    + """
                    ORDER BY c.is_pinned DESC, c.created_at
                    """
                    + createdOrder;
        }
        List<ForumComment> out = new ArrayList<>();
        try (Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(mapRow(rs));
                }
            }
        }
        return out;
    }

    private String normalizeModerationStatus(String status) {
        if (status == null || status.isBlank()) {
            return "PENDING";
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if ("APPROVED".equals(normalized) || "PENDING".equals(normalized) || "REJECTED".equals(normalized)) {
            return normalized;
        }
        if ("REJECT".equals(normalized)) {
            return "REJECTED";
        }
        return "PENDING";
    }
}
