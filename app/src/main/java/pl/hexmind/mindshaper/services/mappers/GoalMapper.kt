package pl.hexmind.mindshaper.services.mappers

import pl.hexmind.mindshaper.database.models.GoalEntity
import pl.hexmind.mindshaper.database.models.GoalWithSteps
import pl.hexmind.mindshaper.database.models.StepEntity
import pl.hexmind.mindshaper.services.dto.GoalDTO
import pl.hexmind.mindshaper.services.dto.StepDTO

/**
 * Manual mapper for Goal/Step entities <-> DTOs.
 *
 * ! Not using MapStruct here because it can't auto-map 1:N relations (Goal -> Steps).
 * TODO: standardize approach and library with other mappers
 */
object GoalMapper {

    fun entityToDTO(row: GoalWithSteps): GoalDTO =
        GoalDTO(
            id             = row.goal.id,
            description    = row.goal.description,
            importance     = row.goal.importance,
            status         = row.goal.status,
            lastModifiedAt = row.goal.lastModifiedAt,
            steps          = row.steps
                .sortedBy { it.position }
                .map { stepEntityToDTO(it) }
        )

    fun goalEntityToDTO(entity: GoalEntity): GoalDTO =
        GoalDTO(
            id             = entity.id,
            description    = entity.description,
            importance     = entity.importance,
            status         = entity.status,
            lastModifiedAt = entity.lastModifiedAt
        )

    fun dtoToEntity(dto: GoalDTO): GoalEntity =
        GoalEntity(
            id             = dto.id,
            description    = dto.description,
            importance     = dto.importance,
            status         = dto.status,
            lastModifiedAt = dto.lastModifiedAt
        )

    fun stepEntityToDTO(entity: StepEntity): StepDTO =
        StepDTO(
            id                 = entity.id,
            goalId             = entity.goalId,
            description        = entity.description,
            position           = entity.position,
            currentRepetitions = entity.currentRepetitions,
            maxRepetitions     = entity.maxRepetitions,
            thoughtId          = entity.thoughtId,
            reminderTime       = entity.reminderTime,
            reminderDays       = entity.reminderDays,
            calendarEventId    = entity.calendarEventId
        )

    fun stepDTOToEntity(dto: StepDTO): StepEntity =
        StepEntity(
            id                 = dto.id,
            goalId             = dto.goalId,
            description        = dto.description,
            position           = dto.position,
            currentRepetitions = dto.currentRepetitions,
            maxRepetitions     = dto.maxRepetitions,
            thoughtId          = dto.thoughtId,
            reminderTime       = dto.reminderTime,
            reminderDays       = dto.reminderDays,
            calendarEventId    = dto.calendarEventId
        )
}
