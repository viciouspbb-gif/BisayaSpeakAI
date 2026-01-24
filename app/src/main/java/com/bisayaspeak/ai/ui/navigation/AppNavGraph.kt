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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.bisayaspeak.ai.BuildConfig
import com.bisayaspeak.ai.MyApp
import com.bisayaspeak.ai.auth.AuthManager
import com.bisayaspeak.ai.data.model.LearningLevel
import com.bisayaspeak.ai.data.model.UserPlan
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
    Account("account"),
    SignIn("signin"),
    SignUp("signup"),
    Feedback("feedback"),
    Upgrade("upgrade"),
    LessonResult("result_screen/{correctCount}/{totalQuestions}/{earnedXP}/{clearedLevel}/{leveledUp}"),
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
    userPlan: UserPlan,
    showPremiumTestToggle: Boolean,
    onTogglePremiumTest: () -> Unit,
    listeningViewModelFactory: ListeningViewModelFactory,
    onRestorePurchase: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity ?: return
    val isLiteBuild = BuildConfig.IS_LITE_BUILD
    val authManager = remember(context, isLiteBuild) {
        if (isLiteBuild) null else AuthManager(context)
    }
    val scope = rememberCoroutineScope()

    var currentUser by remember { mutableStateOf(if (isLiteBuild) null else FirebaseAuth.getInstance().currentUser) }

    if (!isLiteBuild) {
        DisposableEffect(Unit) {
            val authStateListener = FirebaseAuth.AuthStateListener { auth ->
                currentUser = auth.currentUser
            }
            FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
            onDispose {
                FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
            }
        }
    }

    // 邁｡譏鍋噪縺ｪ UI State
    val homeViewModel: HomeViewModel = viewModel()
    val homeStatus by homeViewModel.homeStatus.collectAsState()
    val isDebugBuild = BuildConfig.DEBUG
    val effectivePlan = if (isDebugBuild) UserPlan.PREMIUM else userPlan
    val isPaidPlan = effectivePlan != UserPlan.LITE
    val isPremiumPlan = effectivePlan == UserPlan.PREMIUM
    val isProUnlocked = isPremiumPlan

    val accountUiState = remember(isPremiumPlan, currentUser, isLiteBuild) {
        AccountUiState(
            email = currentUser?.email ?: "",
            isPremium = isPremiumPlan,
            loginType = if (!isLiteBuild && currentUser != null) LoginType.Email else LoginType.Guest
        )
    }

    NavHost(
        navController = navController,
        startDestination = AppRoute.Home.route
    ) {
        composable(AppRoute.Home.route) {
            BannerScreenContainer(userPlan = effectivePlan) {
                HomeScreen(
                    homeStatus = homeStatus,
                    isLiteBuild = isLiteBuild,
                    isPremiumPlan = isPremiumPlan,
                    isProUnlocked = isProUnlocked,

                    onStartLearning = {
                        navController.navigate(AppRoute.LevelSelection.route)
                    },
                    onClickFeature = { feature ->
                        when (feature) {
                            FeatureId.AI_CHAT -> {
                                if (!BuildConfig.DEBUG) {
                                    Toast.makeText(context, "タリ先生は修行中…近日公開！", Toast.LENGTH_SHORT).show()
                                } else if (isPremiumPlan) {
                                    navController.navigate(AppRoute.TariDojo.route)
                                } else {
                                    navController.navigate(AppRoute.Upgrade.route)
                                }
                            }
                            FeatureId.AI_TRANSLATOR -> {
                                if (isPremiumPlan) {
                                    navController.navigate(AppRoute.Dictionary.route)
                                } else {
                                    navController.navigate(AppRoute.Upgrade.route)
                                }
                            }

                            FeatureId.TRANSLATE -> navController.navigate(AppRoute.Translation.route)
                            FeatureId.ADVANCED_ROLE_PLAY -> { /* no-op (deprecated) */ }

                            FeatureId.PRONUNCIATION -> if (!isLiteBuild) navController.navigate(AppRoute.PracticeCategories.route)
                            FeatureId.LISTENING -> navController.navigate(AppRoute.LevelSelection.route)
                            FeatureId.FLASHCARDS -> navController.navigate(AppRoute.Flashcards.route)
                            FeatureId.ACCOUNT -> navController.navigate(AppRoute.Account.route)
                            FeatureId.UPGRADE -> navController.navigate(AppRoute.Upgrade.route)
                            FeatureId.ROLE_PLAY -> {
                                if (isProUnlocked) {
                                    val defaultScenario = "rp_tarsier_morning"
                                    navController.navigate(
                                        AppRoute.RolePlayChat.route.replace("{scenarioId}", defaultScenario)
                                    )
                                } else {
                                    navController.navigate(AppRoute.Upgrade.route)
                                }
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
                    Toast.makeText(context, "ブラウザを開けませんでした", Toast.LENGTH_SHORT).show()
                }
            }
            val legalSupportUrl = "https://gist.github.com/viciouspbb-gif/e63ebfe03645c3281a7ae847f280f9a7"

            if (isLiteBuild) {
                BannerScreenContainer(userPlan = effectivePlan) {
                    AccountScreen(
                        uiState = accountUiState,

                        onBack = { navController.popBackStack() },
                        onLogin = {},
                        onCreateAccount = {},
                        onLogout = {},
                        onOpenPremiumInfo = { /* Premium info not implemented */ },
                        onOpenFeedback = { navController.navigate(AppRoute.Feedback.route) },
                        profileState = profileState,
                        onNicknameChange = accountViewModel::onNicknameChange,
                        onGenderChange = accountViewModel::onGenderChange,
                        onSaveProfile = accountViewModel::saveProfile,
                        onRestorePurchase = onRestorePurchase,
                        onOpenLegalSupport = { openUrl(legalSupportUrl) },
                        onDeleteAccount = {},
                        showPremiumTestToggle = false,
                        premiumTestEnabled = isPremiumPlan,
                        onTogglePremiumTest = null,
                        authEnabled = false
                    )
                }
            } else {
                BannerScreenContainer(userPlan = effectivePlan) {
                    AccountScreen(
                        uiState = accountUiState,

                        onBack = { navController.popBackStack() },
                        onLogin = { navController.navigate(AppRoute.SignIn.route) },
                        onCreateAccount = { navController.navigate(AppRoute.SignUp.route) },
                        onLogout = {
                            authManager?.signOut()
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
                                val result = authManager?.deleteAccount()
                                if (result == null) {
                                    Toast.makeText(context, "アカウント削除は利用できません", Toast.LENGTH_SHORT).show()
                                } else {
                                    result.onSuccess {
                                        Toast.makeText(context, "アカウントを削除しました", Toast.LENGTH_SHORT).show()
                                        navController.navigate(AppRoute.Home.route) {
                                            popUpTo(AppRoute.Home.route) { inclusive = true }
                                        }
                                    }.onFailure {
                                        Toast.makeText(context, it.message ?: "削除に失敗しました", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        showPremiumTestToggle = showPremiumTestToggle,
                        premiumTestEnabled = isPremiumPlan,
                        onTogglePremiumTest = { onTogglePremiumTest() },
                        authEnabled = true
                    )
                }
            }
        }

        if (!isLiteBuild) {
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
                onBack = { navController.popBackStack() }
            )
        }

        composable(AppRoute.TariDojo.route) {
            if (BuildConfig.DEBUG) {
                TariDojoScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            } else {
                ComingSoonScreen(
                    message = "タリ先生は修行中…近日公開！",
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        composable(AppRoute.Dictionary.route) {
            if (!isPremiumPlan) {
                LaunchedEffect(Unit) {
                    navController.navigate(AppRoute.Upgrade.route)
                }
            } else {
                DictionaryScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(AppRoute.MissionScenarioSelect.route) {
            if (!isPremiumPlan) {
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
            if (!isPremiumPlan) {
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
                navArgument("earnedXP") { type = NavType.IntType },
                navArgument("clearedLevel") { type = NavType.IntType },
                navArgument("leveledUp") { type = NavType.BoolType }
            )
        ) { backStackEntry ->
            val correctCount = backStackEntry.arguments?.getInt("correctCount") ?: 0
            val totalQuestions = backStackEntry.arguments?.getInt("totalQuestions") ?: 0
            val earnedXP = backStackEntry.arguments?.getInt("earnedXP") ?: 0
            val clearedLevel = backStackEntry.arguments?.getInt("clearedLevel") ?: 1
            val leveledUp = backStackEntry.arguments?.getBoolean("leveledUp") ?: false
            val listeningViewModel: ListeningViewModel = viewModel(factory = listeningViewModelFactory)
            LessonResultScreen(
                correctCount = correctCount,
                totalQuestions = totalQuestions,
                earnedXP = earnedXP,
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

        composable(
            route = AppRoute.MissionTalk.route,
            arguments = listOf(navArgument("missionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val missionId = backStackEntry.arguments?.getString("missionId")
            if (!isPremiumPlan) {
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

        if (!isLiteBuild) {
            composable(AppRoute.Translation.route) {
                BannerScreenContainer(userPlan = userPlan) {
                    TranslateScreen(
                        isPremium = isPremiumPlan,
                        onOpenPremiumInfo = { /* Premium not implemented */ },
                        onOpenConversationMode = { /* Conversation mode not implemented */ }
                    )
                }
            }
        }

        composable(AppRoute.Flashcards.route) {
            BannerScreenContainer(userPlan = userPlan) {
                FlashcardScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        if (!isLiteBuild) {
            // Practice Categories
            composable(AppRoute.PracticeCategories.route) {
                BannerScreenContainer(userPlan = userPlan) {
                    PracticeCategoryScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onCategorySelected = { category ->
                            // 5蝠城｣邯壼・鬘檎判髱｢縺ｫ驕ｷ遘ｻ
                            navController.navigate("practice/quiz/$category")
                        },
                        userPlan = userPlan,
                        onNavigateToUpgrade = { navController.navigate(AppRoute.Upgrade.route) }
                    )
                }
            }

            // Practice Quiz (5蝠城｣邯壼・鬘・
            composable(
                route = "practice/quiz/{category}",
                arguments = listOf(navArgument("category") { type = NavType.StringType })
            ) { backStackEntry ->
                val category = backStackEntry.arguments?.getString("category") ?: ""
                BannerScreenContainer(userPlan = userPlan) {
                    PracticeQuizScreen(
                        category = category,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToUpgrade = { navController.navigate(AppRoute.Upgrade.route) },
                        isPremium = isPremiumPlan
                    )
                }
            }

            // Practice Word List by Category (譌ｧ逕ｻ髱｢ - 蠢・ｦ√↓蠢懊§縺ｦ谿九☆)
            composable(
                route = "practice/category/{category}",
                arguments = listOf(navArgument("category") { type = NavType.StringType })
            ) { backStackEntry ->
                val category = backStackEntry.arguments?.getString("category") ?: ""
                BannerScreenContainer(userPlan = userPlan) {
                    PracticeWordListScreen(
                        category = category,
                        onNavigateBack = { navController.popBackStack() },
                        onWordClick = { wordId ->
                            navController.navigate("practice/word/$wordId")
                        }
                    )
                }
            }

            // Practice Word Detail
            composable(
                route = "practice/word/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id") ?: ""
                BannerScreenContainer(userPlan = userPlan) {
                    PracticeWordDetailScreen(
                        id = id,
                        onNavigateBack = { navController.popBackStack() },
                        isPremium = isPremiumPlan
                    )
                }
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
            if (!isProUnlocked) {
                LaunchedEffect(Unit) {
                    navController.navigate(AppRoute.Upgrade.route)
                }
            } else {
                val scenarioId = backStackEntry.arguments?.getString("scenarioId") ?: "default"
                val roleplayChatViewModel: RoleplayChatViewModel = viewModel()

                RoleplayChatScreen(
                    scenarioId = scenarioId,
                    isProVersion = true,
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
    userPlan: UserPlan,
    content: @Composable () -> Unit
) {
    val isPaid = userPlan != UserPlan.LITE
    if (isPaid) {
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
