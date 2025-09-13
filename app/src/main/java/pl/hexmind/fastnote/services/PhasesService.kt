package pl.hexmind.fastnote.services

import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhasesService @Inject constructor(
    private val appStorage : AppSettingsStorage
){

    fun saveAppLaunchTime(){
        val today: LocalDate = LocalDate.now()
        appStorage.setAppLaunchDate(today)
    }
}