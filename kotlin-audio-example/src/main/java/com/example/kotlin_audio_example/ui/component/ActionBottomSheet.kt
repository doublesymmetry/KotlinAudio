package com.example.kotlin_audio_example.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
@ExperimentalMaterial3Api
fun ActionBottomSheet(
    onDismiss: () -> Unit,
    onRandomMetadata: () -> Unit,
) {
    val modalBottomSheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        sheetState = modalBottomSheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        InnerSheet(onRandomMetadata = onRandomMetadata)
    }
}

@Composable
fun InnerSheet(onRandomMetadata: () -> Unit = {}) {
    // Add a button to perform an action when clicked
    Button(
        onClick = onRandomMetadata,
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Text("Metadata: Update Randomly")
    }
}

@Preview
@ExperimentalMaterial3Api
@Composable
fun ActionBottomSheetPreview() {
    InnerSheet()
}