import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.render.Texture
import com.soywiz.korge.view.BlendMode
import com.soywiz.korge.view.filter.Filter
import com.soywiz.korge.view.filter.IdentityFilter
import com.soywiz.korim.color.RGBA
import com.soywiz.korma.geom.Matrix

/**
 * Allows to create a single [Filter] that will render several [filters] in order.
 */
class SequenceFilter(private val filters: List<Filter>) : Filter {
    constructor(vararg filters: Filter) : this(filters.toList())

    override val border: Int get() = filters.asSequence().map { it.border }.max() ?: 0

    override fun render(
            ctx: RenderContext,
            matrix: Matrix,
            texture: Texture,
            texWidth: Int,
            texHeight: Int,
            renderColorAdd: Int,
            renderColorMul: RGBA,
            blendMode: BlendMode
    ) {
        if (filters.isEmpty()) {
            IdentityFilter.render(ctx, matrix, texture, texWidth, texHeight, renderColorAdd,
                    renderColorMul, blendMode)
        } else {
            filters.forEach {
                it.render(ctx, matrix, texture, texWidth, texHeight, renderColorAdd, renderColorMul,
                        blendMode)
            }
        }
    }
}
