package com.yelp.cursair.presentation.mouse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.yelp.cursair.domain.ConnectionManager
import com.yelp.cursair.ui.theme.CursairTheme
import kotlinx.coroutines.launch

/**
 * The main screen of the app after a successful connection.
 * @param onDisconnect Callback invoked when the user chooses to disconnect. This should
 * navigate the user back to the onboarding/connection screen.
 */
@Composable
fun MouseScreen(
    onDisconnect: () -> Unit
) {
    val scope = rememberCoroutineScope()

    CursairTheme {
        Scaffold { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Connected!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(48.dp))

                Button(
                    onClick = {
                        scope.launch {
                            // Example: Sending a JSON message.
                            // You can replace this with any string message.
                            val message = "{\"type\":\"test\",\"payload\":\"Hello from Android!\"}"
                            ConnectionManager.sendMessage(message)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Send Test Message")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        scope.launch {
                            ConnectionManager.disconnect()
                            // Navigate back after disconnecting
                            onDisconnect()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Disconnect")
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun MainScreenPreview() {
    MouseScreen (onDisconnect = {})
}