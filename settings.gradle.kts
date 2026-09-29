pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

//Load properties from keystore.properties
var token = ""
val propertyKey = "gitlab_maven_repo_deployToken"
val keystorePropertyFile = file("keystore.properties")
if (keystorePropertyFile.exists()){
    val keystoreProperties = java.util.Properties()
    keystorePropertyFile.inputStream().use {
        keystoreProperties.load(it)
    }

    if (keystoreProperties.containsKey(propertyKey)) {
        token = keystoreProperties.getProperty(propertyKey)
    }
}

val gitlabMavenRepoUrl: String by settings
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri(gitlabMavenRepoUrl)
            credentials(HttpHeaderCredentials::class) {
                name = "Deploy-Token"
                value = token
            }
            authentication {
                create<HttpHeaderAuthentication>("header")
            }
        }
        // usb-serial-for-android (mik3y) is distributed via JitPack, not Maven Central.
        // Not used by the Step 0 detection spike (UsbManager.getDeviceList() is plain
        // Android API), but wired up now since the full Layer 1 build needs it.
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "PlantWave"
include(":app")
