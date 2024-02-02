package com.cardinalblue.media3issue.dsl.effect.lib

import android.content.Context
import androidx.media3.effect.GlShaderProgram
import com.cardinalblue.media3issue.dsl.CompositionDsl

@CompositionDsl
interface ShaderEffectBuilder<T : GlShaderProgram> {
    fun build(context: Context): T
}
