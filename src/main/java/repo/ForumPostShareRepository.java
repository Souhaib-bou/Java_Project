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
public class ForumPostShareRepository {

    public boolean recordShare(long postId, long userId) throws SQLException {
        String sql = """
                INSERT IGNORE INTO forum_post_interaction (post_id, user_id, type)
                VALUES (?, ?, 'SHARE')
                """;
        try (Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, postId);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public int countShares(long postId) throws SQLException {
        String sql = """
                SELECT COUNT(*) AS c
                FROM forum_post_interaction
                WHERE post_id = ? AND type='SHARE'
                """;
        try (Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("c") : 0;
            }
        }
    }
}
