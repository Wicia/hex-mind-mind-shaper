package pl.hexmind.mindshaper.services.dto

data class DomainDTO(
    val id : Int? = null,
    val name : String,
    // Null when the domain has no icon — the default icon is shown
    val iconId : Int?
)