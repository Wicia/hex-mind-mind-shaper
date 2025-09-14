package pl.hexmind.mindshaper.services

import android.content.Context
import pl.hexmind.mindshaper.R

object GreetingsService {

    fun getGreetingsString(context : Context, yourName : String) : String{
        val greetingsList = context.resources.getStringArray(R.array.common_greetings_values_list)
        val randomGreetingTemplate = greetingsList.random()
        return String.format(randomGreetingTemplate, yourName)
    }
}