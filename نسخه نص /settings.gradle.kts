pluginManagement {
  repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
  }
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    
    // هذا السطر هو الحل لتحميل مكتبة Tesseract بنجاح
    maven { url = uri("https://jitpack.io") }
  }
}

rootProject.name = "نسخه نص "

include(":app") 