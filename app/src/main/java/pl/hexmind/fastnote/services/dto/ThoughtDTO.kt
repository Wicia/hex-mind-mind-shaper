package pl.hexmind.fastnote.services.dto

import java.util.Date

data class ThoughtDTO(
    val id : Int? = null,
    val domainIconId : Int? = null,
    val thread : String? = "",
    val essence : String,
    val createdAt: Date
){
    // * For proper DiffUtil comparison
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ThoughtDTO) return false

        return id == other.id &&
                createdAt == other.createdAt &&
                domainIconId == other.domainIconId &&
                thread == other.thread &&
                essence == other.essence
    }

    // * For proper DiffUtil comparison
    override fun hashCode(): Int {
        var result = id ?: 0
        result = 31 * result + (domainIconId ?: 0)
        result = 31 * result + (thread?.hashCode() ?: 0)
        result = 31 * result + essence.hashCode()
        result = 31 * result + createdAt.hashCode()
        return result
    }
}