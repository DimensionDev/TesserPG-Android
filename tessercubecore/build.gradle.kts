plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("android.extensions")
    kotlin("kapt")
}

android {
    compileSdkVersion(appConfig.compileSdkVersion)
    defaultConfig {
        minSdkVersion(appConfig.minSdkVersion)
        targetSdkVersion(appConfig.targetSdkVersion)
        versionCode = appConfig.versionCode
        versionName = appConfig.versionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("debug") {
            resValue("bool", "is_app_center_enabled", appConfig.isAppCenterEnabled.toString())
            resValue("string", "app_center_id", appConfig.appCenterId)
        }
        getByName("release") {
            isMinifyEnabled = false
            resValue("bool", "is_app_center_enabled", "true")
            resValue("string", "app_center_id", appConfig.appCenterId)
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8", appConfig.kotlinVersion))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.1.0")
    implementation("androidx.preference:preference:1.1.0-alpha05")

    implementation("androidx.constraintlayout:constraintlayout:2.0.0-beta1")
    implementation("androidx.transition:transition:1.2.0-alpha01")
    implementation("androidx.appcompat:appcompat:1.1.0-alpha05")
    implementation("androidx.core:core-ktx:1.2.0-alpha01")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("com.google.android.material:material:1.0.0")
    implementation("androidx.gridlayout:gridlayout:1.0.0")

    implementation("com.karumi:dexter:5.0.0")
    implementation("io.reactivex.rxjava2:rxjava:2.2.8")
    implementation("io.reactivex.rxjava2:rxkotlin:2.3.0")
    implementation("uk.co.samuelwall:material-tap-target-prompt:2.14.0")
    implementation("com.github.tylersuehr7:chips-input-layout:2.3")
    implementation("org.ocpsoft.prettytime:prettytime:4.0.1.Final")
    implementation("moe.tlaster:floatinghover:1.0.8")
    implementation("moe.tlaster:kotlinpgp:1.0.20")
    implementation("com.romandanylyk:pageindicatorview:1.0.3")
//    testImplementation("org.web3j:core:4.2.0-android") {// TODO:
//        exclude("org.bouncycastle", "bcprov-jdk15on")
//        exclude("com.squareup.okhttp3", "okhttp")
//        exclude("com.squareup.okhttp3", "logging-interceptor")
//        exclude("io.reactivex.rxjava2", "rxjava")
//        exclude("org.java-websocket", "Java-WebSocket")
//        exclude("com.fasterxml.jackson.core", "jackson-databind")
//        exclude("org.slf4j", "slf4j-api")
//        exclude("nl.jqno.equalsverifier", "equalsverifier")
//        exclude("ch.qos.logback", "logback-classic")
//    }
//    testImplementation("org.bitcoinj:bitcoinj-core:0.15.1")// TODO:

    implementation("com.microsoft.appcenter:appcenter-analytics:${appConfig.dependencyVersion.appCenter}")
    implementation("com.microsoft.appcenter:appcenter-crashes:${appConfig.dependencyVersion.appCenter}")
    debugImplementation("com.squareup.leakcanary:leakcanary-android:${appConfig.dependencyVersion.leakcanary}")
    releaseImplementation("com.squareup.leakcanary:leakcanary-android-no-op:${appConfig.dependencyVersion.leakcanary}")
    debugImplementation("com.squareup.leakcanary:leakcanary-support-fragment:${appConfig.dependencyVersion.leakcanary}")
    implementation("com.github.bumptech.glide:glide:${appConfig.dependencyVersion.glide}")
    kapt("com.github.bumptech.glide:compiler:${appConfig.dependencyVersion.glide}")
    implementation("io.requery:requery:${appConfig.dependencyVersion.requery}")
    implementation("io.requery:requery-android:${appConfig.dependencyVersion.requery}")
    implementation("io.requery:requery-kotlin:${appConfig.dependencyVersion.requery}")
    kapt("io.requery:requery-processor:${appConfig.dependencyVersion.requery}")
    implementation("org.bouncycastle:bcprov-jdk15on:${appConfig.dependencyVersion.bouncycastle}")
    implementation("org.bouncycastle:bcpg-jdk15on:${appConfig.dependencyVersion.bouncycastle}")

    testImplementation("junit:junit:4.12")
    androidTestImplementation("androidx.test:runner:1.2.0-beta01")
    androidTestImplementation("androidx.test.ext:junit:1.1.1-beta01")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.2.0-beta01")
}
