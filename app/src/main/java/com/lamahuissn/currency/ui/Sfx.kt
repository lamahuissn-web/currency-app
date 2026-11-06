package com.lamahuissn.currency.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.lamahuissn.currency.R

/**
 * One short tick for every deliberate touch — a key, a row, a tab, a chip, a swipe action.
 *
 * It is a SoundPool over a 35 ms asset rather than a `ToneGenerator`, because a system beep is
 * harsh and cannot carry a custom character. The pool is built once, keeps at most three voices so
 * fast typing does not stack into noise, and stays silent when the phone's own touch-sound setting
 * is off or the owner turned sound off in the app's menu.
 */
object Sfx {

    private var pool: SoundPool? = null
    private var soundId = 0
    private var loaded = false
    private var enabled = true

    fun init(context: Context) {
        if (pool != null) return
        // MEDIA, not ASSISTANCE_SONIFICATION: the sonification usage is muted by Android whenever
        // the phone's own "Touch sounds" switch is off (the default on most phones), which made the
        // app silent. A media-stream tick is audible on its own and still respects media volume.
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val created = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(attributes)
            .build()
        created.setOnLoadCompleteListener { _, _, status -> loaded = status == 0 }
        soundId = created.load(context.applicationContext, R.raw.tap, 1)
        pool = created
    }

    /** The owner's own switch, from the list menu. */
    fun setEnabled(value: Boolean) {
        enabled = value
    }

    fun click() {
        if (!enabled) return
        // A quiet tick: it should report the touch, not announce the app.
        pool?.play(soundId, 0.6f, 0.6f, 1, 0, 1f)
    }
}
