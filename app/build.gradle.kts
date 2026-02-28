import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Properties

// ========== 加载 local.properties ==========
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

// ========== 可配置常量 ==========
object AppConfig {
    const val APP_NAME = "TVBoxTools"
    const val DATE_FORMAT = "yyyyMMdd"
    val buildDate: String
        get() = LocalDate.now().format(DateTimeFormatter.ofPattern(DATE_FORMAT))
    val buildDateInt: Int
        get() = buildDate.toInt()
}

plugins {
    id("com.android.application")
}
// 顶级构建文件
buildscript {
    repositories {
        // 阿里云镜像
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }

        // 腾讯云镜像
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }

        // 保留官方仓库作为备用
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.1.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.0")
    }
}

android {
    namespace = "anyang.tvbox.tools"
    compileSdk = 35

    defaultConfig {
        applicationId = "anyang.tvbox.tools"
        minSdk = 25
        targetSdk = 35
        versionCode = AppConfig.buildDateInt
        versionName = AppConfig.buildDate

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // APK 输出文件名: TVBoxTools-20260208.apk
    androidComponents {
        onVariants { variant ->
            variant.outputs.forEach { output ->
                if (output is com.android.build.api.variant.impl.VariantOutputImpl) {
                    output.outputFileName = "${AppConfig.APP_NAME}-${AppConfig.buildDate}.apk"
                }
            }
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_FILE") ?: localProperties.getProperty("KEYSTORE_FILE", ""))
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: localProperties.getProperty("KEYSTORE_PASSWORD", "")
            keyAlias = System.getenv("KEY_ALIAS") ?: localProperties.getProperty("KEY_ALIAS", "")
            keyPassword = System.getenv("KEY_PASSWORD") ?: localProperties.getProperty("KEY_PASSWORD", "")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.activity:activity:1.8.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation ("androidx.core:core-ktx:1.13.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

// ========== 自动拷贝 APK 到桌面 ==========
tasks.register<Copy>("copyApkToDesktop") {
    val desktopDir = File(System.getProperty("user.home"), "Desktop/AppOutputs")

    from(layout.buildDirectory.dir("outputs/apk/release"))
    into(desktopDir)
    include("*.apk")

    doFirst {
        desktopDir.mkdirs()
        println("📦 正在拷贝 APK 到: ${desktopDir.absolutePath}")
    }
    doLast {
        println("✅ APK 已拷贝到桌面 AppOutputs 文件夹")
    }
}

afterEvaluate {
    // 绑定到 assembleRelease 任务
    tasks.named("assembleRelease").configure { finalizedBy("copyApkToDesktop") }
    // 绑定到 installRelease 任务
    tasks.findByName("installRelease")?.let { it.finalizedBy("copyApkToDesktop") }
}