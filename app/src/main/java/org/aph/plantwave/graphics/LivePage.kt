package org.aph.plantwave.graphics

/**
 * One page of Live view: a fixed-capacity, never-sliding list of readings
 * plus a running min/max that only ever expands. Feeding LiveGraphRenderer
 * a list like this (instead of a sliding window over the whole session)
 * means already-drawn columns never get remapped to a different value or
 * row on a later tick -- only the newest column changes, which is what
 * keeps physical pin churn low on the real device. See the paging plan for
 * the full rationale.
 *
 * No screen real estate is spent showing the running min/max directly --
 * instead [append] reports when a reading is a genuine new session high or
 * low (past [NOISE_FLOOR_MV], so ordinary jitter at the edge doesn't keep
 * re-triggering it), so the caller can announce it out loud. That gives
 * audible, real-time context for exactly the thing Overview mode otherwise
 * only reveals after the fact: that a spike happened at all.
 */
class LivePage(
    private val capacity: Int,
    seedMinMv: Double? = null,
    seedMaxMv: Double? = null
) {
    enum class BoundsChange { NONE, NEW_MAX, NEW_MIN }

    private val values = ArrayList<Double>(capacity)
    private var minMv: Double? = seedMinMv
    private var maxMv: Double? = seedMaxMv

    val size: Int get() = values.size
    val isFull: Boolean get() = values.size >= capacity

    fun readings(): List<Double> = values

    /** Unpadded running min/max, or null if nothing's been seeded or appended yet. */
    fun rawBounds(): Pair<Double, Double>? {
        val min = minMv ?: return null
        val max = maxMv ?: return null
        return min to max
    }

    fun paddedBounds(): Pair<Double, Double> {
        val (min, max) = rawBounds() ?: (0.0 to 0.0)
        return LiveGraphRenderer.paddedBounds(min, max)
    }

    /**
     * Appends a reading. A brand-new, unseeded page's very first sample just
     * establishes the baseline (nothing "new" to announce yet); after that,
     * a reading only counts as a new high/low if it clears the existing
     * bound by more than [NOISE_FLOOR_MV].
     */
    fun append(valueMv: Double): BoundsChange {
        values += valueMv
        val priorMin = minMv
        val priorMax = maxMv

        if (priorMin == null && priorMax == null) {
            minMv = valueMv
            maxMv = valueMv
            return BoundsChange.NONE
        }

        var change = BoundsChange.NONE
        if (priorMax == null || valueMv > priorMax + NOISE_FLOOR_MV) {
            maxMv = valueMv
            change = BoundsChange.NEW_MAX
        }
        if (priorMin == null || valueMv < priorMin - NOISE_FLOOR_MV) {
            minMv = valueMv
            if (change == BoundsChange.NONE) change = BoundsChange.NEW_MIN
        }
        return change
    }

    companion object {
        private const val NOISE_FLOOR_MV = 20.0
    }
}
