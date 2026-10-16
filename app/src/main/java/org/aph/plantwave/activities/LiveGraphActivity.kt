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
import org.aph.plantwave.graphics.LivePage
import kotlin.math.roundToInt

/**
 * Live-streaming graph screen. Speaks the value at a double-tapped point via
 * the SDK's built-in announceText -- no Translator/braille text service, so
 * only SelfBraillingManager is bound, one fewer AIDL service to manage.
 *
 * Live view (overviewMode=false) renders in fixed-length pages (see
 * LivePage): a page is the next PAGE_CAPACITY_SAMPLES (= PLOT_WIDTH)
 * incoming samples, one per column, never a sliding window. Once a page's
 * columns are drawn they never get remapped to a different value or row --
 * only the newest column changes per tick, which keeps physical pin churn
 * low on the real device (a sliding window, by contrast, can reshape nearly
 * every column every tick even though only one new sample arrived -- this
 * was reported as visible glitching and an untrackable line on real
 * hardware). When a page fills, it rolls over to a fresh one seeded from the
 * previous page's own min/max, via catchUpPage()/rolloverPage() below.
 * Overview mode is untouched by this -- it still renders the whole session
 * history from LiveSession.readingsMv directly, exactly as before.
 *
 * No screen space is spent showing the running min/max -- instead, a
 * genuine new session high/low (LivePage.append() returning NEW_MAX/
 * NEW_MIN) is announced aloud immediately via announceText, so a real spike
 * is audible the moment it happens without sacrificing any of the tactile
 * graph to a stats readout.
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
 * Two view modes, toggled by D-pad Up (live)/D-pad Down (overview): LIVE
 * (default -- the paged strip chart described above) and OVERVIEW (the
 * whole session downsampled to fit, repainted only every
 * OVERVIEW_REFRESH_SAMPLES new samples rather than every 500ms, so the
 * shape doesn't keep resquishing under the reader's finger -- a deliberate
 * slower cadence, confirmed preferred over repainting every sample despite
 * reading as "paused" on first impression).
 *
 * Bound to D-pad Up/Down. The Zoom keys are reserved by the device's screen
 * reader and never reach the app, so they are not used. D-pad Up/Down are
 * the reliable input on this hardware.
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

    private var currentPage = LivePage(PAGE_CAPACITY_SAMPLES)
    private var consumedSessionCount = 0

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
            if (overviewMode) {
                if (readings.size - lastOverviewPaintCount < OVERVIEW_REFRESH_SAMPLES) return@observe
            } else {
                catchUpPage(readings)
            }
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
     * commands.xml). It never reaches here without that mapping. D-pad center
     * and Space are kept only as a fallback for an external keyboard. This
     * device's D-pad has no center position, and Space is not mapped in
     * commands.xml on purpose.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        Log.d(TAG, "onKeyDown keyCode=$keyCode repeat=${event?.repeatCount} aidlBound=${manager.aidlBound} firstRenderDone=$firstRenderDone")
        when (keyCode) {
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_SPACE -> debounced { togglePause() }
            // D-pad Up/Down, not Zoom -- Zoom is reserved by the screen reader and
            // never reaches onKeyDown.
            KeyEvent.KEYCODE_DPAD_UP -> debounced { setOverview(false) }
            KeyEvent.KEYCODE_DPAD_DOWN -> debounced { setOverview(true) }
            else -> return super.onKeyDown(keyCode, event)
        }
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_SPACE,
        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> true
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
            if (!overviewMode) catchUpPage(LiveSession.readingsMv.value ?: emptyList())
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
        // Paused freezes the page clock too (same as the main observer) --
        // don't let switching views while paused sneak in a catch-up.
        if (!enabled && !isPaused) catchUpPage(LiveSession.readingsMv.value ?: emptyList())
        if (firstRenderDone) manager.announceText(if (enabled) "Overview" else "Live view")
        paint()
    }

    /** The frozen snapshot while paused, or the live list otherwise. */
    private fun currentReadings(): List<Double> = pausedReadings ?: (LiveSession.readingsMv.value ?: emptyList())

    /**
     * Drains whatever's new in [readings] (vs. consumedSessionCount) into
     * currentPage, rolling pages over as needed, and announces the most
     * extreme new high/low seen in this batch (usually just one sample;
     * more after a pause/overview gap, batched into a single announcement
     * each rather than one per sample).
     */
    private fun catchUpPage(readings: List<Double>) {
        if (readings.size < consumedSessionCount) {
            // LiveSession.reset() happened (a fresh START) -- start over.
            consumedSessionCount = 0
            currentPage = LivePage(PAGE_CAPACITY_SAMPLES)
        }

        var newHigh: Double? = null
        var newLow: Double? = null
        for (i in consumedSessionCount until readings.size) {
            if (currentPage.isFull) rolloverPage()
            when (currentPage.append(readings[i])) {
                LivePage.BoundsChange.NEW_MAX -> newHigh = readings[i]
                LivePage.BoundsChange.NEW_MIN -> newLow = readings[i]
                LivePage.BoundsChange.NONE -> Unit
            }
        }
        consumedSessionCount = readings.size

        if (firstRenderDone) {
            newHigh?.let { Log.d(TAG, "new high ${it.roundToInt()}mV"); manager.announceText("New high, ${it.roundToInt()} millivolts") }
            newLow?.let { Log.d(TAG, "new low ${it.roundToInt()}mV"); manager.announceText("New low, ${it.roundToInt()} millivolts") }
        }
    }

    private fun rolloverPage() {
        val seed = currentPage.rawBounds()
        Log.d(TAG, "page full -> rollover, seed=$seed")
        currentPage = LivePage(PAGE_CAPACITY_SAMPLES, seed?.first, seed?.second)
    }

    private fun samplesPerColumn(readings: List<Double>): Int =
        if (overviewMode) LiveGraphRenderer.samplesPerColumnToFit(readings.size, AxisRenderer.PLOT_WIDTH) else 1

    /** Live reads the current page (page-relative time); Overview reads the whole session (session-relative time), unchanged. */
    private fun readingsForDisplay(): List<Double> = if (overviewMode) currentReadings() else currentPage.readings()
    private fun spcForDisplay(readings: List<Double>): Int = if (overviewMode) samplesPerColumn(readings) else 1

    private fun speakValueAt(plotColumn: Int) {
        if (!firstRenderDone) return
        val readings = readingsForDisplay()
        val spc = spcForDisplay(readings)
        val value = LiveGraphRenderer.columnValues(readings, AxisRenderer.PLOT_WIDTH, spc).getOrNull(plotColumn) ?: return
        val time = LiveGraphRenderer.timeSecondsForColumn(readings.size, AxisRenderer.PLOT_WIDTH, plotColumn, spc)
        val suffix = if (spc > 1) ", average of $spc samples" else ""
        manager.announceText("At ${LiveGraphRenderer.formatElapsedTime(time)}, ${value.roundToInt()} millivolts$suffix")
    }

    private fun speakXAxisTick(tapColumn: Int) {
        if (!firstRenderDone) return
        val readings = readingsForDisplay()
        val tickColumn = AxisRenderer.nearestXTickColumn(tapColumn) - AxisRenderer.PLOT_COL_START
        val time = LiveGraphRenderer.timeSecondsForColumn(readings.size, AxisRenderer.PLOT_WIDTH, tickColumn, spcForDisplay(readings))
        manager.announceText(LiveGraphRenderer.formatElapsedTime(time))
    }

    private fun clearScreen() {
        brailleScreen.include(DotsMatrix(Array(screenDimensions.height) { ByteArray(screenDimensions.width) }), 0, 0)
    }

    private fun paint() {
        if (!firstRenderDone) return
        val readings = readingsForDisplay()
        val spc = spcForDisplay(readings)
        if (overviewMode) lastOverviewPaintCount = readings.size
        val bounds = if (overviewMode) null else currentPage.paddedBounds()

        clearScreen()
        val axisPixels = AxisRenderer.render(screenDimensions.width, screenDimensions.height)
        brailleScreen.include(DotsMatrix(axisPixels), 0, 0)
        val linePixels = LiveGraphRenderer.render(readings, AxisRenderer.PLOT_WIDTH, AxisRenderer.PLOT_HEIGHT, spc, bounds)
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
        /** Must not exceed AxisRenderer.PLOT_WIDTH, or columns would start sliding again within a page. */
        private const val PAGE_CAPACITY_SAMPLES = AxisRenderer.PLOT_WIDTH
    }
}
