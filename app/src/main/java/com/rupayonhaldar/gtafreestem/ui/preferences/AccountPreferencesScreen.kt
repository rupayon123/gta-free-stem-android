package com.rupayonhaldar.gtafreestem.ui.preferences

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rupayonhaldar.gtafreestem.data.local.AppThemePreference
import com.rupayonhaldar.gtafreestem.data.local.DisplayNameSaveResult
import com.rupayonhaldar.gtafreestem.data.local.LocalAccountDataDeletionResult
import com.rupayonhaldar.gtafreestem.localization.AppLanguage

private val PreferenceScreenTopPadding = 16.dp
private val PreferenceCardSpacing = 14.dp
private val PreferenceCardElevation = 1.5.dp

/**
 * Local profile and app preferences UI. This screen creates no account, network connection,
 * notification schedule, or permission request; callers own navigation and persistence callbacks.
 */
@Composable
fun AccountPreferencesScreen(
    state: AppPreferencesUiState,
    onSaveDisplayName: (String) -> DisplayNameSaveResult,
    onClearProfile: () -> Boolean,
    onLanguageSelected: (AppLanguage?) -> Boolean,
    onThemeSelected: (AppThemePreference) -> Boolean,
    onOpportunityAlertsPreferredChanged: (Boolean) -> Boolean,
    onOpenSavedLibrary: () -> Unit,
    onDeleteAllLocalData: () -> LocalAccountDataDeletionResult,
    onOpenSupport: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenTerms: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val labels = remember(state) { AccountPreferencesLabels(state) }
    val focusManager = LocalFocusManager.current
    var displayNameDraft by rememberSaveable { mutableStateOf(state.displayName.orEmpty()) }
    var lastAppliedDisplayName by rememberSaveable { mutableStateOf(state.displayName) }
    var displayNameError by rememberSaveable { mutableStateOf<String?>(null) }
    var statusMessage by rememberSaveable(state.resolvedLanguage) { mutableStateOf<String?>(null) }
    var languageDialogVisible by rememberSaveable { mutableStateOf(false) }
    var confirmation by rememberSaveable { mutableStateOf<PendingConfirmation?>(null) }

    LaunchedEffect(state.displayName) {
        if (state.displayName != lastAppliedDisplayName) {
            displayNameDraft = state.displayName.orEmpty()
            displayNameError = null
            lastAppliedDisplayName = state.displayName
        }
    }

    fun saveDisplayName() {
        focusManager.clearFocus()
        displayNameError = null
        when (
            runCatching { onSaveDisplayName(displayNameDraft) }
                .getOrDefault(DisplayNameSaveResult.STORAGE_ERROR)
        ) {
            DisplayNameSaveResult.SAVED -> statusMessage = labels.profileSaved
            DisplayNameSaveResult.INVALID -> displayNameError = labels.displayNameInvalid
            DisplayNameSaveResult.STORAGE_ERROR -> statusMessage = labels.saveFailed
        }
    }

    val layoutDirection = if (state.isRightToLeft) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding(),
        ) {
            val horizontalPadding = if (maxWidth >= 600.dp) 24.dp else PreferenceScreenTopPadding

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .testTag(AccountPreferencesTestTags.SCREEN),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp)
                        .padding(
                            start = horizontalPadding,
                            top = PreferenceScreenTopPadding,
                            end = horizontalPadding,
                            bottom = 40.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(PreferenceCardSpacing),
                ) {
                    ScreenHeading(labels)

                    statusMessage?.let { message ->
                        StatusMessage(message)
                    }

                    PreferencesCard {
                        SectionHeading(labels.profileTitle)
                        Text(
                            text = state.displayName ?: labels.guest,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = labels.profileOnDevice,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedTextField(
                            value = displayNameDraft,
                            onValueChange = { candidate ->
                                displayNameDraft = candidate
                                displayNameError = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(AccountPreferencesTestTags.DISPLAY_NAME),
                            label = { Text(labels.displayName) },
                            supportingText = {
                                Text(displayNameError ?: labels.displayNameHelp)
                            },
                            isError = displayNameError != null,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(onDone = { saveDisplayName() }),
                        )
                        Button(
                            onClick = ::saveDisplayName,
                            enabled = displayNameDraft.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag(AccountPreferencesTestTags.SAVE_PROFILE),
                        ) {
                            Text(labels.save, textAlign = TextAlign.Center)
                        }
                        if (state.displayName != null) {
                            DestructiveOutlinedButton(
                                label = labels.clearProfile,
                                onClick = { confirmation = PendingConfirmation.CLEAR_PROFILE },
                                testTag = AccountPreferencesTestTags.CLEAR_PROFILE,
                            )
                        }
                    }

                    PreferencesCard {
                        SectionHeading(labels.savedTitle)
                        Text(
                            text = labels.savedExplanation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedButton(
                            onClick = onOpenSavedLibrary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag(AccountPreferencesTestTags.SAVED_LIBRARY),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            Text(
                                text = labels.openSavedLibrary,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start,
                            )
                        }
                    }

                    PreferencesCard {
                        SectionHeading(labels.languageTitle)
                        OutlinedButton(
                            onClick = { languageDialogVisible = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .testTag(AccountPreferencesTestTags.LANGUAGE_SELECTOR),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = selectedLanguageLabel(state, labels),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                                if (state.followsSystemLanguage) {
                                    Text(
                                        text = labels.followsDeviceLanguage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SectionHeading(labels.themeTitle)
                        AppThemePreference.entries.forEach { theme ->
                            ThemeChoiceRow(
                                label = labels.theme(theme),
                                selected = state.theme == theme,
                                onClick = {
                                    val saved = runCatching { onThemeSelected(theme) }
                                        .getOrDefault(false)
                                    if (!saved) statusMessage = labels.saveFailed
                                },
                            )
                        }
                    }

                    PreferencesCard {
                        SectionHeading(labels.alertsTitle)
                        PreferenceToggleRow(
                            label = labels.alertsPreference,
                            supportingText = labels.alertsNotActive,
                            checked = state.opportunityAlertsPreferred,
                            onCheckedChange = { preferred ->
                                val saved = runCatching {
                                    onOpportunityAlertsPreferredChanged(preferred)
                                }.getOrDefault(false)
                                if (!saved) statusMessage = labels.saveFailed
                            },
                        )
                    }

                    PreferencesCard {
                        SectionHeading(labels.helpAndLegalTitle)
                        Text(
                            text = labels.legalExplanation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        LegalAction(labels.support, onOpenSupport)
                        LegalAction(labels.privacyPolicy, onOpenPrivacyPolicy)
                        LegalAction(labels.terms, onOpenTerms)
                    }

                    PreferencesCard(
                        borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.55f),
                    ) {
                        SectionHeading(labels.localDataTitle)
                        Text(
                            text = labels.localDataExplanation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        DestructiveOutlinedButton(
                            label = if (state.displayName == null) {
                                labels.deleteAllLocalData
                            } else {
                                labels.deleteProfileAndLocalData
                            },
                            onClick = { confirmation = PendingConfirmation.DELETE_LOCAL_DATA },
                            testTag = AccountPreferencesTestTags.DELETE_LOCAL_DATA,
                        )
                    }
                }
            }
        }

        if (languageDialogVisible) {
            LanguageSelectionDialog(
                state = state,
                labels = labels,
                onDismiss = { languageDialogVisible = false },
                onLanguageSelected = { language ->
                    val saved = runCatching { onLanguageSelected(language) }.getOrDefault(false)
                    if (saved) {
                        languageDialogVisible = false
                    } else {
                        statusMessage = labels.saveFailed
                    }
                },
            )
        }

        when (confirmation) {
            PendingConfirmation.CLEAR_PROFILE -> ConfirmLocalActionDialog(
                title = labels.clearProfileTitle,
                explanation = labels.clearProfileExplanation,
                confirmLabel = labels.clearProfile,
                cancelLabel = labels.cancel,
                onDismiss = { confirmation = null },
                onConfirm = {
                    confirmation = null
                    val cleared = runCatching(onClearProfile).getOrDefault(false)
                    if (cleared) {
                        displayNameDraft = ""
                        statusMessage = labels.profileCleared
                    } else {
                        statusMessage = labels.saveFailed
                    }
                },
            )

            PendingConfirmation.DELETE_LOCAL_DATA -> ConfirmLocalActionDialog(
                title = labels.deleteConfirmationTitle,
                explanation = labels.deleteConfirmationExplanation,
                confirmLabel = if (state.displayName == null) {
                    labels.deleteAllLocalData
                } else {
                    labels.deleteProfileAndLocalData
                },
                cancelLabel = labels.cancel,
                onDismiss = { confirmation = null },
                onConfirm = {
                    confirmation = null
                    val hadProfile = state.displayName != null
                    val result = runCatching(onDeleteAllLocalData).getOrElse {
                        LocalAccountDataDeletionResult(
                            profileDeleted = false,
                            searchHistoryDeleted = false,
                            savedOpportunitiesDeleted = false,
                        )
                    }
                    if (result.profileDeleted) displayNameDraft = ""
                    statusMessage = if (result.allLocalAccountDataDeleted) {
                        labels.deletionSuccess(hadProfile)
                    } else {
                        labels.deletionFailure(result)
                    }
                },
            )

            null -> Unit
        }
    }
}

@Composable
private fun ScreenHeading(labels: AccountPreferencesLabels) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = labels.screenTitle,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = labels.screenSummary,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatusMessage(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite }
            .testTag(AccountPreferencesTestTags.STATUS),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = PreferenceCardElevation),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun PreferencesCard(
    borderColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.outlineVariant,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = PreferenceCardElevation),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

@Composable
private fun SectionHeading(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun ThemeChoiceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val container = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            color = container,
            shape = MaterialTheme.shapes.small,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            RadioButton(
                selected = selected,
                onClick = null,
                modifier = Modifier.clearAndSetSemantics {},
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun PreferenceToggleRow(
    label: String,
    supportingText: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .semantics(mergeDescendants = true) {}
            .padding(vertical = 8.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}

@Composable
private fun LegalAction(label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(horizontal = 6.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start,
        )
    }
}

@Composable
private fun DestructiveOutlinedButton(
    label: String,
    onClick: () -> Unit,
    testTag: String,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .testTag(testTag),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(label, textAlign = TextAlign.Center)
    }
}

@Composable
private fun LanguageSelectionDialog(
    state: AppPreferencesUiState,
    labels: AccountPreferencesLabels,
    onDismiss: () -> Unit,
    onLanguageSelected: (AppLanguage?) -> Unit,
) {
    val choices = remember(state, labels) { accountLanguageChoices(state, labels) }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(AccountPreferencesTestTags.LANGUAGE_DIALOG),
        title = {
            Text(
                text = labels.chooseLanguage,
                modifier = Modifier.semantics { heading() },
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .testTag(AccountPreferencesTestTags.LANGUAGE_LIST),
            ) {
                items(
                    items = choices,
                    key = { choice -> choice.language?.catalogCode ?: "system" },
                ) { choice ->
                    LanguageChoiceRow(
                        choice = choice,
                        selected = state.selectedLanguage == choice.language,
                        onClick = { onLanguageSelected(choice.language) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(labels.done)
            }
        },
    )
}

@Composable
private fun LanguageChoiceRow(
    choice: AccountLanguageChoice,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            modifier = Modifier.clearAndSetSemantics {},
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = choice.title, style = MaterialTheme.typography.bodyLarge)
            choice.supportingText?.let { supportingText ->
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ConfirmLocalActionDialog(
    title: String,
    explanation: String,
    confirmLabel: String,
    cancelLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                modifier = Modifier.semantics { heading() },
            )
        },
        text = { Text(explanation) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.heightIn(min = 48.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(cancelLabel)
            }
        },
    )
}

private fun selectedLanguageLabel(
    state: AppPreferencesUiState,
    labels: AccountPreferencesLabels,
): String {
    if (state.followsSystemLanguage) return labels.system
    return state.languageOptions
        .firstOrNull { option -> option.language == state.selectedLanguage }
        ?.let(labels::language)
        ?: labels.system
}

private enum class PendingConfirmation {
    CLEAR_PROFILE,
    DELETE_LOCAL_DATA,
}

internal object AccountPreferencesTestTags {
    const val SCREEN = "account-preferences-screen"
    const val STATUS = "account-preferences-status"
    const val DISPLAY_NAME = "account-preferences-display-name"
    const val SAVE_PROFILE = "account-preferences-save-profile"
    const val CLEAR_PROFILE = "account-preferences-clear-profile"
    const val SAVED_LIBRARY = "account-preferences-saved-library"
    const val LANGUAGE_SELECTOR = "account-preferences-language-selector"
    const val LANGUAGE_DIALOG = "account-preferences-language-dialog"
    const val LANGUAGE_LIST = "account-preferences-language-list"
    const val DELETE_LOCAL_DATA = "account-preferences-delete-local-data"
}
