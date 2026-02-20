@file:OptIn(ExperimentalMaterial3Api::class)

package com.bisayaspeak.ai.ui.navigation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.bisayaspeak.ai.BuildConfig
import com.bisayaspeak.ai.BisayaSpeakApp
import com.bisayaspeak.ai.R
import com.bisayaspeak.ai.auth.AuthManager
import com.bisayaspeak.ai.data.model.LearningLevel
import com.bisayaspeak.ai.data.model.UserPlan
import com.bisayaspeak.ai.data.repository.ScenarioRepository
import com.bisayaspeak.ai.ui.account.AccountScreen
import com.bisayaspeak.ai.ui.account.AccountUiState
import com.bisayaspeak.ai.ui.account.AccountViewModel
import com.bisayaspeak.ai.ui.account.LoginType
import com.bisayaspeak.ai.ads.AdManager
import com.bisayaspeak.ai.ui.ads.AdMobBanner
import com.bisayaspeak.ai.ui.ads.AdUnitIds
import com.bisayaspeak.ai.ui.home.FeatureId
import com.bisayaspeak.ai.ui.home.HomeScreen
import com.bisayaspeak.ai.ui.home.HomeViewModel
import com.bisayaspeak.ai.ui.missions.MissionListScreen
import com.bisayaspeak.ai.ui.missions.MissionScenarioSelectScreen
import com.bisayaspeak.ai.ui.missions.MissionTalkScreen
import com.bisayaspeak.ai.ui.roleplay.RoleplayChatScreen
import com.bisayaspeak.ai.ui.roleplay.RoleplayChatViewModel
import com.bisayaspeak.ai.ui.roleplay.RoleplayListScreen
import com.bisayaspeak.ai.ui.screens.AiTranslatorScreen
import com.bisayaspeak.ai.ui.screens.FeedbackScreen
import com.bisayaspeak.ai.ui.screens.FlashcardScreen
import com.bisayaspeak.ai.ui.screens.DictionaryScreen
import com.bisayaspeak.ai.ui.screens.LessonResultScreen
import com.bisayaspeak.ai.ui.screens.LevelSelectionScreen
import com.bisayaspeak.ai.ui.screens.ListeningScreen
import com.bisayaspeak.ai.ui.screens.MockRolePlayScreen
import com.bisayaspeak.ai.ui.screens.PracticeCategoryScreen
import com.bisayaspeak.ai.ui.screens.PracticeQuizScreen
import com.bisayaspeak.ai.ui.screens.PracticeWordDetailScreen
import com.bisayaspeak.ai.ui.screens.PracticeWordListScreen
import com.bisayaspeak.ai.ui.screens.SignInScreen
import com.bisayaspeak.ai.ui.screens.SignUpScreen
import com.bisayaspeak.ai.ui.screens.TranslateScreen
import com.bisayaspeak.ai.ui.screens.TariDojoScreen
import com.bisayaspeak.ai.ui.screens.ComingSoonScreen
import com.bisayaspeak.ai.ui.viewmodel.ListeningViewModel
import com.bisayaspeak.ai.ui.viewmodel.ListeningViewModelFactory
import com.bisayaspeak.ai.voice.GeminiVoiceService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

enum class AppRoute(val route: String) {
    Home("home"),
    LevelSelection("level_selection"),
    Translation("translation"),
    Flashcards("flashcards"),
    PracticeCategories("practice/categories"),
    PracticeCategory("practice/category/{category}"),
    PracticeWord("practice/word/{id}"),
    Listening("listening/{level}"),
    RolePlayChat("roleplay_chat/{scenarioId}"),
    RolePlayList("roleplay_list"),
    Account("account"),
    SignIn("signin"),
    SignUp("signup"),
    Feedback("feedback"),
    Upgrade("upgrade"),
    LessonResult("result_screen/{correctCount}/{totalQuestions}/{clearedLevel}/{leveledUp}"),
    MissionScenarioSelect("mission/scenario"),
    MissionTalk("mission/talk/{missionId}"),
    AiTranslator("ai/translator"),
    Dictionary("dictionary"),
    TariDojo("tari_dojo")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavGraph(
    navController: androidx.navigation.NavHostController,
    isProVersion: Boolean,
    showPremiumTestToggle: Boolean,
    onTogglePremiumTest: () -> Unit,
    listeningViewModelFactory: ListeningViewModelFactory,
    onRestorePurchase: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity ?: return
    val app = context.applicationContext as BisayaSpeakApp
    val authManager = remember(context) { AuthManager(context) }
    val scope = rememberCoroutineScope()
    val scenarioRepository = remember(context) { ScenarioRepository(context) }

    var currentUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }

    DisposableEffect(Unit) {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
        onDispose {
            FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
        }
    }

    // 邁｡譏鍋噪縺ｪ UI State
    val homeViewModel: HomeViewModel = viewModel()
    val homeStatus by homeViewModel.homeStatus.collectAsState()

    val accountUiState = remember(isProVersion, currentUser) {
        AccountUiState(
            email = currentUser?.email ?: "",
            isPremium = isProVersion,
            loginType = if (currentUser != null) LoginType.Email else LoginType.Guest
        )
    }

    NavHost(
        navController = navController,
        startDestination = AppRoute.Home.route
    ) {
        composable(AppRoute.Home.route) {
            BannerScreenContainer(isProVersion = isProVersion) {
                HomeScreen(
                    homeStatus = homeStatus,
                    isLiteBuild = !isProVersion,
                    isPremiumPlan = isProVersion,
                    isProUnlocked = isProVersion,

                    onStartLearning = {
                        navController.navigate(AppRoute.LevelSelection.route)
                    },
                    onClickFeature = { feature ->
                        when (feature) {
                            FeatureId.AI_CHAT -> {
                                if (isProVersion) {
                                    navController.navigate(AppRoute.TariDojo.route)
                                } else {
                                    navController.navigate(AppRoute.Upgrade.route)
                                }
                            }
                            FeatureId.AI_TRANSLATOR -> {
                                navController.navigate(AppRoute.AiTranslator.route)
                            }

                            FeatureId.TRANSLATE -> navController.navigate(AppRoute.Translation.route)
                            FeatureId.ADVANCED_ROLE_PLAY -> { /* no-op (deprecated) */ }

                            FeatureId.PRONUNCIATION -> navController.navigate(AppRoute.PracticeCategories.route)
                            FeatureId.LISTENING -> navController.navigate(AppRoute.LevelSelection.route)
                            FeatureId.FLASHCARDS -> navController.navigate(AppRoute.Flashcards.route)
                            FeatureId.ACCOUNT -> navController.navigate(AppRoute.Account.route)
                            FeatureId.UPGRADE -> navController.navigate(AppRoute.Upgrade.route)
                            FeatureId.ROLE_PLAY -> {
                                navController.navigate(AppRoute.RolePlayList.route)
                            }

                            else -> { /* Legacy/unused features */ }
                        }
                    },
                    onClickProfile = { navController.navigate(AppRoute.Account.route) }
                )
            }
        }

        composable(AppRoute.Account.route) { backStackEntry ->
            val accountViewModel: AccountViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = AccountViewModel.Factory
            )
            val profileState by accountViewModel.profileState.collectAsState()
            val openUrl: (String) -> Unit = { url ->
                runCatching {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                }.onFailure {
                    Toast.makeText(context, context.getString(R.string.account_open_browser_failed), Toast.LENGTH_SHORT).show()
                }
            }
            val legalSupportUrl = "https://gist.github.com/viciouspbb-gif/e63ebfe03645c3281a7ae847f280f9a7"

            BannerScreenContainer(isProVersion = isProVersion) {
                AccountScreen(
                    uiState = accountUiState,

                    onBack = { navController.popBackStack() },
                    onLogin = { navController.navigate(AppRoute.SignIn.route) },
                    onCreateAccount = { navController.navigate(AppRoute.SignUp.route) },
                    onLogout = {
                        authManager.signOut()
                        navController.navigate(AppRoute.Home.route) {
                            popUpTo(AppRoute.Home.route) { inclusive = true }
                        }
                    },
                    onOpenPremiumInfo = { /* Premium info not implemented */ },
                    onOpenFeedback = { navController.navigate(AppRoute.Feedback.route) },
                    profileState = profileState,
                    onNicknameChange = accountViewModel::onNicknameChange,
                    onGenderChange = accountViewModel::onGenderChange,
                    onSaveProfile = accountViewModel::saveProfile,
                    onRestorePurchase = onRestorePurchase,
                    onOpenLegalSupport = { openUrl(legalSupportUrl) },
                    onDeleteAccount = {
                        scope.launch {
                            val result = authManager.deleteAccount()
                            result?.onSuccess {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.account_delete_success),
                                    Toast.LENGTH_SHORT
                                ).show()
                                navController.navigate(AppRoute.Home.route) {
                                    popUpTo(AppRoute.Home.route) { inclusive = true }
                                }
                            }?.onFailure {
                                Toast.makeText(
                                    context,
                                    it.message ?: context.getString(R.string.account_delete_failed),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } ?: Toast.makeText(
                                context,
                                context.getString(R.string.account_delete_unavailable),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    showPremiumTestToggle = showPremiumTestToggle,
                    premiumTestEnabled = isProVersion,
                    onTogglePremiumTest = { onTogglePremiumTest() },
                    authEnabled = true
                )
            }
        }

        // Sign In
        composable(AppRoute.SignIn.route) {
            SignInScreen(
                onNavigateBack = { navController.popBackStack() },
                onSignInSuccess = {
                    navController.navigate(AppRoute.Account.route) {
                        popUpTo(AppRoute.SignIn.route) { inclusive = true }
                    }
                },
                onNavigateToSignUp = { navController.navigate(AppRoute.SignUp.route) }
            )
        }

        // Sign Up
        composable(AppRoute.SignUp.route) {
            SignUpScreen(
                onNavigateBack = { navController.popBackStack() },
                onSignUpSuccess = {
                    navController.navigate(AppRoute.Account.route) {
                        popUpTo(AppRoute.SignUp.route) { inclusive = true }
                    }
                }
            )
        }

        // Feedback（Liteビルドでも利用可能にするためガード外へ移動）
        composable(AppRoute.Feedback.route) {
            FeedbackScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Upgrade
        composable(AppRoute.Upgrade.route) {
            com.bisayaspeak.ai.ui.upgrade.UpgradeScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AppRoute.AiTranslator.route) {
            AiTranslatorScreen(
                onBack = { navController.popBackStack() },
                onNavigateToUpgrade = {
                    navController.navigate(AppRoute.Upgrade.route)
                }
            )
        }

        composable(AppRoute.RolePlayList.route) {
            val scenarioId = "tari_infinite_mode"
            RoleplayChatScreen(
                scenarioId = scenarioId,
                onBackClick = { navController.popBackStack() },
                isProVersion = isProVersion,
                onSaveAndExit = { history ->
                    navController.popBackStack(AppRoute.Home.route, false)
                },
                onNavigateToUpgrade = { navController.navigate(AppRoute.Upgrade.route) }
            )
        }

        composable(AppRoute.TariDojo.route) {
            val scenarioId = remember {
                scenarioRepository.getRandomDojoScenario()?.id
                    ?: scenarioRepository.getScenarioById("dojo_1")?.id
                    ?: "dojo_1"
            }
            RoleplayChatScreen(
                scenarioId = scenarioId,
                onBackClick = { navController.popBackStack() },
                isProVersion = isProVersion,
                onSaveAndExit = { history ->
                    navController.popBackStack()
                },
                onNavigateToUpgrade = { navController.navigate(AppRoute.Upgrade.route) }
            )
        }

        composable(AppRoute.Dictionary.route) {
            DictionaryScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(AppRoute.MissionScenarioSelect.route) {
            if (!isProVersion) {
                LaunchedEffect(Unit) {
                    navController.navigate(AppRoute.Upgrade.route)
                }
            } else {
                MissionListScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onMissionSelect = { missionId ->
                        navController.navigate(
                            AppRoute.MissionTalk.route.replace("{missionId}", missionId)
                        )
                    }
                )
            }
        }

        composable(
            route = AppRoute.MissionTalk.route,
            arguments = listOf(navArgument("missionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val missionId = backStackEntry.arguments?.getString("missionId")
            if (!isProVersion) {
                LaunchedEffect(Unit) {
                    navController.navigate(AppRoute.Upgrade.route)
                }
            } else if (missionId != null) {
                MissionTalkScreen(
                    scenarioId = missionId,
                    onNavigateBack = { navController.popBackStack() }
                )
            } else {
                navController.popBackStack()
            }
        }

        composable(AppRoute.LevelSelection.route) {
            LevelSelectionScreen(
                onLevelSelected = { level ->
                    navController.navigate(
                        AppRoute.Listening.route.replace("{level}", level.toString())
                    )
                }
            )
        }

        composable(
            route = AppRoute.LessonResult.route,
            arguments = listOf(
                navArgument("correctCount") { type = NavType.IntType },
                navArgument("totalQuestions") { type = NavType.IntType },
                navArgument("clearedLevel") { type = NavType.IntType },
                navArgument("leveledUp") { type = NavType.BoolType }
            )
        ) { backStackEntry ->
            val correctCount = backStackEntry.arguments?.getInt("correctCount") ?: 0
            val totalQuestions = backStackEntry.arguments?.getInt("totalQuestions") ?: 0
            val clearedLevel = backStackEntry.arguments?.getInt("clearedLevel") ?: 1
            val leveledUp = backStackEntry.arguments?.getBoolean("leveledUp") ?: false
            val listeningViewModel: ListeningViewModel = viewModel(factory = listeningViewModelFactory)
            LessonResultScreen(
                correctCount = correctCount,
                totalQuestions = totalQuestions,
                clearedLevel = clearedLevel,
                leveledUp = leveledUp,
                onNavigateHome = {
                    navController.navigate(AppRoute.Home.route) {
                        popUpTo(AppRoute.Home.route) { inclusive = true }
                    }
                },
                onPracticeAgain = {
                    navController.popBackStack()
                },
                viewModel = listeningViewModel
            )
        }


        composable(AppRoute.Translation.route) {
            BannerScreenContainer(isProVersion = isProVersion) {
                TranslateScreen(
                    isPremium = isProVersion,
                    onOpenPremiumInfo = { /* Premium not implemented */ },
                    onOpenConversationMode = { /* Conversation mode not implemented */ }
                )
            }
        }

        composable(AppRoute.Flashcards.route) {
            BannerScreenContainer(isProVersion = isProVersion) {
                FlashcardScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        // Practice Categories
        composable(AppRoute.PracticeCategories.route) {
            BannerScreenContainer(isProVersion = isProVersion) {
                PracticeCategoryScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onCategorySelected = { category ->
                        navController.navigate("practice/quiz/$category")
                    },
                    userPlan = if (isProVersion) UserPlan.PREMIUM else UserPlan.LITE,
                    onNavigateToUpgrade = { navController.navigate(AppRoute.Upgrade.route) }
                )
            }
        }

        // Practice Quiz
        composable(
            route = "practice/quiz/{category}",
            arguments = listOf(navArgument("category") { type = NavType.StringType })
        ) { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category") ?: ""
            BannerScreenContainer(isProVersion = isProVersion) {
                PracticeQuizScreen(
                    category = category,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToUpgrade = { navController.navigate(AppRoute.Upgrade.route) },
                    isPremium = isProVersion
                )
            }
        }

        composable(
            route = "practice/category/{category}",
            arguments = listOf(navArgument("category") { type = NavType.StringType })
        ) { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category") ?: ""
            BannerScreenContainer(isProVersion = isProVersion) {
                PracticeWordListScreen(
                    category = category,
                    onNavigateBack = { navController.popBackStack() },
                    onWordClick = { wordId ->
                        navController.navigate("practice/word/$wordId")
                    }
                )
            }
        }

        composable(
            route = "practice/word/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            BannerScreenContainer(isProVersion = isProVersion) {
                PracticeWordDetailScreen(
                    id = id,
                    onNavigateBack = { navController.popBackStack() },
                    isPremium = isProVersion
                )
            }
        }

        composable(
            route = AppRoute.Listening.route,
            arguments = listOf(navArgument("level") { type = NavType.IntType })
        ) { backStackEntry ->
            val level = backStackEntry.arguments?.getInt("level") ?: 1
            val viewModel: ListeningViewModel = viewModel(factory = listeningViewModelFactory)
            com.bisayaspeak.ai.ui.screens.ListeningScreen(
                navController = navController,
                level = level,
                viewModel = viewModel
            )
        }

        composable(
            route = AppRoute.RolePlayChat.route,
            arguments = listOf(navArgument("scenarioId") { type = NavType.StringType })
        ) { backStackEntry ->
            if (!isProVersion) {
                LaunchedEffect(Unit) {
                    navController.navigate(AppRoute.Upgrade.route)
                }
            } else {
                val scenarioId = backStackEntry.arguments?.getString("scenarioId") ?: "default"
                val roleplayChatViewModel: RoleplayChatViewModel = viewModel()

                RoleplayChatScreen(
                    scenarioId = scenarioId,
                    isProVersion = isProVersion,
                    onBackClick = { navController.popBackStack() },
                    onSaveAndExit = {
                        GeminiVoiceService.stopAllActive()
                        Log.d("AppNavigation", "Roleplay completion -> returning home after save")
                        val popped = navController.popBackStack(AppRoute.Home.route, inclusive = false)
                        if (!popped) {
                            navController.navigate(AppRoute.Home.route) {
                                launchSingleTop = true
                                popUpTo(AppRoute.Home.route) { inclusive = true }
                            }
                        }
                    },
                    viewModel = roleplayChatViewModel
                )
            }
        }


    }
}

@Composable
private fun BannerScreenContainer(
    isProVersion: Boolean,
    content: @Composable () -> Unit
) {
    if (isProVersion) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            Box(modifier = Modifier.weight(1f, fill = true)) {
                content()
            }
            AdMobBanner(
                adUnitId = AdUnitIds.BANNER_MAIN,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            )
        }
    }
}
