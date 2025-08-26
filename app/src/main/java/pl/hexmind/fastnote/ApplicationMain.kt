package pl.hexmind.fastnote

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import pl.hexmind.fastnote.data.DatabaseInitializer
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltAndroidApp
class ApplicationMain : Application() {

    @Inject
    lateinit var databaseInitializer: DatabaseInitializer

    override fun onCreate() {
        super.onCreate()

        // Initialize database on app startup
        CoroutineScope(Dispatchers.IO).launch {
            databaseInitializer.initializeIfNeeded()
        }
    }
}