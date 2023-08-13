package com.example.kotlin_audio_example.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.kotlin_audio_example.R
import com.example.kotlin_audio_example.ext.millisecondsToString
import com.example.kotlin_audio_example.ui.theme.KotlinAudioTheme

@Composable
fun TrackDisplay(
    title: String,
    artist: String,
    artwork: String,
    position: Long,
    duration: Long,
    isLive: Boolean,
    modifier: Modifier = Modifier,
    onSeek: (Long) -> Unit = {},
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        if (artwork.isEmpty())
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_background),
                contentDescription = "Album Cover",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        else
            AsyncImage(
                model = artwork,
                contentDescription = "Album Cover",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .padding(top = 48.dp)
            )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = artist,
            style = MaterialTheme.typography.bodyMedium,
        )
        if (isLive)
            Text(
                text = "Live",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 16.dp)
            )
        else
            Column {
                Slider(
                    value = if (duration == 0L) {
                        0f
                    } else {
                        position.toFloat() / duration.toFloat()
                    },
                    onValueChange = {
                        onSeek((it * duration).toLong())
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 8.dp, end = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp),
                ) {
                    Text(
                        text = position.millisecondsToString(),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = duration.millisecondsToString(),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
    }
}

@Preview(showBackground = true)
@Composable
fun TrackDisplayPreview() {
    KotlinAudioTheme {
        TrackDisplay(
            title = "Title",
            artist = "Artist",
            artwork = "",
            position = 1000,
            duration = 6000,
            isLive = false,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TrackDisplayLivePreview() {
    KotlinAudioTheme {
        TrackDisplay(
            title = "Title",
            artist = "Artist",
            artwork = "",
            position = 1000,
            duration = 6000,
            isLive = true,
        )
    }
}
