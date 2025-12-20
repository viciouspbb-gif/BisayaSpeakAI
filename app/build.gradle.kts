import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// LITEビルド（タスク名に "Lite" を含む）では google-services プラグインを適用しない
val isLiteBuildRequested = gradle.startParameter.taskNames.any { it.contains("Lite", ignoreCase = true) }
if (!isLiteBuildRequested) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.bisayaspeak.ai"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bisayaspeak.ai"
        minSdk = 24
        targetSdk = 35
        versionCode = 33
        versionName = "2.2.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        
        // APIキーをBuildConfigに追加
        val localPropertiesFile = rootProject.file("local.properties")
        val properties = Properties()
        if (localPropertiesFile.exists()) {
            properties.load(FileInputStream(localPropertiesFile))
        }
        
        val geminiApiKey = properties.getProperty("GEMINI_API_KEY") ?: ""
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")

        val serverBaseUrl = properties.getProperty("SERVER_BASE_URL")
            ?: "https://bisaya-speak-ai-server-1.onrender.com"
        buildConfigField("String", "SERVER_BASE_URL", "\"$serverBaseUrl\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file("${rootProject.projectDir}/bisaya-speak-ai.jks")
            storePassword = "Bisaya2025"
            keyAlias = "bisaya-speak-ai"
            keyPassword = "Bisaya2025"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("pro") {
            dimension = "distribution"
            buildConfigField("boolean", "IS_LITE_BUILD", "false")
        }
        create("lite") {
            dimension = "distribution"
            versionNameSuffix = "-lite"
            buildConfigField("boolean", "IS_LITE_BUILD", "true")
        }
    }
    
    // リリースビルド後に自動コピー
    applicationVariants.all {
        if (buildType.name == "release") {
            val capitalizedName = name.replaceFirstChar { it.uppercase() }
            val bundleTask = tasks.findByName("bundle$capitalizedName")
            bundleTask?.doLast {
                val sourceFile = layout.buildDirectory.file("outputs/bundle/release/app-release.aab").get().asFile
                val destDir = file("${rootProject.projectDir}/app/release/release")
                val destFile = file("$destDir/app-release-${defaultConfig.versionName}.aab")
                
                if (sourceFile.exists()) {
                    destDir.mkdirs()
                    sourceFile.copyTo(destFile, overwrite = true)
                    println("✅ AAB copied to: ${destFile.absolutePath}")
                }
            }
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
    
    buildFeatures {
        compose = true
        buildConfig = true
        viewBinding = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.1")
    
    // Compose
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.5")
    
    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")
    
    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // AdMob
    implementation("com.google.android.gms:play-services-ads:23.5.0")
    
    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    
    // reCAPTCHA Enterprise - セキュリティ脆弱性対応（18.4.0以上必須）
    implementation("com.google.android.recaptcha:recaptcha:18.6.1")
    
    // Google Play Billing
    implementation("com.android.billingclient:billing-ktx:7.1.1")
    
    // In-App Updates
    implementation("com.google.android.play:app-update:2.1.0")
    implementation("com.google.android.play:app-update-ktx:2.1.0")
    
    // Audio Recording
    implementation("androidx.media:media:1.7.0")
    
    // Fragment (最新バージョンを明示的に指定)
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    
    // Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")

    // Flexbox layout (for tighter word wrapping)
    implementation("com.google.android.flexbox:flexbox:3.0.0")
    
    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // Gson
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Gemini AI - 最新安定版
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0") {
        exclude(group = "io.ktor", module = "ktor-client-core")
    }
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-client-okhttp:2.3.7")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
