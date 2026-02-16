package repo;

import model.ForumComment;
import util.DB;
import util.InputValidator;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC data access for forum comments.
 * Provides list/filter operations plus comment CRUD.
 */
public class ForumCommentRepository {

    // READ: comments for a post (admin/internal view includes all statuses).
    public List<ForumComment> findByPostId(long postId) throws SQLException {
        String sql = """
                SELECT id, post_id, author_id, content, status, created_at
                FROM forum_comment
                WHERE post_id = ?
                ORDER BY created_at DESC
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
            ps.setString(4, c.getStatus());

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
            ps.setString(2, c.getStatus());
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
        // READ: user-facing comments restricted to approved moderation state.
        String sql = """
                SELECT id, post_id, author_id, content, status, created_at
                FROM forum_comment
                WHERE post_id = ?
                  AND status = 'APPROVED'
                ORDER BY created_at DESC
                """;
        return fetchList(sql, postId);
    }

    public List<ForumComment> findByAuthorId(long authorId) throws SQLException {
        // READ: profile query for comments written by one user.
        String sql = """
                SELECT id, post_id, author_id, content, status, created_at
                FROM forum_comment
                WHERE author_id = ?
                ORDER BY created_at DESC
                """;
        return fetchList(sql, authorId);
    }

    private List<ForumComment> fetchList(String sql, long idParam) throws SQLException {
        // Shared executor for queries that vary only by SQL and one id parameter.
        List<ForumComment> out = new ArrayList<>();
        try (Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, idParam);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ForumComment c = new ForumComment();
                    c.setId(rs.getLong("id"));
                    c.setPostId(rs.getLong("post_id"));
                    c.setAuthorId(rs.getLong("author_id"));
                    c.setContent(rs.getString("content"));
                    c.setStatus(rs.getString("status"));
                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null)
                        c.setCreatedAt(ts.toLocalDateTime());
                    out.add(c);
                }
            }
        }
        return out;
    }
}
