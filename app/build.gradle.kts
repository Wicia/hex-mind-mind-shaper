import java.text.SimpleDateFormat
import java.util.Date

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    id("org.jetbrains.kotlin.kapt") // ! Only for MapStruct
    id("kotlin-parcelize")
}

// Run Unit Tests in every build configuration
tasks.whenTaskAdded {
    if (name.startsWith("assemble")) {
        dependsOn("testDebugUnitTest")
    }
}

android {
    namespace = "pl.hexmind.mindshaper"
    compileSdk = 35

    defaultConfig {
        applicationId = "pl.hexmind.mindshaper"
        minSdk = 30
        targetSdk = 35
        versionCode = 2
        versionName = "MVP_1_0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ksp {
            // Handling Room DB migrations
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.incremental", "true")
            arg("room.expandProjection", "true")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
            isDebuggable = true
        }
        applicationVariants.all {
            outputs.all {
                if (this is com.android.build.gradle.internal.api.BaseVariantOutputImpl) {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd")
                    val date = dateFormat.format(Date())
                    outputFileName = "MindShaper-v${versionCode}-${date}.apk"
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
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn"
        )
    }

    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)

    // Lifecycle & Architecture
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Dependency Injection - Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)

    // Database - Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Settings & Preferences
    implementation(libs.androidx.preference.ktx)

    // UI Components
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.recyclerview)
    implementation(libs.speeddial)
    implementation(libs.html.textview) // displaying HTML elements in TextViews

    // Permissions
    implementation(libs.dexter)

    // Logging - Timber
    implementation(libs.timber)

    // External Libraries
    implementation(libs.androidsvg)
    implementation(libs.kotlinx.coroutines.android)

    // Debug Tools
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // Mappings and DTO
    implementation(libs.mapstruct)
    kapt(libs.mapstruct.processor) // ! using kapt to generate MapStruct mappers

    // Serialization
    implementation(libs.gson)
}