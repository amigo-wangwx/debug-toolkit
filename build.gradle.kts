plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
}

allprojects {
    group = "com.github.amigo-wangwx.debug-toolkit"
    version = providers.gradleProperty("VERSION_NAME").getOrElse("1.0.0")
}
