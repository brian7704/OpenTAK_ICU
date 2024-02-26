import java.io.ByteArrayOutputStream

plugins {
    id("com.android.application")
}

val gitDescribe: String by lazy {
    val stdout = ByteArrayOutputStream()
    rootProject.exec {
        commandLine("git", "describe", "--tags", "--always")
        standardOutput = stdout
    }
    stdout.toString().trim()
}

android {
    namespace = "io.opentakserver.opentakicu"
    compileSdk = 34

    defaultConfig {
        applicationId = "io.opentakserver.opentakicu"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = gitDescribe

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    packaging {
        resources.excludes.apply {
            add("META-INF/**")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.navigation:navigation-fragment:2.7.6")
    implementation("androidx.navigation:navigation-ui:2.7.6")
    implementation("com.github.pedroSG94.RootEncoder:library:2.3.6")
    implementation("com.github.AppIntro:AppIntro:6.3.1")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.datastore:datastore-preferences-rxjava3:1.0.0")
    implementation("androidx.preference:preference:1.2.1")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.15.0")
    implementation("com.fasterxml.woodstox:woodstox-core:6.5.1")
    implementation("javax.xml.stream:stax-api:1.0-2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.sealwu:kscript-tools:1.0.2")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}