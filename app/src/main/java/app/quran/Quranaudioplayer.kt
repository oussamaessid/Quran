package app.quran

import android.media.MediaPlayer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AudioPlayerState { IDLE, LOADING, PLAYING, PAUSED, STOPPED, ERROR }

data class AudioPlaybackInfo(
    val state     : AudioPlayerState = AudioPlayerState.IDLE,
    val positionMs: Long             = 0L,
    val durationMs: Long             = 0L,
    val progress  : Float            = 0f
)

class QuranAudioPlayer {

    private val scope       = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var mediaPlayer : MediaPlayer? = null
    private var tickJob     : Job?         = null
    private var currentUrl  : String       = ""

    /** Called when a track finishes naturally — set by ViewModel. */
    var onCompletion: (() -> Unit)? = null

    private val _playbackInfo = MutableStateFlow(AudioPlaybackInfo())
    val playbackInfo: StateFlow<AudioPlaybackInfo> = _playbackInfo.asStateFlow()

    fun play(url: String, startMs: Long = 0L) {
        scope.launch {
            if (url == currentUrl && mediaPlayer != null) {
                resume()
                return@launch
            }

            stopTick()
            emit(AudioPlayerState.LOADING, 0L, 0L)

            withContext(Dispatchers.IO) {
                try {
                    mediaPlayer?.release()
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(url)
                        prepare()
                    }
                    currentUrl = url
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { emit(AudioPlayerState.ERROR, 0L, 0L) }
                    return@withContext
                }

                withContext(Dispatchers.Main) {
                    val mp  = mediaPlayer ?: return@withContext
                    val dur = mp.duration.toLong().coerceAtLeast(0L)

                    mp.setOnCompletionListener {
                        stopTick()
                        emit(AudioPlayerState.STOPPED, dur, dur)
                        onCompletion?.invoke()   // ← notifie le ViewModel
                    }

                    if (startMs > 0) mp.seekTo(startMs.toInt())
                    mp.start()
                    startTick()
                    emit(AudioPlayerState.PLAYING, mp.currentPosition.toLong(), dur)
                }
            }
        }
    }

    fun togglePlayPause() {
        val mp = mediaPlayer ?: return
        if (mp.isPlaying) {
            mp.pause()
            stopTick()
            emit(AudioPlayerState.PAUSED, mp.currentPosition.toLong(), mp.duration.toLong())
        } else {
            resume()
        }
    }

    private fun resume() {
        val mp = mediaPlayer ?: return
        if (!mp.isPlaying) {
            mp.start()
            startTick()
            emit(AudioPlayerState.PLAYING, mp.currentPosition.toLong(), mp.duration.toLong())
        }
    }

    fun seekTo(ms: Long) {
        val mp  = mediaPlayer ?: return
        val dur = mp.duration.toLong().coerceAtLeast(0L)
        val pos = ms.coerceIn(0L, dur)
        mp.seekTo(pos.toInt())
        val state = if (mp.isPlaying) AudioPlayerState.PLAYING else AudioPlayerState.PAUSED
        emit(state, pos, dur)
    }

    fun stop() {
        stopTick()
        mediaPlayer?.let {
            runCatching { if (it.isPlaying) it.stop() }
            runCatching { it.reset() }
        }
        currentUrl = ""
        emit(AudioPlayerState.STOPPED, 0L, 0L)
    }

    fun release() {
        stopTick()
        scope.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun startTick() {
        stopTick()
        tickJob = scope.launch {
            while (isActive) {
                val mp = mediaPlayer ?: break
                if (!mp.isPlaying) break
                val pos = mp.currentPosition.toLong().coerceAtLeast(0L)
                val dur = mp.duration.toLong().coerceAtLeast(0L)
                _playbackInfo.value = AudioPlaybackInfo(
                    state      = AudioPlayerState.PLAYING,
                    positionMs = pos,
                    durationMs = dur,
                    progress   = if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f
                )
                delay(50)
            }
        }
    }

    private fun stopTick() { tickJob?.cancel(); tickJob = null }

    private fun emit(state: AudioPlayerState, posMs: Long, durMs: Long) {
        val progress = if (durMs > 0) (posMs.toFloat() / durMs).coerceIn(0f, 1f) else 0f
        _playbackInfo.value = AudioPlaybackInfo(state, posMs, durMs, progress)
    }
}