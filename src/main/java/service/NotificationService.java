package service;

import model.Notification;
import model.ForumPost;
import repo.NotificationRepository;
import repo.ForumPostRepository;
import repo.UserRepository;
import util.DebugLog;

/**
 * Builds plain-English forum notifications and persists them.
 */
public class NotificationService {
    private static final String TYPE_POST_LIKED = "POST_LIKED";
    private static final String TYPE_POST_COMMENTED = "POST_COMMENTED";
    private static final String TYPE_POST_APPROVED = "POST_APPROVED";
    private static final String TYPE_POST_REJECTED = "POST_REJECTED";
    private static final String TYPE_POST_SHARED = "POST_SHARED";
    private static final String TYPE_COMMENT_APPROVED = "COMMENT_APPROVED";
    private static final String TYPE_COMMENT_REJECTED = "COMMENT_REJECTED";

    private final NotificationRepository notificationRepo;
    private final UserRepository userRepo;
    private final ForumPostRepository postRepo;

    public NotificationService() {
        this(new NotificationRepository(), new UserRepository(), new ForumPostRepository());
    }

    public NotificationService(NotificationRepository notificationRepo, UserRepository userRepo) {
        this(notificationRepo, userRepo, new ForumPostRepository());
    }

    public NotificationService(NotificationRepository notificationRepo, UserRepository userRepo,
            ForumPostRepository postRepo) {
        this.notificationRepo = notificationRepo;
        this.userRepo = userRepo;
        this.postRepo = postRepo;
    }

    public void notifyPostLiked(long postId, long actorUserId, long receiverUserId) {
        if (shouldSkip(actorUserId, receiverUserId)) {
            return;
        }
        String actorName = safeName(actorUserId);
        createSafe(build(receiverUserId, actorUserId, TYPE_POST_LIKED,
                actorName + " liked your post (#" + postId + ").", postId, null));
    }

    public void notifyPostCommented(long postId, long commentId, long actorUserId, long receiverUserId) {
        if (shouldSkip(actorUserId, receiverUserId)) {
            return;
        }
        String actorName = safeName(actorUserId);
        createSafe(build(receiverUserId, actorUserId, TYPE_POST_COMMENTED,
                actorName + " commented on your post (#" + postId + ").", postId, commentId));
    }

    public void notifyPostApproved(long postId, long receiverUserId) {
        createSafe(build(receiverUserId, null, TYPE_POST_APPROVED,
                "Your post (#" + postId + ") was approved by admin.", postId, null));
    }

    public void notifyPostRejected(long postId, long receiverUserId) {
        createSafe(build(receiverUserId, null, TYPE_POST_REJECTED,
                "Your post (#" + postId + ") was rejected by admin.", postId, null));
    }

    public void notifyPostShared(long postId, long actorUserId, long receiverUserId) {
        if (shouldSkip(actorUserId, receiverUserId)) {
            return;
        }
        String actorName = safeName(actorUserId);
        String postTitle = safePostTitle(postId);
        createSafe(build(receiverUserId, actorUserId, TYPE_POST_SHARED,
                actorName + " shared your post: " + postTitle, postId, null));
    }

    public void notifyCommentApproved(long postId, long commentId, long receiverUserId) {
        createSafe(build(receiverUserId, null, TYPE_COMMENT_APPROVED,
                "Your comment (#" + commentId + ") on post #" + postId + " was approved by admin.", postId, commentId));
    }

    public void notifyCommentRejected(long postId, long commentId, long receiverUserId) {
        createSafe(build(receiverUserId, null, TYPE_COMMENT_REJECTED,
                "Your comment (#" + commentId + ") on post #" + postId + " was rejected by admin.", postId, commentId));
    }

    private Notification build(long receiverUserId, Long actorUserId, String type, String message, Long postId,
            Long commentId) {
        Notification n = new Notification();
        n.setRecipientUserId(receiverUserId);
        n.setActorUserId(actorUserId);
        n.setType(type);
        n.setMessage(trimMessage(message));
        n.setPostId(postId);
        n.setCommentId(commentId);
        n.setRead(false);
        return n;
    }

    private boolean shouldSkip(long actorUserId, long receiverUserId) {
        return actorUserId <= 0 || receiverUserId <= 0 || actorUserId == receiverUserId;
    }

    private String safeName(long actorUserId) {
        String name = userRepo.getDisplayNameById(actorUserId);
        if (name == null || name.isBlank()) {
            return "Someone";
        }
        return name.trim();
    }

    private String trimMessage(String message) {
        String safe = message == null ? "" : message.trim();
        if (safe.length() <= 255) {
            return safe;
        }
        return safe.substring(0, 252) + "...";
    }

    private String safePostTitle(long postId) {
        try {
            ForumPost p = postRepo.findById(postId);
            if (p != null && p.getTitle() != null && !p.getTitle().isBlank()) {
                return "'" + p.getTitle().trim() + "'";
            }
        } catch (Exception ex) {
            DebugLog.error("NotificationService", "Failed loading post title for notification", ex);
        }
        return "post #" + postId;
    }

    private void createSafe(Notification n) {
        try {
            notificationRepo.create(n);
        } catch (Exception ex) {
            DebugLog.error("NotificationService", "Failed creating notification", ex);
        }
    }
}
