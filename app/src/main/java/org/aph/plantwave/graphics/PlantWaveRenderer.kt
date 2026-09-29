package org.aph.plantwave.graphics

import com.humanware.keysoftsdk.selfbrailling.aidl.DotsMatrix

/**
 * Renders the Leaf Sense Home screen: your hand-authored static art
 * (HomeScreenArt.BACKGROUND) plus one conditional region -- the "ENTER"
 * label -- blitted on top only when the Arduino is connected. Everything
 * else in the art never changes regardless of app state.
 */
object PlantWaveRenderer {
    enum class Action { START }

    private const val ENTER_REGION_WIDTH = 14
    private const val ENTER_REGION_HEIGHT = 3

    /** Touch target is exactly the ENTER label's own footprint, not the whole screen. */
    fun resolveAction(px: Int, py: Int): Action? {
        val inRegion = px in HomeScreenArt.ENTER_BUTTON_X until (HomeScreenArt.ENTER_BUTTON_X + ENTER_REGION_WIDTH) &&
            py in HomeScreenArt.ENTER_BUTTON_Y until (HomeScreenArt.ENTER_BUTTON_Y + ENTER_REGION_HEIGHT)
        return if (inRegion) Action.START else null
    }

    fun render(connected: Boolean, screen: DotsMatrix) {
        screen.include(DotsMatrix(HomeScreenArt.BACKGROUND), 0, 0)
        if (connected) {
            screen.include(DotsMatrix(HomeScreenArt.ENTER_BUTTON), HomeScreenArt.ENTER_BUTTON_X, HomeScreenArt.ENTER_BUTTON_Y)
        }
    }
}
