package org.aph.plantwave

import android.app.Application
import com.humanware.keysoftsdk.contextmenu.WriteCommandsXmlFileToInternalMemoryStorageExecutor

// @HiltAndroidApp removed -- the Hilt Gradle plugin was never applied (no
// com.google.dagger.hilt.android plugin, no kapt/ksp), so the annotation
// was always inert: no @Inject/@Module/@AndroidEntryPoint exists anywhere
// in this app. Real dependency injection was never actually happening.
class PlantWaveApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        //pass the commands.xml file to KeySoft
        WriteCommandsXmlFileToInternalMemoryStorageExecutor(this).execute()
    }
}
