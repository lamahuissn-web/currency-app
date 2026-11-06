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
    private var systemEnabled = true

    fun init(context: Context) {
        if (pool != null) return
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
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

    /** The phone's Settings › Sound › Touch sounds switch; the app must not shout over it. */
    fun setSystemEnabled(value: Boolean) {
        systemEnabled = value
    }

    fun click() {
        if (!enabled || !systemEnabled || !loaded) return
        // A quiet tick: it should report the touch, not announce the app.
        pool?.play(soundId, 0.55f, 0.55f, 1, 0, 1f)
    }
}
