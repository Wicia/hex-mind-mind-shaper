package pl.hexmind.mindshaper.common.dormant

enum class ThoughtState {
    ACTIVE,   // normal state — no restrictions
    WARNING,  // approaching dormant threshold (X days before show warnings for user)
    DORMANT   // low value, not modified for Y+ days
}