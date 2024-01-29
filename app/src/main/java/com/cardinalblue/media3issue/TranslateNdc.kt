@file:UnstableApi
package com.cardinalblue.media3issue

import android.graphics.Matrix
import androidx.media3.common.util.Assertions
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.MatrixTransformation

/**
 * Adds translation in normalized device coordinates.
 */
class TranslateNdc(
    private val x: Float = 0f,
    private val y: Float = 0f,
) : MatrixTransformation {
    private var matrix: Matrix? = null

    override fun getMatrix(presentationTimeUs: Long): Matrix {
        return requireNotNull(matrix) { "configure() must be called before getMatrix()." }
    }

    override fun configure(inputWidth: Int, inputHeight: Int): Size {
        Assertions.checkArgument(inputWidth > 0, "inputWidth must be positive")
        Assertions.checkArgument(inputHeight > 0, "inputHeight must be positive")

        matrix = Matrix().apply {
            postTranslate(x, y)
        }

        return Size(inputWidth, inputHeight)
    }
}