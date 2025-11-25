package pl.hexmind.mindshaper.activities

import pl.hexmind.mindshaper.R
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.ranges.contains

class ThoughtGrowthStage(
    val level : Level,
    val ageInDays : Long
) {

    enum class Level(val icon: String, val labelResourceId: Int) {
        SEEDLING("🌱", R.string.common_thought_age_level_1),
        BUD("🌿", R.string.common_thought_age_level_2),
        SPROUT("🍀", R.string.common_thought_age_level_3),
        SAPLING("🌳", R.string.common_thought_age_level_4),
        TREE("\uD83C\uDFDD\uFE0F", R.string.common_thought_age_level_5)
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
