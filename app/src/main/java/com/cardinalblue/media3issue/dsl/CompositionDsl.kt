@file:UnstableApi

package com.cardinalblue.media3issue.dsl

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.GlEffect
import androidx.media3.effect.GlMatrixTransformation
import androidx.media3.effect.GlShaderProgram
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.OverlaySettings
import androidx.media3.effect.TextureOverlay
import androidx.media3.effect.VideoCompositorSettings
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.cardinalblue.media3issue.dsl.effect.lib.ShaderEffectBuilder
import com.google.common.collect.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@DslMarker
annotation class CompositionDsl


@CompositionDsl
class CompositionBuilder(private val context: Context) {
    private val videoSequenceItems = mutableListOf<EditedMediaItemSequence>()
    private val effects = mutableListOf<Effect>()
    private var settingsBuilder: SettingsBuilder? = null

    @CompositionDsl
    fun sequence(block: VideoSequenceItemBuilder.() -> Unit = { }) {
        val builder = VideoSequenceItemBuilder(context)
        builder.block()
        videoSequenceItems.add(builder.build())
    }

    @CompositionDsl
    fun effects(block: EffectsBuilder.() -> Unit = { }) {
        val builder = EffectsBuilder()
        builder.block()
        effects.addAll(builder.build())
    }

    @CompositionDsl
    fun settings(block: SettingsBuilder.() -> Unit = { }) {
        settingsBuilder = SettingsBuilder().apply(block)
    }

    fun build(): Composition {
        return Composition.Builder(videoSequenceItems)
            .setEffects(Effects(emptyList(), effects))
            .let {
                settingsBuilder?.let { builder ->
                    it.setVideoCompositorSettings(builder.build())
                } ?: it
            }
            .build()
    }
}

class SettingsBuilder {
    private var width = 0
    private var height = 0

    fun width(width: Int) {
        this.width = width
    }

    fun height(height: Int) {
        this.height = height
    }

    fun size(size: Size) {
        width(size.width)
        height(size.height)
    }

    fun build(): VideoCompositorSettings {
        return object : VideoCompositorSettings {
            override fun getOutputSize(inputSizes: MutableList<Size>): Size {
                return Size(width, height)
            }

            override fun getOverlaySettings(
                inputId: Int,
                presentationTimeUs: Long
            ): OverlaySettings {
                return VideoCompositorSettings.DEFAULT.getOverlaySettings(
                    inputId,
                    presentationTimeUs
                )
            }

        }
    }

}

@CompositionDsl
class VideoSequenceItemBuilder(private val context: Context) {
    private val videoItems = mutableListOf<EditedMediaItem>()
    private var looping = false


    @CompositionDsl
    fun video(uri: Uri, block: VideoItemBuilder.() -> Unit = { }) {
        val builder = VideoItemBuilder(uri)
        builder.block()
        videoItems.add(builder.build())
    }

    @CompositionDsl
    fun audio(uri: Uri, block: VideoItemBuilder.() -> Unit = { }) {
        video(uri) {
            block()
        }
    }

    @CompositionDsl
    fun looping(looping: Boolean) {
        this.looping = looping
    }

    @CompositionDsl
    fun silence(duration: Duration, id: String = "") {
        if (duration == Duration.ZERO) return
        val silenceFile = getFileFromAssets(context, "transparent.png")
        videoItems.add(
            EditedMediaItem.Builder(
                MediaItem.Builder()
                    .setUri(silenceFile.toUri())
                    .let {
                        if (id.isNotEmpty()) {
                            it.setMediaId(id)
                        } else {
                            it
                        }
                    }
                    .build()
            )
                .setEffects(
                    Effects(emptyList(), emptyList())
                )
                .setFrameRate(24)
                .setDurationUs(duration.inWholeMicroseconds)
                .setRemoveAudio(true)
                .build()
        )
    }


    fun build(): EditedMediaItemSequence {
        return EditedMediaItemSequence(videoItems, looping)
    }
}

@CompositionDsl
class VideoItemBuilder(private val uri: Uri) {
    private val effects = mutableListOf<Effect>()
    private val overlayEffects = mutableListOf<TextureOverlay>()
    private var audioRemoved: Boolean = false
    private var startAtMs: Long = 0
    private var endAtMs: Long = 0
    private var duration: Duration = Duration.ZERO
    private var fps: Int = 0
    private var id: String = ""

    @CompositionDsl
    fun effects(block: EffectsBuilder.() -> Unit = { }) {
        val builder = EffectsBuilder()
        builder.block()
        effects.addAll(builder.build())
    }

    fun removeAudio() {
        audioRemoved = true
    }

    fun id(id: String) {
        this.id = id
    }

    fun startAtMs(startAtMs: Long) {
        this.startAtMs = startAtMs
    }

    fun endAtMs(endAtMs: Long) {
        this.endAtMs = endAtMs
    }

    fun fps(fps: Int) {
        this.fps = fps
    }

    fun duration(duration: Duration) {
        this.duration = duration
    }

    fun overlay(overlayEffect: TextureOverlay) {
        overlayEffects.add(overlayEffect)
    }

    fun build(): EditedMediaItem {
        if (overlayEffects.isNotEmpty()) {
            effects.add(
                OverlayEffect(ImmutableList.copyOf(overlayEffects))
            )
        }
        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .apply {
                        if (startAtMs != 0L) {
                            setStartPositionMs(startAtMs)
                        }
                        if (endAtMs != 0L) {
                            setEndPositionMs(endAtMs)
                        }
                    }
                    .build()
            )
            .let {
                if (id.isNotEmpty()) {
                    it.setMediaId(id)
                } else {
                    it
                }
            }
            .build()
        return EditedMediaItem.Builder(mediaItem).setEffects(
            Effects(mutableListOf(), effects)
        )
            .setRemoveAudio(audioRemoved)
            .let {
                if (duration == Duration.ZERO) it else it.setDurationUs(duration.inWholeMicroseconds)
            }
            .let { if (fps == 0) it else it.setFrameRate(fps) }
            .build()
    }
}

@CompositionDsl
class EffectsBuilder {
    private val effects = mutableListOf<Effect>()

    operator fun <T : GlMatrixTransformation> T.unaryPlus() {
        effects.add(this)
    }

    operator fun <T : GlShaderProgram> ShaderEffectBuilder<T>.unaryPlus() {
        this@EffectsBuilder.effects.add(GlEffect { context, _ -> build(context) })
    }

    fun build(): List<Effect> {
        return effects
    }
}

@CompositionDsl
fun composition(context: Context, block: CompositionBuilder.() -> Unit): Composition {
    val builder = CompositionBuilder(context = context)
    builder.block()
    return builder.build()
}

@CompositionDsl
suspend fun renderComposition(
    context: Context,
    fileName: String = "video_" + System.currentTimeMillis() + ".mp4",
    block: CompositionBuilder.() -> Unit
): Uri {
    val builder = CompositionBuilder(context)
    builder.block()
    return builder.build().render(context, fileName)
}


suspend fun Composition.render(
    context: Context,
    fileName: String = "video_" + System.currentTimeMillis() + ".mp4",
    transformer: Transformer = Transformer.Builder(context).setEncoderFactory(
        DefaultEncoderFactory.Builder(context)
            .setEnableFallback(true)
            .build()
    )
        .build()
): Uri {
    val cacheFile = createCacheFile(context, fileName)
    transformer.startAndAwaitCompletion(this, cacheFile.absolutePath)
    return cacheFile.toUri()
}

/**
 * Creates a cache file, resetting it if it already exists.
 */
@Throws(IOException::class)
private fun createCacheFile(context: Context, fileName: String): File {
    val file: File = File(context.cacheDir, fileName)
    check(!(file.exists() && !file.delete())) { "Could not delete the previous export output file" }
    check(file.createNewFile()) { "Could not create the export output file" }
    return file
}

@OptIn(UnstableApi::class)
private suspend fun Transformer.startAndAwaitCompletion(
    composition: Composition,
    outputPath: String
) = withContext(Dispatchers.Main) {
    suspendCancellableCoroutine {
        val listener = object : Transformer.Listener {
            override fun onError(
                composition: Composition,
                exportResult: ExportResult,
                exportException: ExportException
            ) {
                super.onError(composition, exportResult, exportException)
                it.resumeWithException(exportException)
                removeAllListeners()
            }

            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                super.onCompleted(composition, exportResult)
                it.resume(Unit)
                removeAllListeners()
            }
        }

        addListener(listener)
        start(composition, outputPath)
    }
}

/**
 * Copies a file from the assets directory to the cache directory
 */
fun getFileFromAssets(context: Context, fileName: String): File =
    File(context.cacheDir, fileName)
        .also {
            if (!it.exists()) {
                it.outputStream().use { cache ->
                    context.assets.open(fileName).use { inputStream ->
                        inputStream.copyTo(cache)
                    }
                }
            }
        }


infix fun Int.by(other: Int) = Size(this, other)