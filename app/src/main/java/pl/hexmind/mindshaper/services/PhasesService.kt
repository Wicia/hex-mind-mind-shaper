package pl.hexmind.mindshaper.services

import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

// TODO: Is it needed?
@Singleton
class PhasesService @Inject constructor(
    private val appStorage : AppSettingsStorage
){

    // TODO: probably need to change LocalDate -> Instant (must be common)
    fun saveAppLaunchTime(){
        val today: LocalDate = LocalDate.now()
        appStorage.setAppLaunchDate(today)
    }
}