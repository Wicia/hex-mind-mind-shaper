package pl.hexmind.mindshaper.activities

import androidx.annotation.StringRes
import pl.hexmind.mindshaper.R
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Class for dedicated feature (@MC) :)
 */
class ThoughtGrowthStage(
    val level : Level,
    val ageInDays : Long
) {
    enum class Level(@StringRes val iconResId: Int) {
        SEEDLING(R.string.common_thought_age_level_1_icon),
        BUD(R.string.common_thought_age_level_2_icon),
        SPROUT(R.string.common_thought_age_level_3_icon),
        SAPLING(R.string.common_thought_age_level_4_icon),
        TREE(R.string.common_thought_age_level_5_icon)
    }

    companion object Creator {
        fun newThoughtGrowthStage(createdAt: Instant): ThoughtGrowthStage {
            val days = getAgeInDays(createdAt)
            val weeksApprox = days / 7
            val monthsApprox = days / 30

            val level =  when {
                days <= 1 -> Level.SEEDLING
                days in 2..7 -> Level.BUD
                weeksApprox in 1..4 -> Level.SPROUT
                monthsApprox in 1..3 -> Level.SAPLING
                else -> Level.TREE
            }

            return ThoughtGrowthStage(level, days)
        }

        fun getAgeInDays(createdAt: Instant): Long {
            val now = Instant.now()
            return ChronoUnit.DAYS.between(createdAt, now)
        }
    }
}
