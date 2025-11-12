package pl.hexmind.mindshaper

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import pl.hexmind.mindshaper.database.initialization.DatabaseInitializer
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pl.hexmind.mindshaper.database.initialization.DataSnapshotManager
import pl.hexmind.mindshaper.services.DomainIconsService
import pl.hexmind.mindshaper.services.PhasesService
import timber.log.Timber

@HiltAndroidApp
class ApplicationMain : Application() {

    @Inject
    lateinit var databaseInitializer: DatabaseInitializer

    @Inject
    lateinit var phasesService: PhasesService

    @Inject
    lateinit var domainIconsService: DomainIconsService

    @Inject
    lateinit var snapshotManager: DataSnapshotManager

    override fun onCreate() {
        super.onCreate()

        // Debug build - show logs
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Initialize database on app startup
        CoroutineScope(Dispatchers.IO).launch {
            databaseInitializer.initializeIfNeeded()
            domainIconsService.preloadAllIcons()
            snapshotManager.createSnapshot() // For preventing data loss
        }

        phasesService.saveAppLaunchTime()
    }
}