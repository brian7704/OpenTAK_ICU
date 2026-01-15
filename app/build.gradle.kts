import java.util.Properties
import java.io.FileInputStream

// Create a variable called keystorePropertiesFile, and initialize it to your
// keystore.properties file, in the rootProject folder.
val keystorePropertiesFile = file("../../keystore.properties")

// Initialize a new Properties() object called keystoreProperties.
val keystoreProperties = Properties()

// Load your keystore.properties file into the keystoreProperties object.
keystoreProperties.load(FileInputStream(keystorePropertiesFile))

plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("io.github.reactivecircus.app-versioning") version "1.3.2"
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21"
}

android {
    namespace = "io.opentakserver.opentakicu"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.opentakserver.opentakicu"
        minSdk = 26
        targetSdk = 36

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    packaging {
        resources.excludes.apply {
            add("META-INF/**")
        }
    }

    signingConfigs {
        create("release") {
            keyAlias = keystoreProperties["RELEASE_KEY_ALIAS"] as String
            keyPassword = keystoreProperties["RELEASE_KEY_PASSWORD"] as String
            storeFile = file("../../android_cert.jks")
            storePassword = keystoreProperties["RELEASE_STORE_PASSWORD"] as String
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }

        getByName("debug") {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
        compose = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation("androidx.appcompat:appcompat:1.7.1")
    //implementation("com.google.android.material:material:1.12.0")
    // Use 1.13.0-alpha08 because it adds orientation to the Slider
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.navigation:navigation-fragment:2.9.6")
    implementation("androidx.navigation:navigation-ui:2.9.6")
    implementation("androidx.fragment:fragment-ktx:1.8.9")
    implementation("com.github.pedroSG94.RootEncoder:library:2.6.6")
    implementation("com.github.AppIntro:AppIntro:6.3.1")
    implementation("androidx.datastore:datastore-preferences:1.2.0")
    implementation("androidx.datastore:datastore-preferences-rxjava3:1.2.0")
    implementation("androidx.preference:preference:1.2.1")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.15.0")
    implementation("com.fasterxml.woodstox:woodstox-core:6.5.1")
    implementation("javax.xml.stream:stax-api:1.0-2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.sealwu:kscript-tools:1.0.2")
    implementation("com.google.firebase:firebase-analytics:23.0.0")
    implementation(platform("com.google.firebase:firebase-bom:34.7.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.github.topjohnwu.libsu:core:6.0.0")
    implementation("com.github.topjohnwu.libsu:nio:6.0.0")
    implementation("me.zhanghai.compose.preference:preference:2.1.0")
    implementation("com.github.pedroSG94.RootEncoder:extra-sources:2.6.6")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.compose.ui:ui-tooling-preview-android:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.3")
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    implementation("com.composables:composeunstyled-theming:1.49.3")
    implementation("com.composables:composeunstyled:1.49.3")
    //implementation("com.composables:composeunstyled-primitives-android:1.49.3")
    implementation("com.composables:composeunstyled-platformtheme:1.49.3")
    implementation("androidx.activity:activity-compose:1.12.2")
    implementation("androidx.compose.ui:ui:1.10.0")
    implementation("androidx.compose.foundation:foundation:1.10.0")
    implementation("androidx.compose.runtime:runtime:1.10.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}