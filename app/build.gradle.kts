import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

/**
 * The signing key lives OUTSIDE the working tree: a keystore inside the repository is a keystore
 * anyone who can clone can sign with, and the signature is the only thing that lets an update
 * install over an existing copy of the app.
 */
val keystorePropertiesFile: File = System.getenv("CURRENCY_KEYSTORE_PROPERTIES")
    ?.let { file(it) }
    ?: File(System.getProperty("user.home"), ".currency/keystore.properties")

val keystoreProperties: Properties = Properties()
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { stream -> keystoreProperties.load(stream) }
}

var hasReleaseSigning = false

android {
    namespace = "com.lamahuissn.currency"

    // Compose 1.12 / core-ktx 1.19 are compiled against API 37; target stays 36.
    compileSdk = 37

    defaultConfig {
        applicationId = "com.lamahuissn.currency"
        minSdk = 26
        targetSdk = 36
        versionCode = 6
        versionName = "1.0.5"
    }

    if (!keystoreProperties.isEmpty) {
        signingConfigs {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }

        hasReleaseSigning = true
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

tasks.matching { it.name.contains("Release") }.configureEach {
    doFirst {
        require(hasReleaseSigning) {
            "No release key: put storeFile/storePassword/keyAlias/keyPassword in $keystorePropertiesFile"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.okhttp)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    // org.json is an Android platform class; on a JVM unit test it is a stub that throws, so the
    // real jar on the test classpath is what makes the parsers testable at all.
    testImplementation(libs.json)
}
