plugins {
    id("org.jetbrains.kotlin.jvm")
    `java-gradle-plugin`
    `maven-publish`
}

group = "com.github.amigo-wangwx.debug-toolkit"
version = providers.gradleProperty("VERSION_NAME").getOrElse("1.0.0")

dependencies {
    implementation(gradleApi())
    implementation("com.android.tools.build:gradle-api:8.13.2")
}

kotlin {
    jvmToolchain(17)
}

gradlePlugin {
    plugins {
        register("debugNetworkInterceptor") {
            id = "com.debugtoolkit.debug-network-interceptor"
            implementationClass = "DebugNetworkInterceptorPlugin"
        }
    }
}
