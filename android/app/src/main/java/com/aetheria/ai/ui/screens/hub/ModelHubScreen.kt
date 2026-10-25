package com.aetheria.ai.ui.screens.hub

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aetheria.ai.data.local.entity.CustomModelEntity
import com.aetheria.ai.data.model.DownloadStatus
import com.aetheria.ai.data.model.OnDemandModel
import com.aetheria.ai.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelHubScreen(
    viewModel: ModelHubViewModel,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTabIndex by remember { mutableStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }

    val onDemandModels by viewModel.onDemandModels.collectAsState()
    val onlineModels by viewModel.onlineModels.collectAsState()
    val totalCount by viewModel.totalModelCount.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val testResults by viewModel.testResults.collectAsState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(RavenCard)
                            .border(0.8.dp, RavenBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = RavenTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Raven Model Hub",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = RavenTextPrimary
                        )
                        Text(
                            text = "Manage on-device GGUF & up to 100 cloud models",
                            style = MaterialTheme.typography.labelSmall,
                            color = RavenTextSecondary
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (selectedTabIndex == 1) {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null, tint = RavenTextPrimary) },
                    text = { Text("Add Model (${totalCount}/100)", fontWeight = FontWeight.Bold, color = RavenTextPrimary) },
                    containerColor = RavenViolet
                )
            }
        },
        containerColor = RavenVoid
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Elegant Tab Selector
            Surface(
                color = RavenSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, RavenBorder)
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    val tabModifier = Modifier
                        .weight(1f)
                        .height(40.dp)

                    Box(
                        modifier = tabModifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedTabIndex == 0) RavenBrandGradient else androidx.compose.ui.graphics.SolidColor(Color.Transparent))
                            .clickable { selectedTabIndex = 0 },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = if (selectedTabIndex == 0) RavenTextPrimary else RavenTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "📱 Offline GGUF",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (selectedTabIndex == 0) RavenTextPrimary else RavenTextSecondary
                            )
                        }
                    }

                    Box(
                        modifier = tabModifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedTabIndex == 1) RavenBrandGradient else androidx.compose.ui.graphics.SolidColor(Color.Transparent))
                            .clickable { selectedTabIndex = 1 },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CloudQueue,
                                contentDescription = null,
                                tint = if (selectedTabIndex == 1) RavenTextPrimary else RavenTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "☁️ Online (${totalCount}/100)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (selectedTabIndex == 1) RavenTextPrimary else RavenTextSecondary
                            )
                        }
                    }
                }
            }

            when (selectedTabIndex) {
                0 -> {
                    // Offline Hugging Face Models
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = RavenCard,
                                border = androidx.compose.foundation.BorderStroke(1.dp, RavenBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(RavenCyan.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.VerifiedUser, null, tint = RavenCyan, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Zero-Cloud Privacy Guarantee", fontWeight = FontWeight.Bold, color = RavenTextPrimary, fontSize = 13.sp)
                                        Text("Models run directly on this phone. No tokens leave your hardware.", color = RavenTextSecondary, fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        items(onDemandModels) { model ->
                            RavenOfflineCard(
                                model = model,
                                onDownload = { viewModel.downloadOnDemandModel(model) },
                                onSystemDownload = { viewModel.downloadViaSystemManager(model) },
                                onCancel = { viewModel.cancelDownload(model) },
                                onDelete = { viewModel.deleteOnDemandModel(model) }
                            )
                        }
                    }
                }

                1 -> {
                    // Online Cloud Models (Up to 100)
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Search bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Search your 100 models...", color = RavenTextMuted, fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = RavenTextSecondary) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = RavenCard,
                                unfocusedContainerColor = RavenCard,
                                focusedBorderColor = RavenViolet,
                                unfocusedBorderColor = RavenBorder,
                                focusedTextColor = RavenTextPrimary,
                                unfocusedTextColor = RavenTextPrimary
                            ),
                            singleLine = true
                        )

                        // Category Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 14.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("All", "⭐ Favorites", "General", "Coding", "Creative", "Roleplay").forEach { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { viewModel.setSelectedCategory(cat) },
                                    label = { Text(cat, fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = RavenViolet,
                                        selectedLabelColor = RavenTextPrimary,
                                        containerColor = RavenCard,
                                        labelColor = RavenTextSecondary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selectedCategory == cat,
                                        borderColor = RavenBorder
                                    )
                                )
                            }
                        }

                        // Model counter
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Active Models: $totalCount / 100 Maximum",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = RavenCyan
                            )
                            Text(
                                text = "Cloud Storage Sync",
                                style = MaterialTheme.typography.labelSmall,
                                color = RavenTextMuted
                            )
                        }

                        // List of models
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            if (onlineModels.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 40.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.CloudQueue, null, tint = RavenTextMuted, modifier = Modifier.size(48.dp))
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = if (searchQuery.isBlank()) "No cloud models configured yet.\nTap 'Add Model' below to add up to 100 models." else "No models matching \"$searchQuery\"",
                                                color = RavenTextSecondary,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }

                            items(onlineModels) { model ->
                                RavenOnlineCard(
                                    model = model,
                                    testResult = testResults[model.id],
                                    onTest = { viewModel.testConnection(model) },
                                    onToggleFavorite = { viewModel.toggleFavorite(model) },
                                    onDelete = { viewModel.deleteCustomModel(model) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AddModelDialog(
                onDismiss = { showAddDialog = false },
                onSave = { newModel ->
                    viewModel.addCustomModel(newModel) { success, msg ->
                        coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                        if (success) showAddDialog = false
                    }
                },
                onTestConnection = { tempModel ->
                    viewModel.cloudApiClient.testConnection(tempModel)
                }
            )
        }
    }
}

@Composable
fun RavenOfflineCard(
    model: OnDemandModel,
    onDownload: () -> Unit,
    onSystemDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = RavenCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, RavenBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (model.isBuiltIn) RavenAmber.copy(alpha = 0.2f) else RavenCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (model.isBuiltIn) Icons.Default.Bolt else Icons.Default.PhoneAndroid,
                            null,
                            tint = if (model.isBuiltIn) RavenAmber else RavenCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = model.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = RavenTextPrimary
                        )
                        Text(
                            text = model.sourceRepo,
                            style = MaterialTheme.typography.labelSmall,
                            color = RavenTextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                if (model.isBuiltIn) {
                    Surface(
                        color = RavenAmber.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Instant Ready",
                            color = RavenAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                } else if (model.isUncensored) {
                    Surface(
                        color = RavenRose.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Uncensored RP",
                            color = RavenRose,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Specs row
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SpecBadge(label = model.quantization)
                SpecBadge(label = model.sizeFormatted)
                SpecBadge(label = "RAM: ${model.ramRequired}")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = model.description,
                style = MaterialTheme.typography.bodySmall,
                color = RavenTextSecondary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            when (model.status) {
                DownloadStatus.NOT_DOWNLOADED -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onDownload,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = RavenViolet, contentColor = RavenTextPrimary),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download Model (${model.sizeFormatted})", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onSystemDownload,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, RavenBorder)
                        ) {
                            Icon(Icons.Default.NotificationsActive, null, modifier = Modifier.size(14.dp), tint = RavenSky)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download in Background (Notification)", fontSize = 11.sp, color = RavenSky)
                        }
                    }
                }
                DownloadStatus.DOWNLOADING -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val downloadedMB = model.downloadedBytes / (1024 * 1024)
                        val totalMB = model.totalBytes / (1024 * 1024)
                        val percent = (model.downloadProgress * 100).toInt()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (totalMB > 0) "$downloadedMB MB / $totalMB MB ($percent%)" else "Connecting to CDN...",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = RavenCyan
                            )
                            TextButton(
                                onClick = onCancel,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Cancel", color = RavenRose, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { model.downloadProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = RavenCyan,
                            trackColor = RavenSurface
                        )
                    }
                }
                DownloadStatus.DOWNLOADED -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (model.isBuiltIn) Icons.Default.Bolt else Icons.Default.CheckCircle,
                                null,
                                tint = if (model.isBuiltIn) RavenAmber else RavenEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (model.isBuiltIn) "Instant Engine Ready" else "100% Offline Ready",
                                color = if (model.isBuiltIn) RavenAmber else RavenEmerald,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (!model.isBuiltIn) {
                            OutlinedButton(
                                onClick = onDelete,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, RavenRose.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.DeleteOutline, null, tint = RavenRose, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete File", color = RavenRose, fontSize = 11.sp)
                            }
                        }
                    }
                }
                DownloadStatus.ERROR -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        model.errorMessage?.let { error ->
                            Surface(
                                color = RavenRose.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            ) {
                                Text(
                                    text = "Error: $error",
                                    color = RavenRose,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Download failed", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                            Button(
                                onClick = onDownload,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RavenViolet)
                            ) {
                                Text("Retry Download", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpecBadge(label: String) {
    Surface(
        color = RavenSurface,
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, RavenBorder)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = RavenTextSecondary,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun RavenOnlineCard(
    model: CustomModelEntity,
    testResult: Pair<Boolean, String>?,
    onTest: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = RavenCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, RavenBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(RavenAmethyst.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CloudQueue, null, tint = RavenAmethyst, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = model.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = RavenTextPrimary
                        )
                        Text(
                            text = "${model.providerType} • ${model.modelId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = RavenTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Row {
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (model.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (model.isFavorite) Color(0xFFFFD700) else RavenTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = RavenRose, modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (model.systemPrompt.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Persona: \"${model.systemPrompt.take(65)}...\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = RavenTextMuted
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = RavenSurface,
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, RavenBorder)
                ) {
                    Text(
                        text = model.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = RavenCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    testResult?.let { (success, latency) ->
                        Text(
                            text = latency,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (success) RavenEmerald else RavenRose
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    OutlinedButton(
                        onClick = onTest,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, RavenBorder)
                    ) {
                        Icon(Icons.Default.NetworkPing, null, modifier = Modifier.size(12.dp), tint = RavenSky)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Test Ping", fontSize = 10.sp, color = RavenSky)
                    }
                }
            }
        }
    }
}
