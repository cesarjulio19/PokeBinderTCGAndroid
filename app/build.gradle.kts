import org.jetbrains.dokka.gradle.DokkaTask
import org.gradle.api.tasks.Copy

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")

}

apply(plugin = "org.jetbrains.dokka")


tasks.withType<DokkaTask>().configureEach {
    outputDirectory.set(buildDir.resolve("docs/kotlin"))
}



android {
    namespace = "com.example.pokemontcg"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.pokemontcg"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.paging:paging-common-android:3.3.6")
    implementation("androidx.paging:paging-runtime-ktx:3.3.6")
    implementation("androidx.hilt:hilt-common:1.2.0")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    kapt("androidx.hilt:hilt-compiler:1.2.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation ("com.squareup.okhttp3:okhttp:4.9.3")


    // Fragmentos
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.5")

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")

    // Kotlin coroutines + Flow
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Glide (para imágenes)
    implementation("com.github.bumptech.glide:glide:4.15.1")
    kapt("com.github.bumptech.glide:compiler:4.15.1")

    // Hilt (inyección de dependencias)
    implementation ("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")

    // ViewModel + LiveData + StateFlow
    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Para usar StateFlow
    implementation ("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Room
    implementation ("androidx.room:room-runtime:2.6.1")
    kapt ("androidx.room:room-compiler:2.6.1")
    implementation ("androidx.room:room-paging:2.5.2")

// Opcional: para usar Flow directamente con Room
    implementation ("androidx.room:room-ktx:2.6.1")
}
kapt {
    correctErrorTypes = true
}

// Tarea para debug
tasks.register<Copy>("copyDebugApk") {
    dependsOn("assembleDebug")
    val apkSource = layout.buildDirectory.dir("outputs/apk/debug")
    from(apkSource)
    include("*.apk")
    into(layout.projectDirectory.dir("../apk"))  // ../apk desde app/
}

// Tarea para release
tasks.register<Copy>("copyReleaseApk") {
    dependsOn("assembleRelease")
    val apkSource = layout.buildDirectory.dir("outputs/apk/release")
    from(apkSource)
    include("*.apk")
    into(layout.projectDirectory.dir("../apk"))
}



