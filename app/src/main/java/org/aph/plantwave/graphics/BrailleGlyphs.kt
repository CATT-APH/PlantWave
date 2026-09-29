package org.aph.plantwave.graphics

/**
 * Renders text as 6-dot braille pins: each cell is 2 columns x 3 rows at
 * 1 pin per dot, with a 1-column gap between cells -- the same convention
 * the user's own hand-drawn axis labels use (decoded from the sketch: they
 * spell "mv" and "time" in exactly this layout). No Translator/AIDL
 * involved, so none of that service's startup-delay or crash caveats apply.
 *
 * Dot numbering per cell: left column top-to-bottom = 1,2,3; right column
 * = 4,5,6. Digits are UEB: numeric indicator (dots 3456) then a-j patterns.
 */
object BrailleGlyphs {
    private const val PIN_UP: Byte = 1
    const val CELL_WIDTH = 2
    const val CELL_HEIGHT = 3
    const val CELL_GAP = 1
    const val NUMERIC_INDICATOR = '#'

    private val DOTS: Map<Char, Set<Int>> = mapOf(
        'a' to setOf(1), 'b' to setOf(1, 2), 'c' to setOf(1, 4), 'd' to setOf(1, 4, 5), 'e' to setOf(1, 5),
        'f' to setOf(1, 2, 4), 'g' to setOf(1, 2, 4, 5), 'h' to setOf(1, 2, 5), 'i' to setOf(2, 4), 'j' to setOf(2, 4, 5),
        'k' to setOf(1, 3), 'l' to setOf(1, 2, 3), 'm' to setOf(1, 3, 4), 'n' to setOf(1, 3, 4, 5), 'o' to setOf(1, 3, 5),
        'p' to setOf(1, 2, 3, 4), 'q' to setOf(1, 2, 3, 4, 5), 'r' to setOf(1, 2, 3, 5), 's' to setOf(2, 3, 4), 't' to setOf(2, 3, 4, 5),
        'u' to setOf(1, 3, 6), 'v' to setOf(1, 2, 3, 6), 'w' to setOf(2, 4, 5, 6), 'x' to setOf(1, 3, 4, 6), 'y' to setOf(1, 3, 4, 5, 6),
        'z' to setOf(1, 3, 5, 6),
        '1' to setOf(1), '2' to setOf(1, 2), '3' to setOf(1, 4), '4' to setOf(1, 4, 5), '5' to setOf(1, 5),
        '6' to setOf(1, 2, 4), '7' to setOf(1, 2, 4, 5), '8' to setOf(1, 2, 5), '9' to setOf(2, 4), '0' to setOf(2, 4, 5),
        NUMERIC_INDICATOR to setOf(3, 4, 5, 6),
        '-' to setOf(3, 6),
        ' ' to emptySet()
    )

    fun widthOf(text: String): Int =
        if (text.isEmpty()) 0 else text.length * (CELL_WIDTH + CELL_GAP) - CELL_GAP

    /** UEB number: numeric indicator followed by the digits. */
    fun number(value: Int): String = "$NUMERIC_INDICATOR$value"

    /** Pixel buffer exactly [CELL_HEIGHT] tall and [widthOf] wide -- sized to its own footprint, safe to blit as-is. */
    fun render(text: String): Array<ByteArray> {
        val buffer = Array(CELL_HEIGHT) { ByteArray(widthOf(text)) }
        text.forEachIndexed { i, ch ->
            val x = i * (CELL_WIDTH + CELL_GAP)
            for (dot in DOTS[ch.lowercaseChar()] ?: emptySet()) {
                val col = if (dot <= 3) 0 else 1
                val row = (dot - 1) % 3
                buffer[row][x + col] = PIN_UP
            }
        }
        return buffer
    }
}
