package com.bepngon.merge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) SoundManager.release()
    }
}
