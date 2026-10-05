import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Build-time configuration comes from Gradle properties (-PNAME=value, ~/.gradle/gradle.properties)
// or environment variables. Nothing secret is ever committed. See docs/ENVIRONMENT.md.
fun config(name: String, default: String = ""): String =
    providers.gradleProperty(name).orElse(providers.environmentVariable(name)).getOrElse(default)

// Google's published *test* AdMob ids — safe for development, never for production.
val testAdMobAppId = "ca-app-pub-3940256099942544~3347511713"
val testBannerId = "ca-app-pub-3940256099942544/9214589741"
val testInterstitialId = "ca-app-pub-3940256099942544/1033173712"

val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use(::load)
}

android {
    namespace = "app.quranaudio"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.quranaudio"
        minSdk = 26
        targetSdk = 36
        versionCode = config("VERSION_CODE", "1").toInt()
        versionName = config("VERSION_NAME", "1.0.0")
        testInstrumentationRunner = "app.quranaudio.HiltTestRunner"

        // Empty = talk to the public mp3quran.net API directly (no backend needed for development).
        buildConfigField("String", "CATALOG_BASE_URL", "\"${config("CATALOG_BASE_URL")}\"")
        buildConfigField("String", "PRIVACY_POLICY_URL", "\"${config("PRIVACY_POLICY_URL", "https://example.org/privacy")}\"")
        buildConfigField("String", "TERMS_URL", "\"${config("TERMS_URL", "https://example.org/terms")}\"")
        buildConfigField("String", "CONTACT_EMAIL", "\"${config("CONTACT_EMAIL", "support@example.org")}\"")
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            manifestPlaceholders["admobAppId"] = testAdMobAppId
            buildConfigField("boolean", "ADS_ENABLED", "true")
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$testBannerId\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$testInterstitialId\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystoreProps.isNotEmpty()) signingConfig = signingConfigs.getByName("release")
            // Production ad ids must be supplied explicitly. Without them ads are disabled in release
            // builds rather than shipping Google's test ids.
            val appId = config("ADMOB_APP_ID")
            val banner = config("ADMOB_BANNER_ID")
            val interstitial = config("ADMOB_INTERSTITIAL_ID")
            val adsConfigured = appId.isNotBlank() && banner.isNotBlank() && interstitial.isNotBlank()
            manifestPlaceholders["admobAppId"] = appId.ifBlank { testAdMobAppId }
            buildConfigField("boolean", "ADS_ENABLED", adsConfigured.toString())
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$banner\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$interstitial\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "/META-INF/LICENSE*", "/META-INF/NOTICE*")
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        warningsAsErrors = false
        disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion")
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation("app.quranaudio:shared")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.datasource.okhttp)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.datastore.preferences)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)

    implementation(libs.play.services.ads)
    implementation(libs.ump)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.room.testing)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
}
