// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath ("com.google.dagger:hilt-android-gradle-plugin:2.48")
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:1.8.10")
    }
}



plugins {
    id("com.android.application") version "8.1.3" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
}