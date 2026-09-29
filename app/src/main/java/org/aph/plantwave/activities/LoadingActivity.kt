package org.aph.plantwave.activities

import android.os.Bundle
import android.util.Size
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.humanware.keysoftsdk.selfbrailling.SelfBraillingManager
import com.humanware.keysoftsdk.selfbrailling.aidl.DotsMatrix
import com.humanware.keysoftsdk.selfbrailling.widget.SelfBraillingWidget
import com.humanware.keysoftsdk.translator.Translator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.aph.plantwave.graphics.LoadingRenderer

/**
 * Page 2: a passive ~30s countdown screen shown while the Arduino reads the
 * plant sensor, matching its fixed 30s read window. Purely a waiting
 * animation -- no USB/serial code, no knowledge of GraphActivity. Finishes
 * itself when its own timer reaches zero; the actual DONE-driven navigation
 * to GraphActivity happens independently in PlantWaveActivity (which keeps
 * running in the background, same pattern GraphActivity itself already
 * uses). Whichever happens first, the back stack ends up correct either
 * way -- finishing a non-top activity is a silent no-op to what's visible.
 */
class LoadingActivity : AppCompatActivity() {

    private lateinit var manager: SelfBraillingManager
    private lateinit var widget: SelfBraillingWidget
    private lateinit var translator: Translator
    private lateinit var screenDimensions: Size
    private lateinit var brailleScreen: DotsMatrix
    private val mutableLiveDots = MutableLiveData<Array<ByteArray>>()
    private val mutableViewedImage = MutableLiveData<Array<ByteArray>>()

    private var areServicesBound = false
    private var firstRenderDone = false
    private var secondsRemaining = DURATION_SECONDS
    private var animationFrame = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        areServicesBound = true

        manager = SelfBraillingManager(this).apply { bindService() }
        widget = SelfBraillingWidget(this).also { setContentView(it) }
        supportActionBar?.hide()

        mutableViewedImage.observe(this) { widget.refresh(it) }
        mutableLiveDots.observe(this) { manager.displayDots(it) }
        screenDimensions = Size(manager.brailleDisplayDotsSizeX, manager.brailleDisplayDotsSizeY)
        brailleScreen = DotsMatrix(screenDimensions.height, screenDimensions.width)

        translator = Translator(this).apply { bindService() }

        widget.onSelfBraillingWidgetListener = object : SelfBraillingWidget.OnSelfBraillingWidgetListener {
            override fun onFocused() = paint()
            override fun onSingleTapAtBraillePosition(p0: Int, p1: Int) {}
            override fun onDoubleTapAtBraillePosition(p0: Int, p1: Int) {}
        }

        lifecycleScope.launch { delay(1000) }.invokeOnCompletion {
            firstRenderDone = true
            paint()
            startTimers()
        }
    }

    override fun onResume() {
        super.onResume()
        manager.bindService()
        if (!areServicesBound) {
            areServicesBound = true
            translator.bindService()
            lifecycleScope.launch { delay(1000) }.invokeOnCompletion {
                firstRenderDone = true
                paint()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (areServicesBound) {
            areServicesBound = false
            firstRenderDone = false
            manager.unbindService()
            translator.unbindService()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (areServicesBound) {
            areServicesBound = false
            manager.unbindService()
            translator.unbindService()
        }
    }

    /** Two independent loops: a fast one for the dot animation, a 1s one for the countdown/finish. */
    private fun startTimers() {
        lifecycleScope.launch {
            while (true) {
                delay(ANIMATION_FRAME_MS)
                animationFrame++
                paint()
            }
        }
        lifecycleScope.launch {
            while (secondsRemaining > 0) {
                delay(1000)
                secondsRemaining--
                paint()
            }
            finish()
        }
    }

    private fun paint() {
        if (!firstRenderDone) return
        clearScreen()
        LoadingRenderer.render(animationFrame, secondsRemaining, brailleScreen, translator, screenDimensions.width)
        mutableViewedImage.value = brailleScreen.matrix
        mutableLiveDots.value = brailleScreen.matrix
    }

    private fun clearScreen() {
        brailleScreen.include(DotsMatrix(Array(screenDimensions.height) { ByteArray(screenDimensions.width) }), 0, 0)
    }

    companion object {
        private const val DURATION_SECONDS = 30
        private const val ANIMATION_FRAME_MS = 400L
    }
}
