package org.aph.plantwave.graphics

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Procedurally generated leaf icon (~20 wide x 13 tall): a solid tapered
 * outline (widest at the middle row, pointed at top/bottom) with a center
 * vein and four side veins carved into it as grooves (pin-down within the
 * otherwise pin-up leaf body), so the vein pattern is tactilely distinct
 * without needing a hand-authored bitmap. No reference image was
 * available, so this is a deliberately simple design.
 */
object LeafGraphic {
    const val WIDTH = 20
    const val HEIGHT = 13

    private const val PIN_UP: Byte = 1
    private const val PIN_DOWN: Byte = 0
    private const val MAX_HALF_WIDTH = 9

    fun render(): Array<ByteArray> {
        val buffer = Array(HEIGHT) { ByteArray(WIDTH) }
        val centerRow = (HEIGHT - 1) / 2.0
        val centerCol = (WIDTH - 1) / 2.0

        for (y in 0 until HEIGHT) {
            val taper = (1.0 - abs(y - centerRow) / centerRow).coerceAtLeast(0.0)
            val halfWidth = (taper * MAX_HALF_WIDTH).roundToInt()
            val left = (centerCol - halfWidth).roundToInt().coerceIn(0, WIDTH - 1)
            val right = (centerCol + halfWidth).roundToInt().coerceIn(0, WIDTH - 1)
            for (x in left..right) buffer[y][x] = PIN_UP
        }

        val veinCol = centerCol.roundToInt()
        for (y in 1 until HEIGHT - 1) {
            if (buffer[y][veinCol] == PIN_UP) buffer[y][veinCol] = PIN_DOWN
        }

        carveVein(buffer, veinCol, 3, veinCol - 4, 1)
        carveVein(buffer, veinCol, 3, veinCol + 4, 1)
        carveVein(buffer, veinCol, 9, veinCol - 4, 11)
        carveVein(buffer, veinCol, 9, veinCol + 4, 11)

        return buffer
    }

    /** Bresenham line, marking cells PIN_DOWN (a groove) only where the leaf body is already filled. */
    private fun carveVein(buffer: Array<ByteArray>, x0: Int, y0: Int, x1: Int, y1: Int) {
        var x = x0
        var y = y0
        val dx = abs(x1 - x)
        val sx = if (x < x1) 1 else -1
        val dy = -abs(y1 - y)
        val sy = if (y < y1) 1 else -1
        var err = dx + dy

        while (true) {
            if (x in 0 until WIDTH && y in 0 until HEIGHT && buffer[y][x] == PIN_UP) {
                buffer[y][x] = PIN_DOWN
            }
            if (x == x1 && y == y1) break
            val e2 = 2 * err
            if (e2 >= dy) { err += dy; x += sx }
            if (e2 <= dx) { err += dx; y += sy }
        }
    }
}
