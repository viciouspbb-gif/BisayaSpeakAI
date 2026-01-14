@file:OptIn(ExperimentalMaterial3Api::class)

package com.bisayaspeak.ai.ui.navigation

import android.app.Activity
import android.util.Log
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
import com.bisayaspeak.ai.ui.screens.GenderSelectionScreen
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
import com.bisayaspeak.ai.ui.screens.SignInScreen
import com.bisayaspeak.ai.ui.screens.SignUpScreen
import com.bisayaspeak.ai.ui.screens.TranslateScreen
import com.bisayaspeak.ai.ui.viewmodel.GenderSelectionViewModel
import com.bisayaspeak.ai.ui.viewmodel.ListeningViewModel
import com.bisayaspeak.ai.ui.viewmodel.ListeningViewModelFactory
import com.bisayaspeak.ai.voice.GeminiVoiceService
import com.google.firebase.auth.FirebaseAuth

enum class AppRoute(val route: String) {
    Home("home"),
    GenderSelection("gender_selection"),
    LevelSelection("level_selection"),
    Translation("translation"),
    Flashcards("flashcards"),
    PracticeCategories("practice/categories"),
    PracticeCategory("practice/category/{category}"),
    PracticeWord("practice/word/{id}"),
    Listening("listening/{level}"),
    RolePlayList("roleplay_list"),
    RolePlayChat("roleplay_chat/{scenarioId}"),
    RolePlayScenario("role_play_scenario/{scenarioId}"),
    Account("account"),
    SignIn("signin"),
    SignUp("signup"),
    Feedback("feedback"),
    Upgrade("upgrade"),
    LessonResult("result_screen/{correctCount}/{totalQuestions}/{earnedXP}/{clearedLevel}/{leveledUp}"),
    MissionScenarioSelect("mission/scenario"),
    MissionTalk("mission/talk/{missionId}"),
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

    // 邁｡譏鍋噪縺ｪ UI State
    val homeViewModel: HomeViewModel = viewModel()
    val homeStatus by homeViewModel.homeStatus.collectAsState()
    val isPaidPlan = userPlan != UserPlan.LITE
    val isPremiumPlan = userPlan == UserPlan.PREMIUM
    val isProUnlocked = userPlan == UserPlan.STANDARD || userPlan == UserPlan.PREMIUM

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
        composable(AppRoute.GenderSelection.route) {
            val genderSelectionViewModel: GenderSelectionViewModel = viewModel()
            GenderSelectionScreen(
                navController = navController,
                viewModel = genderSelectionViewModel
            )
        }

        composable(AppRoute.Home.route) {
            BannerScreenContainer(userPlan = userPlan) {
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
                                if (isPremiumPlan) {
                                    navController.navigate(AppRoute.MissionScenarioSelect.route)
                                } else {
                                    navController.navigate(AppRoute.Upgrade.route)
                                }
                            }
                            FeatureId.AI_TRANSLATOR -> {
                                if (isPremiumPlan) {
                                    navController.navigate(AppRoute.AiTranslator.route)
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
                            FeatureId.ROLE_PLAY -> navController.navigate(AppRoute.GenderSelection.route)

                            else -> { /* Legacy/unused features */ }
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
                        onOpenFeedback = { /* Lite迚医〒縺ｯ繝輔ぅ繝ｼ繝峨ヰ繝・け逕ｻ髱｢繧貞茜逕ｨ縺励↑縺・*/ },
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

        composable(AppRoute.AiTranslator.route) {
            AiTranslatorScreen(
                onBack = { navController.popBackStack() }
            )
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

        composable(AppRoute.RolePlayList.route) {
            BannerScreenContainer(userPlan = userPlan) {
                RoleplayListScreen(
                    userCurrentLevel = homeStatus.currentLevel,
                    onScenarioClick = { scenario ->
                        navController.navigate(
                            AppRoute.RolePlayChat.route.replace("{scenarioId}", scenario.id)
                        )
                    }
                )
            }
        }

        // 繝√Ε繝・ヨ逕ｻ髱｢・育┌譚｡莉ｶ縺ｧ霑ｽ蜉�・・
        composable(
            route = AppRoute.RolePlayChat.route,
            arguments = listOf(navArgument("scenarioId") { type = NavType.StringType })
        ) { backStackEntry ->
            val scenarioId = backStackEntry.arguments?.getString("scenarioId") ?: "default"
            val roleplayChatViewModel: RoleplayChatViewModel = viewModel()

            RoleplayChatScreen(
                scenarioId = scenarioId,
                isProVersion = isProUnlocked,
                onBackClick = { navController.popBackStack() },
                onCompleted = { result ->
                    Log.d("AppNavigation", "Roleplay completion -> navigating to LessonResult. Stopping all TTS.")
                    GeminiVoiceService.stopAllActive()
                    val destinationRoute = AppRoute.LessonResult.route
                        .replace("{correctCount}", result.correctCount.toString())
                        .replace("{totalQuestions}", result.totalQuestions.toString())
                        .replace("{earnedXP}", result.earnedXp.toString())
                        .replace("{clearedLevel}", result.clearedLevel.toString())
                        .replace("{leveledUp}", result.leveledUp.toString())
                    val alreadyOnResult = navController.currentDestination?.route
                        ?.startsWith("result_screen") == true
                    if (!alreadyOnResult) {
                        try {
                            navController.navigate(destinationRoute) {
                                popUpTo(AppRoute.RolePlayChat.route) { inclusive = true }
                            }
                        } catch (e: IllegalArgumentException) {
                            Log.e("AppNavGraph", "Failed to navigate to LessonResultScreen", e)
                        }
                    }
                },
                viewModel = roleplayChatViewModel
            )
        }

        // 譌｢蟄倥・繝｢繝・け繧ｷ繝翫Μ繧ｪ逕ｨ・亥ｿｵ縺ｮ縺溘ａ谿九＠縺ｦ縺・∪縺呻ｼ・
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
                // 繧ｷ繝翫Μ繧ｪ縺瑚ｦ九▽縺九ｉ縺ｪ縺・�ｴ蜷医・謌ｻ繧・
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
