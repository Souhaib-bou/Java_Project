package repo;

import model.ForumPost;
import util.DB;
import util.InputValidator;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC data access for forum posts.
 * Handles CRUD operations and mapping between SQL rows and ForumPost objects.
 */
public class ForumPostRepository {

    // READ: admin/global feed query (all statuses).
    public List<ForumPost> findAll() throws SQLException {
        String sql = """
                SELECT id, author_id, title, content, category, status, is_pinned, is_locked, created_at
                FROM forum_post
                ORDER BY is_pinned DESC, created_at DESC
                """;

        List<ForumPost> out = new ArrayList<>();

        // try-with-resources ensures DB connection/statement/result are always closed.
        try (Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                ForumPost p = new ForumPost();
                p.setId(rs.getLong("id"));
                p.setAuthorId(rs.getLong("author_id"));
                p.setTitle(rs.getString("title"));
                p.setContent(rs.getString("content"));
                p.setCategory(rs.getString("category"));
                p.setStatus(rs.getString("status"));
                p.setPinned(rs.getBoolean("is_pinned"));
                p.setLocked(rs.getBoolean("is_locked"));

                Timestamp ts = rs.getTimestamp("created_at");
                if (ts != null)
                    p.setCreatedAt(ts.toLocalDateTime());

                out.add(p);
            }
        }
        return out;
    }

    // CREATE: inserts a new post row and returns generated id.
    public long insert(ForumPost p) throws SQLException {
        // Validate at repository boundary so all callers enforce the same rules.
        List<String> errors = InputValidator.validatePost(p.getTitle(), p.getContent(), p.getCategory());
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(" | ", errors));
        }

        String sql = """
                INSERT INTO forum_post (author_id, title, content, category, status, is_pinned, is_locked)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, p.getAuthorId());
            ps.setString(2, p.getTitle());
            ps.setString(3, p.getContent());
            ps.setString(4, p.getCategory());
            ps.setString(5, p.getStatus());
            ps.setBoolean(6, p.isPinned());
            ps.setBoolean(7, p.isLocked());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next())
                    return keys.getLong(1);
            }
            return 0;
        }
    }

    // UPDATE: applies content/moderation changes to an existing post.
    public void update(ForumPost p) throws SQLException {
        // Re-validate on update to prevent invalid edits from any UI path.
        List<String> errors = InputValidator.validatePost(p.getTitle(), p.getContent(), p.getCategory());
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(" | ", errors));
        }

        String sql = """
                UPDATE forum_post
                SET title=?, content=?, category=?, status=?, is_pinned=?, is_locked=?, updated_at=CURRENT_TIMESTAMP
                WHERE id=?
                """;

        try (Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, p.getTitle());
            ps.setString(2, p.getContent());
            ps.setString(3, p.getCategory());
            ps.setString(4, p.getStatus());
            ps.setBoolean(5, p.isPinned());
            ps.setBoolean(6, p.isLocked());
            ps.setLong(7, p.getId());

            ps.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        // DELETE: removing post also removes comments via DB cascade rules.
        String sql = "DELETE FROM forum_post WHERE id=?";
        try (Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public List<ForumPost> findApproved() throws SQLException {
        // READ: user feed query (moderation-safe list only).
        String sql = """
                SELECT id, author_id, title, content, category, status, is_pinned, is_locked, created_at
                FROM forum_post
                WHERE status = 'APPROVED'
                ORDER BY is_pinned DESC, created_at DESC
                """;

        List<ForumPost> out = new ArrayList<>();

        try (Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                ForumPost p = mapRow(rs);
                out.add(p);
            }
        }
        return out;
    }

    public List<ForumPost> findByAuthorId(long authorId) throws SQLException {
        // READ: profile query for a single author.
        String sql = """
                SELECT id, author_id, title, content, category, status, is_pinned, is_locked, created_at
                FROM forum_post
                WHERE author_id = ?
                ORDER BY is_pinned DESC, created_at DESC
                """;

        List<ForumPost> out = new ArrayList<>();

        try (Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, authorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ForumPost p = mapRow(rs);
                    out.add(p);
                }
            }
        }
        return out;
    }

    private ForumPost mapRow(ResultSet rs) throws SQLException {
        // Single row mapper to keep query methods consistent.
        ForumPost p = new ForumPost();
        p.setId(rs.getLong("id"));
        p.setAuthorId(rs.getLong("author_id"));
        p.setTitle(rs.getString("title"));
        p.setContent(rs.getString("content"));
        p.setCategory(rs.getString("category"));
        p.setStatus(rs.getString("status"));
        p.setPinned(rs.getBoolean("is_pinned"));
        p.setLocked(rs.getBoolean("is_locked"));

        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null)
            p.setCreatedAt(ts.toLocalDateTime());
        return p;
    }
}
