package com.bisayaspeak.ai.ui.roleplay

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bisayaspeak.ai.R
import com.bisayaspeak.ai.data.UserGender
import com.bisayaspeak.ai.data.model.MissionHistoryMessage
import com.bisayaspeak.ai.ui.roleplay.RoleplayChatViewModel
import com.bisayaspeak.ai.ui.roleplay.RoleplayMode
import com.bisayaspeak.ai.ui.roleplay.primarySpeechText
import com.bisayaspeak.ai.voice.GeminiVoiceCue
import com.bisayaspeak.ai.voice.GeminiVoiceService
import com.bisayaspeak.ai.util.LocaleUtils
import com.bisayaspeak.ai.ui.roleplay.CompletionDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.max

data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val translation: String? = null,
    val voiceCue: GeminiVoiceCue? = null
)


@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class, ExperimentalAnimationApi::class)
@Composable
fun RoleplayChatScreen(
    scenarioId: String,
    onBackClick: () -> Unit,
    isProVersion: Boolean = false,
    onSaveAndExit: (List<MissionHistoryMessage>) -> Unit,
    viewModel: RoleplayChatViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val speakingMessageId by viewModel.speakingMessageId.collectAsState()
    val locale by LocaleUtils.localeState.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val voiceService = remember { GeminiVoiceService(context) }

    var initialLineSpoken by remember(scenarioId) { mutableStateOf(false) }
    val completionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val audioPermissionGrantedState = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var pendingPermissionRequest by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        audioPermissionGrantedState.value = granted
        if (granted && pendingPermissionRequest) {
            viewModel.startVoiceRecording()
        }
        if (!granted) {
            val activity = context.findActivity()
            val rationale = ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.RECORD_AUDIO
            )
            if (!rationale) {
                showPermissionDialog = true
            }
        }
        pendingPermissionRequest = false
    }
    val ttsLogTag = remember { "RoleplayTTS" }

    LaunchedEffect(locale.language) {
        Log.d("RoleplayChatScreen", "Current locale: ${locale.language}")
    }

    LaunchedEffect(Unit) {
        Log.d(ttsLogTag, "Screen entered -> stopping all previous GeminiVoiceService instances")
        GeminiVoiceService.stopAllActive()
    }

    val speakAiLine = remember(voiceService) {
        line@ { text: String, cue: GeminiVoiceCue?, messageId: String? ->
            val speechText = primarySpeechText(text)
            if (speechText.isNotBlank()) {
                Log.d(ttsLogTag, "AI speak request id=$messageId length=${text.length}")
            }
            if (speechText.isBlank()) {
                Log.w(ttsLogTag, "Skipping empty Tari line (id=$messageId)")
                return@line
            }
            val requestedCue = cue ?: GeminiVoiceCue.ROLEPLAY_NOVA_CUTE
            fun speakWithCue(currentCue: GeminiVoiceCue, canRetry: Boolean) {
                voiceService.speak(
                    text = speechText,
                    cue = currentCue,
                    onStart = {
                        Log.d(ttsLogTag, "AI speak started id=$messageId cue=${currentCue.name}")
                        messageId?.let { viewModel.notifyVoicePlaybackStarted(it) }
                    },
                    onComplete = {
                        Log.d(ttsLogTag, "AI speak completed id=$messageId cue=${currentCue.name}")
                        messageId?.let { viewModel.notifyVoicePlaybackFinished(it) }
                    },
                    onError = { error ->
                        if (canRetry && currentCue != GeminiVoiceCue.DEFAULT) {
                            Log.w(
                                ttsLogTag,
                                "AI speak error id=$messageId cue=${currentCue.name} -> retry DEFAULT",
                                error
                            )
                            speakWithCue(GeminiVoiceCue.DEFAULT, false)
                        } else {
                            Log.e(ttsLogTag, "AI speak error id=$messageId cue=${currentCue.name}", error)
                            messageId?.let { viewModel.notifyVoicePlaybackFinished(it) }
                        }
                    }
                )
            }
            speakWithCue(requestedCue, true)
        }
    }

    val userPreviewCue = remember(uiState.userGender) {
        when (uiState.userGender) {
            UserGender.MALE -> GeminiVoiceCue.TALK_HIGH
            UserGender.FEMALE -> GeminiVoiceCue.TALK_HIGH
            UserGender.OTHER -> GeminiVoiceCue.ROLEPLAY_NOVA_CUTE
        }
    }

    val speakUserPreview = remember(voiceService, userPreviewCue) {
        { text: String ->
            if (text.isNotBlank()) {
                Log.d(ttsLogTag, "User preview request length=${text.length} cue=${userPreviewCue.name}")
                voiceService.speak(
                    text = text,
                    cue = userPreviewCue,
                    onStart = { Log.d(ttsLogTag, "User preview started length=${text.length}") },
                    onComplete = { Log.d(ttsLogTag, "User preview completed length=${text.length}") },
                    onError = { Log.w(ttsLogTag, "User preview error length=${text.length}", it) }
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            Log.d(ttsLogTag, "RoleplayChatScreen disposed - stopping TTS playback only")
            voiceService.stop()
        }
    }

    LaunchedEffect(isProVersion) {
        viewModel.setProAccess(isProVersion)
    }

    LaunchedEffect(scenarioId, isProVersion) {
        viewModel.loadScenario(scenarioId, isProVersion)
    }

    val latestUserMessage = uiState.messages.lastOrNull { it.isUser }
    val latestAiLine: ChatMessage? = uiState.messages.lastOrNull { !it.isUser }
    val introLine = remember(
        uiState.activeSceneIntroLine,
        uiState.activeThemeIntroLine,
        uiState.currentScenario?.initialMessage
    ) {
        uiState.activeSceneIntroLine.takeIf { it.isNotBlank() }
            ?: uiState.activeThemeIntroLine.takeIf { it.isNotBlank() }
            ?: uiState.currentScenario?.initialMessage?.takeIf { !it.isNullOrBlank() }?.trim()
    }
    val scenarioLabel = uiState.activeThemeTitle
        .ifBlank { uiState.currentScenario?.title.orEmpty() }
        .ifBlank { stringResource(id = R.string.roleplay) }
    val completionMessage = if (uiState.isEndingSession) {
        stringResource(R.string.roleplay_finish_message, scenarioLabel)
    } else null
    val isSanpoMode = uiState.roleplayMode == RoleplayMode.SANPO
    val isSanpoInputLocked = isSanpoMode && uiState.isSessionEnded

    LaunchedEffect(latestAiLine?.id) {
        latestAiLine?.let { message ->
            speakAiLine(message.text, message.voiceCue, message.id)
            initialLineSpoken = true
        }
    }

    LaunchedEffect(introLine, initialLineSpoken) {
        if (!initialLineSpoken && !introLine.isNullOrBlank()) {
            speakAiLine(introLine, GeminiVoiceCue.ROLEPLAY_NOVA_CUTE, null)
            initialLineSpoken = true
        }
    }

    val pendingExitHistory = uiState.pendingExitHistory
    val exitEnabled = pendingExitHistory != null
    val handleExitClick: () -> Unit = {
        pendingExitHistory?.let {
            onSaveAndExit(it)
            viewModel.consumePendingExitHistory()
        }
    }
    val handleImmediateExit: () -> Unit = {
        val snapshot = viewModel.prepareImmediateExit()
        onSaveAndExit(snapshot)
    }

    val screenScrollState = rememberScrollState()

    val audioPermissionGranted = audioPermissionGrantedState.value
    val micButtonAction: () -> Unit = micAction@{
        if (isSanpoInputLocked) {
            return@micAction
        }
        when {
            uiState.isVoiceRecording -> viewModel.stopVoiceRecordingAndSend()
            uiState.isVoiceTranscribing -> Unit
            !uiState.isVoiceRecording -> {
                if (!audioPermissionGranted) {
                    pendingPermissionRequest = true
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                } else {
                    viewModel.startVoiceRecording()
                }
            }
        }
    }

    LaunchedEffect(uiState.showCompletionDialog) {
        if (uiState.showCompletionDialog) {
            completionSheetState.show()
        } else {
            completionSheetState.hide()
        }
    }

    if (uiState.showCompletionDialog) {
        val onPlayFarewell = {
            val text = uiState.activeThemeFarewellBisaya
            if (text.isNotBlank()) {
                speakAiLine(text, GeminiVoiceCue.ROLEPLAY_NOVA_CUTE, null)
            }
        }
        val onCopyFarewell = {
            val combined = listOf(
                uiState.activeThemeFarewellBisaya,
                uiState.activeThemeFarewellTranslation.takeIf { it.isNotBlank() }
            ).filterNotNull().filter { it.isNotBlank() }
                .joinToString(separator = "\n")
            if (combined.isNotBlank()) {
                clipboardManager.setText(AnnotatedString(combined))
            }
        }
        val onClose = {
            viewModel.dismissCompletionDialog()
            onBackClick()
        }
        CompletionDialog(
            themeTitle = uiState.activeThemeTitle,
            flavor = uiState.activeThemeFlavor,
            goal = uiState.activeThemeGoal,
            farewellLine = uiState.activeThemeFarewellBisaya,
            farewellTranslation = uiState.activeThemeFarewellTranslation,
            farewellExplanation = uiState.activeThemeFarewellExplanation,
            sheetState = completionSheetState,
            onPlayFarewell = onPlayFarewell,
            onCopyFarewell = onCopyFarewell,
            onGoHome = onClose
        )
    }

    val themeTitle = uiState.activeSceneLabel.ifBlank { uiState.activeThemeTitle.ifBlank { "Tari" } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = themeTitle,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.roleplay_back_desc)
                        )
                    }
                },
                actions = {
                    TextButton(onClick = handleImmediateExit) {
                        Text(
                            text = stringResource(R.string.roleplay_immediate_exit),
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF0F172A),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F172A),
                            Color(0xFF172554),
                            Color(0xFF0F172A)
                        )
                    )
                )
        ) {
            val screenHeight = maxHeight
            val condensedLayout = screenHeight < 700.dp
            val isCompactHeight = screenHeight < 640.dp
            val scaleFactor = if (isCompactHeight) 0.9f else 1f
            val textScaleFactor = if (condensedLayout) 0.9f else 1f
            val stageScale = if (condensedLayout) 0.85f else 1f
            val columnModifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(screenScrollState)
            val sectionSpacing = if (condensedLayout) 10.dp else 16.dp
            val optionSpacing = if (condensedLayout) 8.dp else 12.dp
            val cardHorizontalPadding = if (condensedLayout) 14.dp else 20.dp
            val cardVerticalPadding = if (condensedLayout) 12.dp else 18.dp
            val voicePanelScale = if (condensedLayout) 0.9f else 1f
            val voicePanelVerticalPadding = if (condensedLayout) 8.dp else 16.dp
            val voicePanelBottomSpace = if (condensedLayout) 32.dp else 48.dp
            val bottomContentSpacer = voicePanelBottomSpace + if (condensedLayout) 96.dp else 140.dp

            val isJapaneseLocale = locale.language.equals("ja", ignoreCase = true)

            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = columnModifier,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    StageSection(
                        latestAiLine = latestAiLine,
                        speakingMessageId = speakingMessageId,
                        initialLine = introLine,
                        onReplayRequest = {
                            latestAiLine?.let { message ->
                                speakAiLine(message.text, message.voiceCue, message.id)
                            } ?: introLine?.let { intro ->
                                speakAiLine(intro, GeminiVoiceCue.ROLEPLAY_NOVA_CUTE, null)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        scale = stageScale,
                        textScale = textScaleFactor,
                        isEndingSession = uiState.isEndingSession,
                        completionMessage = completionMessage,
                        roleplayMode = uiState.roleplayMode,
                        onTopLinkClick = if (isSanpoMode) handleImmediateExit else null
                    )

                    if (latestUserMessage != null) {
                        Spacer(modifier = Modifier.height(sectionSpacing))
                        UserMessageBubble(
                            message = latestUserMessage,
                            scale = textScaleFactor
                        )
                    }

                    Spacer(modifier = Modifier.height(sectionSpacing))

                    AnimatedVisibility(visible = uiState.showOptionTutorial) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0x3322C55E),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22C55E)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = stringResource(R.string.roleplay_info_desc),
                                    tint = Color(0xFF22C55E)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.roleplay_tutorial_message),
                                        color = Color.White,
                                        fontSize = 13.sp * textScaleFactor
                                    )
                                    Text(
                                        text = stringResource(R.string.roleplay_tutorial_hint),
                                        color = Color(0xFF9FB4D3),
                                        fontSize = 11.sp * textScaleFactor,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                TextButton(onClick = { viewModel.dismissOptionTutorial() }) {
                                    Text(stringResource(R.string.roleplay_tutorial_ok), color = Color(0xFF22C55E))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(sectionSpacing))

                    if (!uiState.isEndingSession) {
                        ResponsePanel(
                            modifier = Modifier
                                .fillMaxWidth(),
                            isLoading = uiState.isLoading,
                            options = uiState.options,
                            peekedHintIds = uiState.peekedHintOptionIds,
                            onSelect = { viewModel.selectOption(it) },
                            onHintPeek = { viewModel.markHintPeeked(it) },
                            onPreview = { text -> speakUserPreview(text) },
                            optionSpacing = optionSpacing,
                            scale = scaleFactor,
                            cardHorizontalPadding = cardHorizontalPadding,
                            cardVerticalPadding = cardVerticalPadding,
                            isJapaneseLocale = isJapaneseLocale
                        )

                        Spacer(modifier = Modifier.height(sectionSpacing))
                    }

                    Spacer(modifier = Modifier.height(bottomContentSpacer))
                }

                if (!uiState.isEndingSession) {
                    VoiceInputPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 20.dp, vertical = voicePanelVerticalPadding)
                            .padding(bottom = voicePanelBottomSpace)
                            .navigationBarsPadding(),
                        isRecording = uiState.isVoiceRecording,
                        isTranscribing = uiState.isVoiceTranscribing,
                        permissionGranted = audioPermissionGranted,
                        lastTranscribedText = uiState.lastTranscribedText,
                        errorMessage = uiState.voiceErrorMessage,
                        onMicClick = micButtonAction,
                        onCancelRecording = { viewModel.cancelVoiceRecording() },
                        scale = voicePanelScale,
                        inputEnabled = !isSanpoInputLocked,
                        onPanelClick = { micButtonAction() }
                    )
                }

                if (uiState.isSessionEnded) {
                    val finaleText = uiState.finalMessage.ifBlank {
                        stringResource(R.string.roleplay_finish_message, scenarioLabel)
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.7f))
                            .align(Alignment.Center)
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = finaleText,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(24.dp)
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = handleImmediateExit,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ) {
                            Text(
                                text = stringResource(R.string.roleplay_back_to_top),
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    openAppSettings(context)
                }) {
                    Text(stringResource(R.string.roleplay_permission_open_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text(stringResource(R.string.roleplay_permission_close))
                }
            },
            title = { Text(stringResource(R.string.roleplay_permission_title)) },
            text = { Text(stringResource(R.string.roleplay_permission_message)) }
        )
    }
}

private tailrec fun Context.findActivity(): Activity {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> throw IllegalStateException("Context is not an Activity")
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
private fun StageSection(
    latestAiLine: ChatMessage?,
    speakingMessageId: String?,
    initialLine: String?,
    onReplayRequest: () -> Unit,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
    textScale: Float = 1f,
    isEndingSession: Boolean,
    completionMessage: String?,
    roleplayMode: RoleplayMode,
    onTopLinkClick: (() -> Unit)? = null
) {
    val characterImageRes = if (roleplayMode == RoleplayMode.DOJO) {
        R.drawable.taridoujo
    } else {
        R.drawable.char_tarsier
    }
    if (isEndingSession && !completionMessage.isNullOrBlank()) {
        Row(
            modifier = modifier
                .clip(RoundedCornerShape(32.dp))
                .background(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF0F172A), Color(0xFF1D2B55))
                    )
                )
                .padding(horizontal = 16.dp * scale, vertical = 12.dp * scale),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp * scale)
        ) {
            Image(
                painter = painterResource(id = characterImageRes),
                contentDescription = stringResource(R.string.roleplay_teacher_desc),
                modifier = Modifier
                    .size(110.dp * scale)
                    .padding(end = 4.dp * scale),
                contentScale = ContentScale.Fit
            )
            Surface(
                modifier = Modifier.weight(1f),
                color = Color(0xFFFFF4DB),
                shape = RoundedCornerShape(32.dp),
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp * scale, vertical = 18.dp * scale),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp * scale)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = stringResource(R.string.roleplay_back_to_home),
                        tint = Color(0xFF22C55E),
                        modifier = Modifier.size(32.dp * scale)
                    )
                    Text(
                        text = completionMessage,
                        color = Color(0xFF154231),
                        fontSize = 18.sp * scale * textScale,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        return
    }

    val bubbleKey = latestAiLine?.id ?: "initial"
    val defaultIntro = stringResource(R.string.roleplay_default_intro)
    val fullDisplayText = latestAiLine?.text ?: initialLine ?: defaultIntro
    val splitResult = remember(fullDisplayText) {
        splitInlineTranslation(fullDisplayText)
    }
    val primaryLine = splitResult.first
    val inlineTranslation = splitResult.second
    val translationText = latestAiLine?.translation.orEmpty()
    val translationToShow = inlineTranslation ?: translationText
    val hasTranslation = translationToShow.isNotBlank()
    var showTranslation by remember(bubbleKey) { mutableStateOf(false) }
    val displayText = primaryLine.ifBlank { fullDisplayText }
    val isSpeaking = latestAiLine?.id != null && speakingMessageId == latestAiLine.id
    val interactionSource = remember(bubbleKey) { MutableInteractionSource() }

    LaunchedEffect(bubbleKey, interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release || interaction is PressInteraction.Cancel) {
                showTranslation = false
            }
        }
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF0F172A), Color(0xFF1D2B55))
                )
            )
            .padding(horizontal = 16.dp * scale, vertical = 12.dp * scale),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp * scale)
    ) {
        Image(
            painter = painterResource(id = characterImageRes),
            contentDescription = stringResource(R.string.roleplay_teacher_desc),
            modifier = Modifier
                .size(110.dp * scale)
                .padding(end = 4.dp * scale),
            contentScale = ContentScale.Fit
        )

        Surface(
            modifier = Modifier
                .weight(1f)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { onReplayRequest() },
                    onLongClick = {
                        if (hasTranslation) {
                            showTranslation = true
                        }
                    }
                ),
            color = Color(0xFFFFF4DB),
            shape = RoundedCornerShape(32.dp),
            tonalElevation = if (isSpeaking) 10.dp else 2.dp,
            shadowElevation = if (isSpeaking) 14.dp else 2.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp * scale, vertical = 18.dp * scale),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedContent(
                    targetState = showTranslation && hasTranslation,
                    transitionSpec = { fadeIn() with fadeOut() },
                    label = "ai-line"
                ) { isTranslation ->
                    val textToShow = if (isTranslation) translationToShow else displayText
                    val fontSize = if (isTranslation) 16.sp else 20.sp
                    val color = if (isTranslation) Color(0xCC5B3600) else Color(0xFF5B3600)
                    val containsTopLink = !isTranslation && roleplayMode == RoleplayMode.SANPO &&
                        onTopLinkClick != null && textToShow.contains("[TOPページへ]")
                    if (containsTopLink) {
                        val cleaned = textToShow.replace("[TOPページへ]", "").trim()
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (cleaned.isNotBlank()) {
                                Text(
                                    text = cleaned,
                                    color = color,
                                    fontSize = fontSize * scale * textScale,
                                    textAlign = TextAlign.Center,
                                    fontWeight = if (isSpeaking) FontWeight.Black else FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            Text(
                                text = "[TOPページへ]",
                                color = Color(0xFF3B82F6),
                                fontSize = 18.sp * scale * textScale,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { onTopLinkClick?.invoke() }
                            )
                        }
                    } else {
                        Text(
                            text = textToShow,
                            color = color,
                            fontSize = fontSize * scale * textScale,
                            textAlign = TextAlign.Center,
                            fontWeight = if (!isTranslation && isSpeaking) FontWeight.Black else FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UserMessageBubble(
    message: ChatMessage,
    scale: Float = 1f
) {
    val bubbleKey = message.id
    var showTranslation by remember(bubbleKey) { mutableStateOf(false) }
    val hasTranslation = message.translation?.isNotBlank() == true

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(bubbleKey) {
                detectTapGestures(
                    onTap = { showTranslation = false },
                    onLongPress = {
                        if (hasTranslation) showTranslation = true
                    }
                )
            },
        color = Color(0xFF122035),
        shape = RoundedCornerShape(22.dp * scale)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp * scale, vertical = 14.dp * scale),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp * scale)
        ) {
            Text(
                text = message.text,
                color = Color(0xFFE0ECFF),
                fontSize = 18.sp * scale,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            AnimatedVisibility(visible = showTranslation && hasTranslation) {
                Text(
                    text = message.translation.orEmpty(),
                    color = Color(0xFF9AB6FF),
                    fontSize = 15.sp * scale,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ThemeMetaRow(
    flavor: RoleplayThemeFlavor,
    persona: String,
    goal: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MetaChip(
            text = flavorLabel(flavor),
            color = if (flavor == RoleplayThemeFlavor.CASUAL) Color(0xFF22C55E) else Color(0xFF3B82F6)
        )
        if (persona.isNotBlank()) {
            MetaChip(text = persona, color = Color(0xFF38BDF8))
        }
        if (goal.isNotBlank()) {
            MetaChip(text = goal, color = Color(0xFFFACC15))
        }
    }
}

@Composable
private fun MetaChip(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.18f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

private fun flavorLabel(flavor: RoleplayThemeFlavor): String = when (flavor) {
    RoleplayThemeFlavor.CASUAL -> "CASUAL"
    RoleplayThemeFlavor.SCENARIO -> "SCENARIO"
}

@Composable
private fun ResponsePanel(
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    options: List<RoleplayOption>,
    peekedHintIds: Set<String>,
    onSelect: (String) -> Unit,
    onHintPeek: (String) -> Unit,
    onPreview: (String) -> Unit,
    optionSpacing: Dp,
    scale: Float,
    cardHorizontalPadding: Dp,
    cardVerticalPadding: Dp,
    isJapaneseLocale: Boolean
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(36.dp * scale.coerceAtLeast(0.85f))),
        color = Color(0xCC0D1424)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp * scale.coerceAtLeast(0.85f), vertical = 18.dp * scale.coerceAtLeast(0.85f))
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                options.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.roleplay_waiting_teacher),
                            color = Color(0xFFFFF176)
                        )
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(optionSpacing)
                    ) {
                        options.forEach { option ->
                            RoleplayOptionCard(
                                option = option,
                                hintRevealed = option.id in peekedHintIds,
                                onSelect = onSelect,
                                onPeekHint = onHintPeek,
                                onPreview = onPreview,
                                scale = scale,
                                horizontalPadding = cardHorizontalPadding,
                                verticalPadding = cardVerticalPadding,
                                isJapaneseLocale = isJapaneseLocale
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RoleplayOptionCard(
    option: RoleplayOption,
    hintRevealed: Boolean,
    onSelect: (String) -> Unit,
    onPeekHint: (String) -> Unit,
    onPreview: (String) -> Unit,
    scale: Float = 1f,
    horizontalPadding: Dp = 20.dp,
    verticalPadding: Dp = 18.dp,
    isJapaneseLocale: Boolean
) {
    var showTranslation by remember(option.id) { mutableStateOf(false) }
    var hasPeeked by remember(option.id) { mutableStateOf(false) }
    val (primaryOptionText, inlineTranslation) = remember(option.id, option.text) {
        splitInlineTranslation(option.text)
    }
    val translationText = remember(option.id, option.hint, inlineTranslation, isJapaneseLocale) {
        option.hint ?: inlineTranslation ?: if (isJapaneseLocale) "訳はまだありません" else "Translation unavailable"
    }
    val translationAvailable = translationText.isNotBlank()
    val displayText = primaryOptionText.ifBlank { option.text }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp * scale.coerceAtLeast(0.85f))
            .clickable { onSelect(option.id) },
        color = if (hintRevealed) Color(0xFF273251) else Color(0xFF16223B),
        shape = RoundedCornerShape(26.dp * scale.coerceAtLeast(0.85f)),
        tonalElevation = if (showTranslation) 6.dp else 2.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (showTranslation) Color(0xFF64F2C2) else Color(0xFF2C3A56)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (showTranslation && translationAvailable) translationText else displayText,
                color = Color(0xFFFFF176),
                fontSize = 16.sp * scale,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onPreview(option.text) },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0x332A3A54))
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFF7DD3FC)
                    )
                }
                IconButton(
                    onClick = {
                        if (translationAvailable) {
                            showTranslation = !showTranslation
                            if (showTranslation && !hasPeeked) {
                                onPeekHint(option.id)
                                hasPeeked = true
                            }
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (showTranslation) Color(0xFF12324A) else Color(0x332A3A54)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Translate,
                        contentDescription = null,
                        tint = if (showTranslation) Color(0xFF64F2C2) else Color(0xFF9FB4D3)
                    )
                }
            }
        }
    }
}

@Composable
private fun VoiceInputPanel(
    modifier: Modifier = Modifier,
    isRecording: Boolean,
    isTranscribing: Boolean,
    permissionGranted: Boolean,
    lastTranscribedText: String?,
    errorMessage: String?,
    onMicClick: () -> Unit,
    onCancelRecording: () -> Unit,
    scale: Float = 1f,
    inputEnabled: Boolean = true,
    onPanelClick: () -> Unit
) {
    val trimmedPreview = lastTranscribedText?.let { text ->
        if (text.length > 80) text.take(80) + "…" else text
    }
    val statusText = when {
        isRecording -> stringResource(R.string.roleplay_voice_status_recording)
        isTranscribing -> stringResource(R.string.roleplay_voice_status_transcribing)
        else -> stringResource(R.string.roleplay_voice_status_idle)
    }
    val helperText = when {
        isRecording -> stringResource(R.string.roleplay_voice_helper_recording)
        isTranscribing -> stringResource(R.string.roleplay_voice_helper_transcribing)
        else -> stringResource(R.string.roleplay_voice_helper_idle)
    }
    val buttonColor = when {
        !permissionGranted -> Color(0xFF475569)
        isRecording -> Color(0xFFE11D48)
        isTranscribing -> Color(0xFF2563EB)
        else -> Color(0xFF22C55E)
    }
    val icon = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic

    Surface(
        modifier = modifier
            .clickable(enabled = inputEnabled) { onPanelClick() },
        shape = RoundedCornerShape(24.dp * scale.coerceAtLeast(0.85f)),
        color = Color(0xFF0B1124),
        tonalElevation = 4.dp,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp * scale.coerceAtLeast(0.85f), vertical = 14.dp * scale.coerceAtLeast(0.85f))) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp * scale.coerceAtLeast(0.85f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilledIconButton(
                    onClick = onMicClick,
                    enabled = permissionGranted && !isTranscribing && inputEnabled,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = buttonColor,
                        disabledContainerColor = Color(0xFF1E293B)
                    ),
                    modifier = Modifier.size(56.dp * scale)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = if (isRecording) {
                            stringResource(R.string.roleplay_mic_stop_desc)
                        } else {
                            stringResource(R.string.roleplay_mic_start_desc)
                        },
                        tint = Color.White
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = statusText,
                        color = Color.White,
                        fontSize = 16.sp * scale,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = helperText,
                        color = Color(0xFF9FB4D3),
                        fontSize = 13.sp * scale,
                        modifier = Modifier.padding(top = 2.dp * scale.coerceAtLeast(0.85f))
                    )
                }
                if (isRecording) {
                    TextButton(onClick = onCancelRecording, contentPadding = PaddingValues(horizontal = 8.dp * scale.coerceAtLeast(0.85f))) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.roleplay_voice_cancel_desc),
                            tint = Color.White
                        )
                        Text(
                            text = stringResource(R.string.roleplay_voice_cancel),
                            color = Color.White,
                            modifier = Modifier.padding(start = 4.dp * scale.coerceAtLeast(0.85f))
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isTranscribing, modifier = Modifier.padding(top = 8.dp * scale.coerceAtLeast(0.85f))) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF38BDF8),
                    trackColor = Color(0xFF1E3A5F)
                )
            }

            AnimatedVisibility(visible = !permissionGranted, modifier = Modifier.padding(top = 8.dp * scale.coerceAtLeast(0.85f))) {
                Text(
                    text = stringResource(R.string.roleplay_voice_permission_hint),
                    color = Color(0xFFFFB4AB),
                    fontSize = 13.sp * scale
                )
            }

            AnimatedVisibility(
                visible = !errorMessage.isNullOrBlank(),
                modifier = Modifier.padding(top = 8.dp * scale.coerceAtLeast(0.85f))
            ) {
                Text(
                    text = errorMessage.orEmpty(),
                    color = Color(0xFFFFB4AB),
                    fontSize = 13.sp * scale
                )
            }

            AnimatedVisibility(
                visible = !trimmedPreview.isNullOrBlank() && !isRecording && !isTranscribing,
                modifier = Modifier.padding(top = 8.dp * scale.coerceAtLeast(0.85f))
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.roleplay_voice_last_memo),
                        color = Color(0xFF8FD3FF),
                        fontSize = 13.sp * scale
                    )
                    Text(
                        text = stringResource(R.string.roleplay_voice_last_memo_quote, trimmedPreview.orEmpty()),
                        color = Color.White,
                        fontSize = 14.sp * scale,
                        modifier = Modifier.padding(top = 4.dp * scale.coerceAtLeast(0.85f))
                    )
                }
            }
        }
    }
}

