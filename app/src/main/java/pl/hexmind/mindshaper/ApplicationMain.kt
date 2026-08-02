package pl.hexmind.mindshaper

import android.app.Application
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import dagger.hilt.android.HiltAndroidApp
import pl.hexmind.mindshaper.activities.capture.CaptureActivity
import pl.hexmind.mindshaper.activities.home.HomeActivity
import pl.hexmind.mindshaper.database.initialization.DatabaseInitializer
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pl.hexmind.mindshaper.database.initialization.DataSnapshotManager
import pl.hexmind.mindshaper.services.AppSettingsStorage
import pl.hexmind.mindshaper.services.DomainIconsService
import timber.log.Timber

@HiltAndroidApp
class ApplicationMain : Application() {

    @Inject
    lateinit var databaseInitializer: DatabaseInitializer

    @Inject
    lateinit var domainIconsService: DomainIconsService

    @Inject
    lateinit var snapshotManager: DataSnapshotManager

    @Inject
    lateinit var appSettingsStorage: AppSettingsStorage

    override fun onCreate() {
        super.onCreate()

        // Debug build - show logs
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        registerNewThoughtShortcut()

        CoroutineScope(Dispatchers.IO).launch {
            databaseInitializer.initializeIfNeeded()
            domainIconsService.preloadAllIcons() // loading icons resources from /drawable

            if (appSettingsStorage.isBackupEnabled()) {
                snapshotManager.createSnapshot() // For preventing data loss
            }
        }
    }

    // ! dynamic shortcut instead of static res/xml/shortcuts.xml - raw XML cannot expand ${applicationId}, so it misses the .debug build
    private fun registerNewThoughtShortcut() {
        val homeIntent = Intent(this, HomeActivity::class.java).apply {
            action = Intent.ACTION_MAIN
        }

        val captureIntent = Intent(this, CaptureActivity::class.java).apply {
            action = Intent.ACTION_VIEW
        }

        val shortcut = ShortcutInfoCompat.Builder(this, "new_thought")
            .setShortLabel(getString(R.string.shortcut_new_thought_short))
            .setLongLabel(getString(R.string.shortcut_new_thought_long))
            .setIcon(IconCompat.createWithResource(this, R.drawable.ic_shortcut_new_thought))
            // Home first, Capture last: the launcher stacks earlier intents behind the last one,
            // so BACK from a cold start lands on Home instead of leaving the app
            .setIntents(arrayOf(homeIntent, captureIntent))
            .build()

        ShortcutManagerCompat.pushDynamicShortcut(this, shortcut)
    }
}