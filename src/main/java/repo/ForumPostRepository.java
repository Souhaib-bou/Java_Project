package repo;

import model.ForumPost;
import util.DB;
import util.InputValidator;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * JDBC data access for forum posts.
 * Handles CRUD operations and mapping between SQL rows and ForumPost objects.
 */
public class ForumPostRepository {
    private volatile boolean duplicateColumnsDetected = false;
    private volatile boolean hasDuplicateScoreColumn = false;
    private volatile boolean hasDuplicateOfPostIdColumn = false;

    // READ: admin/global feed query (all statuses).
    public List<ForumPost> findAll() throws SQLException {
        ensureDuplicateColumns();
        String sql = """
                SELECT p.id, p.author_id, p.title, p.content, p.tag, p.status, p.is_pinned, p.is_locked, p.created_at,
                       (SELECT COUNT(*) FROM forum_interaction i WHERE i.target_type='POST' AND i.target_id = p.id AND i.interaction_type='LIKE') AS like_count,
                       (SELECT COUNT(*) FROM forum_interaction i WHERE i.target_type='POST' AND i.target_id = p.id AND i.interaction_type='SHARE') AS share_count
                """
                + selectDuplicateColumnsSql()
                + """
                        FROM forum_post p
                        ORDER BY p.is_pinned DESC, p.created_at DESC
                        """;

        List<ForumPost> out = new ArrayList<>();

        // try-with-resources ensures DB connection/statement/result are always closed.
        try (Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                out.add(mapRow(rs));
            }
        }
        return out;
    }

    // CREATE: inserts a new post row and returns generated id.
    public long insert(ForumPost p) throws SQLException {
        String normalizedTag = InputValidator.normalizeSingleTag(p.getTag());
        // Validate at repository boundary so all callers enforce the same rules.
        List<String> errors = InputValidator.validatePost(p.getTitle(), p.getContent(), normalizedTag);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(" | ", errors));
        }
        ensureDuplicateColumns();

        String sql = """
                INSERT INTO forum_post (author_id, title, content, tag, status, is_pinned, is_locked
                """
                + insertDuplicateColumnsSql()
                + """
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?
                """
                + insertDuplicateValuesSql()
                + """
                        )
                        """;

        try (Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, p.getAuthorId());
            ps.setString(2, p.getTitle());
            ps.setString(3, p.getContent());
            ps.setString(4, normalizedTag);
            ps.setString(5, p.getStatus());
            ps.setBoolean(6, p.isPinned());
            ps.setBoolean(7, p.isLocked());
            int index = 8;
            if (hasDuplicateScoreColumn) {
                ps.setDouble(index++, p.getDuplicateScore());
            }
            if (hasDuplicateOfPostIdColumn) {
                if (p.getDuplicateOfPostId() == null) {
                    ps.setNull(index++, Types.BIGINT);
                } else {
                    ps.setLong(index++, p.getDuplicateOfPostId());
                }
            }

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
        String normalizedTag = InputValidator.normalizeSingleTag(p.getTag());
        // Re-validate on update to prevent invalid edits from any UI path.
        List<String> errors = InputValidator.validatePost(p.getTitle(), p.getContent(), normalizedTag);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(" | ", errors));
        }

        ensureDuplicateColumns();
        String sql = """
                UPDATE forum_post
                SET title=?, content=?, tag=?, status=?, is_pinned=?, is_locked=?
                """
                + updateDuplicateColumnsSql()
                + """
                        , updated_at=CURRENT_TIMESTAMP
                        WHERE id=?
                        """;

        try (Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ensureDuplicateColumns(con);

            ps.setString(1, p.getTitle());
            ps.setString(2, p.getContent());
            ps.setString(3, normalizedTag);
            ps.setString(4, p.getStatus());
            ps.setBoolean(5, p.isPinned());
            ps.setBoolean(6, p.isLocked());
            int index = 7;
            if (hasDuplicateScoreColumn) {
                ps.setDouble(index++, p.getDuplicateScore());
            }
            if (hasDuplicateOfPostIdColumn) {
                if (p.getDuplicateOfPostId() == null) {
                    ps.setNull(index++, Types.BIGINT);
                } else {
                    ps.setLong(index++, p.getDuplicateOfPostId());
                }
            }
            ps.setLong(index, p.getId());

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
        ensureDuplicateColumns();
        String sql = """
                SELECT p.id, p.author_id, p.title, p.content, p.tag, p.status, p.is_pinned, p.is_locked, p.created_at,
                       (SELECT COUNT(*) FROM forum_interaction i WHERE i.target_type='POST' AND i.target_id = p.id AND i.interaction_type='LIKE') AS like_count,
                       (SELECT COUNT(*) FROM forum_interaction i WHERE i.target_type='POST' AND i.target_id = p.id AND i.interaction_type='SHARE') AS share_count
                """
                + selectDuplicateColumnsSql()
                + """
                        FROM forum_post p
                        WHERE p.status = 'APPROVED'
                        ORDER BY p.is_pinned DESC, p.created_at DESC
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
        ensureDuplicateColumns();
        String sql = """
                SELECT p.id, p.author_id, p.title, p.content, p.tag, p.status, p.is_pinned, p.is_locked, p.created_at,
                       (SELECT COUNT(*) FROM forum_interaction i WHERE i.target_type='POST' AND i.target_id = p.id AND i.interaction_type='LIKE') AS like_count,
                       (SELECT COUNT(*) FROM forum_interaction i WHERE i.target_type='POST' AND i.target_id = p.id AND i.interaction_type='SHARE') AS share_count
                """
                + selectDuplicateColumnsSql()
                + """
                        FROM forum_post p
                        WHERE p.author_id = ?
                        ORDER BY p.is_pinned DESC, p.created_at DESC
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

    public List<ForumPost> findLatest(int limit) throws SQLException {
        ensureDuplicateColumns();
        int safeLimit = Math.max(1, limit);
        String sql = """
                SELECT p.id, p.author_id, p.title, p.content, p.tag, p.status, p.is_pinned, p.is_locked, p.created_at,
                       (SELECT COUNT(*) FROM forum_interaction i WHERE i.target_type='POST' AND i.target_id = p.id AND i.interaction_type='LIKE') AS like_count,
                       (SELECT COUNT(*) FROM forum_interaction i WHERE i.target_type='POST' AND i.target_id = p.id AND i.interaction_type='SHARE') AS share_count
                """
                + selectDuplicateColumnsSql()
                + """
                        FROM forum_post p
                        ORDER BY p.created_at DESC
                        LIMIT ?
                        """;

        List<ForumPost> out = new ArrayList<>();
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

    public ForumPost findById(long postId) throws SQLException {
        ensureDuplicateColumns();
        String sql = """
                SELECT p.id, p.author_id, p.title, p.content, p.tag, p.status, p.is_pinned, p.is_locked, p.created_at,
                       (SELECT COUNT(*) FROM forum_interaction i WHERE i.target_type='POST' AND i.target_id = p.id AND i.interaction_type='LIKE') AS like_count,
                       (SELECT COUNT(*) FROM forum_interaction i WHERE i.target_type='POST' AND i.target_id = p.id AND i.interaction_type='SHARE') AS share_count
                """
                + selectDuplicateColumnsSql()
                + """
                        FROM forum_post p
                        WHERE p.id = ?
                        LIMIT 1
                        """;
        try (Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    private ForumPost mapRow(ResultSet rs) throws SQLException {
        // Single row mapper to keep query methods consistent.
        ForumPost p = new ForumPost();
        p.setId(rs.getLong("id"));
        p.setAuthorId(rs.getLong("author_id"));
        p.setTitle(rs.getString("title"));
        p.setContent(rs.getString("content"));
        p.setTag(rs.getString("tag"));
        p.setStatus(rs.getString("status"));
        p.setLikeCount(readOptionalInt(rs, "like_count", 0));
        p.setShareCount(readOptionalInt(rs, "share_count", 0));
        p.setDuplicateScore(readOptionalDouble(rs, "duplicate_score", 0.0));
        p.setDuplicateOfPostId(readOptionalLong(rs, "duplicate_of_post_id"));
        p.setPinned(rs.getBoolean("is_pinned"));
        p.setLocked(rs.getBoolean("is_locked"));

        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null)
            p.setCreatedAt(ts.toLocalDateTime());
        return p;
    }

    public void updateTag(long postId, String tag) throws SQLException {
        String normalized = InputValidator.normalizeSingleTag(tag);
        if (normalized == null || normalized.isBlank()) {
            normalized = "#General";
        }
        String sql = """
                UPDATE forum_post
                SET tag=?, updated_at=CURRENT_TIMESTAMP
                WHERE id=?
                """;
        try (Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, normalized);
            ps.setLong(2, postId);
            ps.executeUpdate();
        }
    }

    /** @deprecated use {@link #updateTag(long, String)} */
    @Deprecated
    public void updateCategory(long postId, String category) throws SQLException {
        updateTag(postId, category);
    }

    public void updateStatus(long postId, String status) throws SQLException {
        String normalized = normalizeModerationStatus(status);
        String sql = """
                UPDATE forum_post
                SET status=?, updated_at=CURRENT_TIMESTAMP
                WHERE id=?
                """;
        try (Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, normalized);
            ps.setLong(2, postId);
            ps.executeUpdate();
        }
    }

    private String selectDuplicateColumnsSql() {
        StringBuilder sb = new StringBuilder();
        if (hasDuplicateScoreColumn) {
            sb.append(", duplicate_score");
        }
        if (hasDuplicateOfPostIdColumn) {
            sb.append(", duplicate_of_post_id");
        }
        return sb.toString();
    }

    private String insertDuplicateColumnsSql() {
        StringBuilder sb = new StringBuilder();
        if (hasDuplicateScoreColumn) {
            sb.append(", duplicate_score");
        }
        if (hasDuplicateOfPostIdColumn) {
            sb.append(", duplicate_of_post_id");
        }
        return sb.toString();
    }

    private String insertDuplicateValuesSql() {
        StringBuilder sb = new StringBuilder();
        if (hasDuplicateScoreColumn) {
            sb.append(", ?");
        }
        if (hasDuplicateOfPostIdColumn) {
            sb.append(", ?");
        }
        return sb.toString();
    }

    private String updateDuplicateColumnsSql() {
        StringBuilder sb = new StringBuilder();
        if (hasDuplicateScoreColumn) {
            sb.append(", duplicate_score=?");
        }
        if (hasDuplicateOfPostIdColumn) {
            sb.append(", duplicate_of_post_id=?");
        }
        return sb.toString();
    }

    private void ensureDuplicateColumns() throws SQLException {
        if (duplicateColumnsDetected) {
            return;
        }
        try (Connection con = DB.getConnection()) {
            ensureDuplicateColumns(con);
        }
    }

    private synchronized void ensureDuplicateColumns(Connection con) throws SQLException {
        if (duplicateColumnsDetected) {
            return;
        }

        DatabaseMetaData metaData = con.getMetaData();
        boolean[] flags = readDuplicateColumns(metaData, con.getCatalog(), "forum_post");
        if (!flags[0] && !flags[1]) {
            flags = readDuplicateColumns(metaData, con.getCatalog(), "FORUM_POST");
        }

        boolean duplicateScore = flags[0];
        boolean duplicateOfPostId = flags[1];
        hasDuplicateScoreColumn = duplicateScore;
        hasDuplicateOfPostIdColumn = duplicateOfPostId;
        duplicateColumnsDetected = true;
    }

    private boolean[] readDuplicateColumns(DatabaseMetaData metaData, String catalog, String tableNamePattern)
            throws SQLException {
        boolean duplicateScore = false;
        boolean duplicateOfPostId = false;
        try (ResultSet columns = metaData.getColumns(catalog, null, tableNamePattern, null)) {
            while (columns.next()) {
                String name = columns.getString("COLUMN_NAME");
                if (name == null) {
                    continue;
                }
                String normalized = name.toLowerCase(Locale.ROOT);
                if ("duplicate_score".equals(normalized)) {
                    duplicateScore = true;
                } else if ("duplicate_of_post_id".equals(normalized)) {
                    duplicateOfPostId = true;
                }
            }
        }
        return new boolean[] { duplicateScore, duplicateOfPostId };
    }

    private double readOptionalDouble(ResultSet rs, String column, double fallback) {
        try {
            double value = rs.getDouble(column);
            return rs.wasNull() ? fallback : value;
        } catch (SQLException ignored) {
            return fallback;
        }
    }

    private Long readOptionalLong(ResultSet rs, String column) {
        try {
            long value = rs.getLong(column);
            return rs.wasNull() ? null : value;
        } catch (SQLException ignored) {
            return null;
        }
    }

    private int readOptionalInt(ResultSet rs, String column, int fallback) {
        try {
            int value = rs.getInt(column);
            return rs.wasNull() ? fallback : value;
        } catch (SQLException ignored) {
            return fallback;
        }
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
