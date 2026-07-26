package pl.hexmind.mindshaper.database.repositories

import pl.hexmind.mindshaper.database.models.PathEntity
import pl.hexmind.mindshaper.database.models.PathStepEntity
import pl.hexmind.mindshaper.database.models.PathWithSteps
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PathRepository @Inject constructor(
    private val pathDAO: PathDAO,
    private val pathStepDAO: PathStepDAO
) {

    suspend fun getActiveTodayWithSteps(today: Long): List<PathWithSteps> =
        pathDAO.getActiveTodayWithSteps(today)

    suspend fun getRandomPickable(today: Long): PathEntity? =
        pathDAO.getRandomPickable(today)

    suspend fun getPathByKey(pathKey: String): PathEntity? =
        pathDAO.getByKey(pathKey)

    suspend fun updatePath(path: PathEntity) =
        pathDAO.update(path)

    suspend fun getAllPaths(): List<PathEntity> =
        pathDAO.getAllPaths()

    suspend fun countSteps(pathKey: String): Int =
        pathStepDAO.countByPath(pathKey)

    suspend fun insertPaths(paths: List<PathEntity>) =
        pathDAO.insertAll(paths)

    suspend fun insertSteps(steps: List<PathStepEntity>) =
        pathStepDAO.insertAll(steps)

    suspend fun pathCount(): Int =
        pathDAO.count()
}