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
        sequence {
            video(getFileFromAssets("dance.mp4").toUri()) {
                removeAudio()
                effects {
                    +ColorToTransparent {
                        color(0x00FF00)
                    }
                }
            }
            looping(true)
        }

        sequence {
            audio(getFileFromAssets("spy_family.mp3").toUri()) {
                startAtMs(0)
                endAtMs(21000)
            }
        }

        sequence {
            val sixteenByNineEffect = Presentation.createForWidthAndHeight(
                1920, 1080, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP
            )
            video(getFileFromAssets("bankruptcy.mp4").toUri()) {
                removeAudio()
                effects {
                    +sixteenByNineEffect
                    +TranslateAndScale(-1f, 0.0f)
                }
            }
            video(getFileFromAssets("loydosan.mp4").toUri()) {
                removeAudio()
                effects {
                    +sixteenByNineEffect
                    +TranslateAndScale(1f, 0.0f)
                }
            }
        }

        settings {
            width(1920)
            height(1080)
        }
    }


    private fun getFileFromAssets(fileName: String): File {
        return getFileFromAssets(this, fileName)
    }
}