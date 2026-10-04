package com.photosoap.android.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.photosoap.android.BuildConfig
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
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
            SettingsGroup(stringResource(R.string.settings_experience)) {
                SettingsToggle(
                    title = stringResource(R.string.settings_haptics),
                    subtitle = stringResource(R.string.settings_haptics_desc),
                    checked = state.hapticsEnabled,
                    onCheckedChange = viewModel::toggleHaptics,
                )
            }

            SettingsGroup(stringResource(R.string.settings_deletion)) {
                SettingsToggle(
                    title = stringResource(R.string.settings_use_delete_list),
                    subtitle = stringResource(R.string.settings_use_delete_list_desc),
                    checked = state.useDeleteQueue,
                    onCheckedChange = viewModel::toggleDeleteQueue,
                )
            }

            SettingsGroup(stringResource(R.string.settings_privacy)) {
                SettingsTextAction(
                    text = stringResource(R.string.settings_photo_access),
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
                    subtitle = stringResource(R.string.settings_share_analytics_desc),
                    checked = state.analyticsEnabled,
                    onCheckedChange = viewModel::toggleAnalytics,
                )
                SettingsTextAction(
                    text = stringResource(R.string.settings_privacy_policy),
                    onClick = onNavigateToPrivacyPolicy,
                )
                SettingsTextAction(
                    text = stringResource(R.string.settings_support),
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
                                context.getString(R.string.support_email_fallback, SUPPORT_EMAIL),
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
}

private const val SUPPORT_EMAIL = "photosoap@brokenmoha.de"

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        content()
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun SettingsTextAction(
    text: String,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = text)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
