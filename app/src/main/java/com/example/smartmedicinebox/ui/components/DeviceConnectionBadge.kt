package com.example.smartmedicinebox.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smartmedicinebox.data.remote.DeviceConnectionState
import com.example.smartmedicinebox.data.remote.DeviceConnectionStatus

@Composable
fun DeviceConnectionBadge(connection: DeviceConnectionState) {
    val connected = connection.status == DeviceConnectionStatus.CONNECTED
    Surface(
        modifier = Modifier.heightIn(min = 40.dp),
        shape = MaterialTheme.shapes.small,
        color = when (connection.status) {
            DeviceConnectionStatus.CONNECTED -> MaterialTheme.colorScheme.primaryContainer
            DeviceConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.secondaryContainer
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            when (connection.status) {
                DeviceConnectionStatus.CHECKING -> CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                else -> Icon(
                    imageVector = if (connected) Icons.Default.Wifi else Icons.Default.WifiOff,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(connection.message, style = MaterialTheme.typography.labelMedium)
        }
    }
}
