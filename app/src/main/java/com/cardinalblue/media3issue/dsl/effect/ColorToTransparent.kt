@file:UnstableApi

package com.cardinalblue.media3issue.dsl.effect

import android.content.Context
import android.graphics.Color
import android.opengl.GLES20
import androidx.core.graphics.red
import androidx.core.graphics.toColor
import androidx.media3.common.util.GlProgram
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BaseGlShaderProgram
import com.cardinalblue.media3issue.dsl.effect.lib.ShaderEffectBuilder
import com.cardinalblue.media3issue.dsl.effect.lib.ShaderGlEffectAdapter

class ColorToTransparent(context: Context, private val color: Int) :
    BaseGlShaderProgram(false, 1) {

    private val glProgram: GlProgram =
        GlProgram(context, VERTEX_SHADER_PATH, FRAGMENT_SHADER_PATH).apply {
            setBufferAttribute(
                "aFramePosition",
                GlUtil.getNormalizedCoordinateBounds(),
                GlUtil.HOMOGENEOUS_COORDINATE_VECTOR_SIZE
            )

            val c = color.toColor()
            setFloatsUniform("uColor", floatArrayOf(c.red(), c.green(), c.blue()))
        }

    override fun configure(inputWidth: Int, inputHeight: Int): Size = Size(inputWidth, inputHeight)

    override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
        glProgram.use()
        glProgram.setSamplerTexIdUniform("uTexSampler", inputTexId, 0)
        glProgram.bindAttributesAndUniforms()
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }

    override fun release() {
        super.release()
        glProgram.delete()
    }

    class Builder : ShaderEffectBuilder<ColorToTransparent> {
        private var color: Int = 0

        fun color(color: Int) = apply { this.color = color }

        override fun build(context: Context): ColorToTransparent =
            ColorToTransparent(context, color)
    }

    companion object : ShaderGlEffectAdapter<Builder, ColorToTransparent>(::Builder) {
        private const val VERTEX_SHADER_PATH = "vertex_shader_copy_es2.glsl"
        private const val FRAGMENT_SHADER_PATH = "fragment_shader_remove_green_screen_es2.glsl"
    }
}
