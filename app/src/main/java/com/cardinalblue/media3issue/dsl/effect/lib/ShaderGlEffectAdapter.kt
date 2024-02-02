package com.cardinalblue.media3issue.dsl.effect.lib

import androidx.media3.effect.BaseGlShaderProgram
import com.cardinalblue.media3issue.dsl.CompositionDsl

abstract class ShaderGlEffectAdapter<T : ShaderEffectBuilder<U>, U : BaseGlShaderProgram>(
    private val builderConstructor: () -> T
) {
    @CompositionDsl
    operator fun invoke(config: T.() -> Unit): T {
        return builderConstructor().apply(config)
    }
}