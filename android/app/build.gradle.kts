plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "kr.surprise.memorymap"
    compileSdk = 36

    defaultConfig {
        applicationId = "kr.surprise.memorymap"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    /**
     * dev / prod. **서버 한 벌을 고르는 축**입니다 — 어느 Firebase 프로젝트에 붙을지는
     * `AppContainer` 의 `FirebaseEnv` 가 `BuildConfig.FLAVOR` 를 보고 정합니다.
     *
     * dev 는 패키지에 `.dev` 를 붙여 **폰에 prod 와 나란히 설치**됩니다 — 시험하다
     * 실제 쓰는 앱의 데이터를 건드리지 않으려는 것입니다. 이름도 '짜국 Dev' 로
     * 갈라 둡니다(`src/dev/res`) — 두 아이콘이 같은 이름이면 어느 쪽인지 모릅니다.
     */
    flavorDimensions += "env"
    productFlavors {
        create("dev") {
            dimension = "env"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
        }
        create("prod") {
            dimension = "env"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // buildConfig 는 `BuildConfig.FLAVOR` 때문에 켭니다 — 조립부가 서버 한 벌을 고를 때 봅니다.
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":core:network"))
    implementation(project(":domain"))
    implementation(project(":data:auth"))
    implementation(project(":data:photo"))
    implementation(project(":data:space"))
    implementation(project(":data:region"))
    implementation(project(":feature:space"))
    implementation(project(":feature:map"))
    implementation(project(":feature:calendar"))
    implementation(project(":feature:upload"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.animation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    // 구글 로그인. play-services 쪽은 런타임 구현체라 implementation 으로만 씁니다.
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)

    implementation(libs.okhttp)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    testImplementation(libs.junit)
}
