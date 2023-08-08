package com.example.kotlin_audio_example.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.kotlin_audio_example.ui.theme.KotlinAudioTheme

@Composable
fun PlayerControls(
    modifier: Modifier = Modifier,
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {},
    isPaused: Boolean,
    onPlayPause: () -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        IconButton(
            onClick = onPrevious,
            modifier = Modifier
                .height(48.dp)
                .width(48.dp)
        ) {
            Icon(
                Icons.Rounded.FastRewind,
                contentDescription = "Previous",
                modifier = Modifier
                    .height(48.dp)
                    .width(48.dp)
            )
        }
        Spacer(modifier = Modifier.width(20.dp))
        IconButton(
            onClick = onPlayPause,
            modifier = Modifier
                .height(68.dp)
                .width(68.dp)
        ) {
            Icon(
                if (isPaused) {
                    Icons.Rounded.PlayCircle
                } else {
                    Icons.Rounded.PauseCircle
                },
                contentDescription = "Play",
                modifier = Modifier
                    .height(68.dp)
                    .width(68.dp)
                    .clip(RoundedCornerShape(50))
            )
        }
        Spacer(modifier = Modifier.width(20.dp))
        IconButton(
            onClick = onNext,
            modifier = Modifier
                .height(48.dp)
                .width(48.dp)
        ) {
            Icon(
                Icons.Rounded.FastForward,
                contentDescription = "Next",
                modifier = Modifier
                    .height(48.dp)
                    .width(48.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlayerControlsPreview() {
    KotlinAudioTheme {
        Column {
            PlayerControls(isPaused = true)
            PlayerControls(isPaused = false)
        }
    }
}
