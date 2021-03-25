import com.soywiz.korge.view.Container
import com.soywiz.korge.view.Image
import com.soywiz.korge.view.SolidRect
import com.soywiz.korge.view.Text
import com.soywiz.korge.view.container
import com.soywiz.korge.view.filter.ColorMatrixFilter
import com.soywiz.korge.view.filter.ComposedFilter
import com.soywiz.korge.view.filter.IdentityFilter
import com.soywiz.korge.view.image
import com.soywiz.korge.view.position
import com.soywiz.korge.view.rotation
import com.soywiz.korge.view.scale
import com.soywiz.korge.view.size
import com.soywiz.korge.view.solidRect
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.BitmapSlice
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.font.BitmapFont
import com.soywiz.korma.geom.degrees
import me.kumatheta.feh.message.UnitAdded

private const val HP_BAR_WIDTH = 60.0
private val OUTLINE_FILTER = SequenceFilter(
        ComposedFilter(TestFilter(Colors.BLACK), TestFilter(Colors["#222222"])), IdentityFilter)
private val GRAYSCALE_OUTLINE_FILTER = ComposedFilter(OUTLINE_FILTER,
        ColorMatrixFilter(ColorMatrixFilter.GRAYSCALE_MATRIX))

val COLOR_PLAYER = Colors["#4ce2ff"]
val COLOR_PLAYER_SHADOW = Colors["#0a2533"]

val COLOR_ENEMY = Colors["#ff4c6a"]
val COLOR_ENEMY_SHADOW = Colors["#330a11"]

class UnitView private constructor(
        private val unitAdded: UnitAdded,
        private val hpBar: SolidRect,
        private val hpText: Text,
        private val image: Image
) : Container() {
    var active: Boolean = true
        set(value) {
            field = value
            val filter = if (active) {
                OUTLINE_FILTER
            } else {
                GRAYSCALE_OUTLINE_FILTER
            }
            image.parent!!.filter = filter
            hpText.parent!!.filter = filter
        }
    val unitId
        get() = unitAdded.unitId
    val playerControl
        get() = unitAdded.playerControl
    val unitName
        get() = unitAdded.name
    var boardX = unitAdded.startX
    var boardY = unitAdded.startY
    var hp = unitAdded.maxHp
        set(value) {
            field = value
            hpBar.width = value * HP_BAR_WIDTH / unitAdded.maxHp
            hpText.text = value.toString()
        }

    fun resetUnit() {
        boardX = unitAdded.startX
        boardY = unitAdded.startY
        hp = unitAdded.maxHp
        active = true
    }

    companion object {
        fun unitView(
                unitAdded: UnitAdded,
                bitmapSlice: BitmapSlice<Bitmap>,
                font: BitmapFont,
                moveTypeBitmap: BitmapSlice<Bitmap>,
                attackTypeBitmap: BitmapSlice<Bitmap>
        ): UnitView {
            val color: RGBA
            val outlineColor: RGBA
            val weaponTypeX: Int
            if (unitAdded.playerControl) {
                color = COLOR_PLAYER
                outlineColor = COLOR_PLAYER_SHADOW
                weaponTypeX = 0
            } else {
                color = COLOR_ENEMY
                outlineColor = COLOR_ENEMY_SHADOW
                weaponTypeX = 64
            }

            val hpBar = SolidRect(HP_BAR_WIDTH, 3, color).apply {
                position(28, 82)
            }
            val hpText = Text(unitAdded.maxHp.toString(), textSize = 22.0, color = color,
                    font = font).apply {
                position(2, 90 - height)
            }
            val image = Image(bitmapSlice).apply {
                size(68, 68)
                position(11, 12)
            }

            return UnitView(unitAdded, hpBar, hpText, image).apply {
                container {
                    addChild(image)
                    filter = OUTLINE_FILTER
                }
                container {
                    addChild(hpText)
                    solidRect(HP_BAR_WIDTH, 3, outlineColor) {
                        position(28, 82)
                    }
                    addChild(hpBar)
                    filter = OUTLINE_FILTER
                }
                image(moveTypeBitmap) {
                    position(65, 81)
                    rotation((-90).degrees)
                    scale(0.5)
                }
                image(attackTypeBitmap) {
                    position(weaponTypeX, 1)
                    scale(0.5)
                }
            }
        }
    }
}

fun unitView(
        unitAdded: UnitAdded,
        bitmapSlice: BitmapSlice<Bitmap>,
        font: BitmapFont,
        moveTypeBitmap: BitmapSlice<Bitmap>,
        attackTypeBitmap: BitmapSlice<Bitmap>
): UnitView {
    return UnitView.unitView(unitAdded, bitmapSlice, font, moveTypeBitmap, attackTypeBitmap)
}