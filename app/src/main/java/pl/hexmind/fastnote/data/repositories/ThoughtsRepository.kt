package pl.hexmind.fastnote.data.repositories

import pl.hexmind.fastnote.data.models.AreaIdentifier
import pl.hexmind.fastnote.data.models.AreaEntity
import pl.hexmind.fastnote.data.models.ThoughtEntity

class ThoughtsRepository (
    private val thoughtsDAO: ThoughtsDAO,
    private val areaDAO: AreaDAO) {

    fun getAllThoughtsWithArea() = thoughtsDAO.getAllThoughtsWithArea()

    suspend fun createThoughtWithExistingArea(
        essence: String,
        areaId: AreaIdentifier,
        priority: Int = 3,
        thread : String? = null
    ): Long {
        val thought = ThoughtEntity(
            essence = essence,
            areaId = areaId.value,
            thread = thread,
            priority = priority
        )
        return thoughtsDAO.insert(thought)
    }
}