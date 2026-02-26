package repo;

import model.Notification;
import util.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC repository for forum notifications.
 */
public class NotificationRepository {

    public List<Notification> findLatestForUser(long recipientUserId, int limit) throws Exception {
        int safeLimit = Math.max(1, limit);
        String sql = """
                SELECT id, recipient_user_id, actor_user_id, type, message, post_id, comment_id, is_read, created_at
                FROM forum_notification
                WHERE recipient_user_id = ?
                ORDER BY created_at DESC
                LIMIT ?
                """;
        List<Notification> out = new ArrayList<>();
        try (Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, recipientUserId);
            ps.setInt(2, safeLimit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(mapRow(rs));
                }
            }
        }
        return out;
    }

    public int countUnread(long recipientUserId) throws Exception {
        String sql = """
                SELECT COUNT(*) AS c
                FROM forum_notification
                WHERE recipient_user_id = ? AND is_read = 0
                """;
        try (Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, recipientUserId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("c") : 0;
            }
        }
    }

    public void markRead(long notificationId, long recipientUserId) throws Exception {
        String sql = """
                UPDATE forum_notification
                SET is_read = 1
                WHERE id = ? AND recipient_user_id = ?
                """;
        try (Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, notificationId);
            ps.setLong(2, recipientUserId);
            ps.executeUpdate();
        }
    }

    public void markAllRead(long recipientUserId) throws Exception {
        String sql = """
                UPDATE forum_notification
                SET is_read = 1
                WHERE recipient_user_id = ?
                """;
        try (Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, recipientUserId);
            ps.executeUpdate();
        }
    }

    public void create(Notification n) throws Exception {
        if (n == null) {
            return;
        }
        String sql = """
                INSERT INTO forum_notification (
                    recipient_user_id, actor_user_id, type, message, post_id, comment_id, is_read
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, n.getRecipientUserId());
            if (n.getActorUserId() == null) {
                ps.setNull(2, Types.BIGINT);
            } else {
                ps.setLong(2, n.getActorUserId());
            }
            ps.setString(3, n.getType());
            ps.setString(4, n.getMessage());
            if (n.getPostId() == null) {
                ps.setNull(5, Types.BIGINT);
            } else {
                ps.setLong(5, n.getPostId());
            }
            if (n.getCommentId() == null) {
                ps.setNull(6, Types.BIGINT);
            } else {
                ps.setLong(6, n.getCommentId());
            }
            ps.setBoolean(7, n.isRead());
            ps.executeUpdate();
        }
    }

    private Notification mapRow(ResultSet rs) throws Exception {
        Notification n = new Notification();
        n.setId(rs.getLong("id"));
        n.setRecipientUserId(rs.getLong("recipient_user_id"));

        long actor = rs.getLong("actor_user_id");
        n.setActorUserId(rs.wasNull() ? null : actor);
        n.setType(rs.getString("type"));
        n.setMessage(rs.getString("message"));

        long postId = rs.getLong("post_id");
        n.setPostId(rs.wasNull() ? null : postId);

        long commentId = rs.getLong("comment_id");
        n.setCommentId(rs.wasNull() ? null : commentId);

        n.setRead(rs.getBoolean("is_read"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            n.setCreatedAt(createdAt.toLocalDateTime());
        }
        return n;
    }
}
