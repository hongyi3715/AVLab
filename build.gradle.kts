// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}
ktlint {
    verbose.set(true)
    android.set(true)
    outputColorName.set("RED")
}

detekt {
    ignoreFailures = true
    buildUponDefaultConfig = true
    parallel = true
}
