plugins {
    id("com.android.application")
}

android {
    compileSdkVersion(appConfig.compileSdkVersion)
    defaultConfig {
        applicationId = appConfig.appId
        // applicationIdSuffix ".apk"
        minSdkVersion(appConfig.minSdkVersion)
        targetSdkVersion(appConfig.targetSdkVersion)
        versionCode = appConfig.versionCode
        versionName = appConfig.versionName
    }

    signingConfigs {
        create("release") {
            storeFile = file(appConfig.signKeyStore)
            storePassword = appConfig.signKeyStorePassword
            keyAlias = appConfig.signKeyAlias
            keyPassword = appConfig.signKeyPassword
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFile("proguard-android.txt")
        }
    }

    sourceSets {
        getByName("main") {
            manifest.srcFile("../LatinIME/java/AndroidManifest.xml")
            java.setSrcDirs(listOf("../LatinIME/java/src", "../LatinIME/common/src", "../LatinIME/java-overridable/src", "../inputmethodcommon/java"))
            res.setSrcDirs(listOf("../LatinIME/java/res", "../LatinIME/java-overridable/res"))
        }
    }

    externalNativeBuild {
        ndkBuild {
            setPath("../LatinIME/native/jni/Android.mk")
        }
    }

    lintOptions {
        isAbortOnError = false
        isCheckReleaseBuilds = false
    }

    defaultConfig {
        externalNativeBuild {
            ndkBuild {
                // Suppress build failing warnings from dependencies
                cppFlags.add("-w")
                arguments.add("TARGET_BUILD_APPS=true")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}


dependencies {
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation(project(":tessercubecore"))
}
