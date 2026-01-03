package si.uni.fri.sprouty.util.limiters

object ActionRateLimiter {
    private val lastActionTimes = mutableMapOf<String, Long>()

    fun canPerformAction(actionKey: String, cooldownMillis: Long): Boolean {
        val currentTime = System.currentTimeMillis()
        val lastTime = lastActionTimes[actionKey] ?: 0L

        return if (currentTime - lastTime >= cooldownMillis) {
            lastActionTimes[actionKey] = currentTime
            true
        } else {
            false
        }
    }
}