// ✅ Top-level build.gradle.kts (Project-level)
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.google.gms.google.services) apply false
    alias(libs.plugins.google.firebase.crashlytics) apply false
    alias(libs.plugins.google.firebase.perf) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt.android) apply false
}

// ✅ Kotlin DSL मध्ये 'extra' वापरायचा — 'ext' नाही
extra["kotlin_version"] = "2.0.0"
extra["compose_version"] = "1.7.3"

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
