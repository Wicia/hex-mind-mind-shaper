package pl.hexmind.fastnote.data.models

/**
 * Data class representing possible ID in database for Domains (limiting entries)
 */
data class DomainIdentifier(val value: Int = MIN_IDENTIFIER) {

    init{
        if (value <  MIN_IDENTIFIER || value > MAX_IDENTIFIER){
            // TODO: Add exception throwing and handling
        }
    }

    companion object {
        val MAX_IDENTIFIER = 10
        val MIN_IDENTIFIER = 1

        fun availableIdentifiers(): List<Int>{
            return (MIN_IDENTIFIER..MAX_IDENTIFIER).toList()
        }
    }
}