package org.aph.plantwave.activities

import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.util.Size
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.humanware.keysoftsdk.selfbrailling.SelfBraillingManager
import com.humanware.keysoftsdk.selfbrailling.aidl.DotsMatrix
import com.humanware.keysoftsdk.selfbrailling.widget.SelfBraillingWidget
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.aph.plantwave.LiveSession
import org.aph.plantwave.graphics.AxisRenderer
import org.aph.plantwave.graphics.BrailleGlyphs
import org.aph.plantwave.graphics.LiveGraphRenderer
import kotlin.math.roundToInt

/**
 * Live-streaming graph screen. Renders LiveSession.readingsMv as it grows
 * (a session runs until the user backs out, not until a fixed duration
 * elapses) and speaks the value at a double-tapped point via the SDK's
 * built-in announceText -- no Translator/braille text service, so only
 * SelfBraillingManager is bound, one fewer AIDL service to manage.
 *
 * Double-tap, not single-tap: the SDK's Known Issues confirm single-tap is
 * blocked by the screen reader, so every "speak on tap" interaction here
 * goes through onDoubleTapAtBraillePosition.
 *
 * The line plots into AxisRenderer's fixed plot rectangle (rows 1-32, cols
 * 7-95 -- the full width, nothing overlays it). The current reading is
 * shown as braille numerals on the strip below the X axis, beside the
 * "time" label, so it never hides data. (An earlier bottom-right sidebar
 * box was dropped: it clipped the newest ~12s of low readings to frame a
 * number that fits in 14 pins.)
 *
 * Two view modes, toggled by the physical Zoom keys: LIVE (default -- a
 * scrolling strip chart, one sample per column, unchanged by the overview
 * work) and OVERVIEW (the whole session downsampled to fit, repainted only
 * every OVERVIEW_REFRESH_SAMPLES new samples rather than every 500ms, so
 * the shape doesn't keep resquishing under the reader's finger -- a
 * deliberate slower cadence, confirmed preferred over repainting every
 * sample despite reading as "paused" on first impression).
 *
 * Enter (Dot 8) toggles pause: a view-layer freeze. LiveSession keeps
 * accumulating regardless. A double-tap while paused speaks the value as it
 * was at pause time (see currentReadings()). D-pad center/Space are wired
 * to the same action but are dead on this hardware's onboard controls --
 * see onKeyDown's doc comment for why they're kept anyway.
 *
 * Physical keys arrive on finger-lift with both onKeyDown and onKeyUp
 * firing together (SDK Known Issues), and may be delivered more than once
 * per press -- so key actions are debounced and onKeyUp is consumed for the
 * handled keys. Key traffic is logged under TAG for adb logcat.
 *
 * Doesn't own the USB connection -- PlantWaveActivity does, and keeps
 * running in the background while this is in front. Backing out sends STOP
 * via LiveSession.requestStop(); PlantWaveActivity notices the session
 * ended when it resumes.
 */
class LiveGraphActivity : AppCompatActivity() {

    private lateinit var manager: SelfBraillingManager
    private lateinit var widget: SelfBraillingWidget
    private lateinit var screenDimensions: Size
    private lateinit var brailleScreen: DotsMatrix
    private val mutableLiveDots = MutableLiveData<Array<ByteArray>>()
    private val mutableViewedImage = MutableLiveData<Array<ByteArray>>()

    private var areServicesBound = false
    private var firstRenderDone = false

    private var isPaused = false
    private var pausedReadings: List<Double>? = null

    private var overviewMode = false
    private var lastOverviewPaintCount = 0

    private var lastKeyActionMs = 0L

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
                // No-op: single-tap is blocked by the screen reader on this device.
            }
            override fun onDoubleTapAtBraillePosition(p0: Int, p1: Int) {
                // The Y-axis gutter is a silent no-op: its ticks were removed as
                // redundant with the axis-name label right next to them.
                when {
                    p1 >= AxisRenderer.X_AXIS_ROW -> speakXAxisTick(p0)
                    p0 <= AxisRenderer.Y_AXIS_COL -> Unit
                    else -> speakValueAt(p0 - AxisRenderer.PLOT_COL_START)
                }
            }
        }

        LiveSession.readingsMv.observe(this) { readings ->
            if (isPaused) return@observe
            if (overviewMode && readings.size - lastOverviewPaintCount < OVERVIEW_REFRESH_SAMPLES) return@observe
            paint()
        }

        // SelfBraillingManager's AIDL service takes a moment to bind; calling into it
        // before that finishes hangs the main thread for ~5s and gets killed by the system.
        lifecycleScope.launch { delay(1000) }.invokeOnCompletion {
            firstRenderDone = true
            paint()
        }
    }

    override fun onResume() {
        super.onResume()
        manager.bindService()
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
        if (areServicesBound) {
            areServicesBound = false
            manager.unbindService()
        }
    }

    @Deprecated("Deprecated in Java", ReplaceWith("super.onBackPressed()"))
    override fun onBackPressed() {
        LiveSession.requestStop()
        super.onBackPressed()
    }

    /**
     * Pause's real trigger is Enter (Dot 8 on the braille keyboard, mapped in
     * commands.xml) -- confirmed via adb logcat that it never reached here at
     * all before that mapping existed. D-pad center and Space are kept only
     * as a fallback for an external keyboard: the vendor's own Command
     * Summary confirms this device's D-pad has no center/select position (no
     * commands.xml key token exists for it either), and Space has no
     * confirmed mapping in any reference project -- it's also a modifier
     * prefix for reserved shortcuts, so it isn't mapped here on purpose.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        Log.d(TAG, "onKeyDown keyCode=$keyCode repeat=${event?.repeatCount} aidlBound=${manager.aidlBound} firstRenderDone=$firstRenderDone")
        when (keyCode) {
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_SPACE -> debounced { togglePause() }
            KeyEvent.KEYCODE_ZOOM_OUT -> debounced { setOverview(true) }
            KeyEvent.KEYCODE_ZOOM_IN -> debounced { setOverview(false) }
            else -> return super.onKeyDown(keyCode, event)
        }
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_SPACE,
        KeyEvent.KEYCODE_ZOOM_IN, KeyEvent.KEYCODE_ZOOM_OUT -> true
        else -> super.onKeyUp(keyCode, event)
    }

    /** One physical press can surface as more than one key event on this device; only the first within the window counts. */
    private fun debounced(action: () -> Unit) {
        val now = SystemClock.uptimeMillis()
        val sinceLast = now - lastKeyActionMs
        if (sinceLast < KEY_DEBOUNCE_MS) {
            Log.d(TAG, "key action ignored: duplicate ${sinceLast}ms after the last one")
            return
        }
        lastKeyActionMs = now
        action()
    }

    /** Freezes the display at the current moment, or catches back up to live data.
     *  LiveSession keeps accumulating readings regardless -- purely a view-layer toggle. */
    private fun togglePause() {
        isPaused = !isPaused
        Log.d(TAG, "togglePause -> isPaused=$isPaused")
        if (isPaused) {
            pausedReadings = LiveSession.readingsMv.value ?: emptyList()
            if (firstRenderDone) manager.announceText("Paused")
        } else {
            pausedReadings = null
            if (firstRenderDone) manager.announceText("Resumed")
        }
        // Repaint on BOTH transitions -- pausing needs this too, to actually
        // show the indicator dot. Without it there was no on-screen change
        // at the moment of pausing at all, only the (possibly-unheard) word.
        paint()
    }

    private fun setOverview(enabled: Boolean) {
        if (overviewMode == enabled) {
            if (firstRenderDone) manager.announceText(if (enabled) "Already in overview" else "Already in live view")
            return
        }
        overviewMode = enabled
        Log.d(TAG, "setOverview -> overviewMode=$overviewMode")
        if (firstRenderDone) manager.announceText(if (enabled) "Overview" else "Live view")
        paint()
    }

    /** The frozen snapshot while paused, or the live list otherwise. */
    private fun currentReadings(): List<Double> = pausedReadings ?: (LiveSession.readingsMv.value ?: emptyList())

    private fun samplesPerColumn(readings: List<Double>): Int =
        if (overviewMode) LiveGraphRenderer.samplesPerColumnToFit(readings.size, AxisRenderer.PLOT_WIDTH) else 1

    private fun speakValueAt(plotColumn: Int) {
        if (!firstRenderDone) return
        val readings = currentReadings()
        val spc = samplesPerColumn(readings)
        val value = LiveGraphRenderer.columnValues(readings, AxisRenderer.PLOT_WIDTH, spc).getOrNull(plotColumn) ?: return
        val time = LiveGraphRenderer.timeSecondsForColumn(readings.size, AxisRenderer.PLOT_WIDTH, plotColumn, spc)
        val suffix = if (spc > 1) ", average of $spc samples" else ""
        manager.announceText("At ${LiveGraphRenderer.formatElapsedTime(time)}, ${value.roundToInt()} millivolts$suffix")
    }

    private fun speakXAxisTick(tapColumn: Int) {
        if (!firstRenderDone) return
        val readings = currentReadings()
        val tickColumn = AxisRenderer.nearestXTickColumn(tapColumn) - AxisRenderer.PLOT_COL_START
        val time = LiveGraphRenderer.timeSecondsForColumn(readings.size, AxisRenderer.PLOT_WIDTH, tickColumn, samplesPerColumn(readings))
        manager.announceText(LiveGraphRenderer.formatElapsedTime(time))
    }

    private fun clearScreen() {
        brailleScreen.include(DotsMatrix(Array(screenDimensions.height) { ByteArray(screenDimensions.width) }), 0, 0)
    }

    private fun paint() {
        if (!firstRenderDone) return
        val readings = currentReadings()
        val spc = samplesPerColumn(readings)
        if (overviewMode) lastOverviewPaintCount = readings.size

        clearScreen()
        val axisPixels = AxisRenderer.render(screenDimensions.width, screenDimensions.height)
        brailleScreen.include(DotsMatrix(axisPixels), 0, 0)
        val linePixels = LiveGraphRenderer.render(readings, AxisRenderer.PLOT_WIDTH, AxisRenderer.PLOT_HEIGHT, spc)
        brailleScreen.include(DotsMatrix(linePixels), AxisRenderer.PLOT_COL_START, AxisRenderer.PLOT_ROW_START)
        // Live reading as braille numerals on the strip below the X axis --
        // outside the plot, so it never hides data. Tight buffer, safe to blit.
        readings.lastOrNull()?.let {
            val glyphs = BrailleGlyphs.render(BrailleGlyphs.number(it.roundToInt()))
            brailleScreen.include(DotsMatrix(glyphs), AxisRenderer.READING_COL, AxisRenderer.READING_ROW)
        }
        // Permanent pause indicator: one pin, top-right corner, always-blank
        // otherwise. Driven by nothing but isPaused, so it's also an
        // unambiguous diagnostic -- if it never changes, togglePause() isn't
        // running at all, independent of whether announceText is heard.
        if (isPaused) {
            brailleScreen.include(DotsMatrix(arrayOf(byteArrayOf(1))), screenDimensions.width - 1, 0)
        }
        mutableViewedImage.value = brailleScreen.matrix
        mutableLiveDots.value = brailleScreen.matrix
    }

    companion object {
        private const val TAG = "PlantWave"
        private const val KEY_DEBOUNCE_MS = 400L
        private const val OVERVIEW_REFRESH_SAMPLES = 10
    }
}
