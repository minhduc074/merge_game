package com.bepngon.merge.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.annotation.RawRes
import com.bepngon.merge.R

object SoundManager {
    enum class Sfx(@RawRes val res: Int) {
        Spawn(R.raw.spawn),
        Tap(R.raw.tap),
        Merge(R.raw.merge),
        Sell(R.raw.sell),
        Deliver(R.raw.deliver),
        Complete(R.raw.complete),
        LevelUp(R.raw.levelup),
        Error(R.raw.error),
        Coin(R.raw.coin),
        Achieve(R.raw.achieve),
        Unlock(R.raw.unlock),
        Daily(R.raw.daily),
    }

    var enabled = true

    private var pool: SoundPool? = null
    private val ids = mutableMapOf<Sfx, Int>()
    private var loaded = 0

    fun init(context: Context) {
        if (pool != null) return
        pool = SoundPool.Builder()
            .setMaxStreams(6)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .build()
            .also { p ->
                p.setOnLoadCompleteListener { _, _, status -> if (status == 0) loaded++ }
            }
        Sfx.entries.forEach { sfx ->
            ids[sfx] = pool!!.load(context.applicationContext, sfx.res, 1)
        }
    }

    fun play(sfx: Sfx) {
        if (!enabled) return
        val id = ids[sfx] ?: return
        pool?.play(id, 0.7f, 0.7f, 1, 0, 1f)
    }

    fun release() {
        pool?.release()
        pool = null
        ids.clear()
    }
}
