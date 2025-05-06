package com.example.app

import android.content.Intent
import android.graphics.Color
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.app.data.AppDatabase
import com.example.app.data.PlaylistRepository
import com.example.app.models.Playlist
import com.example.app.ui.playlists.PlaylistsViewModel
import com.example.app.ui.playlists.ViewModelFactory
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.app.models.Sound

class MediaPlayerPage : AppCompatActivity() {

    private var playlist: Playlist? =  null
    private var currentPosition = 0
    var currentSound: Sound? = null
    private var currentSongId: Int = -1
    private var currentPositionBeforePause: Int = 0
    private var isMediaPlaying  = false
    private lateinit var playPauseButton: ImageButton
    private var currentSongIndex: Int = 0
    private var mediaPlayer: MediaPlayer? = null
    private var soundMap = emptyMap<String, Sound>()
    private lateinit var seekBar: SeekBar
    private var userSeeking = false
    private val handler = Handler(Looper.getMainLooper())
    private enum class RepeatMode { NONE, REPEAT_ONE, LOOP }
    private var repeatMode = RepeatMode.NONE
    private var randomMode = false
    var isLiked = false

    private val playlistsViewModel: PlaylistsViewModel by viewModels {
        val repository = PlaylistRepository(AppDatabase.getDatabase(this).playlistDao())
        ViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mediaplayer_page)

        // —– view lookups —–
        seekBar       = findViewById(R.id.seekBar)
        val btnLike: ImageButton        = findViewById(R.id.btnLike)
        val circleView: View            = findViewById(R.id.CirclView)
        val tvSongPlaylist: TextView    = findViewById(R.id.tvSongPlaylist)
        val tvPlaylistName: TextView    = findViewById(R.id.tvPlaylistName)
        val btnRepeat: ImageButton      = findViewById(R.id.btnRepeat)
        val btnRandom: ImageButton      = findViewById(R.id.btnShuffle)
        val btnPlayPause: ImageButton   = findViewById(R.id.btnPlayPause)
        val btnNext: ImageButton        = findViewById(R.id.btnNext)
        val btnPrev: ImageButton        = findViewById(R.id.btnPrev)

        // Random‐color your circle
        DrawableCompat.setTint(circleView.background, getRandomColor())

        // Pull intent extras
        val soundId   = intent.getStringExtra("sound_id") ?: ""
        val playlistId= intent.getIntExtra("playlist_id", -1)

        // Launch a coroutine to load data and start playback
        lifecycleScope.launch {
            if (playlistId != -1) {
                // — Case A: real playlist —
                playlist = playlistsViewModel.getPlaylistById(playlistId)
                val sounds = playlistsViewModel.getSoundsForPlaylist(playlistId)
                soundMap = sounds.associateBy { it.id }

                // Update UI
                tvPlaylistName.text = playlist?.name ?: "Unknown"
                tvSongPlaylist  .text = currentSound?.name ?: "Unknown"

            } else {
                // — Case B: only one sound —
                val single = playlistsViewModel.getSoundByID(soundId)
                if (single != null) {
                    soundMap = mapOf(single.id to single)
                } else {
                    // nothing found
                    soundMap = emptyMap()
                }

                // Update UI
                tvPlaylistName.text = "Unknown"
                tvSongPlaylist  .text = single?.name ?: "Unknown"
            }

            // In both cases: start playing the tapped sound
            playCurrentByIndex(soundId)
        }

        // SeekBar logic (unchanged)…
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(sb: SeekBar) { userSeeking = true }
            override fun onStopTrackingTouch(sb: SeekBar) {
                userSeeking = false
                mediaPlayer?.seekTo(sb.progress)
            }
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {}
        })

        // Play/Pause toggle
        btnPlayPause.setOnClickListener {
            if (isMediaPlaying) {
                pauseSong()
                btnPlayPause.setImageResource(R.drawable.playicon)
            } else {
                // resume at current position
                val cs = soundMap[playlist?.soundIds?.getOrNull(currentSongIndex)]
                playSong(cs)
                btnPlayPause.setImageResource(R.drawable.pauseicon)
            }
        }

        // Next / Prev
        btnNext.setOnClickListener     { nextSong()     }
        btnPrev.setOnClickListener     { previousSong() }

        // Repeat
        btnRepeat.setOnClickListener {
            repeatMode = when (repeatMode) {
                RepeatMode.NONE       -> RepeatMode.REPEAT_ONE
                RepeatMode.REPEAT_ONE -> RepeatMode.LOOP
                RepeatMode.LOOP       -> RepeatMode.NONE
            }
            btnRepeat.setImageResource(
                when (repeatMode) {
                    RepeatMode.NONE       -> R.drawable.norepeaticon
                    RepeatMode.REPEAT_ONE -> R.drawable.repeatonetimeicon
                    RepeatMode.LOOP       -> R.drawable.repeaticon
                }
            )
        }

        // Shuffle
        btnRandom.setOnClickListener {
            randomMode = !randomMode
            btnRandom.setImageResource(
                if (randomMode) R.drawable.mixplaylisticon
                else              R.drawable.nomixicon
            )
        }

        // Like/unlike
        btnLike.setOnClickListener {
            currentSound?.let { cs ->
                val newIcon = if (cs.isFavorite) {
                    playlistsViewModel.toggleFavoriteStatus(cs)
                    R.drawable.unliked
                } else {
                    playlistsViewModel.toggleFavoriteStatus(cs)
                    R.drawable.liked
                }
                btnLike.setImageResource(newIcon)
            }
        }
    }

    private fun playCurrentByIndex(jumpToId: String? = null) {
        // Build a list of IDs to page through:
        val soundIdsList = playlist?.soundIds ?: soundMap.keys.toList()

        // If they gave us a soundId, find its index in the list:
        jumpToId?.let { id ->
            val idx = soundIdsList.indexOf(id)
            if (idx != -1) currentSongIndex = idx
        }

        // Nothing to play?
        if (soundIdsList.isEmpty()) return

        // Clamp
        if (currentSongIndex !in soundIdsList.indices) currentSongIndex = 0

        // Look up and play
        val id = soundIdsList[currentSongIndex]
        playSong(soundMap[id])
    }

    private fun nextSong() {
        val soundIdsList = playlist?.soundIds ?: soundMap.keys.toList()
        if (soundIdsList.isEmpty()) return

        currentSongIndex = (currentSongIndex + 1) % soundIdsList.size
        playCurrentByIndex()
    }

    // Go to the previous track (or wrap around)
    private fun previousSong() {
        val soundIdsList = playlist?.soundIds ?: soundMap.keys.toList()
        if (soundIdsList.isEmpty()) return

        currentSongIndex = if (currentSongIndex > 0) currentSongIndex - 1
        else soundIdsList.size - 1
        playCurrentByIndex()
    }

    private fun updateSeekPosition() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying && !userSeeking) {
                        seekBar.progress = mp.currentPosition
                    }
                    handler.postDelayed(this, 500)
                }
            }
        }, 0)
    }
    private fun getRandomColor(): Int {
        val randomRed = (Math.random() * 128 + 128).toInt()  // Random red value from 128 to 255
        val randomGreen = (Math.random() * 128 + 128).toInt() // Random green value from 128 to 255
        val randomBlue = (Math.random() * 128 + 128).toInt()  // Random blue value from 128 to 255

        return Color.argb(255, randomRed, randomGreen, randomBlue)
    }
    private fun playSong(sound: Sound?) {
        if (sound == null) return

        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setAudioStreamType(AudioManager.STREAM_MUSIC)
            val uri = Uri.parse(sound.filePath)
            setDataSource(this@MediaPlayerPage, uri)
            prepare()
            start()
            setOnCompletionListener {
                when {
                    randomMode               -> playRandomSong()
                    repeatMode == RepeatMode.REPEAT_ONE -> playCurrentByIndex()
                    repeatMode == RepeatMode.LOOP       -> nextSong()
                    else                                -> isMediaPlaying = false
                }
            }
            // seekbar max
            seekBar.max = duration
        }

        // UI updates
        updateSeekPosition()
        updateLikeButton(sound)
        updateSongInfo(sound)
        isMediaPlaying = true
    }

    private fun updateLikeButton(sound: Sound) {
        if (sound != null) {
            val btnLike: ImageButton = findViewById(R.id.btnLike)
            // Check the favorite status of the current song and update the button
            if (sound!!.isFavorite) {
                btnLike.setImageResource(R.drawable.liked)  // Set liked icon
            } else {
                btnLike.setImageResource(R.drawable.unliked)  // Set unliked icon
            }
        }
    }
    private fun playRandomSong() {
        playlist?.let { pl ->
            if (pl.soundIds.isEmpty()) return
            currentSongIndex = pl.soundIds.indices.random()
            playCurrentByIndex()
        }
    }

    private fun pauseSong() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                // save the current position
                currentPosition = player.currentPosition
                player.pause()
                isMediaPlaying = false
            }
        }
    }



    private fun updateSongInfo(sound: Sound) {
        // Title
        val songName = sound.name ?: "Unknown"
        findViewById<TextView>(R.id.tvSongTitle).text = "Now Playing: $songName"

        // Playlist name (or Unknown)
        val playlistName = playlist?.name ?: "Unknown"
        findViewById<TextView>(R.id.tvPlaylistName).text    = playlistName
        findViewById<TextView>(R.id.tvSongPlaylist).text    = playlistName

        // Randomize your circle
        setRandomColorToCircleView(findViewById(R.id.CirclView))
    }

    fun setRandomColorToCircleView(circleView: View) {
        val randomColor = getRandomColor()
        val drawable = circleView.background
        DrawableCompat.setTint(drawable, randomColor)
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)                  // update the intent
        handleIntentPlayback(intent)       // custom method to re‑read extras & start the media
    }
    private fun handleIntentPlayback(intent: Intent) {
        val playlistId = intent.getIntExtra("playlist_id", -1)
        val soundId = intent.getStringExtra("sound_id")
        if (playlistId != -1 && soundId != null) {
            // 1) stop any current playback
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null

            // 2) load the new playlist/song by ID
            lifecycleScope.launch {
                // load playlist and sound objects from ViewModel/DB,
                // then call your playSong(sound) method
                val playlist = playlistsViewModel.getPlaylistById(playlistId)
                val sound = playlistsViewModel.getSoundByID(soundId)
                if (sound != null) {
                    playSong(sound)
                    updateSongInfo(sound)
                }
            }
        }
    }
}