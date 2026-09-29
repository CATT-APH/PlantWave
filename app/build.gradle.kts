plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

//Needed for Monarch Development
val jenkinsBuildCode: String = System.getenv("J_VERCODE") ?: "1"
val jenkinsBuildName: String = System.getenv("J_VERNAME") ?: "1.0"

//When testing debug builds on a Monarch that already has the app loaded on it from HumanWare.
//val jenkinsBuildCode = System.getenv("J_VERCODE") ?: "99999999"
//val jenkinsBuildName = System.getenv("J_VERNAME") ?: "Debug [99999999]"

android {
    namespace = "org.aph.plantwave"
    compileSdk = 34

    defaultConfig {
        applicationId = "org.aph.plantwave"
        minSdk = 33
        targetSdk = 33
        versionCode = jenkinsBuildCode.toInt()
        versionName = jenkinsBuildName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("com.humanware:KeySoftSDK:latest.release") //Needed for Monarch Development
    implementation("com.google.dagger:hilt-android:2.45") //Needed for Monarch Development
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")

    // USB CDC/FTDI/CH340 serial driver -- handles the vendor-specific control
    // transfers (baud rate, DTR/RTS) that raw UsbManager.bulkTransfer alone
    // can't do reliably. Not used by the Step 0 detection spike, but wired up
    // now for the full Layer 1 serial read.
    implementation("com.github.mik3y:usb-serial-for-android:3.9.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
