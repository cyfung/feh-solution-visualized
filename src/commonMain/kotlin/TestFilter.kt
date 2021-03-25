import com.soywiz.korag.DefaultShaders
import com.soywiz.korag.shader.FragmentShader
import com.soywiz.korag.shader.Uniform
import com.soywiz.korag.shader.VarType
import com.soywiz.korag.shader.storageFor
import com.soywiz.korge.view.filter.ShaderFilter
import com.soywiz.korim.color.RGBA

class TestFilter(color: RGBA) : ShaderFilter() {
    companion object {
        private val u_Color = Uniform("color", VarType.Float3)
    }

    var colorR by uniforms.storageFor(u_Color).doubleDelegateX(color.rd)
    var colorG by uniforms.storageFor(u_Color).doubleDelegateY(color.gd)
    var colorB by uniforms.storageFor(u_Color).doubleDelegateZ(color.bd)

    override val border: Int = 2

    override val fragment = FragmentShader {
        DefaultShaders {
            out setTo vec4(colorR.lit, colorG.lit, colorB.lit, 0f.lit)
            for (y in -1..1) {
                for (x in -1..1) {
                    out["a"] setTo max(out["a"], tex(fragmentCoords + vec2(
                            x.toFloat().lit * 0.25f.lit,
                            y.toFloat().lit * 0.25f.lit
                    ))["a"])
                }
            }
            out["a"] setTo min(1f.lit, out["a"] * 2f.lit)
        }
    }
}
