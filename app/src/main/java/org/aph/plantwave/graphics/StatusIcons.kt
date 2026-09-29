package org.aph.plantwave.graphics

/** Hand-designed 6x6 solid raised-pin icons for the Arduino connection status indicator. */
object StatusIcons {
    const val SIZE = 6

    /** Checkmark (connected). */
    fun checkmark(): Array<ByteArray> = arrayOf(
        byteArrayOf(0, 0, 0, 0, 0, 1),
        byteArrayOf(0, 0, 0, 0, 1, 1),
        byteArrayOf(0, 0, 0, 1, 1, 0),
        byteArrayOf(1, 0, 1, 1, 0, 0),
        byteArrayOf(1, 1, 1, 0, 0, 0),
        byteArrayOf(0, 1, 0, 0, 0, 0),
    )

    /** X (not connected). */
    fun cross(): Array<ByteArray> = arrayOf(
        byteArrayOf(1, 1, 0, 0, 1, 1),
        byteArrayOf(0, 1, 1, 1, 1, 0),
        byteArrayOf(0, 0, 1, 1, 0, 0),
        byteArrayOf(0, 0, 1, 1, 0, 0),
        byteArrayOf(0, 1, 1, 1, 1, 0),
        byteArrayOf(1, 1, 0, 0, 1, 1),
    )
}
