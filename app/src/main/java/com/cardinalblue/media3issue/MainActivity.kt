@file:OptIn(UnstableApi::class)

package com.cardinalblue.media3issue

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.ClippingConfiguration
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.GlEffect
import androidx.media3.effect.OverlaySettings
import androidx.media3.effect.Presentation
import androidx.media3.effect.VideoCompositorSettings
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.cardinalblue.media3issue.databinding.ActivityMainBinding
import com.cardinalblue.media3issue.dsl.effect.ColorToTransparent
import com.cardinalblue.media3issue.dsl.effect.TranslateAndScale
import com.cardinalblue.media3issue.dsl.getFileFromAssets
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var player: ExoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        player = ExoPlayer.Builder(this).build()
        binding.playerView.player = player

        // To test faulty behavior re run the transformer using the button
        binding.runTransformerButton.setOnClickListener {
            // Re-run transformer to demonstrate faulty ordering of the input sequences
            renderMovieAndShowInPlayer()
        }

        // Run the transformer once on startup
        renderMovieAndShowInPlayer()
    }

    @OptIn(UnstableApi::class)
    private fun renderMovieAndShowInPlayer() {
        binding.runTransformerButton.isEnabled = false
        lifecycleScope.launch {
            player.stop()
            player.removeMediaItem(0)
            binding.playerView.visibility = View.GONE
            binding.generatingTextView.visibility = View.VISIBLE

            val uri = renderMovie()

            binding.playerView.visibility = View.VISIBLE
            binding.generatingTextView.visibility = View.GONE
            player.addMediaItem(MediaItem.fromUri(uri))
            player.play()
            binding.runTransformerButton.isEnabled = true
        }
    }

    private suspend fun renderMovie(): Uri {
        val bankruptcy = getFileFromAssets("bankruptcy.mp4")
        val loydosan = getFileFromAssets("loydosan.mp4")

        val bankruptcyMediaItem = MediaItem.Builder()
            .setUri(Uri.fromFile(bankruptcy))
            .build()

        val loydosanMediaItem = MediaItem.Builder()
            .setUri(Uri.fromFile(loydosan))
            .build()

        val spyFamilyMusicMediaItem = MediaItem.Builder()
            .setUri(Uri.parse("asset:///spy_family.mp3"))
            .setClippingConfiguration(
                ClippingConfiguration.Builder()
                    .setStartPositionMs(0)
                    .setEndPositionMs(21000)
                    .build()
            )
            .build()

        val dancingMediaItem = MediaItem.Builder()
            .setUri(Uri.parse("asset:///dance.mp4"))
            .build()

        val sixteenByNineEffect = Presentation.createForWidthAndHeight(
            1920,
            1080,
            Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP
        )
        val translateLeftEffect = TranslateAndScale(-1f, 0f)
        val editedBankruptcy = EditedMediaItem.Builder(bankruptcyMediaItem)
            .setEffects(Effects(emptyList(), listOf(sixteenByNineEffect, translateLeftEffect)))
            .setRemoveAudio(true)
            .build()

        val translateRightEffect = TranslateAndScale(1f, 0f)
        val editedLoydosan = EditedMediaItem.Builder(loydosanMediaItem)
            .setEffects(Effects(emptyList(), listOf(sixteenByNineEffect, translateRightEffect)))
            .setRemoveAudio(true)
            .build()

        val editedSpyFamilyMusic = EditedMediaItem.Builder(spyFamilyMusicMediaItem)
            .build()

        val removeGreenScreenEffect = GlEffect { context, useHdr ->
              ColorToTransparent.Builder()
                .color(0xFF00FF00.toInt())
                .build(context)
        }

        val dancingEditedMediaItem = EditedMediaItem.Builder(dancingMediaItem)
            .setEffects(Effects(emptyList(), listOf(removeGreenScreenEffect)))
            .setRemoveAudio(true)
            .build()

        val sequence = EditedMediaItemSequence(editedBankruptcy, editedLoydosan)
        val musicSequence = EditedMediaItemSequence(editedSpyFamilyMusic)
        val dancingSequence = EditedMediaItemSequence(listOf(dancingEditedMediaItem), true)
        val composition = Composition.Builder(dancingSequence, sequence, musicSequence)
            .setVideoCompositorSettings(
                object : VideoCompositorSettings {
                    override fun getOutputSize(inputSizes: MutableList<Size>): Size {
                        return Size(1920, 1080)
                    }

                    override fun getOverlaySettings(
                        inputId: Int,
                        presentationTimeUs: Long
                    ): OverlaySettings {
                        return VideoCompositorSettings.DEFAULT.getOverlaySettings(inputId, presentationTimeUs)
                    }
                }
            )
            .build()

        @Suppress("BlockingMethodInNonBlockingContext")
        val tempFile = File.createTempFile("output", ".mp4")

        suspendCancellableCoroutine<Unit> {
            val transformer = Transformer.Builder(this)
                .build()
            transformer.addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    super.onCompleted(composition, exportResult)
                    it.resume(Unit)
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    super.onError(composition, exportResult, exportException)
                    it.resumeWithException(exportException)
                }
            })
            transformer.start(composition, tempFile.path)
        }

        return tempFile.toUri()
    }

    private fun getFileFromAssets(fileName: String): File {
        return getFileFromAssets(this, fileName)
    }
}