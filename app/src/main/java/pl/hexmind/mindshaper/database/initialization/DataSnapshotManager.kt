package pl.hexmind.mindshaper.database.initialization

import android.content.Context
import android.content.Intent
import android.os.Build
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
import pl.hexmind.mindshaper.database.models.IconEntity
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
            )

            val backupDir = getBackupDirectory()
            backupDir.mkdirs()

            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
                .format(Date())
            val snapshotFile = File(backupDir, "snapshot_v${snapshot.version}_$timestamp.json")

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

            // ! Insert in specific order: parent tables -> child tables
            database.withTransaction  {
                // 1. Tables with no foreign keys
                snapshot.domainIcons?.apply {
                    database.iconDAO().insertOrReplace(snapshot.domainIcons)
                    restoredCount++
                }

                // 2. Tables with foreign keys
                snapshot.domains?.apply {
                    database.domainDAO().insertOrReplace(snapshot.domains)
                    restoredCount++
                }

                snapshot.thoughts?.apply {
                    database.thoughtsDao().insertOrReplace(snapshot.thoughts)
                    restoredCount++
                }
            }

            return Result.success(restoredCount)
        }
        catch (e: Exception) {
            return Result.failure(e)
        }
    }

    fun listSnapshots(): List<SnapshotInfo> {
        val backupDir = getBackupDirectory()
        return backupDir.listFiles()
            ?.filter { it.extension == "json" }
            ?.sortedByDescending { it.lastModified() }
            ?.map { file ->
                SnapshotInfo(
                    file = file,
                    name = file.name,
                    size = file.length(),
                    date = Date(file.lastModified())
                )
            } ?: emptyList()
    }

    private fun getBackupDirectory(): File {
        return File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "mindshaper_backup"
        )
    }

    fun checkAndRequestPermissions(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = "package:${context.packageName}".toUri()
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // WAŻNE dla Context
                }
                context.startActivity(intent)
                return false
            }
            return true
        } else {
            throw IllegalStateException("Need Activity reference for Android < 11")
        }
    }
}

data class DatabaseSnapshot(
    val version: Int,
    val timestamp: Long,

    val thoughts: List<ThoughtEntity>?,
    val domains: List<DomainEntity>?,
    val domainIcons: List<IconEntity>?,
)

data class SnapshotInfo(
    val file: File,
    val name: String,
    val size: Long,
    val date: Date
)