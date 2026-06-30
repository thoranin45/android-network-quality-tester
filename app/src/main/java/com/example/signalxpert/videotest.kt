package com.example.dedotest.screen

import android.net.TrafficStats
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import com.example.signalxpert.ui.theme.SignalXpertTheme

class VideoTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SignalXpertTheme {
                VideoTestScreen()
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoTestScreen() {
    val context = LocalContext.current

    // UI States
    var initialLoadTime360 = remember { mutableStateOf("--") }
    var bufferingTime360 = remember { mutableStateOf("--") }
    var dataUsage360 = remember { mutableStateOf("--") }
    var initialLoadTime720 = remember { mutableStateOf("--") }
    var bufferingTime720 = remember { mutableStateOf("--") }
    var dataUsage720 = remember { mutableStateOf("--") }
    var initialLoadTime1080 = remember { mutableStateOf("--") }
    var bufferingTime1080 = remember { mutableStateOf("--") }
    var dataUsage1080 = remember { mutableStateOf("--") }

    var currentResolutionIndex by remember { mutableStateOf(0) }
    var testRunning by remember { mutableStateOf(false) }
    var initialLoadStartTime by remember { mutableStateOf(0L) }
    var bufferStartTime by remember { mutableStateOf(0L) }
    var totalBufferTime by remember { mutableStateOf(0L) }
    var totalDataUsed360 by remember { mutableStateOf(0L) }
    var totalDataUsed720 by remember { mutableStateOf(0L) }
    var totalDataUsed1080 by remember { mutableStateOf(0L) }
    var initialDataUsage by remember { mutableStateOf(0L) }
    var actualPlayStartTime by remember { mutableStateOf(0L) }

    val handler = remember { Handler(Looper.getMainLooper()) }
    val mp4Urls = listOf(
        "https://cdn.pixabay.com/video/2024/07/25/223165_tiny.mp4", // 360p
        "https://cdn.pixabay.com/video/2024/07/25/223165_small.mp4", // 720p
        "https://cdn.pixabay.com/video/2024/07/25/223165.mp4"  // 1080p
    )

    val trackSelector = remember { DefaultTrackSelector(context) }
    val player = remember {
        ExoPlayer.Builder(context).setTrackSelector(trackSelector).build().apply {
            addListener(object : Player.Listener {

                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> {
                            if (initialLoadStartTime == 0L) {
                                initialLoadStartTime = System.currentTimeMillis()
                            } else {
                                bufferStartTime = System.currentTimeMillis()
                            }
                        }
                        Player.STATE_READY -> {
                            if (initialLoadStartTime != 0L) {
                                val initialLoadEndTime = System.currentTimeMillis()
                                val initialLoadTime = initialLoadEndTime - initialLoadStartTime
                                updateInitialLoadTimeTextView(
                                    initialLoadTime,
                                    currentResolutionIndex,
                                    initialLoadTime360,
                                    initialLoadTime720,
                                    initialLoadTime1080
                                )
                                actualPlayStartTime = System.currentTimeMillis() // Start timer
                                initialLoadStartTime = 0L
                            } else if (bufferStartTime != 0L) {
                                val bufferEndTime = System.currentTimeMillis()
                                totalBufferTime += bufferEndTime - bufferStartTime
                                updateBufferingTimeTextView(
                                    totalBufferTime,
                                    currentResolutionIndex,
                                    bufferingTime360,
                                    bufferingTime720,
                                    bufferingTime1080
                                )
                                bufferStartTime = 0L
                            }
                        }
                        Player.STATE_ENDED -> {
                            val currentDataUsage = TrafficStats.getUidRxBytes(android.os.Process.myUid())
                            val dataUsed = currentDataUsage - initialDataUsage

                            when (currentResolutionIndex) {
                                0 -> totalDataUsed360 = dataUsed
                                1 -> totalDataUsed720 = dataUsed
                                2 -> totalDataUsed1080 = dataUsed
                            }

                            updateDataUsageTextView(
                                dataUsed,
                                currentResolutionIndex,
                                dataUsage360,
                                dataUsage720,
                                dataUsage1080
                            )
                        }
                    }
                }
            })
        }
    }

    LaunchedEffect(Unit) {
        player.prepare()
    }

    DisposableEffect(Unit) {
        onDispose {
            player.release()
            handler.removeCallbacksAndMessages(null)
        }
    }

    // Function to play next resolution
    fun playNextResolution() {
        if (currentResolutionIndex >= mp4Urls.size) {
            testRunning = false
            return
        }

        // Reset buffering and data usage
        totalBufferTime = 0L
        initialDataUsage = TrafficStats.getUidRxBytes(android.os.Process.myUid())

        // Play video for current resolution
        playResolution(player, currentResolutionIndex, mp4Urls, trackSelector)

        // Schedule the next resolution after 10 seconds (Full 10 seconds of video playing)
        handler.postDelayed({
            currentResolutionIndex++
            if (currentResolutionIndex < mp4Urls.size) {
                playNextResolution() // Play next resolution
            } else {
                player.stop() // Stop player after all resolutions played
                testRunning = false // Reset the testRunning flag
            }
        }, 10000)
    }

    // UI Layout
    Column(modifier = Modifier
        .padding(16.dp)
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
    ) {
        // Resolution Metrics
        ResolutionCard("360p", initialLoadTime360.value, bufferingTime360.value, dataUsage360.value)
        Spacer(modifier = Modifier.height(8.dp))
        ResolutionCard("720p", initialLoadTime720.value, bufferingTime720.value, dataUsage720.value)
        Spacer(modifier = Modifier.height(8.dp))
        ResolutionCard("1080p", initialLoadTime1080.value, bufferingTime1080.value, dataUsage1080.value)

        // Test Button
        Button(
            onClick = {
                if (!testRunning) {  // Prevent pressing the button during the test
                    currentResolutionIndex = 0
                    testRunning = true
                    playNextResolution()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            enabled = !testRunning, // Disable button when testing is running
            colors = ButtonDefaults.buttonColors(
                containerColor = if (testRunning) Color.Gray else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(text = if (testRunning) "TESTING..." else "START")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Video Player
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    this.player = player
                    useController = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(8.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
        )
    }
}

@Composable
fun ResolutionCard(resolution: String, loadTime: String, bufferTime: String, dataUsage: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 4.dp,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = resolution, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            MetricRow("Initial Load Time", loadTime)
            MetricRow("Buffering Time", bufferTime)
            MetricRow("Data Usage", dataUsage)
        }
    }
}

@Composable
fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.End)
    }
}

// Function to play specific resolution
@OptIn(UnstableApi::class)
fun playResolution(
    player: ExoPlayer,
    resolutionIndex: Int,
    mp4Urls: List<String>,
    trackSelector: DefaultTrackSelector
) {
    val parametersBuilder = trackSelector.buildUponParameters()
    when (resolutionIndex) {
        0 -> parametersBuilder.setMaxVideoSize(640, 360) // 360p
        1 -> parametersBuilder.setMaxVideoSize(1280, 720) // 720p
        2 -> parametersBuilder.setMaxVideoSize(1920, 1080) // 1080p
    }
    trackSelector.setParameters(parametersBuilder)

    val mediaItem = MediaItem.fromUri(mp4Urls[resolutionIndex])
    player.setMediaItem(mediaItem)
    player.prepare()
    player.playWhenReady = true
}

// Functions to update UI
fun updateInitialLoadTimeTextView(
    initialLoadTime: Long,
    resolutionIndex: Int,
    initialLoadTime360: MutableState<String>,
    initialLoadTime720: MutableState<String>,
    initialLoadTime1080: MutableState<String>
) {
    val initialLoadTimeInSeconds = initialLoadTime / 1000.0
    when (resolutionIndex) {
        0 -> initialLoadTime360.value = "${String.format("%.2f", initialLoadTimeInSeconds)} s"
        1 -> initialLoadTime720.value = "${String.format("%.2f", initialLoadTimeInSeconds)} s"
        2 -> initialLoadTime1080.value = "${String.format("%.2f", initialLoadTimeInSeconds)} s"
    }
}

fun updateBufferingTimeTextView(
    bufferingTime: Long,
    resolutionIndex: Int,
    bufferingTime360: MutableState<String>,
    bufferingTime720: MutableState<String>,
    bufferingTime1080: MutableState<String>
) {
    val bufferingTimeInSeconds = bufferingTime / 1000.0
    when (resolutionIndex) {
        0 -> bufferingTime360.value = "${String.format("%.2f", bufferingTimeInSeconds)} s"
        1 -> bufferingTime720.value = "${String.format("%.2f", bufferingTimeInSeconds)} s"
        2 -> bufferingTime1080.value = "${String.format("%.2f", bufferingTimeInSeconds)} s"
    }
}

fun updateDataUsageTextView(
    dataUsed: Long,
    resolutionIndex: Int,
    dataUsage360: MutableState<String>,
    dataUsage720: MutableState<String>,
    dataUsage1080: MutableState<String>
) {
    val dataUsedInMb = dataUsed / (1024.0 * 1024.0)
    val formattedDataUsed = String.format("%.2f", dataUsedInMb)
    when (resolutionIndex) {
        0 -> dataUsage360.value = "$formattedDataUsed MB"
        1 -> dataUsage720.value = "$formattedDataUsed MB"
        2 -> dataUsage1080.value = "$formattedDataUsed MB"
    }
}

// Function to reset test
fun resetTest(
    initialLoadTime360: MutableState<String>,
    bufferingTime360: MutableState<String>,
    dataUsage360: MutableState<String>,
    initialLoadTime720: MutableState<String>,
    bufferingTime720: MutableState<String>,
    dataUsage720: MutableState<String>,
    initialLoadTime1080: MutableState<String>,
    bufferingTime1080: MutableState<String>,
    dataUsage1080: MutableState<String>
) {
    initialLoadTime360.value = "--"
    bufferingTime360.value = "--"
    dataUsage360.value = "--"
    initialLoadTime720.value = "--"
    bufferingTime720.value = "--"
    dataUsage720.value = "--"
    initialLoadTime1080.value = "--"
    bufferingTime1080.value = "--"
    dataUsage1080.value = "--"
}
