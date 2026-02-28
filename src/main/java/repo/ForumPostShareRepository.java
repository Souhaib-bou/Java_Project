package repo;

import java.sql.SQLException;
import model.InteractionType;
import model.TargetType;

/**
 * @deprecated Use {@link ForumPostInteractionRepository} instead (forum_interaction).
 */
@Deprecated
public class ForumPostShareRepository {
    private final InteractionRepository interactionRepository = new InteractionRepository();

    public boolean recordShare(long postId, long userId) throws SQLException {
        if (interactionRepository.hasInteraction(TargetType.POST, postId, userId, InteractionType.SHARE)) {
            return false;
        }
        return interactionRepository.toggleInteraction(TargetType.POST, postId, userId, InteractionType.SHARE);
    }

    public int countShares(long postId) throws SQLException {
        return interactionRepository.countInteractions(TargetType.POST, postId, InteractionType.SHARE);
    }
}
