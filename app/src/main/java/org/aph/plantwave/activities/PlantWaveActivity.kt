package org.aph.plantwave.activities

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Size
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import com.humanware.keysoftsdk.selfbrailling.SelfBraillingManager
import com.humanware.keysoftsdk.selfbrailling.aidl.DotsMatrix
import com.humanware.keysoftsdk.selfbrailling.widget.SelfBraillingWidget
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.aph.plantwave.LiveSession
import org.aph.plantwave.graphics.PlantWaveRenderer

/**
 * Layer 1: USB serial read + tactile UI.
 *
 * Talks to an Arduino over USB (CH340/FTDI/genuine-Arduino CDC, via
 * usb-serial-for-android's UsbSerialProber, which already knows the
 * vendor-specific setup those chips need). On START, sends "START\n" and
 * launches LiveGraphActivity; the Arduino now streams indefinitely (no
 * fixed duration, no DONE marker) until a "STOP\n" is sent, which
 * LiveGraphActivity does on its way out. Incoming readings are forwarded to
 * LiveSession (shared with LiveGraphActivity) rather than buffered locally.
 *
 * The screen itself is a hand-drawn SelfBraillingWidget canvas, not plain
 * Android views -- regular views were confirmed to produce no tactile
 * output at all on the Monarch's physical display. The art (PlantWaveRenderer/
 * HomeScreenArt) is user-authored, not generated here -- this activity only
 * wires up interactivity on top of it. START is double-tap on the ENTER
 * label (only present when connected) plus a plain-key fallback (Enter/
 * Space/D-pad-center) for an external keyboard, also gated on connection.
 * Error/connection states are silent -- no announceText, no on-screen text --
 * they just fall back to the WAITING screen art.
 */
class PlantWaveActivity : AppCompatActivity() {

    private enum class ReadState { WAITING, CONNECTED, READING }

    private lateinit var usbManager: UsbManager
    private lateinit var manager: SelfBraillingManager
    private lateinit var widget: SelfBraillingWidget
    private lateinit var screenDimensions: Size
    private lateinit var brailleScreen: DotsMatrix
    private val mutableLiveDots = MutableLiveData<Array<ByteArray>>()
    private val mutableViewedImage = MutableLiveData<Array<ByteArray>>()

    private var areServicesBound = false

    /** True once the initial ANR-avoidance delay has elapsed and it's safe to touch the AIDL services. */
    private var firstRenderDone = false

    private var state = ReadState.WAITING
    private var currentDriver: UsbSerialDriver? = null
    private var serialPort: UsbSerialPort? = null
    private var ioManager: SerialInputOutputManager? = null

    /** Accumulates incoming bytes until a full line (ending in '\n') is available. */
    private val lineAccumulator = StringBuilder()

    private val permissionIntent: PendingIntent by lazy {
        PendingIntent.getBroadcast(
            this, 0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_MUTABLE
        )
    }

    private val permissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != ACTION_USB_PERMISSION) return
            val device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
            val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
            val driver = currentDriver
            if (granted && device != null && driver != null && device == driver.device) {
                openConnection(driver)
            }
        }
    }

    /** Re-attempts connection on attach; tears down cleanly on detach. */
    private val usbAttachReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> attemptConnect()
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    if (device != null && device == currentDriver?.device) {
                        teardownConnection()
                    }
                }
            }
        }
    }

    private val serialListener = object : SerialInputOutputManager.Listener {
        override fun onNewData(data: ByteArray) {
            runOnUiThread { handleIncomingBytes(data) }
        }

        override fun onRunError(e: Exception) {
            runOnUiThread { teardownConnection() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        areServicesBound = true

        manager = SelfBraillingManager(this).apply { bindService() }
        widget = SelfBraillingWidget(this).also { setContentView(it) }
        supportActionBar?.hide()

        mutableViewedImage.observe(this) { widget.refresh(it) }
        mutableLiveDots.observe(this) { manager.displayDots(it) }
        screenDimensions = Size(manager.brailleDisplayDotsSizeX, manager.brailleDisplayDotsSizeY)
        brailleScreen = DotsMatrix(screenDimensions.height, screenDimensions.width)

        widget.onSelfBraillingWidgetListener = object : SelfBraillingWidget.OnSelfBraillingWidgetListener {
            override fun onFocused() = paint()
            override fun onSingleTapAtBraillePosition(p0: Int, p1: Int) {
                // No-op: only double-tap activates START in this UI.
            }
            override fun onDoubleTapAtBraillePosition(p0: Int, p1: Int) {
                if (PlantWaveRenderer.resolveAction(p0, p1) == PlantWaveRenderer.Action.START) {
                    attemptStart()
                }
            }
        }

        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        registerReceiver(
            permissionReceiver, IntentFilter(ACTION_USB_PERMISSION), Context.RECEIVER_NOT_EXPORTED
        )
        val attachFilter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        registerReceiver(usbAttachReceiver, attachFilter, Context.RECEIVER_NOT_EXPORTED)
        attemptConnect()

        // SelfBraillingManager's AIDL service takes a moment to bind; calling into it before
        // that finishes hangs the main thread for ~5s and gets killed by the system. USB setup
        // above has nothing to do with this service and correctly runs immediately; paint()
        // itself is guarded by firstRenderDone so any state changes that land before this fires
        // (e.g. instant USB connect) aren't lost -- they're picked up by this first paint.
        lifecycleScope.launch { delay(1000) }.invokeOnCompletion {
            firstRenderDone = true
            paint()
        }
    }

    override fun onResume() {
        super.onResume()
        manager.bindService()
        // The only way back to this screen while state==READING is via LiveGraphActivity's
        // back press, which always sends STOP first -- so a live session is over by now.
        if (state == ReadState.READING) {
            state = ReadState.CONNECTED
            paint()
        }
        if (!areServicesBound) {
            areServicesBound = true
            lifecycleScope.launch { delay(1000) }.invokeOnCompletion {
                firstRenderDone = true
                paint()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (areServicesBound) {
            areServicesBound = false
            firstRenderDone = false
            manager.unbindService()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        teardownConnection()
        unregisterReceiver(permissionReceiver)
        unregisterReceiver(usbAttachReceiver)
        if (areServicesBound) {
            areServicesBound = false
            manager.unbindService()
        }
    }

    /**
     * Real trigger is Enter (Dot 8 on the braille keyboard) -- reaches
     * onKeyDown only because commands.xml now maps it (`Dots8` -> keycode
     * 66); confirmed via adb logcat on the graph screen that this was never
     * true without that mapping. D-pad center/Space are kept only as an
     * external-keyboard fallback -- this device's D-pad has no center
     * position at all (confirmed against the vendor's own Command Summary),
     * and Space isn't mapped in commands.xml on purpose (see that file).
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_SPACE -> attemptStart()
            else -> return super.onKeyDown(keyCode, event)
        }
        return true
    }

    /** Looks for a supported serial device and requests permission or opens it. */
    private fun attemptConnect() {
        // Already holding an open connection -- nothing to do. Without this guard, a
        // duplicate ATTACHED broadcast would open a second port on top of the first,
        // leaking the old one and its I/O thread.
        if (serialPort != null) return

        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        val driver = availableDrivers.firstOrNull()
        if (driver == null) {
            teardownConnection()
            return
        }

        currentDriver = driver
        if (usbManager.hasPermission(driver.device)) {
            openConnection(driver)
        } else {
            usbManager.requestPermission(driver.device, permissionIntent)
        }
    }

    private fun openConnection(driver: UsbSerialDriver) {
        val connection = usbManager.openDevice(driver.device)
        if (connection == null) return

        val port = driver.ports.firstOrNull()
        if (port == null) return

        try {
            port.open(connection)
            port.setParameters(
                BAUD_RATE, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE
            )
        } catch (e: Exception) {
            return
        }

        serialPort = port
        LiveSession.activePort = port
        val serialIoManager = SerialInputOutputManager(port, serialListener)
        ioManager = serialIoManager
        serialIoManager.start()

        state = ReadState.CONNECTED
        paint()
    }

    private fun teardownConnection() {
        ioManager?.stop()
        ioManager = null
        try {
            serialPort?.close()
        } catch (_: Exception) {
            // Already gone (e.g. device unplugged) -- nothing more to clean up.
        }
        serialPort = null
        LiveSession.activePort = null
        currentDriver = null
        lineAccumulator.clear()
        state = ReadState.WAITING
    }

    private fun attemptStart() {
        if (state == ReadState.CONNECTED) onStartTapped()
    }

    private fun onStartTapped() {
        val port = serialPort
        if (port == null) {
            attemptConnect()
            return
        }

        lineAccumulator.clear()
        LiveSession.reset()
        state = ReadState.READING
        paint()

        try {
            port.write("START\n".toByteArray(), WRITE_TIMEOUT_MS)
            startActivity(Intent(this, LiveGraphActivity::class.java))
        } catch (e: Exception) {
            teardownConnection()
        }
    }

    /** Splits incoming bytes into lines, forwarding each as an integer reading to LiveSession. */
    private fun handleIncomingBytes(data: ByteArray) {
        if (state != ReadState.READING) return

        lineAccumulator.append(String(data, Charsets.US_ASCII))

        var newlineIndex = lineAccumulator.indexOf("\n")
        while (newlineIndex >= 0) {
            val line = lineAccumulator.substring(0, newlineIndex).trim()
            lineAccumulator.delete(0, newlineIndex + 1)

            val value = line.toIntOrNull()
            if (value != null) {
                LiveSession.addReading(value)
            }

            newlineIndex = lineAccumulator.indexOf("\n")
        }
    }

    private fun clearScreen() {
        brailleScreen.include(DotsMatrix(Array(screenDimensions.height) { ByteArray(screenDimensions.width) }), 0, 0)
    }

    private fun paint() {
        if (!firstRenderDone) return

        val connected = state != ReadState.WAITING

        clearScreen()
        PlantWaveRenderer.render(connected, brailleScreen)
        mutableViewedImage.value = brailleScreen.matrix
        mutableLiveDots.value = brailleScreen.matrix
    }

    companion object {
        private const val ACTION_USB_PERMISSION = "org.aph.plantwave.USB_PERMISSION"
        private const val BAUD_RATE = 9600
        private const val WRITE_TIMEOUT_MS = 2000
    }
}
