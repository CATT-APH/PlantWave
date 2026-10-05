package org.aph.plantwave.graphics

import kotlin.math.abs

/**
 * Y-axis (left, col 6) and X-axis (bottom, row 33) spines, the two static
 * axis-name labels, and 3 tick marks on the X axis only (oldest/mid/newest
 * visible time) -- Y-axis ticks were removed as redundant/cluttered once
 * the axis-name label sat right next to them (there's also no fixed mV
 * scale to label since LiveGraphRenderer auto-ranges). Plot interior is
 * rows 1-32, cols 7-95: the full width, with nothing overlaid on it.
 *
 * The strip below the X axis (rows 35-38) is the info row: the live mV
 * reading (drawn by LiveGraphActivity via BrailleGlyphs at READING_COL/ROW
 * = col 42/row 35) sits where the "time" label originally was, and "time"
 * ended up at row 36/col 3 after a couple of rounds of nudging (see the
 * inline comment by X_AXIS_LABEL_COL/ROW below for the exact history). An
 * earlier bottom-right sidebar box was dropped -- it clipped the newest
 * ~12s of low readings to frame a number that fits in 14 pins.
 *
 * X-axis ticks are single-pin bumps, silent by default -- LiveGraphActivity
 * speaks a tapped tick's value via announceText. The two axis-name labels
 * (Y_AXIS_LABEL, X_AXIS_LABEL) are exact bitmaps transcribed from the
 * sketch (they decode as braille "mv" and "time"), not rendered text -- so
 * this still needs no Translator.
 */
object AxisRenderer {
    private const val PIN_UP: Byte = 1

    const val Y_AXIS_COL = 6
    const val X_AXIS_ROW = 33
    const val PLOT_COL_START = 7
    const val PLOT_COL_END = 95
    const val PLOT_ROW_START = 1
    const val PLOT_ROW_END = 32
    const val PLOT_WIDTH = PLOT_COL_END - PLOT_COL_START + 1
    const val PLOT_HEIGHT = PLOT_ROW_END - PLOT_ROW_START + 1

    private const val X_TICK_ROW = X_AXIS_ROW + 1

    /** Where the live reading's braille starts on the info row -- the "time" label's original slot. */
    const val READING_COL = 42
    const val READING_ROW = 35

    // Axis-name labels, transcribed exactly (verbatim, not redesigned) from the
    // user's sketch -- the sparse dot clusters at rows 14-16 (left of the Y
    // spine) and rows 36-38 (below the X spine) were the axis names all along,
    // not tick-mark placeholders. Both regions are outside the plot's row/col
    // range (cols 0-4 is left of PLOT_COL_START; rows 36-38 is below
    // PLOT_ROW_END), so the line never draws there -- no overlap to worry about.
    // "time"'s column moved three times: right to col 83 (to free its
    // sketched col 42 slot for the live reading), then back left to col 0,
    // then nudged to its current col 3/row 36 per a later "1 row down, 3
    // columns right" request -- reading stays at col 42, so the two don't
    // sit adjacent, just both on the info row.
    private const val Y_AXIS_LABEL_COL = 0
    private const val Y_AXIS_LABEL_ROW = 14
    private val Y_AXIS_LABEL: Array<ByteArray> = arrayOf(
        byteArrayOf(1, 1, 0, 1, 0),
        byteArrayOf(0, 0, 0, 1, 0),
        byteArrayOf(1, 0, 0, 1, 1)
    )

    private const val X_AXIS_LABEL_COL = 3
    private const val X_AXIS_LABEL_ROW = 36
    private val X_AXIS_LABEL: Array<ByteArray> = arrayOf(
        byteArrayOf(0, 1, 0, 0, 1, 0, 1, 1, 0, 1, 0),
        byteArrayOf(1, 1, 0, 1, 0, 0, 0, 0, 0, 0, 1),
        byteArrayOf(1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0)
    )

    /** Absolute screen columns for the 3 X ticks: oldest visible, mid, newest ("now"). */
    fun xTickColumns(): List<Int> = listOf(
        PLOT_COL_START,
        PLOT_COL_START + (PLOT_WIDTH - 1) / 2,
        PLOT_COL_START + PLOT_WIDTH - 1
    )

    /** Full-screen-sized pixel buffer: axis spines + tick bumps, ready to include() under the plotted line. */
    fun render(width: Int, height: Int): Array<ByteArray> {
        val buffer = Array(height) { ByteArray(width) }

        for (row in PLOT_ROW_START..X_AXIS_ROW) {
            if (row < height) buffer[row][Y_AXIS_COL] = PIN_UP
        }
        for (col in Y_AXIS_COL..PLOT_COL_END) {
            if (col < width) buffer[X_AXIS_ROW][col] = PIN_UP
        }
        for (col in xTickColumns()) {
            if (col < width) buffer[X_TICK_ROW][col] = PIN_UP
        }
        stampLabel(buffer, Y_AXIS_LABEL, Y_AXIS_LABEL_COL, Y_AXIS_LABEL_ROW)
        stampLabel(buffer, X_AXIS_LABEL, X_AXIS_LABEL_COL, X_AXIS_LABEL_ROW)
        return buffer
    }

    private fun stampLabel(buffer: Array<ByteArray>, label: Array<ByteArray>, col: Int, row: Int) {
        for (r in label.indices) {
            for (c in label[r].indices) {
                if (label[r][c] == PIN_UP && row + r < buffer.size && col + c < buffer[0].size) {
                    buffer[row + r][col + c] = PIN_UP
                }
            }
        }
    }

    /** Nearest X tick column (absolute) to a raw tap column -- always returns a column, snaps to closest. */
    fun nearestXTickColumn(tapCol: Int): Int = xTickColumns().minBy { abs(it - tapCol) }
}
