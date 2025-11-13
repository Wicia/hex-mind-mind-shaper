package pl.hexmind.mindshaper.common.regex

class HexTagsUtils {

    companion object {
        fun parseInput(input : String?) :  HexTags{
            val input = input.orEmpty()

            val soulMateRegex = Regex("@(\\S+)")
            val projectRegex = Regex("#(\\S+)")

            val soulMate = soulMateRegex.find(input)?.groupValues?.get(1)
            val project = projectRegex.find(input)?.groupValues?.get(1)

            val thread = input
                .replace(soulMateRegex, "")
                .replace(projectRegex, "")
                .trim()

            return  HexTags(thread = thread, soulMate = soulMate, project = project)
        }
    }
}

data class HexTags (
    val thread : String? = null,
    val soulMate: String? = null,
    val project: String? = null
){
    fun areCriteriaEmpty() : Boolean{
        return thread.isNullOrBlank() && soulMate.isNullOrBlank() && project.isNullOrBlank()
    }
}