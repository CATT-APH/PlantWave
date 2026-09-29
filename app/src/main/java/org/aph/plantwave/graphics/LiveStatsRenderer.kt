package org.aph.plantwave.graphics

import com.humanware.keysoftsdk.selfbrailling.aidl.DotsMatrix
import com.humanware.keysoftsdk.translator.Translator
import org.aph.plantwave.monarch_utils.BrailleUtil
import org.aph.plantwave.monarch_utils.Drawing
import org.aph.plantwave.monarch_utils.Position
import kotlin.math.roundToInt

/**
 * Renders the top-right stats box: current reading, elapsed session time,
 * and session min/max (all mV, matching what touch-to-speak announces).
 *
 * Returned as a standalone DotsMatrix -- the caller blits it over the graph
 * after the line (and axes) are drawn. DotsMatrix.include() overwrites its
 * full footprint (blank cells included, not just lit ones), so stamping
 * this on top cleanly clears whatever line segments were underneath rather
 * than overlaying text on a still-visible line -- that's what makes it read
 * as "etched out" of the graph instead of floating on it.
 */
object LiveStatsRenderer {
    const val WIDTH = 36
    const val HEIGHT = 19
    private const val LINE_PITCH = 5

    fun render(currentMv: Double, elapsedSeconds: Double, minMv: Double, maxMv: Double, translator: Translator): DotsMatrix {
        val box = DotsMatrix(Array(HEIGHT) { ByteArray(WIDTH) })

        drawLine("NOW ${currentMv.roundToInt()} MV", box, translator, 0)
        drawLine("TIME ${elapsedSeconds.roundToInt()} S", box, translator, LINE_PITCH)
        drawLine("MIN ${minMv.roundToInt()} MV", box, translator, 2 * LINE_PITCH)
        drawLine("MAX ${maxMv.roundToInt()} MV", box, translator, 3 * LINE_PITCH)

        return box
    }

    private fun drawLine(text: String, box: DotsMatrix, translator: Translator, y: Int) {
        if (y >= HEIGHT) return
        val translated = BrailleUtil.translate(text, translator)
        val maxCells = (WIDTH / 3).coerceAtLeast(0)
        val fitted = if (translated.length > maxCells) translated.take(maxCells) else translated
        Drawing.freeformBraille(fitted, box, Position(0, y))
    }
}
