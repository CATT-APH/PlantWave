package org.aph.plantwave.graphics

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt
import org.aph.plantwave.LiveSession

/**
 * Rasterizes the live millivolt stream, auto-ranging the vertical scale to
 * whatever's actually on screen rather than a fixed assumed band.
 *
 * At [samplesPerColumn] = 1 (live view) this is a scrolling strip chart: the
 * most recent up to [width] readings, one per column, oldest falling off
 * the left -- so touch-to-time mapping stays stable rather than constantly
 * rescaling as a session grows. At samplesPerColumn > 1 (overview) each
 * column is the average of a chunk of consecutive readings, so a whole
 * session fits in [width] columns; see [samplesPerColumnToFit].
 *
 * The Y scale used to be a fixed 1700-2400 mV window. That assumed a
 * specific voltage-divider output range; in practice the sensor circuit's
 * actual output depends on the resistor/wiring in use (observed ~4200 mV
 * with the current 1M-ohm resistor) and keeps shifting as the hardware is
 * tuned, so a fixed window just pinned real readings at the top or bottom
 * of the screen instead of showing their variation. Scaling to the visible
 * window's own min/max (see [scaleBounds]) stays correct regardless of
 * what voltage range the circuit happens to produce.
 */
object LiveGraphRenderer {
    private const val PIN_UP: Byte = 1

    /** Minimum vertical span to scale against, in mV -- keeps a near-flat
     * signal from being blown up into visual noise by tiny fluctuation. */
    private const val MIN_SPAN_MV = 50.0

    fun windowStartIndex(totalCount: Int, width: Int, samplesPerColumn: Int = 1): Int =
        (totalCount - width * samplesPerColumn).coerceAtLeast(0)

    /** Smallest samples-per-column that fits the entire session into [width] columns. */
    fun samplesPerColumnToFit(totalCount: Int, width: Int): Int =
        ceil(totalCount / width.toDouble()).toInt().coerceAtLeast(1)

    /**
     * One value per drawn column, in column order: the raw sample at 1x, the
     * chunk average otherwise. Fewer than [width] entries early in a session.
     * The newest column may legitimately average a partial (smaller) chunk --
     * that's the still-filling right edge, not a bug.
     */
    fun columnValues(readingsMv: List<Double>, width: Int, samplesPerColumn: Int = 1): List<Double> {
        val start = windowStartIndex(readingsMv.size, width, samplesPerColumn)
        val values = ArrayList<Double>(width)
        for (col in 0 until width) {
            val chunkStart = start + col * samplesPerColumn
            if (chunkStart >= readingsMv.size) break
            val chunkEnd = (chunkStart + samplesPerColumn).coerceAtMost(readingsMv.size)
            values += readingsMv.subList(chunkStart, chunkEnd).average()
        }
        return values
    }

    /**
     * [bounds] lets a caller (LiveGraphActivity's paged Live view) freeze the
     * Y scale across repaints instead of recomputing it from the window every
     * time -- otherwise already-drawn columns would keep getting remapped to
     * slightly different rows as new samples arrive, even when the window
     * itself isn't sliding. Overview mode passes null and keeps today's
     * recompute-every-time behavior, since it doesn't track a running bound.
     */
    fun render(
        readingsMv: List<Double>, width: Int, height: Int, samplesPerColumn: Int = 1,
        bounds: Pair<Double, Double>? = null
    ): Array<ByteArray> {
        val buffer = Array(height) { ByteArray(width) }
        if (readingsMv.isEmpty()) return buffer

        val values = columnValues(readingsMv, width, samplesPerColumn)
        val (minMv, maxMv) = bounds ?: scaleBounds(values)
        val points = values.mapIndexed { i, mv -> i to scaleY(mv, height, minMv, maxMv) }

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

    /**
     * Elapsed session time for an on-screen column: the midpoint of the
     * sample indices that column represents. At samplesPerColumn = 1 that
     * is exactly the sample's own index, so live view is unchanged.
     */
    fun timeSecondsForColumn(totalCount: Int, width: Int, column: Int, samplesPerColumn: Int = 1): Double {
        val chunkStart = windowStartIndex(totalCount, width, samplesPerColumn) + column * samplesPerColumn
        val chunkEnd = (chunkStart + samplesPerColumn).coerceAtMost(totalCount)
        val midIndex = (chunkStart + (chunkEnd - 1).coerceAtLeast(chunkStart)) / 2.0
        return midIndex * LiveSession.SAMPLE_INTERVAL_MS / 1000.0
    }

    /**
     * Spoken elapsed time in whichever unit reads most naturally: seconds
     * under a minute, minutes under an hour, hours beyond that. With
     * LiveSession's current 1000-point buffer cap (~8.3 min max session)
     * only seconds/minutes are reachable today; hours is ready for when
     * the buffer cap grows.
     */
    fun formatElapsedTime(totalSeconds: Double): String = when {
        totalSeconds < 60 -> "%.1f seconds".format(totalSeconds)
        totalSeconds < 3600 -> "%.1f minutes".format(totalSeconds / 60)
        else -> "%.1f hours".format(totalSeconds / 3600)
    }

    /** The visible window's own min/max, padded out to [MIN_SPAN_MV] if narrower than that. */
    private fun scaleBounds(values: List<Double>): Pair<Double, Double> = paddedBounds(values.min(), values.max())

    /** Pads an externally-tracked min/max (e.g. LivePage's running bounds) out to [MIN_SPAN_MV] if narrower. */
    fun paddedBounds(minMv: Double, maxMv: Double): Pair<Double, Double> {
        val span = maxMv - minMv
        if (span >= MIN_SPAN_MV) return minMv to maxMv
        val pad = (MIN_SPAN_MV - span) / 2.0
        return (minMv - pad) to (maxMv + pad)
    }

    /** Scale into [0, height-1] against [minMv]/[maxMv]. Higher mV -> higher on screen (lower row index). */
    private fun scaleY(mv: Double, height: Int, minMv: Double, maxMv: Double): Int {
        val normalized = (mv.coerceIn(minMv, maxMv) - minMv) / (maxMv - minMv)
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
