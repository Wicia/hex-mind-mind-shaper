package pl.hexmind.mindshaper.services.dto

import java.time.Instant

data class ThoughtDTO(
    var essence : String? = "",
    var createdAt: Instant? = Instant.now(),
    var id : Int? = null,
    var domainIconId : Int? = null,
    var thread : String? = "",
    var richText: String? = ""
){
    // * For proper DiffUtil comparison
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ThoughtDTO) return false

        return id == other.id &&
                createdAt == other.createdAt &&
                domainIconId == other.domainIconId &&
                thread == other.thread &&
                essence == other.essence &&
                richText == other.richText
    }

    // * For proper DiffUtil comparison
    override fun hashCode(): Int {
        var result = id ?: 0
        result = 31 * result + (domainIconId ?: 0)
        result = 31 * result + (thread?.hashCode() ?: 0)
        result = 31 * result + essence.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + richText.hashCode()
        return result
    }
}