import com.gradleup.librarian.gradle.Librarian

plugins {
  alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
  jvm()
  macosArm64()

  sourceSets {
    commonMain {
      dependencies {
        api(libs.kotlinx.coroutines.core)
        api(libs.kotlinx.io)
        implementation(libs.kable.core)
        implementation(libs.klibnanolog)
      }
    }
  }
}

Librarian.module(project)
