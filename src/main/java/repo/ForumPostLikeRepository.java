package repo;

import util.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @deprecated Use {@link ForumPostInteractionRepository} instead (forum_post_interaction).
 */
@Deprecated
public class ForumPostLikeRepository {

    public boolean toggleLike(long postId, long userId) throws SQLException {
        if (isLikedByUser(postId, userId)) {
            long pid = postId;
            long uid = userId;
            removeLike(pid, uid);
            return false;
        }
        addLike(postId, userId);
        return true;
    }

    public boolean isLikedByUser(long postId, long userId) throws SQLException {
        return hasInteraction(postId, userId, "LIKE");
    }

    public int countLikes(long postId) throws SQLException {
        return countInteractions(postId, "LIKE");
    }

    private void addLike(long postId, long userId) throws SQLException {
        String sql = """
                INSERT IGNORE INTO forum_post_interaction (post_id, user_id, type)
                VALUES (?, ?, 'LIKE')
                """;
        try (Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, postId);
            ps.setLong(2, userId);
            ps.executeUpdate();
        }
    }

    private void removeLike(long postId, long userId) throws SQLException {
        String sql = """
                DELETE FROM forum_post_interaction
                WHERE post_id=? AND user_id=? AND type='LIKE'
                """;
        try (Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, postId);
            ps.setLong(2, userId);
            ps.executeUpdate();
        }
    }

    private boolean hasInteraction(long postId, long userId, String type) throws SQLException {
        String sql = """
                SELECT 1
                FROM forum_post_interaction
                WHERE post_id=? AND user_id=? AND type=?
                LIMIT 1
                """;
        try (Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, postId);
            ps.setLong(2, userId);
            ps.setString(3, type);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private int countInteractions(long postId, String type) throws SQLException {
        String sql = """
                SELECT COUNT(*) AS c
                FROM forum_post_interaction
                WHERE post_id=? AND type=?
                """;
        try (Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, postId);
            ps.setString(2, type);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("c") : 0;
            }
        }
    }
}
