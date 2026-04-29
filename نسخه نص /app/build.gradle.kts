plugins {
    id("com.android.application")
}

android {
    namespace = "com.yourname.universalcopy"
    compileSdk = 33
    
    defaultConfig {
        applicationId = "com.yourname.universalcopy"
        minSdk = 21
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
        
        vectorDrawables { 
            useSupportLibrary = true
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildTypes {
        getByName("release") {
            // 🛡️ تفعيل التشفير (المركز والخفيف)
            isMinifyEnabled = true 
            
            // 🪶 إيقاف حذف الموارد لتقليل استهلاك الرام ومنع انهيار AndroidIDE
            isShrinkResources = false 
            
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        
        getByName("debug") {
            isMinifyEnabled = false // اتركه معطلاً في Debug للسرعة أثناء التجربة
        }
    }

    // 🚀 طرد المفتش (Lint) لتسريع البناء وتوفير موارد الجهاز
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")

    // مكتبات الترجمة والتعرف على اللغة
    implementation("com.google.mlkit:translate:17.0.2")
    implementation("com.google.mlkit:language-id:17.0.5")
    
    // مكتبة إعلانات ياندكس
    implementation("com.yandex.android:mobileads:7.0.0") 
    implementation("io.appmetrica.analytics:analytics:6.3.0")
}