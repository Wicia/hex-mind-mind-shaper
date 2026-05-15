package pl.hexmind.mindshaper.services.mappers

import pl.hexmind.mindshaper.database.models.GoalEntity
import pl.hexmind.mindshaper.database.models.GoalWithGuidelines
import pl.hexmind.mindshaper.database.models.GuidelineEntity
import pl.hexmind.mindshaper.services.dto.GoalDTO
import pl.hexmind.mindshaper.services.dto.GuidelineDTO

/**
 * Manual mapper for Goal/Guideline entities ↔ DTOs.
 *
 * ! Not using MapStruct here because it can't auto-map 1:N relations (Goal -> Guidelines).
 * TODO: standardize approach and library wih other mappers
 */
object GoalMapper {

    fun entityToDTO(row: GoalWithGuidelines): GoalDTO =
        GoalDTO(
            id             = row.goal.id,
            description    = row.goal.description,
            importance     = row.goal.importance,
            lastModifiedAt = row.goal.lastModifiedAt,
            guidelines     = row.guidelines
                .sortedBy { it.position }
                .map { guidelineEntityToDTO(it) }
        )

    fun goalEntityToDTO(entity: GoalEntity): GoalDTO =
        GoalDTO(
            id             = entity.id,
            description    = entity.description,
            importance     = entity.importance,
            lastModifiedAt = entity.lastModifiedAt
        )

    fun dtoToEntity(dto: GoalDTO): GoalEntity =
        GoalEntity(
            id             = dto.id,
            description    = dto.description,
            importance     = dto.importance,
            lastModifiedAt = dto.lastModifiedAt
        )

    fun guidelineEntityToDTO(entity: GuidelineEntity): GuidelineDTO =
        GuidelineDTO(
            id          = entity.id,
            goalId      = entity.goalId,
            description = entity.description,
            isDone      = entity.isDone,
            position    = entity.position
        )

    fun guidelineDTOToEntity(dto: GuidelineDTO): GuidelineEntity =
        GuidelineEntity(
            id          = dto.id,
            goalId      = dto.goalId,
            description = dto.description,
            isDone      = dto.isDone,
            position    = dto.position
        )
}
