@file:OptIn(UnstableApi::class)

package com.cardinalblue.media3issue

import android.graphics.fonts.Font
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.View
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.net.toUri
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.text.inSpans
import androidx.core.text.toSpannable
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.ClippingConfiguration
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.GlEffect
import androidx.media3.effect.OverlaySettings
import androidx.media3.effect.Presentation
import androidx.media3.effect.TextOverlay
import androidx.media3.effect.TextureOverlay
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
import com.cardinalblue.media3issue.dsl.renderComposition
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration.Companion.seconds

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

    private suspend fun renderMovie() = renderComposition(this) {
        val video = getFileFromAssets("bankruptcy.mp4").toUri()
        val music = getFileFromAssets("spy_family.mp3").toUri()
        var k = 0
        for (i in -1 .. 1 step 2) {
            for (j in -1 .. 1 step 2) {
                sequence {
                    if (k != 0) {
                        silence(k.seconds)
                    }
                    video(video) {
                        removeAudio()
                        effects {
                            +Presentation.createForWidthAndHeight(1920, 1080,
                                Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP
                            )
                            +TranslateAndScale(i / 2.0f, j / 2.0f, 0.5f, 0.5f)
                        }
                    }
                    k++
                }
            }
        }

        sequence {
            audio(music) {
                startAtMs(0)
                endAtMs(11000)
            }
        }
    }


    private fun getFileFromAssets(fileName: String): File {
        return getFileFromAssets(this, fileName)
    }
}