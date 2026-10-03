package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.util.Log
import com.example.model.AlertTone

object TonePlayer {
    private const val TAG = "TonePlayer"
    private var mediaPlayer: MediaPlayer? = null
    var currentlyPlayingToneId: String? = null
        private set

    fun playTone(context: Context, tone: AlertTone, onCompletion: (() -> Unit)? = null) {
        stopTone()

        try {
            val player = if (tone.rawResId != null) {
                MediaPlayer.create(context.applicationContext, tone.rawResId)
            } else {
                // System default notification ringtone
                val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                            .build()
                    )
                    setDataSource(context.applicationContext, defaultUri)
                    prepare()
                }
            }

            if (player != null) {
                mediaPlayer = player
                currentlyPlayingToneId = tone.id
                player.setOnCompletionListener {
                    stopTone()
                    onCompletion?.invoke()
                }
                player.start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing tone ${tone.id}: ${e.message}")
            stopTone()
            onCompletion?.invoke()
        }
    }

    fun stopTone() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media player: ${e.message}")
        } finally {
            mediaPlayer = null
            currentlyPlayingToneId = null
        }
    }

    fun isPlaying(tone: AlertTone): Boolean {
        return currentlyPlayingToneId == tone.id
    }
}
