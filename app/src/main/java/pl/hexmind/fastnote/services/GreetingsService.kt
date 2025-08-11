package pl.hexmind.fastnote.services

import android.content.Context
import pl.hexmind.fastnote.R

object GreetingsService {

    fun getGreetingsString(context : Context, yourName : String) : String{
        val greetingsList = context.resources.getStringArray(R.array.common_greetings_values_list)
        val randomGreetingTemplate = greetingsList.random()
        return String.format(randomGreetingTemplate, yourName)
    }
}