package com.aetheria.ai.ui.screens.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aetheria.ai.data.local.entity.ChatMessageEntity
import com.aetheria.ai.data.model.DefaultOnDemandCatalog
import com.aetheria.ai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateToModelHub: () -> Unit
) {
    val context = LocalContext.current
    val isOnline by viewModel.isOnline.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val customModels by viewModel.customModels.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val currentStreamingText by viewModel.currentStreamingText.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showModelMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, currentStreamingText) {
        if (messages.isNotEmpty() || currentStreamingText.isNotEmpty()) {
            listState.animateScrollToItem((messages.size).coerceAtLeast(0))
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = RavenVoid,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 0.5.dp, color = RavenBorder, shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Brand Logo & Network Status
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(RavenBrandGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Raven",
                                tint = RavenTextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Raven",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = RavenTextPrimary,
                                letterSpacing = 0.5.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (isOnline) RavenEmerald else RavenAmber)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (isOnline) "Online Mode" else "100% Offline Mode",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isOnline) RavenEmerald else RavenAmber
                                )
                            }
                        }
                    }

                    // Action controls
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Model Switcher Pill
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { showModelMenu = true }
                                .border(0.8.dp, RavenBorder, RoundedCornerShape(20.dp)),
                            color = RavenCard
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val (modelName, isLocal) = when (val m = selectedModel) {
                                    is SelectedModel.Offline -> Pair(m.model.name, true)
                                    is SelectedModel.Online -> Pair(m.model.name, false)
                                }
                                Icon(
                                    imageVector = if (isLocal) Icons.Default.PhoneAndroid else Icons.Default.CloudQueue,
                                    contentDescription = null,
                                    tint = if (isLocal) RavenCyan else RavenAmethyst,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = modelName.take(13),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = RavenTextPrimary
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = RavenTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showModelMenu,
                                onDismissRequest = { showModelMenu = false },
                                modifier = Modifier.background(RavenCard).border(1.dp, RavenBorder, RoundedCornerShape(12.dp))
                            ) {
                                Text(
                                    text = "📱 OFFLINE GGUF MODELS",
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = RavenCyan
                                )
                                DefaultOnDemandCatalog.models.forEach { model ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(model.name, fontWeight = FontWeight.SemiBold, color = RavenTextPrimary)
                                                Text("${model.sizeFormatted} • ${model.quantization}", style = MaterialTheme.typography.labelSmall, color = RavenTextSecondary)
                                            }
                                        },
                                        leadingIcon = { Icon(Icons.Default.DownloadDone, null, tint = RavenCyan) },
                                        onClick = {
                                            viewModel.selectModel(SelectedModel.Offline(model))
                                            showModelMenu = false
                                        }
                                    )
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = RavenBorder)

                                Text(
                                    text = "☁️ ONLINE CLOUD MODELS (${customModels.size}/100)",
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = RavenAmethyst
                                )
                                if (customModels.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("+ Add Custom Model (Up to 100)", color = RavenCyan) },
                                        leadingIcon = { Icon(Icons.Default.AddCircleOutline, null, tint = RavenCyan) },
                                        onClick = {
                                            showModelMenu = false
                                            onNavigateToModelHub()
                                        }
                                    )
                                } else {
                                    customModels.forEach { model ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(model.name, fontWeight = FontWeight.SemiBold, color = RavenTextPrimary)
                                                    Text("${model.providerType} • ${model.modelId}", style = MaterialTheme.typography.labelSmall, color = RavenTextSecondary)
                                                }
                                            },
                                            leadingIcon = { Icon(Icons.Default.Cloud, null, tint = RavenAmethyst) },
                                            onClick = {
                                                viewModel.selectModel(SelectedModel.Online(model))
                                                showModelMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Hub navigation button
                        IconButton(
                            onClick = onNavigateToModelHub,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(RavenCard)
                                .border(0.8.dp, RavenBorder, CircleShape)
                        ) {
                            Icon(Icons.Default.Hub, contentDescription = "Model Hub", tint = RavenCyan, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        containerColor = RavenVoid
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Chat messages list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp)
            ) {
                // Empty state greeting
                if (messages.isEmpty() && currentStreamingText.isEmpty()) {
                    item {
                        EmptyStateRavenHero(
                            onSelectPrompt = { prompt ->
                                viewModel.sendMessage(prompt)
                            }
                        )
                    }
                }

                items(messages) { msg ->
                    RavenChatBubble(
                        message = msg,
                        onCopy = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Raven AI", msg.content))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                if (isGenerating && currentStreamingText.isNotEmpty()) {
                    item {
                        RavenStreamingBubble(text = currentStreamingText)
                    }
                }
            }

            // Floating Input Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .navigationBarsPadding(),
                color = RavenCardElevated,
                shape = RoundedCornerShape(28.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, RavenBorderLight)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                "Message Raven (offline or online)...",
                                color = RavenTextMuted,
                                fontSize = 14.sp
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = RavenTextPrimary,
                            unfocusedTextColor = RavenTextPrimary,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    val canSend = inputText.isNotBlank() && !isGenerating

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (canSend) RavenBrandGradient else androidx.compose.ui.graphics.SolidColor(RavenCard))
                            .clickable(enabled = canSend) {
                                val query = inputText
                                inputText = ""
                                viewModel.sendMessage(query)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (canSend) RavenTextPrimary else RavenTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateRavenHero(
    onSelectPrompt: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Glowing Hero Icon
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(RavenBrandGradient)
                .shadow(elevation = 20.dp, shape = RoundedCornerShape(26.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = RavenTextPrimary,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Raven AI",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = RavenTextPrimary,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Dual-Engine: 100% Offline GGUF + Up to 100 Cloud Models",
            style = MaterialTheme.typography.bodyMedium,
            color = RavenTextSecondary
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Quick starter prompts
        Text(
            text = "TRY ASKING",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = RavenTextMuted,
            letterSpacing = 1.5.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        val prompts = listOf(
            "⚡ Write a clean Kotlin coroutine flow",
            "🐬 Roleplay a futuristic space navigator",
            "🧠 Explain quantum computing simply",
            "📴 What are the benefits of offline AI?"
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            prompts.forEach { prompt ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = RavenCard,
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, RavenBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelectPrompt(prompt) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.bodyMedium,
                            color = RavenTextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowOutward,
                            contentDescription = null,
                            tint = RavenCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RavenChatBubble(
    message: ChatMessageEntity,
    onCopy: () -> Unit
) {
    val isUser = message.role == "user"

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        if (isUser) {
            // User message bubble with gradient
            Surface(
                shape = RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp),
                color = Color.Transparent,
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .background(RavenUserBubbleGradient, RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    lineHeight = 22.sp
                )
            }
        } else {
            // Assistant message bubble
            Surface(
                shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp),
                color = RavenCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, RavenBorderLight),
                modifier = Modifier.widthIn(max = 330.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (message.isOffline) Icons.Default.PhoneAndroid else Icons.Default.CloudQueue,
                                contentDescription = null,
                                tint = if (message.isOffline) RavenCyan else RavenAmethyst,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = message.modelUsed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (message.isOffline) RavenCyan else RavenAmethyst
                            )
                        }

                        IconButton(
                            onClick = onCopy,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = RavenTextMuted,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = RavenTextPrimary,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}

@Composable
fun RavenStreamingBubble(text: String) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp),
            color = RavenCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, RavenCyan.copy(alpha = 0.5f)),
            modifier = Modifier.widthIn(max = 330.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 2.dp,
                        color = RavenCyan
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Raven generating response...",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = RavenCyan
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = RavenTextPrimary,
                    lineHeight = 22.sp
                )
            }
        }
    }
}
