package com.cardinalblue.media3issue

import android.os.Bundle
import android.view.View
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.OverlaySettings
import androidx.media3.effect.VideoCompositorSettings
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
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

        val outputPath = createCacheFile("result.mp4").absolutePath

        player = ExoPlayer.Builder(this).build()
        binding.playerView.player = player

        // To test faulty behavior re run the transformer using the button
        binding.runTransformerButton.setOnClickListener {
            // Re-run transformer to demonstrate faulty ordering of the input sequences
            demonstrateZIndexUndefined(outputPath)
        }

        // Run the transformer once on startup
        demonstrateZIndexUndefined(outputPath)
    }

    @OptIn(UnstableApi::class)
    private fun demonstrateZIndexUndefined(outputPath: String) {
        binding.runTransformerButton.isEnabled = false
        val danceMediaItem = MediaItem.fromUri(getFileFromAssets("dance.mp4").toUri())
        val bankruptcyMediaItem = MediaItem.fromUri(getFileFromAssets("bankruptcy.mp4").toUri())

        val transformer = Transformer.Builder(this).setEncoderFactory(
            DefaultEncoderFactory.Builder(this)
                .setEnableFallback(true)
                .build()
        ).build()

        // Create 4 sequences translated to 4 corners of the screen, with different delays
        val danceSequences = buildList {
            var k = 1
            for (i in -1..1 step 2) {
                for (j in -1..1 step 2) {
                    val sequence = EditedMediaItemSequence(
                        createSilenceEditedMediaItem(k.seconds),
                        EditedMediaItem.Builder(danceMediaItem)
                            .setRemoveAudio(true)
                            .setEffects(
                                Effects(
                                    emptyList(),
                                    listOf(TranslateNdc(i.toFloat(), j.toFloat()))
                                )
                            )
                            .build()
                    )
                    add(sequence)
                    k++
                }
            }
        }

        // Create a sequence that should be behind all others (last item in the sequences list)
        val bankruptcySequence = EditedMediaItemSequence(
            EditedMediaItem.Builder(bankruptcyMediaItem).build()
        )

        // Create a composition starting with the 4 dance sequences, and ending with the bankruptcy sequence
        val composition = Composition.Builder(danceSequences + listOf(bankruptcySequence))
            .setVideoCompositorSettings(object : VideoCompositorSettings {
                // Force a size of 1080x1080 to avoid the video compositor to use the size of the first input (10x10 transparent image)
                override fun getOutputSize(inputSizes: MutableList<Size>): Size {
                    return Size(1080, 1080)
                }

                override fun getOverlaySettings(
                    inputId: Int,
                    presentationTimeUs: Long
                ): OverlaySettings {
                    return VideoCompositorSettings.DEFAULT.getOverlaySettings(inputId, presentationTimeUs)
                }
            })
            .build()

        lifecycleScope.launch {
            player.stop()
            player.removeMediaItem(0)
            binding.playerView.visibility = View.GONE
            binding.generatingTextView.visibility = View.VISIBLE

            // Run transformer
            transformer.startAndAwaitCompletion(composition, outputPath)

            binding.playerView.visibility = View.VISIBLE
            binding.generatingTextView.visibility = View.GONE
            player.addMediaItem(MediaItem.fromUri(outputPath))
            player.play()
            binding.runTransformerButton.isEnabled = true
        }
    }


    /**
     * Helper function to await completion of a transformer
     */
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
     * creates a transparent media item with the given duration
     * used to add a delay before playing another media item
     */
    @OptIn(UnstableApi::class)
    private fun createSilenceEditedMediaItem(duration: Duration): EditedMediaItem {
        val transparentAsset = getFileFromAssets("transparent.png")
        val mediaItem = MediaItem.Builder()
            .setUri(transparentAsset.toUri())
            .setImageDurationMs(duration.inWholeMilliseconds)
            .build()

        return EditedMediaItem.Builder(mediaItem)
            .setRemoveAudio(true)
            .setDurationUs(duration.inWholeMicroseconds)
            .setFrameRate(24)
            .build()
    }

    /**
     * Creates a file in the cache directory
     */
    private fun createCacheFile(fileName: String): File {
        val file = File(cacheDir, fileName)
        check(!(file.exists() && !file.delete())) { "Could not delete the previous export output file" }
        check(file.createNewFile()) { "Could not create the export output file" }
        return file
    }

    /**
     * Copies a file from the assets directory to the cache directory
     */
    private fun getFileFromAssets(fileName: String): File =
        File(cacheDir, fileName)
            .also {
                if (!it.exists()) {
                    it.outputStream().use { cache ->
                        assets.open(fileName).use { inputStream ->
                            inputStream.copyTo(cache)
                        }
                    }
                }
            }
}