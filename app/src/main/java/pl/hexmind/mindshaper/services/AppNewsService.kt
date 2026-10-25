package pl.hexmind.mindshaper.services

import pl.hexmind.mindshaper.common.dormant.ThoughtState
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import javax.inject.Inject

/**
 * App news on Home: which entries there are and which of them the user has not seen yet.
 * An entry is read once the user has opened it in the news bottom sheet or marked it as read;
 * marking it as unread brings it back.
 *
 * Near-dormant entry: unread while it holds a thought not there when it was last read -
 * a thought that newly comes close to falling asleep makes it unread again.
 */
class AppNewsService @Inject constructor(
    private val appSettingsStorage: AppSettingsStorage,
    private val thoughtStatusService: ThoughtStatusService
) {
    // Without the dormant mode every thought is active - no near-dormant entry
    fun nearDormantThoughtIds(thoughts: List<ThoughtDTO>): Set<String> =
        thoughts
            .filter { thought -> thoughtStatusService.computeState(thought) == ThoughtState.WARNING }
            .mapNotNull { thought -> thought.id?.toString() }
            .toSet()

    fun countUnread(thoughts: List<ThoughtDTO>): Int =
        if (isNearDormantUnread(thoughts)) 1 else 0

    fun isNearDormantUnread(thoughts: List<ThoughtDTO>): Boolean =
        !appSettingsStorage.getNewsNearDormantReadIds().containsAll(nearDormantThoughtIds(thoughts))

    // ! Replaces the stored ids - a thought that left the entry and comes back later is news again
    fun markNearDormantRead(thoughts: List<ThoughtDTO>) {
        appSettingsStorage.setNewsNearDormantReadIds(nearDormantThoughtIds(thoughts))
    }

    // No stored ids = every thought in the entry is unseen again
    fun markNearDormantUnread() {
        appSettingsStorage.setNewsNearDormantReadIds(emptySet())
    }
}
