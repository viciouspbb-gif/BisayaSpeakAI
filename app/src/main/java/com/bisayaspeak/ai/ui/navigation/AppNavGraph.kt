@file:OptIn(ExperimentalMaterial3Api::class)

package com.bisayaspeak.ai.ui.navigation

import android.app.Activity
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
import com.bisayaspeak.ai.ui.account.LoginType
import com.bisayaspeak.ai.ui.ads.AdMobBanner
import com.bisayaspeak.ai.ui.ads.AdMobManager
import com.bisayaspeak.ai.ui.ads.AdUnitIds
import com.bisayaspeak.ai.ui.home.FeatureId
import com.bisayaspeak.ai.ui.home.HomeScreen
import com.bisayaspeak.ai.ui.home.HomeViewModel
import com.bisayaspeak.ai.ui.missions.MissionScenarioSelectScreen
import com.bisayaspeak.ai.ui.missions.MissionTalkScreen
import com.bisayaspeak.ai.ui.roleplay.RoleplayChatScreen
import com.bisayaspeak.ai.ui.roleplay.RoleplayListScreen
import com.bisayaspeak.ai.ui.screens.AiTranslatorScreen
import com.bisayaspeak.ai.ui.screens.FeedbackScreen
import com.bisayaspeak.ai.ui.screens.FlashcardScreen
import com.bisayaspeak.ai.ui.screens.LessonResultScreen
import com.bisayaspeak.ai.ui.screens.LevelSelectionScreen
import com.bisayaspeak.ai.ui.screens.ListeningScreen
import com.bisayaspeak.ai.ui.screens.MockRolePlayScreen
import com.bisayaspeak.ai.ui.screens.PracticeCategoryScreen
import com.bisayaspeak.ai.ui.screens.PracticeQuizScreen
import com.bisayaspeak.ai.ui.screens.PracticeWordDetailScreen
import com.bisayaspeak.ai.ui.screens.PracticeWordListScreen
import com.bisayaspeak.ai.ui.screens.QuizScreen
import com.bisayaspeak.ai.ui.screens.SignInScreen
import com.bisayaspeak.ai.ui.screens.SignUpScreen
import com.bisayaspeak.ai.ui.screens.TranslateScreen
import com.bisayaspeak.ai.ui.viewmodel.ListeningViewModel
import com.bisayaspeak.ai.ui.viewmodel.ListeningViewModelFactory
import com.google.firebase.auth.FirebaseAuth

enum class AppRoute(val route: String) {
    Home("home"),
    LevelSelection("level_selection"),
    Translation("translation"),
    Flashcards("flashcards"),
    PracticeCategories("practice/categories"),
    PracticeCategory("practice/category/{category}"),
    PracticeWord("practice/word/{id}"),
    Listening("listening/{level}"),
    Quiz("quiz"),
    RolePlay("roleplay"),
    RolePlayScenario("role_play_scenario/{scenarioId}"),
    Account("account"),
    SignIn("signin"),
    SignUp("signup"),
    Feedback("feedback"),
    Upgrade("upgrade"),
    LessonResult("result_screen/{correctCount}/{earnedXP}/{clearedLevel}"),
    MissionScenarioSelect("mission/scenario"),
    MissionTalk("mission/talk/{scenarioId}"),
    AiTranslator("ai/translator")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavGraph(
    navController: androidx.navigation.NavHostController,
    userPlan: UserPlan,
    showPremiumTestToggle: Boolean,
    onTogglePremiumTest: () -> Unit,
    listeningViewModelFactory: ListeningViewModelFactory
) {
    val context = LocalContext.current
    val activity = context as? Activity ?: return
    val isLiteBuild = BuildConfig.IS_LITE_BUILD
    val authManager = remember(context, isLiteBuild) {
        if (isLiteBuild) null else AuthManager(context)
    }

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

    // 簡易的な UI State
    val homeViewModel: HomeViewModel = viewModel()
    val homeStatus by homeViewModel.homeStatus.collectAsState()
    val isPaidPlan = userPlan != UserPlan.LITE
    val isPremiumPlan = userPlan == UserPlan.PREMIUM

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
            BannerScreenContainer(userPlan = userPlan) {
                HomeScreen(
                    homeStatus = homeStatus,
                    isLiteBuild = isLiteBuild,
                    isPremiumPlan = isPremiumPlan,

                    onStartLearning = {
                        val destination = if (homeStatus.currentLevel < 3) {
                            AppRoute.LevelSelection.route
                        } else {
                            AppRoute.Quiz.route
                        }
                        navController.navigate(destination)
                    },
                    onClickFeature = { feature ->
                        when (feature) {
                            // Premium機能（ロック済み）は何もしない
                            FeatureId.TRANSLATE -> { /* Locked - do nothing */ }
                            FeatureId.AI_CHAT -> {
                                if (isPremiumPlan) {
                                    navController.navigate(AppRoute.MissionScenarioSelect.route)
                                } else {
                                    navController.navigate(AppRoute.Upgrade.route)
                                }
                            }
                            FeatureId.ADVANCED_ROLE_PLAY -> { /* Locked - do nothing */ }
                            FeatureId.AI_TRANSLATOR -> {
                                if (isPremiumPlan) {
                                    navController.navigate(AppRoute.AiTranslator.route)
                                } else {
                                    navController.navigate(AppRoute.Upgrade.route)
                                }
                            }

                            // 無料機能のみナビゲーション
                            FeatureId.PRONUNCIATION -> if (!isLiteBuild) navController.navigate(AppRoute.PracticeCategories.route)
                            FeatureId.LISTENING -> navController.navigate(AppRoute.LevelSelection.route)
                            FeatureId.QUIZ -> navController.navigate(AppRoute.Quiz.route)
                            FeatureId.FLASHCARDS -> navController.navigate(AppRoute.Flashcards.route)
                            FeatureId.ROLE_PLAY -> navController.navigate("roleplay_list")
                            FeatureId.ACCOUNT -> navController.navigate(AppRoute.Account.route)
                            FeatureId.UPGRADE -> navController.navigate(AppRoute.Upgrade.route)
                        }
                    },
                    onClickProfile = { navController.navigate(AppRoute.Account.route) }
                )
            }
        }

        composable(AppRoute.Account.route) {
            if (isLiteBuild) {
                BannerScreenContainer(userPlan = userPlan) {
                    AccountScreen(
                        uiState = accountUiState,

                        onBack = { navController.popBackStack() },
                        onLogin = {},
                        onCreateAccount = {},
                        onLogout = {},
                        onOpenPremiumInfo = { /* Premium info not implemented */ },
                        onOpenFeedback = { /* Lite版ではフィードバック画面を利用しない */ },
                        showPremiumTestToggle = false,
                        premiumTestEnabled = isPremiumPlan,
                        onTogglePremiumTest = null,
                        authEnabled = false
                    )
                }
            } else {
                BannerScreenContainer(userPlan = userPlan) {
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

            // Feedback
            composable(AppRoute.Feedback.route) {
                FeedbackScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        // Upgrade
        composable(AppRoute.Upgrade.route) {
            com.bisayaspeak.ai.ui.upgrade.UpgradeScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AppRoute.MissionScenarioSelect.route) {
            if (!isPremiumPlan) {
                LaunchedEffect(Unit) {
                    navController.navigate(AppRoute.Upgrade.route)
                }
            } else {
                MissionScenarioSelectScreen(
                    onBack = { navController.popBackStack() },
                    onScenarioSelected = { scenario ->
                        navController.navigate(
                            AppRoute.MissionTalk.route.replace("{scenarioId}", scenario.id)
                        )
                    }
                )
            }
        }

        composable(
            route = AppRoute.MissionTalk.route,
            arguments = listOf(navArgument("scenarioId") { type = NavType.StringType })
        ) { backStackEntry ->
            val scenarioId = backStackEntry.arguments?.getString("scenarioId")
            if (!isPremiumPlan) {
                LaunchedEffect(Unit) {
                    navController.navigate(AppRoute.Upgrade.route)
                }
            } else if (scenarioId != null) {
                MissionTalkScreen(
                    scenarioId = scenarioId,
                    onNavigateBack = { navController.popBackStack() }
                )
            } else {
                navController.popBackStack()
            }
        }

        composable(AppRoute.LevelSelection.route) {
            val app = context.applicationContext as MyApp
            val unlockedFlow = remember { app.userProgressRepository.getUnlockedLevels() }
            val unlockedLevels by unlockedFlow.collectAsState(initial = setOf(1))
            LevelSelectionScreen(
                unlockedLevels = if (unlockedLevels.isEmpty()) setOf(1) else unlockedLevels,
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
                navArgument("earnedXP") { type = NavType.IntType },
                navArgument("clearedLevel") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val correctCount = backStackEntry.arguments?.getInt("correctCount") ?: 0
            val earnedXP = backStackEntry.arguments?.getInt("earnedXP") ?: 0
            val clearedLevel = backStackEntry.arguments?.getInt("clearedLevel") ?: 1
            LessonResultScreen(
                correctCount = correctCount,
                earnedXP = earnedXP,
                clearedLevel = clearedLevel,
                onNavigateHome = {
                    navController.navigate(AppRoute.Home.route) {
                        popUpTo(AppRoute.Home.route) { inclusive = true }
                    }
                },
                onPracticeAgain = {
                    navController.popBackStack()
                }
            )
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
                            // 5問連続出題画面に遷移
                            navController.navigate("practice/quiz/$category")
                        },
                        userPlan = userPlan,
                        onNavigateToUpgrade = { navController.navigate(AppRoute.Upgrade.route) }
                    )
                }
            }

            // Practice Quiz (5問連続出題)
            composable(
                route = "practice/quiz/{category}",
                arguments = listOf(navArgument("category") { type = NavType.StringType })
            ) { backStackEntry ->
                val category = backStackEntry.arguments?.getString("category") ?: ""
                BannerScreenContainer(userPlan = userPlan) {
                    PracticeQuizScreen(
                        category = category,
                        onNavigateBack = { navController.popBackStack() },
                        isPremium = isPremiumPlan
                    )
                }
            }

            // Practice Word List by Category (旧画面 - 必要に応じて残す)
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
            BannerScreenContainer(userPlan = userPlan) {
                val viewModel: ListeningViewModel = viewModel(factory = listeningViewModelFactory)
                ListeningScreen(
                    navController = navController,
                    level = level,
                    isPremium = isPremiumPlan,

                    onNavigateBack = { navController.popBackStack() },
                    onShowRewardedAd = {
                        AdMobManager.loadRewarded(context)
                        AdMobManager.showRewarded(
                            activity = activity,
                            onEarned = { _, _ -> },
                            onDismissed = {}
                        )
                    },
                    viewModel = viewModel
                )
            }
        }

        // Quiz - Direct to QuizScreen
        composable(AppRoute.Quiz.route) {
            BannerScreenContainer(userPlan = userPlan) {
                QuizScreen(
                    level = LearningLevel.BEGINNER,

                    onNavigateBack = { navController.popBackStack() },
                    onQuizStart = {
                        // クイズ開始時は何もしない
                    },
                    onQuizComplete = {
                        // クイズ終了時にインタースティシャル広告を表示（無料版のみ）
                        if (!isPremiumPlan) {
                            AdMobManager.showInterstitial(activity) {
                                AdMobManager.loadInterstitial(context)
                            }
                        }
                    },
                    isPremium = isPremiumPlan,
                    navController = navController
                )
            }
        }

        // リスト画面（無条件で追加）
        composable("roleplay_list") {
            // テスト用にLv100を渡す
            RoleplayListScreen(
                userCurrentLevel = 100,
                onScenarioClick = { scenario ->
                    navController.navigate("roleplay_chat/${scenario.id}")
                }
            )
        }

        // チャット画面（無条件で追加）
        composable(
            route = "roleplay_chat/{scenarioId}",
            arguments = listOf(navArgument("scenarioId") { type = NavType.StringType })
        ) { backStackEntry ->
            val scenarioId = backStackEntry.arguments?.getString("scenarioId") ?: ""
            RoleplayChatScreen(
                scenarioId = scenarioId,
                onBackClick = { navController.popBackStack() }
            )
        }

        // 既存のモックシナリオ用（念のため残しています）
        composable(
            route = "role_play_scenario/{scenarioId}",
            arguments = listOf(navArgument("scenarioId") { type = NavType.StringType })
        ) { backStackEntry ->
            val scenarioId = backStackEntry.arguments?.getString("scenarioId") ?: ""
            val repository = remember { com.bisayaspeak.ai.data.repository.mock.MockRolePlayRepository() }
            val scenario = remember(scenarioId) { repository.getScenarios().find { it.id == scenarioId } }

            if (scenario != null) {
                BannerScreenContainer(userPlan = userPlan) {
                    MockRolePlayScreen(
                        scenario = scenario,
                        onNavigateBack = { navController.popBackStack() },
                        isPremium = isPremiumPlan
                    )
                }
            } else {
                // シナリオが見つからない場合は戻る
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
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