package org.aph.plantwave.activities

import android.os.Bundle
import android.util.Size
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.humanware.keysoftsdk.selfbrailling.SelfBraillingManager
import com.humanware.keysoftsdk.selfbrailling.aidl.DotsMatrix
import com.humanware.keysoftsdk.selfbrailling.widget.SelfBraillingWidget
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.aph.plantwave.graphics.LineGraphRenderer

/**
 * Layer 2: renders the Layer 1 sensor buffer as a tactile line graph on the
 * Monarch's Braille display.
 *
 * A SelfBraillingWidget must occupy the entire screen with no other UI
 * elements (per the KeySoftSDK demo README), so this is a separate
 * full-screen activity rather than part of PlantWaveActivity's debug UI.
 * Launched via an explicit intent carrying the buffered values -- see
 * PlantWaveActivity.handleIncomingBytes().
 */
class GraphActivity : AppCompatActivity() {

    private lateinit var manager: SelfBraillingManager
    private lateinit var widget: SelfBraillingWidget
    private lateinit var screenDimensions: Size
    private lateinit var brailleScreen: DotsMatrix
    private val mutableLiveDots = MutableLiveData<Array<ByteArray>>()
    private val mutableViewedImage = MutableLiveData<Array<ByteArray>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        manager = SelfBraillingManager(this).apply { bindService() }
        widget = SelfBraillingWidget(this).also { setContentView(it) }
        supportActionBar?.hide()

        mutableViewedImage.observe(this) { widget.refresh(it) }
        mutableLiveDots.observe(this) { manager.displayDots(it) }

        screenDimensions = Size(manager.brailleDisplayDotsSizeX, manager.brailleDisplayDotsSizeY)
        brailleScreen = DotsMatrix(screenDimensions.height, screenDimensions.width)

        val values = intent.getIntArrayExtra(EXTRA_VALUES)?.toList().orEmpty()

        // SelfBraillingManager's AIDL service takes a moment to bind; calling into it
        // before that finishes hangs the main thread for ~5s and gets killed by the
        // system (observed and documented in MonarchBlocks' DEVELOPMENT_LOG.txt).
        lifecycleScope.launch { delay(1000) }.invokeOnCompletion { renderGraph(values) }
    }

    override fun onResume() {
        super.onResume()
        manager.bindService()
    }

    override fun onStop() {
        super.onStop()
        manager.unbindService()
    }

    private fun renderGraph(values: List<Int>) {
        val pixels = LineGraphRenderer.render(values, screenDimensions.width, screenDimensions.height)
        brailleScreen.include(DotsMatrix(pixels), 0, 0)
        refreshScreen()
    }

    private fun refreshScreen() {
        mutableViewedImage.value = brailleScreen.matrix
        mutableLiveDots.value = brailleScreen.matrix
    }

    companion object {
        const val EXTRA_VALUES = "org.aph.plantwave.EXTRA_VALUES"
    }
}
