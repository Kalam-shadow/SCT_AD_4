package com.example.qrnova

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

data class AiMessage(val content: String, val isFromUser: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantOverlay(
    lastScannedResult: String?,
    onClose: () -> Unit
) {
    var userMessage by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<AiMessage>() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "AI Assistant",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Close")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.LightGray, shape = RoundedCornerShape(8.dp))
        ) {
            items(messages) { message ->
                MessageBubble(message = message)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = userMessage,
            onValueChange = { userMessage = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Enter your message") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (userMessage.isNotBlank()) {
                    messages.add(AiMessage(userMessage, true))
                    //clear the message to show the next one.
                    userMessage = ""
                    //send the message to the Ai
                    //TODO implement the response
                    messages.add(AiMessage("you said ${userMessage} and the last scanned result is $lastScannedResult",false))
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Send")
        }
    }
}

@Composable
fun MessageBubble(message: AiMessage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if(message.isFromUser){
            Text(
                text = message.content,
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(8.dp))
                    .padding(8.dp),
                textAlign = TextAlign.End,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }else {
            Text(
                text = message.content,
                modifier = Modifier
                    .weight(1f)
                    .background(Color.White, shape = RoundedCornerShape(8.dp))
                    .padding(8.dp),
                textAlign = TextAlign.Start,
                color = Color.Black
            )
        }

    }
}