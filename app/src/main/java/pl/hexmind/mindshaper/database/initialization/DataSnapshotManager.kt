package pl.hexmind.mindshaper.database.initialization

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.provider.Settings
import androidx.core.net.toUri
import androidx.room.withTransaction
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import pl.hexmind.mindshaper.database.AppDatabase
import pl.hexmind.mindshaper.database.models.DomainEntity
import pl.hexmind.mindshaper.database.models.GoalEntity
import pl.hexmind.mindshaper.database.models.GuidelineEntity
import pl.hexmind.mindshaper.database.models.IconEntity
import pl.hexmind.mindshaper.database.models.PathEntity
import pl.hexmind.mindshaper.database.models.PathStepEntity
import pl.hexmind.mindshaper.database.models.ThoughtEntity
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for saving all DB records as .json file in device memory and then loading it again to DB.
 * Snapshot policy: max 1 per day + overriding file
 */
@Singleton
class DataSnapshotManager @Inject constructor(
    private val database: AppDatabase
) {
    private val gson = GsonBuilder()
        // ! Adapter for Instant type fields
        .registerTypeAdapter(Instant::class.java, JsonSerializer<Instant> { src, _, _ ->
            JsonPrimitive(src.toString())
        })
        .registerTypeAdapter(Instant::class.java, JsonDeserializer { json, _, _ ->
            Instant.parse(json.asString)
        })
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        .create()

    suspend fun createSnapshot(): Result<File> {
        try {
            // ! Download all data from all tables
            val snapshot = DatabaseSnapshot(
                version = database.openHelper.readableDatabase.version,
                timestamp = System.currentTimeMillis(),
                thoughts = database.thoughtsDao().getAllThoughts(),
                domains = database.domainDAO().getAllDomains(),
                domainIcons = database.iconDAO().getAllIcons(),
                goals = database.goalDao().getAllGoals(),
                guidelines = database.guidelineDao().getAllGuidelines(),
                paths = database.pathDao().getAllPaths(),
                pathSteps = database.pathStepDao().getAllSteps(),
            )

            val backupDir = getBackupDirectory()
            backupDir.mkdirs()

            // ! Daily snapshot: filename is date-only → same file is overwritten each day on each launch
            val dateStamp = SimpleDateFormat("dd_MM_yyyy", Locale.getDefault())
                .format(Date())
            val snapshotFile = File(backupDir, "mindshaper_v${snapshot.version}_$dateStamp.json")

            snapshotFile.writeText(gson.toJson(snapshot))

            return Result.success(snapshotFile)
        }
        catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun restoreSnapshot(fileName: String, context: Context): Result<Int> {
        try {
            checkAndRequestPermissions(context) // ! Needed for granting read permissions
            val snapshotFile = File(getBackupDirectory(), fileName)
            val json = snapshotFile.readText()
            val snapshot = gson.fromJson(json, DatabaseSnapshot::class.java)

            var restoredCount = 0

            // ! Insert in specific order: PARENT tables first -> then CHILD tables (FK constraints)
            database.withTransaction  {

                // 1. PARENTS tables with no foreign keys
                snapshot.domainIcons?.apply {
                    database.iconDAO().clearAll()
                    database.iconDAO().insertOrReplace(this)
                    restoredCount++
                }

                snapshot.goals?.apply {
                    database.goalDao().clearAll()
                    database.goalDao().insertOrReplace(this)
                    restoredCount++
                }

                snapshot.paths?.apply {
                    database.pathDao().clearAll()
                    database.pathDao().insertOrReplace(this)
                    restoredCount++
                }

                // 2. CHILDREN tables with foreign keys
                snapshot.domains?.apply {
                    database.domainDAO().clearAll()
                    database.domainDAO().insertOrReplace(this)
                    restoredCount++
                }

                snapshot.guidelines?.apply {
                    database.guidelineDao().clearAll()
                    database.guidelineDao().insertOrReplace(this)
                    restoredCount++
                }

                snapshot.pathSteps?.apply {
                    database.pathStepDao().clearAll()
                    database.pathStepDao().insertOrReplace(this)
                    restoredCount++
                }

                snapshot.thoughts?.apply {
                    database.thoughtsDao().clearAll()
                    database.thoughtsDao().insertOrReplace(this)
                    restoredCount++
                }
            }

            return Result.success(restoredCount)
        }
        catch (e: Exception) {
            return Result.failure(e)
        }
    }

    private fun getBackupDirectory(): File {
        return File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "mindshaper_backup"
        )
    }

    fun checkAndRequestPermissions(context: Context): Boolean {
        // Android 11+
        if (!Environment.isExternalStorageManager()) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = "package:${context.packageName}".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // important for Context
            }
            context.startActivity(intent)
            return false
        }
        return true
    }

    fun getSnapshotStats(): SnapshotStats {
        val files = getBackupDirectory().listFiles { f -> f.extension == "json" } ?: emptyArray()
        val totalBytes = files.sumOf { it.length() }
        return SnapshotStats(
            count = files.size,
            totalSizeMb = totalBytes / (1024f * 1024f)
        )
    }
}

data class DatabaseSnapshot(
    val version: Int,
    val timestamp: Long,

    val thoughts: List<ThoughtEntity>?,
    val domains: List<DomainEntity>?,
    val domainIcons: List<IconEntity>?,
    val goals: List<GoalEntity>?,
    val guidelines: List<GuidelineEntity>?,
    val paths: List<PathEntity>?,
    val pathSteps: List<PathStepEntity>?,
)

data class SnapshotStats(
    val count: Int,
    val totalSizeMb: Float
)