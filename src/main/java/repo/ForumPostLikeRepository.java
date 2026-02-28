package repo;

import java.sql.SQLException;
import model.InteractionType;
import model.TargetType;

/**
 * @deprecated Use {@link ForumPostInteractionRepository} instead (forum_interaction).
 */
@Deprecated
public class ForumPostLikeRepository {
    private final InteractionRepository interactionRepository = new InteractionRepository();

    public boolean toggleLike(long postId, long userId) throws SQLException {
        return interactionRepository.toggleInteraction(TargetType.POST, postId, userId, InteractionType.LIKE);
    }

    public boolean isLikedByUser(long postId, long userId) throws SQLException {
        return interactionRepository.hasInteraction(TargetType.POST, postId, userId, InteractionType.LIKE);
    }

    public int countLikes(long postId) throws SQLException {
        return interactionRepository.countInteractions(TargetType.POST, postId, InteractionType.LIKE);
    }
}
