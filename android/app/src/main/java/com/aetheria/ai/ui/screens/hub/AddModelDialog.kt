package com.aetheria.ai.ui.screens.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aetheria.ai.data.local.entity.CustomModelEntity
import com.aetheria.ai.ui.theme.*
import kotlinx.coroutines.launch

data class ProviderTemplate(
    val title: String,
    val providerType: String,
    val defaultBaseUrl: String,
    val defaultModelId: String,
    val defaultName: String
)

val PROVIDER_TEMPLATES = listOf(
    ProviderTemplate("DeepSeek (V3 / R1)", "OPENAI_COMPATIBLE", "https://api.deepseek.com/v1", "deepseek-chat", "DeepSeek Chat"),
    ProviderTemplate("Groq (Ultra-Fast LPU)", "OPENAI_COMPATIBLE", "https://api.groq.com/openai/v1", "llama-3.3-70b-versatile", "Groq Llama 3.3"),
    ProviderTemplate("Google Gemini", "GEMINI", "https://generativelanguage.googleapis.com/v1beta", "gemini-2.5-flash", "Gemini 2.5 Flash"),
    ProviderTemplate("OpenAI", "OPENAI_COMPATIBLE", "https://api.openai.com/v1", "gpt-4o-mini", "GPT-4o Mini"),
    ProviderTemplate("OpenRouter", "OPENAI_COMPATIBLE", "https://openrouter.ai/api/v1", "meta-llama/llama-3.3-70b-instruct", "OpenRouter Multi-Model"),
    ProviderTemplate("Ollama (Local Network)", "OPENAI_COMPATIBLE", "http://10.0.2.2:11434/v1", "llama3.2:1b", "Local Ollama Llama 3.2"),
    ProviderTemplate("Custom Endpoint", "OPENAI_COMPATIBLE", "", "", "Custom Agent")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddModelDialog(
    onDismiss: () -> Unit,
    onSave: (CustomModelEntity) -> Unit,
    onTestConnection: suspend (CustomModelEntity) -> Pair<Boolean, String>
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedTemplateIndex by remember { mutableStateOf(0) }
    val template = PROVIDER_TEMPLATES[selectedTemplateIndex]

    var modelName by remember { mutableStateOf(template.defaultName) }
    var providerType by remember { mutableStateOf(template.providerType) }
    var baseUrl by remember { mutableStateOf(template.defaultBaseUrl) }
    var modelId by remember { mutableStateOf(template.defaultModelId) }
    var apiKey by remember { mutableStateOf("") }
    var systemPrompt by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("General") }

    var showPassword by remember { mutableStateOf(false) }
    var testResultText by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = RavenCardElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, RavenBorderLight),
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Add Cloud Model",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = RavenTextPrimary
                        )
                        Text(
                            text = "Slot capacity: Up to 100 models",
                            style = MaterialTheme.typography.labelSmall,
                            color = RavenCyan
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = RavenTextSecondary)
                    }
                }

                Text("1. Quick Provider Preset", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = RavenViolet)

                // Provider Preset Selector
                var templateExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = templateExpanded,
                    onExpandedChange = { templateExpanded = !templateExpanded }
                ) {
                    OutlinedTextField(
                        value = template.title,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = templateExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = RavenSurface,
                            unfocusedContainerColor = RavenSurface,
                            focusedBorderColor = RavenViolet,
                            unfocusedBorderColor = RavenBorder,
                            focusedTextColor = RavenTextPrimary,
                            unfocusedTextColor = RavenTextPrimary
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = templateExpanded,
                        onDismissRequest = { templateExpanded = false },
                        modifier = Modifier.background(RavenCard)
                    ) {
                        PROVIDER_TEMPLATES.forEachIndexed { index, item ->
                            DropdownMenuItem(
                                text = { Text(item.title, color = RavenTextPrimary) },
                                onClick = {
                                    selectedTemplateIndex = index
                                    modelName = item.defaultName
                                    providerType = item.providerType
                                    baseUrl = item.defaultBaseUrl
                                    modelId = item.defaultModelId
                                    templateExpanded = false
                                }
                            )
                        }
                    }
                }

                Text("2. Model Configuration", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = RavenViolet)

                OutlinedTextField(
                    value = modelName,
                    onValueChange = { modelName = it },
                    label = { Text("Display Name") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = RavenSurface,
                        unfocusedContainerColor = RavenSurface,
                        focusedBorderColor = RavenViolet,
                        unfocusedBorderColor = RavenBorder,
                        focusedTextColor = RavenTextPrimary,
                        unfocusedTextColor = RavenTextPrimary
                    )
                )

                OutlinedTextField(
                    value = modelId,
                    onValueChange = { modelId = it },
                    label = { Text("Model ID (e.g. gpt-4o, deepseek-chat)") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = RavenSurface,
                        unfocusedContainerColor = RavenSurface,
                        focusedBorderColor = RavenViolet,
                        unfocusedBorderColor = RavenBorder,
                        focusedTextColor = RavenTextPrimary,
                        unfocusedTextColor = RavenTextPrimary
                    )
                )

                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Base API URL") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = RavenSurface,
                        unfocusedContainerColor = RavenSurface,
                        focusedBorderColor = RavenViolet,
                        unfocusedBorderColor = RavenBorder,
                        focusedTextColor = RavenTextPrimary,
                        unfocusedTextColor = RavenTextPrimary
                    )
                )

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = RavenTextSecondary
                            )
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = RavenSurface,
                        unfocusedContainerColor = RavenSurface,
                        focusedBorderColor = RavenViolet,
                        unfocusedBorderColor = RavenBorder,
                        focusedTextColor = RavenTextPrimary,
                        unfocusedTextColor = RavenTextPrimary
                    )
                )

                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it },
                    label = { Text("System Persona Prompt (Optional)") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = RavenSurface,
                        unfocusedContainerColor = RavenSurface,
                        focusedBorderColor = RavenViolet,
                        unfocusedBorderColor = RavenBorder,
                        focusedTextColor = RavenTextPrimary,
                        unfocusedTextColor = RavenTextPrimary
                    )
                )

                // Category chips
                Text("Tag / Persona", style = MaterialTheme.typography.labelSmall, color = RavenTextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("General", "Coding", "Creative", "Roleplay").forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RavenViolet,
                                selectedLabelColor = RavenTextPrimary,
                                containerColor = RavenSurface,
                                labelColor = RavenTextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = category == cat,
                                borderColor = RavenBorder
                            )
                        )
                    }
                }

                // Test Connection Feedback
                testResultText?.let { result ->
                    Surface(
                        color = RavenSurface,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, RavenBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = result,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = if (result.startsWith("Connected")) RavenEmerald else RavenRose,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val temp = CustomModelEntity(
                                name = modelName,
                                providerType = providerType,
                                baseUrl = baseUrl,
                                modelId = modelId,
                                apiKey = apiKey,
                                systemPrompt = systemPrompt,
                                category = category
                            )
                            isTesting = true
                            testResultText = "Pinging endpoint..."
                            coroutineScope.launch {
                                val (success, message) = onTestConnection(temp)
                                isTesting = false
                                testResultText = if (success) "Connected: $message" else "Error: $message"
                            }
                        },
                        enabled = !isTesting && baseUrl.isNotBlank() && modelId.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RavenBorder)
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = RavenCyan)
                        } else {
                            Text("Test Ping", color = RavenCyan)
                        }
                    }

                    Button(
                        onClick = {
                            val entity = CustomModelEntity(
                                name = modelName.ifBlank { modelId },
                                providerType = providerType,
                                baseUrl = baseUrl,
                                modelId = modelId,
                                apiKey = apiKey,
                                systemPrompt = systemPrompt,
                                category = category
                            )
                            onSave(entity)
                        },
                        enabled = baseUrl.isNotBlank() && modelId.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = RavenViolet, contentColor = RavenTextPrimary)
                    ) {
                        Text("Save Model", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
