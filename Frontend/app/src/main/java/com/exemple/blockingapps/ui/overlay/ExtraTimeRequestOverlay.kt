package com.exemple.blockingapps.ui.overlay

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.exemple.blockingapps.data.model.ExtraTimeRequest

@Composable
fun ExtraTimeRequestOverlay(
    request: ExtraTimeRequest,
    onClose: () -> Unit
) {
    val hours = request.requestedMinutes / 60
    val minutes = request.requestedMinutes % 60

    AlertDialog(
        onDismissRequest = {},
        title = {
            Text("Extra time requested")
        },
        text = {
            Column {
                Text(
                    "${request.childName} has requested extra time",
                    fontWeight = FontWeight.Medium
                )

                Spacer(Modifier.height(12.dp))

                Text("App: ${request.appName}")
                Spacer(Modifier.height(6.dp))

                Text("Requested time: ${hours}h ${minutes}m")

                if (request.reason.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Reason: ${request.reason}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) {
                Text("Close")
            }
        }
    )
}
