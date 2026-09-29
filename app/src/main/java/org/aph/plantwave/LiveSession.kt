package org.aph.plantwave

import androidx.lifecycle.MutableLiveData
import com.hoho.android.usbserial.driver.UsbSerialPort
import org.aph.plantwave.graphics.ADCConverter

/**
 * Shared live-session state between PlantWaveActivity (owns the USB serial
 * connection) and LiveGraphActivity (renders it). Deliberately a plain
 * in-process singleton, not a formal repository/service layer -- this is a
 * two-activity, single-process app and a shared object is enough.
 */
object LiveSession {
    const val MAX_POINTS = 1000
    const val SAMPLE_INTERVAL_MS = 500L

    val readingsMv = MutableLiveData<List<Double>>(emptyList())

    /** Set by PlantWaveActivity whenever it has an open port; used by LiveGraphActivity to send STOP. */
    var activePort: UsbSerialPort? = null

    fun reset() {
        readingsMv.value = emptyList()
    }

    fun addReading(adcValue: Int) {
        val updated = (readingsMv.value ?: emptyList()) + ADCConverter.toMillivolts(adcValue)
        readingsMv.value = if (updated.size > MAX_POINTS) updated.takeLast(MAX_POINTS) else updated
    }

    /** Best-effort: writes "STOP\n" to whatever port is currently open. */
    fun requestStop() {
        try {
            activePort?.write("STOP\n".toByteArray(), 2000)
        } catch (_: Exception) {
            // Port's already gone -- nothing to stop.
        }
    }
}
