@file:UnstableApi
package com.cardinalblue.media3issue.dsl.effect

import android.graphics.Matrix
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.MatrixTransformation

class TranslateAndScale(
    private val x: Float,
    private val y: Float,
    private val sx: Float = 1f,
    private val sy: Float = 1f,
) : MatrixTransformation {
    private val matrix = Matrix().apply {
        postScale(sx, sy)
        postTranslate(x, y)
    }
    override fun getMatrix(presentationTimeUs: Long): Matrix {
        return matrix
    }

    override fun configure(inputWidth: Int, inputHeight: Int): Size {
        return super.configure(inputWidth, inputHeight)
    }
}