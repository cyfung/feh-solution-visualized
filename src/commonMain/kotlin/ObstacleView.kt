import com.soywiz.korge.view.Container
import com.soywiz.korge.view.ViewsDslMarker
import com.soywiz.korge.view.addTo
import com.soywiz.korge.view.image
import com.soywiz.korim.bitmap.BmpSlice

inline fun Container.obstacleView(
        obstacle: Int,
        obstacle1: BmpSlice,
        obstacle2: BmpSlice,
        callback: @ViewsDslMarker ObstacleView.() -> Unit = {}
) = ObstacleView(obstacle, obstacle1, obstacle2).addTo(this).apply(callback)

class ObstacleView(
        private var obstacle: Int,
        private val obstacle1: BmpSlice,
        private val obstacle2: BmpSlice
) : Container() {
    private val obstacleImage = image(getImage()) {
        scale = 0.5
    }

    private fun getImage(): BmpSlice {
        return when (obstacle) {
            2 -> obstacle2
            1 -> obstacle1
            else -> throw IllegalStateException()
        }
    }

    fun breakObstacle() {
        when (obstacle) {
            2 -> obstacleImage.bitmap = obstacle1
            1 -> obstacleImage.visible = false
            else -> throw IllegalStateException()
        }
        obstacle--
    }
}