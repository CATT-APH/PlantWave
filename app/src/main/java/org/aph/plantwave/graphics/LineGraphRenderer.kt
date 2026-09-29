package org.aph.plantwave.graphics

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Rasterizes a buffer of sensor readings into a tactile line graph.
 *
 * Pure and SDK-independent by design: takes plain values and dimensions,
 * returns a plain pixel buffer (row-major, [row][col], 0 = pin down, 1 =
 * pin up) -- the caller wraps it as a DotsMatrix. No line-drawing primitive
 * exists in the KeySoftSDK or either Monarch reference project, so this is
 * a from-scratch Bresenham rasterizer.
 */
object LineGraphRenderer {
    private const val PIN_UP: Byte = 1

    /**
     * @param values sensor readings in chronological order
     * @param width target pixel width (columns)
     * @param height target pixel height (rows)
     * @return a [height][width] pixel buffer with the rasterized line
     */
    fun render(values: List<Int>, width: Int, height: Int): Array<ByteArray> {
        val buffer = Array(height) { ByteArray(width) }
        if (values.isEmpty()) return buffer

        val min = values.min()
        val max = values.max()
        val points = values.mapIndexed { index, value ->
            val x = if (values.size == 1) width / 2 else (index * (width - 1)) / (values.size - 1)
            x to scaleY(value, min, max, height)
        }

        if (points.size == 1) {
            val (x, y) = points[0]
            if (x in 0 until width && y in 0 until height) buffer[y][x] = PIN_UP
        } else {
            for (i in 0 until points.size - 1) {
                drawSegment(buffer, points[i], points[i + 1], width, height)
            }
        }
        return buffer
    }

    /** Autoscales value into [0, height-1]. Higher value -> higher on screen (lower row index). */
    private fun scaleY(value: Int, min: Int, max: Int, height: Int): Int {
        if (max == min) return height / 2
        val normalized = (value - min).toDouble() / (max - min)
        return ((height - 1) * (1.0 - normalized)).roundToInt().coerceIn(0, height - 1)
    }

    /** Bresenham line algorithm, with bounds guards since DotsMatrix.include() does none. */
    private fun drawSegment(
        buffer: Array<ByteArray>, p0: Pair<Int, Int>, p1: Pair<Int, Int>, width: Int, height: Int
    ) {
        var x0 = p0.first
        var y0 = p0.second
        val dx = abs(p1.first - x0)
        val sx = if (x0 < p1.first) 1 else -1
        val dy = -abs(p1.second - y0)
        val sy = if (y0 < p1.second) 1 else -1
        var err = dx + dy

        while (true) {
            if (x0 in 0 until width && y0 in 0 until height) buffer[y0][x0] = PIN_UP
            if (x0 == p1.first && y0 == p1.second) break
            val e2 = 2 * err
            if (e2 >= dy) { err += dy; x0 += sx }
            if (e2 <= dx) { err += dx; y0 += sy }
        }
    }
}
