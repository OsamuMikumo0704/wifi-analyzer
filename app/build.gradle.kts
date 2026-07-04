plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.sase.roomwifilogger"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sase.roomwifilogger"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.06.01")

    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.1")
    implementation("androidx.navigation:navigation-compose:2.9.1")
    implementation("androidx.room:room-runtime:2.7.2")
    implementation("androidx.room:room-ktx:2.7.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    ksp("androidx.room:room-compiler:2.7.2")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.room:room-testing:2.7.2")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}

val asciiTestClasspathRoot = File(System.getProperty("java.io.tmpdir"), "wifi-analyzer-test-classpath")

val copyDebugUnitTestKotlinClasses by tasks.registering(Copy::class) {
    dependsOn("compileDebugUnitTestKotlin")
    from(layout.buildDirectory.dir("tmp/kotlin-classes/debugUnitTest"))
    into(layout.buildDirectory.dir("intermediates/javac/debugUnitTest/compileDebugUnitTestJavaWithJavac/classes"))
}

val copyDebugClassesToAsciiPath by tasks.registering(Copy::class) {
    dependsOn("compileDebugKotlin", "compileDebugUnitTestKotlin")
    from(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
        into("debug")
    }
    from(layout.buildDirectory.dir("tmp/kotlin-classes/debugUnitTest")) {
        into("debugUnitTest")
    }
    into(asciiTestClasspathRoot)
}

afterEvaluate {
    tasks.named<org.gradle.api.tasks.testing.Test>("testDebugUnitTest").configure {
        dependsOn(copyDebugUnitTestKotlinClasses)
        dependsOn(copyDebugClassesToAsciiPath)
        classpath += files(
            asciiTestClasspathRoot.resolve("debug"),
            asciiTestClasspathRoot.resolve("debugUnitTest"),
        )
    }
}
