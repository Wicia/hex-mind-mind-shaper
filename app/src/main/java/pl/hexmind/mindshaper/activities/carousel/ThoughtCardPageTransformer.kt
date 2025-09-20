package pl.hexmind.mindshaper.activities.carousel

import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

/**
 * For making cool card changing effect :)
 */
class ThoughtCardPageTransformer : ViewPager2.PageTransformer {

    override fun transformPage(page: View, position: Float) {
        when {
            position < -1 -> {
                page.alpha = 0f
            }
            position <= 1 -> {
                page.alpha = 1f
                page.translationX = 0f
                page.scaleX = 1f
                page.scaleY = 1f

                // 3D rotation effect
                page.rotationY = position * -30f

                // Depth effect
                val scaleFactor = 0.85f + (1 - abs(position)) * 0.15f
                page.scaleX = scaleFactor
                page.scaleY = scaleFactor

                // Fade effect for side pages
                page.alpha = 0.5f + (1 - abs(position)) * 0.5f
            }
            else -> {
                page.alpha = 0f
            }
        }
    }
}