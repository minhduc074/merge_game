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
        Merge1(R.raw.merge1),
        Merge2(R.raw.merge2),
        Merge3(R.raw.merge3),
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
        play(sfx, 1f)
    }

    /** [rate] = playback rate 0.5–2.0 (SoundPool pitch shift). */
    fun play(sfx: Sfx, rate: Float) {
        if (!enabled) return
        val id = ids[sfx] ?: return
        pool?.play(id, 0.7f, 0.7f, 1, 0, rate.coerceIn(0.5f, 2f))
    }

    private val mergeVariants = listOf(Sfx.Merge1, Sfx.Merge2, Sfx.Merge3)

    fun playMerge() = play(mergeVariants.random())

    /** Block-puzzle clear: pitch climbs with lines + combo (dopamine ramp). */
    fun playMergePitch(lines: Int, combo: Int) {
        val rate = 1f + (lines - 1) * 0.12f + combo * 0.05f
        play(mergeVariants.random(), rate)
    }

    fun release() {
        pool?.release()
        pool = null
        ids.clear()
    }
}
