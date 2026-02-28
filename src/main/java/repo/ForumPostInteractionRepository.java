package repo;

import java.sql.SQLException;
import model.InteractionType;
import model.TargetType;

/**
 * JDBC repository for post interactions (LIKE/SHARE).
 * Delegates to unified forum_interaction target rows.
 */
public class ForumPostInteractionRepository {
    private final InteractionRepository interactionRepository = new InteractionRepository();

    public boolean addLike(long postId, long userId) throws SQLException {
        if (isLiked(postId, userId)) {
            return false;
        }
        return interactionRepository.toggleInteraction(TargetType.POST, postId, userId, InteractionType.LIKE);
    }

    public void removeLike(long postId, long userId) throws SQLException {
        if (!isLiked(postId, userId)) {
            return;
        }
        interactionRepository.toggleInteraction(TargetType.POST, postId, userId, InteractionType.LIKE);
    }

    public boolean isLiked(long postId, long userId) throws SQLException {
        return interactionRepository.hasInteraction(TargetType.POST, postId, userId, InteractionType.LIKE);
    }

    public int countLikes(long postId) throws SQLException {
        return interactionRepository.countInteractions(TargetType.POST, postId, InteractionType.LIKE);
    }

    public boolean addShare(long postId, long userId) throws SQLException {
        if (isShared(postId, userId)) {
            return false;
        }
        return interactionRepository.toggleInteraction(TargetType.POST, postId, userId, InteractionType.SHARE);
    }

    public boolean isShared(long postId, long userId) throws SQLException {
        return interactionRepository.hasInteraction(TargetType.POST, postId, userId, InteractionType.SHARE);
    }

    public int countShares(long postId) throws SQLException {
        return interactionRepository.countInteractions(TargetType.POST, postId, InteractionType.SHARE);
    }
}
