package com.bepngon.merge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.bepngon.merge.sound.SoundManager
import com.bepngon.merge.ui.GameScreen
import com.bepngon.merge.ui.theme.BepNgonTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SoundManager.init(this)
        enableEdgeToEdge()
        setContent {
            BepNgonTheme {
                GameScreen()
            }
        }
        enterImmersiveMode()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterImmersiveMode()
    }

    /** Sticky immersive "game mode": bars hidden, edge swipes show them
     *  transiently — so a drag starting at the screen edge doesn't trigger
     *  the system back/home gesture on the first swipe. */
    private fun enterImmersiveMode() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) SoundManager.release()
    }
}
