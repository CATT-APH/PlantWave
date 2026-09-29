package org.aph.plantwave.graphics

import com.humanware.keysoftsdk.selfbrailling.aidl.DotsMatrix
import com.humanware.keysoftsdk.translator.Translator
import org.aph.plantwave.monarch_utils.BrailleUtil
import org.aph.plantwave.monarch_utils.Drawing
import org.aph.plantwave.monarch_utils.PIN_UP
import org.aph.plantwave.monarch_utils.Position

/** Renders the Loading screen: an animated three-dot indicator + a braille countdown message. */
object LoadingRenderer {
    private const val DOT_SIZE = 3
    private const val DOT_GAP = 4
    private const val DOTS_Y = 15
    private const val TEXT_Y = 28

    /** Animation cycles through: dot1 -> dot1+2 -> dot1+2+3 -> all clear -> repeat. */
    const val FRAME_COUNT = 4

    fun render(frame: Int, secondsRemaining: Int, screen: DotsMatrix, translator: Translator, width: Int) {
        drawDots(frame, screen, width)
        val message = BrailleUtil.translate("LOADING $secondsRemaining S", translator)
        Drawing.freeformBraille(message, screen, Position(0, TEXT_Y), center = true)
    }

    private fun drawDots(frame: Int, screen: DotsMatrix, width: Int) {
        val totalWidth = DOT_SIZE * 3 + DOT_GAP * 2
        val startX = (width - totalWidth) / 2
        val filledDot = DotsMatrix(Array(DOT_SIZE) { ByteArray(DOT_SIZE) { PIN_UP } })

        val filledCount = when (frame % FRAME_COUNT) {
            0 -> 1
            1 -> 2
            2 -> 3
            else -> 0
        }
        for (i in 0 until filledCount) {
            screen.include(filledDot, startX + i * (DOT_SIZE + DOT_GAP), DOTS_Y)
        }
    }
}
