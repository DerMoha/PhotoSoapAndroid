package com.photosoap.android.ui.settings

import android.os.Build
import com.photosoap.android.domain.model.AccentColor
import com.photosoap.android.ui.theme.accentSwatch
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import com.photosoap.android.domain.model.ThemeMode
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import com.photosoap.android.BuildConfig
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.photosoap.android.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit = {},
    onNavigateToDeveloperOptions: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val supportFallback = stringResource(R.string.support_email_fallback, SUPPORT_EMAIL)
    var showAccentPicker by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsGroup(stringResource(R.string.settings_appearance)) {
                SettingsTextAction(
                    text = stringResource(R.string.settings_theme),
                    icon = Icons.Outlined.DarkMode,
                    subtitle = stringResource(state.themeMode.labelResource()),
                    onClick = { showThemePicker = true },
                )
                SettingsTextAction(
                    text = stringResource(R.string.settings_accent_color),
                    icon = Icons.Outlined.Palette,
                    subtitle = stringResource(if (state.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                        R.string.settings_dynamic_color else state.accentColor.labelResource()),
                    onClick = { showAccentPicker = true },
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SettingsToggle(
                        title = stringResource(R.string.settings_dynamic_color),
                    icon = Icons.Outlined.Palette,
                        subtitle = stringResource(R.string.settings_dynamic_color_desc),
                        checked = state.dynamicColor,
                        onCheckedChange = viewModel::toggleDynamicColor,
                    )
                }
            }

            SettingsGroup(stringResource(R.string.settings_experience)) {
                SettingsToggle(
                    title = stringResource(R.string.settings_haptics),
                    icon = Icons.Outlined.Vibration,
                    subtitle = stringResource(R.string.settings_haptics_desc),
                    checked = state.hapticsEnabled,
                    onCheckedChange = viewModel::toggleHaptics,
                )
            }

            SettingsGroup(stringResource(R.string.settings_deletion)) {
                SettingsToggle(
                    title = stringResource(R.string.settings_use_delete_list),
                    icon = Icons.Outlined.DeleteOutline,
                    subtitle = stringResource(R.string.settings_use_delete_list_desc),
                    checked = state.useDeleteQueue,
                    onCheckedChange = viewModel::toggleDeleteQueue,
                )
            }

            SettingsGroup(stringResource(R.string.settings_privacy)) {
                SettingsTextAction(
                    text = stringResource(R.string.settings_photo_access),
                    icon = Icons.Outlined.PhotoLibrary,
                    subtitle = stringResource(R.string.settings_photo_access_desc),
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            },
                        )
                    },
                )
                SettingsToggle(
                    title = stringResource(R.string.settings_share_analytics),
                    icon = Icons.Outlined.Insights,
                    subtitle = stringResource(R.string.settings_share_analytics_desc),
                    checked = state.analyticsEnabled,
                    onCheckedChange = viewModel::toggleAnalytics,
                )
                SettingsTextAction(
                    text = stringResource(R.string.settings_privacy_policy),
                    icon = Icons.Outlined.PrivacyTip,
                    onClick = onNavigateToPrivacyPolicy,
                )
                SettingsTextAction(
                    text = stringResource(R.string.settings_support),
                    icon = Icons.Outlined.SupportAgent,
                    onClick = {
                        val emailUri = Uri.Builder()
                            .scheme("mailto")
                            .opaquePart(SUPPORT_EMAIL)
                            .appendQueryParameter("subject", "PhotoSoap Android Support")
                            .appendQueryParameter(
                                "body",
                                "PhotoSoap ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n\n",
                            )
                            .build()
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_SENDTO, emailUri))
                        }.onFailure {
                            android.widget.Toast.makeText(context,
                                supportFallback,
                                android.widget.Toast.LENGTH_LONG).show()
                        }
                    },
                )
            }

            SettingsGroup(stringResource(R.string.settings_about)) {
                Text(
                    text = stringResource(
                        R.string.settings_version,
                        BuildConfig.VERSION_NAME,
                        BuildConfig.VERSION_CODE,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }

            if (com.photosoap.android.BuildConfig.DEBUG) {
                SettingsGroup("Developer") {
                    TextButton(
                        onClick = onNavigateToDeveloperOptions,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "Open Developer Options",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
    if (showAccentPicker) {
        AlertDialog(
            onDismissRequest = { showAccentPicker = false },
            title = { Text(stringResource(R.string.settings_accent_color)) },
            text = {
                Column {
                    Text(stringResource(R.string.settings_accent_color_desc), style = MaterialTheme.typography.bodyMedium)
                    AccentColor.entries.forEach { accent ->
                        Row(
                            modifier = Modifier.fillMaxWidth().selectable(
                                selected = !(state.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) && state.accentColor == accent,
                                role = Role.RadioButton,
                                onClick = { viewModel.setAccentColor(accent); showAccentPicker = false },
                            ).padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = !(state.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) && state.accentColor == accent, onClick = null)
                            Spacer(Modifier.width(12.dp))
                            Spacer(Modifier.size(24.dp).background(accentSwatch(accent), CircleShape))
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(accent.labelResource()))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAccentPicker = false }) { Text(stringResource(R.string.close)) } },
        )
    }
    if (showThemePicker) {
        AlertDialog(
            onDismissRequest = { showThemePicker = false },
            title = { Text(stringResource(R.string.settings_theme)) },
            text = {
                Column {
                    ThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier.fillMaxWidth().selectable(
                                selected = state.themeMode == mode,
                                role = Role.RadioButton,
                                onClick = { viewModel.setThemeMode(mode); showThemePicker = false },
                            ).padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = state.themeMode == mode, onClick = null)
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(mode.labelResource()))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showThemePicker = false }) { Text(stringResource(R.string.close)) } },
        )
    }

}

private const val SUPPORT_EMAIL = "photosoap@brokenmoha.de"

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp, top = 8.dp))
        Surface(shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainer) {
            Column { content() }
        }
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        content = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    )
}

@Composable
private fun SettingsTextAction(
    text: String,
    subtitle: String? = null,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    ListItem(
        onClick = onClick,
        content = { Text(text) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    )
}

private fun ThemeMode.labelResource(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

private fun AccentColor.labelResource(): Int = when (this) {
    AccentColor.TEAL -> R.string.accent_teal
    AccentColor.BLUE -> R.string.accent_blue
    AccentColor.PURPLE -> R.string.accent_purple
    AccentColor.ROSE -> R.string.accent_rose
}
