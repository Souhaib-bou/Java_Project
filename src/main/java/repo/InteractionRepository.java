package repo;

import model.InteractionType;
import model.TargetType;
import util.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Unified interaction repository for posts and comments.
 * Backed by forum_interaction with uniqueness on target+user+interaction type.
 */
public class InteractionRepository {

    public boolean hasInteraction(TargetType targetType, long targetId, long userId, InteractionType interactionType)
            throws SQLException {
        String sql = """
                SELECT 1
                FROM forum_interaction
                WHERE target_type=? AND target_id=? AND user_id=? AND interaction_type=?
                LIMIT 1
                """;
        try (Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, targetType.name());
            ps.setLong(2, targetId);
            ps.setLong(3, userId);
            ps.setString(4, interactionType.name());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int countInteractions(TargetType targetType, long targetId, InteractionType interactionType)
            throws SQLException {
        String sql = """
                SELECT COUNT(*) AS c
                FROM forum_interaction
                WHERE target_type=? AND target_id=? AND interaction_type=?
                """;
        try (Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, targetType.name());
            ps.setLong(2, targetId);
            ps.setString(3, interactionType.name());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("c") : 0;
            }
        }
    }

    public boolean toggleInteraction(TargetType targetType, long targetId, long userId, InteractionType interactionType)
            throws SQLException {
        String deleteSql = """
                DELETE FROM forum_interaction
                WHERE target_type=? AND target_id=? AND user_id=? AND interaction_type=?
                """;
        String insertSql = """
                INSERT INTO forum_interaction (target_type, target_id, user_id, interaction_type)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection con = DB.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement del = con.prepareStatement(deleteSql)) {
                    del.setString(1, targetType.name());
                    del.setLong(2, targetId);
                    del.setLong(3, userId);
                    del.setString(4, interactionType.name());
                    int removed = del.executeUpdate();
                    if (removed > 0) {
                        con.commit();
                        return false;
                    }
                }
                try (PreparedStatement ins = con.prepareStatement(insertSql)) {
                    ins.setString(1, targetType.name());
                    ins.setLong(2, targetId);
                    ins.setLong(3, userId);
                    ins.setString(4, interactionType.name());
                    ins.executeUpdate();
                }
                con.commit();
                return true;
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    public Map<Long, Integer> countLikesForComments(long postId) throws SQLException {
        String sql = """
                SELECT c.id AS comment_id, COUNT(i.id) AS like_count
                FROM forum_comment c
                LEFT JOIN forum_interaction i
                  ON i.target_type='COMMENT'
                 AND i.interaction_type='LIKE'
                 AND i.target_id=c.id
                WHERE c.post_id=?
                GROUP BY c.id
                """;
        Map<Long, Integer> out = new HashMap<>();
        try (Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.put(rs.getLong("comment_id"), rs.getInt("like_count"));
                }
            }
        }
        return out;
    }

    public Set<Long> likedCommentIdsByUser(long postId, long userId) throws SQLException {
        String sql = """
                SELECT i.target_id
                FROM forum_interaction i
                JOIN forum_comment c ON c.id=i.target_id
                WHERE c.post_id=?
                  AND i.user_id=?
                  AND i.target_type='COMMENT'
                  AND i.interaction_type='LIKE'
                """;
        Set<Long> out = new HashSet<>();
        try (Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, postId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(rs.getLong("target_id"));
                }
            }
        }
        return out;
    }
}

