@file:OptIn(UnstableApi::class)

package com.cardinalblue.media3issue

import android.os.Bundle
import android.text.SpannableString
import android.view.View
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.OverlaySettings
import androidx.media3.effect.Presentation
import androidx.media3.effect.TextOverlay
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.cardinalblue.media3issue.databinding.ActivityMainBinding
import com.cardinalblue.media3issue.dsl.by
import com.cardinalblue.media3issue.dsl.composition
import com.cardinalblue.media3issue.dsl.effect.ColorToTransparent
import com.cardinalblue.media3issue.dsl.effect.TranslateNdc
import com.cardinalblue.media3issue.dsl.getFileFromAssets
import com.cardinalblue.media3issue.dsl.renderComposition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration
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
            demonstrateZIndexUndefined()
        }

        // Run the transformer once on startup
        demonstrateZIndexUndefined()
    }

    @OptIn(UnstableApi::class)
    private fun demonstrateZIndexUndefined() {
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
            silence(10.seconds)
        }
        var k = 0
        for (i in -1..1 step 2) {
            for (j in -1..1 step 2) {
                sequence {
                    silence(k++.seconds, "silence$k")
                    repeat(2) {
                        video(getFileFromAssets("dance.mp4").toUri()) {
                            removeAudio()
                            id("dance$k")

                            effects {
                                +ColorToTransparent {
                                    color(0x00ff00)
                                }
                                +TranslateNdc(x = i.toFloat(), y = j.toFloat())
                            }
                        }
                    }
                }
            }
        }

        sequence {
            audio(getFileFromAssets("spy_family.mp3").toUri())
        }

        sequence {
            silence(1.seconds)
            video(getFileFromAssets("bankruptcy.mp4").toUri()) {
                id("bankruptcy")

                startAtMs(5000)
                endAtMs(10000)
                duration(5.seconds)
                removeAudio()

                overlay(TextOverlay.createStaticTextOverlay(SpannableString.valueOf("This is an overlay")))
            }

            video(getFileFromAssets("loydosan.mp4").toUri()) {
                id("loydosan")
                startAtMs(1)
                endAtMs(5000)
                duration(5.seconds)
                removeAudio()

                overlay(TextOverlay.createStaticTextOverlay(SpannableString.valueOf("Hello World")))
            }
        }

        settings {
            size(1280 by 720)
        }
    }

    private fun getFileFromAssets(fileName: String): File {
        return getFileFromAssets(this, fileName)
    }
}