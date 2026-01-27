package com.bisayaspeak.ai.ui.account

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.annotation.StringRes
import com.bisayaspeak.ai.R
import com.bisayaspeak.ai.data.UserGender
import com.bisayaspeak.ai.data.repository.UserProfilePreferences

enum class LoginType(@StringRes val labelRes: Int) {
    Guest(R.string.account_login_type_guest),
    Email(R.string.account_login_type_email),
    Google(R.string.account_login_type_google),
    Apple(R.string.account_login_type_apple)
}

@Composable
private fun PremiumTestToggleCard(
    isPremium: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.account_premium_test_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.account_premium_test_desc),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = isPremium,
                onCheckedChange = onToggle
            )
        }
    }
}

@Immutable
data class AccountUiState(
    val email: String = "",
    val isPremium: Boolean = false,
    val loginType: LoginType = LoginType.Guest
) {
    companion object {
        val Saver: Saver<AccountUiState, List<Any?>> = Saver(
            save = { state -> listOf(state.email, state.isPremium, state.loginType.name) },
            restore = { saved ->
                val email = saved.getOrNull(0) as? String ?: ""
                val isPremium = saved.getOrNull(1) as? Boolean ?: false
                val loginTypeName = saved.getOrNull(2) as? String ?: LoginType.Guest.name
                val loginType = runCatching { LoginType.valueOf(loginTypeName) }.getOrDefault(LoginType.Guest)
                AccountUiState(
                    email = email,
                    isPremium = isPremium,
                    loginType = loginType
                )
            }
        )
    }
}

@Composable
fun AccountScreen(
    uiState: AccountUiState,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onCreateAccount: () -> Unit,
    onLogout: () -> Unit,
    onOpenPremiumInfo: () -> Unit,
    onOpenFeedback: () -> Unit,
    profileState: AccountProfileUiState,
    onNicknameChange: (String) -> Unit,
    onGenderChange: (UserGender) -> Unit,
    onSaveProfile: () -> Unit,
    onRestorePurchase: () -> Unit,
    onOpenLegalSupport: () -> Unit,
    onDeleteAccount: () -> Unit,
    showPremiumTestToggle: Boolean = false,
    premiumTestEnabled: Boolean = uiState.isPremium,
    onTogglePremiumTest: ((Boolean) -> Unit)? = null,
    authEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(profileState.lastSavedAt) {
        profileState.lastSavedAt?.let {
            Toast.makeText(context, context.getString(R.string.account_profile_saved_toast), Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(profileState.errorMessage) {
        profileState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ヘッダー
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = Color.White
                )
            }

            Text(
                text = stringResource(R.string.account_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }

        Divider()

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {

            StatusCard(
                uiState = uiState,
                onOpenPremiumInfo = onOpenPremiumInfo,
                nickname = profileState.savedNickname,
                gender = profileState.savedGender
            )

            Spacer(modifier = Modifier.height(16.dp))

            ProfileEditorCard(
                profileState = profileState,
                onNicknameChange = onNicknameChange,
                onGenderChange = onGenderChange,
                onSaveProfile = onSaveProfile
            )

            Spacer(modifier = Modifier.height(16.dp))

            UpgradeCtaSection(
                isPremium = uiState.isPremium,
                onUpgrade = onOpenPremiumInfo,
                onRestorePurchase = onRestorePurchase
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Actions
            if (!authEnabled) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E88E5).copy(alpha = 0.08f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.account_lite_notice_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.account_lite_notice_desc),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                            )
                        )
                    }
                }
            } else if (uiState.loginType == LoginType.Guest) {
                // Guest state
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E88E5).copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.account_guest_card_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E88E5)
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.account_guest_card_desc),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = onLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(stringResource(R.string.account_login_button))
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onCreateAccount,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(stringResource(R.string.account_create_button))
                }
            } else if (authEnabled) {
                Button(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(stringResource(R.string.account_logout_button))
                }
            }

            if (showPremiumTestToggle && onTogglePremiumTest != null) {
                Spacer(modifier = Modifier.height(24.dp))
                PremiumTestToggleCard(
                    isPremium = premiumTestEnabled,
                    onToggle = onTogglePremiumTest
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            LegalSupportCard(onOpenLegalSupport = onOpenLegalSupport)

            Spacer(modifier = Modifier.height(16.dp))

            if (authEnabled) {
                DangerZoneCard(onDeleteAccount = { showDeleteDialog = true })
                Spacer(modifier = Modifier.height(16.dp))
            }

            FeedbackCard(onOpenFeedback = onOpenFeedback)
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.account_delete_dialog_title)) },
            text = {
                Text(stringResource(R.string.account_delete_dialog_message))
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDeleteAccount()
                }) {
                    Text(stringResource(R.string.account_delete_confirm), color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.account_delete_cancel))
                }
            }
        )
    }
}

@Composable
private fun StatusCard(
    uiState: AccountUiState,
    onOpenPremiumInfo: () -> Unit,
    nickname: String,
    gender: UserGender,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                val loginLabel = stringResource(uiState.loginType.labelRes)
                val status = if (uiState.loginType == LoginType.Guest) {
                    stringResource(R.string.account_status_logged_in_guest)
                } else {
                    stringResource(R.string.account_status_logged_in_format, loginLabel)
                }
                Text(
                    text = status,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Email,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(
                        R.string.account_email_label,
                        uiState.email.ifBlank { stringResource(R.string.account_value_unset) }
                    ),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                val nicknameLabel = nickname.ifBlank { stringResource(R.string.account_value_unset) }
                Text(
                    text = stringResource(
                        R.string.account_nickname_gender,
                        nicknameLabel,
                        stringResource(gender.displayLabelRes())
                    ),
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (uiState.isPremium) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (uiState.isPremium) Color(0xFFFFD700) else Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                val planText = if (uiState.isPremium) {
                    stringResource(R.string.account_status_plan_premium)
                } else {
                    stringResource(R.string.account_status_plan_free)
                }
                Text(
                    text = stringResource(R.string.account_status_plan_label, planText),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!uiState.isPremium) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onOpenPremiumInfo() }
                        .padding(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.account_benefit_section_title),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PremiumBenefitList(
                        textColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileEditorCard(
    profileState: AccountProfileUiState,
    onNicknameChange: (String) -> Unit,
    onGenderChange: (UserGender) -> Unit,
    onSaveProfile: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    LaunchedEffect(profileState.lastSavedAt) {
        profileState.lastSavedAt?.let { isEditing = false }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111928))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.account_profile_title),
                        style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = if (isEditing) {
                            stringResource(R.string.account_profile_subtitle_editing)
                        } else {
                            stringResource(R.string.account_profile_subtitle_view)
                        },
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.7f))
                    )
                }
                TextButton(onClick = { isEditing = !isEditing }) {
                    Icon(imageVector = Icons.Outlined.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isEditing) stringResource(R.string.account_profile_edit_done) else stringResource(R.string.account_profile_edit))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!isEditing) {
                Text(
                    text = stringResource(
                        R.string.account_profile_display_nickname,
                        profileState.savedNickname.ifBlank { stringResource(R.string.account_value_unset) }
                    ),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.account_profile_display_gender,
                        stringResource(profileState.savedGender.displayLabelRes())
                    ),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                OutlinedTextField(
                    value = profileState.nickname,
                    onValueChange = onNicknameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.account_profile_nickname_label), color = Color.White.copy(alpha = 0.8f)) },
                    placeholder = { Text(stringResource(R.string.account_profile_nickname_placeholder), color = Color.White.copy(alpha = 0.4f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF0B1220),
                        unfocusedContainerColor = Color(0xFF0B1220),
                        cursorColor = Color.White,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF60A5FA),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.4f)
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(stringResource(R.string.account_profile_gender_label), color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GenderChip(label = stringResource(R.string.account_gender_male), selected = profileState.gender == UserGender.MALE) {
                        onGenderChange(UserGender.MALE)
                    }
                    GenderChip(label = stringResource(R.string.account_gender_female), selected = profileState.gender == UserGender.FEMALE) {
                        onGenderChange(UserGender.FEMALE)
                    }
                    GenderChip(label = stringResource(R.string.account_gender_unspecified), selected = profileState.gender == UserGender.OTHER) {
                        onGenderChange(UserGender.OTHER)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onSaveProfile,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = profileState.isDirty && profileState.isValid && !profileState.isSaving
                ) {
                    Text(
                        if (profileState.isSaving) stringResource(R.string.account_profile_saving)
                        else stringResource(R.string.account_profile_save_button)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenderChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}

@Composable
private fun UpgradeCtaSection(
    isPremium: Boolean,
    onUpgrade: () -> Unit,
    onRestorePurchase: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2A44))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = if (isPremium) stringResource(R.string.account_premium_cta_title_premium) else stringResource(R.string.account_premium_cta_title_free),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (isPremium) {
                Text(
                    text = stringResource(R.string.account_premium_cta_current_plan),
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium
                )
                PremiumBenefitList()
            } else {
                Button(
                    onClick = onUpgrade,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.account_upgrade_button))
                }
                TextButton(
                    onClick = onRestorePurchase,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(stringResource(R.string.account_restore_button), textDecoration = TextDecoration.Underline)
                }
                Spacer(modifier = Modifier.height(8.dp))
                PremiumBenefitList(
                    textColor = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun PremiumBenefitList(
    modifier: Modifier = Modifier,
    textColor: Color = Color.White
) {
    val benefits = listOf(
        stringResource(R.string.account_benefit_hide_ads),
        stringResource(R.string.account_benefit_unlimited_translator),
        stringResource(R.string.account_benefit_roleplay_mode)
    )
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        benefits.forEach { benefit ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "•",
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(12.dp)
                )
                Text(
                    text = benefit,
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun LegalSupportCard(
    onOpenLegalSupport: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101828))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.account_legal_title), color = Color.White, fontWeight = FontWeight.Bold)
            Text(
                text = stringResource(R.string.account_legal_description),
                color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodySmall
            )
            LegalSupportButton(
                label = stringResource(R.string.account_legal_button),
                onClick = onOpenLegalSupport
            )
        }
    }
}

@Composable
private fun LegalSupportButton(label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = Color.White.copy(alpha = 0.05f)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.White, modifier = Modifier.weight(1f))
            Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null, tint = Color.White)
        }
    }
}

@Composable
private fun DangerZoneCard(onDeleteAccount: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D0F0F))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.account_danger_title), color = Color.White, fontWeight = FontWeight.Bold)
            Text(
                text = stringResource(R.string.account_danger_description),
                color = Color.White.copy(alpha = 0.8f)
            )
            OutlinedButton(
                onClick = onDeleteAccount,
                border = BorderStroke(1.dp, Color.Red),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Filled.Delete, contentDescription = null, tint = Color.Red)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.account_danger_button), color = Color.Red)
            }
        }
    }
}

@StringRes
private fun UserGender.displayLabelRes(): Int = when (this) {
    UserGender.MALE -> R.string.account_gender_male
    UserGender.FEMALE -> R.string.account_gender_female
    UserGender.OTHER -> R.string.account_gender_unspecified
}

@Composable
private fun FeedbackCard(
    onOpenFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenFeedback() }
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Feedback,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.account_feedback_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.account_feedback_description),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            )
        }
    }
}
