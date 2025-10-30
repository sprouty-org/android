package si.uni.fri.sprouty.util.animations

import android.view.View

fun View.slideInFromLeft(duration: Long = 400L, offset: Float? = null) {
    // Start position (off-screen to the left)
    val startX = offset ?: -this.width.toFloat()

    this.translationX = startX
    this.alpha = 0f
    this.visibility = View.VISIBLE

    this.animate()
        .translationX(0f)
        .alpha(1f)
        .setDuration(duration)
        .start()
}