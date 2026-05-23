plugins {
  alias(libs.plugins.kotlin.jvm)
}

dependencies {
  // Kotlin
  implementation(libs.kotlinx.coroutines.jdk9)

  // Logging
  implementation(libs.klibnanolog)

  // Library
  implementation(project(":klibperipage"))
}
