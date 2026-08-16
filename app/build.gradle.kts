import org.gradle.api.GradleException
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

val signingEnvironmentKeys = listOf(
    "GTA_UPLOAD_KEYSTORE_PATH",
    "GTA_UPLOAD_STORE_PASSWORD",
    "GTA_UPLOAD_KEY_ALIAS",
    "GTA_UPLOAD_KEY_PASSWORD",
)
val signingEnvironmentValues = signingEnvironmentKeys.associateWith { key ->
    providers.environmentVariable(key).orNull
}
val anySigningEnvironmentValue = signingEnvironmentValues.values.any { value -> value != null }
val signingEnvironmentConfigured = signingEnvironmentValues.values.all { value -> !value.isNullOrBlank() }

if (anySigningEnvironmentValue && !signingEnvironmentConfigured) {
    val missingKeys = signingEnvironmentValues
        .filterValues { value -> value.isNullOrBlank() }
        .keys
        .joinToString()
    throw GradleException(
        "Environment-backed release signing is incomplete. Set all four GTA_UPLOAD_* variables; " +
            "missing: $missingKeys",
    )
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (!signingEnvironmentConfigured && keystorePropertiesFile.isFile) {
        keystorePropertiesFile.inputStream().use(::load)
    }
}

android {
    namespace = "com.rupayonhaldar.gtafreestem"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.rupayonhaldar.gtafreestem"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val releaseSigning = when {
        signingEnvironmentConfigured -> signingConfigs.create("release") {
            storeFile = rootProject.file(requireNotNull(signingEnvironmentValues["GTA_UPLOAD_KEYSTORE_PATH"]))
            storePassword = requireNotNull(signingEnvironmentValues["GTA_UPLOAD_STORE_PASSWORD"])
            keyAlias = requireNotNull(signingEnvironmentValues["GTA_UPLOAD_KEY_ALIAS"])
            keyPassword = requireNotNull(signingEnvironmentValues["GTA_UPLOAD_KEY_PASSWORD"])
        }
        keystorePropertiesFile.isFile -> signingConfigs.create("release") {
            storeFile = rootProject.file(requireNotNull(keystoreProperties.getProperty("storeFile")))
            storePassword = requireNotNull(keystoreProperties.getProperty("storePassword"))
            keyAlias = requireNotNull(keystoreProperties.getProperty("keyAlias"))
            keyPassword = requireNotNull(keystoreProperties.getProperty("keyPassword"))
        }
        else -> null
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = releaseSigning
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = false
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }

    lint {
      abortOnError = true
      checkReleaseBuilds = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.serialization.json)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.lifecycle.viewmodel.ktx)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

}
