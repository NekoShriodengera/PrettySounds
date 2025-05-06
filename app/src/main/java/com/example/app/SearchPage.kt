package com.example.app

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView

import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog

import com.example.app.data.AppDatabase
import com.example.app.data.PlaylistRepository
import com.example.app.ui.playlists.PlaylistsViewModel
import com.example.app.ui.playlists.ViewModelFactory

import androidx.recyclerview.widget.LinearLayoutManager

import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.app.models.Sound

class SearchPage : BaseActivity() {

    private val playlistsViewModel: PlaylistsViewModel by viewModels {
        val repository = PlaylistRepository(AppDatabase.getDatabase(this).playlistDao())
        ViewModelFactory(repository)
    }

    private lateinit var soundAdapter: SoundAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_page)

        // We don't have a real playlist here, so just pass -1
        val currentPlaylistId = -1

        soundAdapter = SoundAdapter(
            playlistId = currentPlaylistId,
            onSoundClicked = { sound, _ ->     // we ignore the playlistId
                Log.d("SearchPage", "Play clicked for sound: ${sound.id}")

                val intent = Intent(this, MediaPlayerPage::class.java).apply {
                    putExtra("playlist_id", -1)   // or omit if your page can handle a missing playlist
                    putExtra("sound_id", sound.id)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
            },
            onAddToPlaylistClicked = { sound ->
                showAddToPlaylistDialog(sound)
            }
        )

        // Set up RecyclerView
        val recyclerView: RecyclerView = findViewById(R.id.searchRecyclerView) // Replace with your RecyclerView ID
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = soundAdapter

        // Observing LiveData for all sounds
        playlistsViewModel.allSounds.observe(this, Observer { sounds ->
            soundAdapter.submitList(sounds) // Update your adapter with the latest list of songs
        })

        val searchEditText: EditText = findViewById(R.id.searchEditText)
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {
                // Not necessary for this case
            }

            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                // You can update your search here
            }

            override fun afterTextChanged(editable: Editable?) {
                // Perform the search when the text changes
                performSearch(editable.toString())
            }
        })

    }

    private fun performSearch(query: String) {
        val filteredSounds = playlistsViewModel.allSounds.value?.filter {
            it.name.contains(query, ignoreCase = true)
        } ?: listOf() // Return an empty list if the value is null

        soundAdapter.submitList(filteredSounds) // Update the adapter with filtered sounds
    }
    private fun showAddToPlaylistDialog(selectedSound: Sound) {
        Log.d("AddToPlaylistDialog", "Opening dialog to add sound '${selectedSound.name}' to playlists")

        // Observing _allPlaylists directly
        playlistsViewModel._allPlaylists.observe(this) { allPlaylists ->
            if (allPlaylists != null) {
                Log.d("AddToPlaylistDialog", "Found ${allPlaylists.size} playlists")
            } else {
                Log.d("AddToPlaylistDialog", "No playlists found")
                return@observe
            }

            if (allPlaylists.isEmpty()) {
                Toast.makeText(this, "Немає доступних плейлистів для додавання", Toast.LENGTH_SHORT).show()
                return@observe
            }

            // Log the playlist names for debugging
            Log.d("AddToPlaylistDialog", "Playlist names: ${allPlaylists.map { it.name }}")

            // Extract playlist names and IDs
            val playlistNames = allPlaylists.map { it.name }.toTypedArray()  // Assuming 'name' is a property of Playlist
            val playlistIds = allPlaylists.map { it.id.toString() }.toTypedArray()  // Assuming 'id' is a property of Playlist
            val selectedPlaylists = BooleanArray(playlistNames.size)

            // Show the dialog
            Log.d("AddToPlaylistDialog", "Showing dialog to choose a playlist...")
            AlertDialog.Builder(this)
                .setTitle("Додати '${selectedSound.name}' до плейлисту")
                .setMultiChoiceItems(playlistNames, selectedPlaylists) { _, which, isChecked ->
                    selectedPlaylists[which] = isChecked
                    Log.d("AddToPlaylistDialog", "Playlist ${playlistNames[which]} is ${if (isChecked) "selected" else "deselected"}")
                }
                .setPositiveButton("Додати") { _, _ ->
                    val selectedIndexes = selectedPlaylists
                        .toList()
                        .mapIndexedNotNull { index, isSelected -> if (isSelected) index else null }

                    if (selectedIndexes.isNotEmpty()) {
                        val playlistId = playlistIds[selectedIndexes.first()].toInt()
                        Log.d("AddToPlaylistDialog", "Adding sound '${selectedSound.id}' to playlist $playlistId")

                        playlistsViewModel.addSoundsToPlaylist(
                            listOf(selectedSound.id),
                            playlistId
                        )

                        Toast.makeText(this, "Додано до плейлисту", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.d("AddToPlaylistDialog", "No playlist selected")
                    }
                }

                .setNegativeButton("Скасувати", null)
                .show()
        }
    }
    private fun PlaySong(){

    }
}
class SoundAdapter(
    private val playlistId: Int,
    private val onSoundClicked: (Sound, Int) -> Unit,
    private val onAddToPlaylistClicked: (Sound) -> Unit
) : RecyclerView.Adapter<SoundAdapter.SoundViewHolder>() {

    private var sounds: List<Sound> = emptyList()

    inner class SoundViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val songTitle: TextView    = itemView.findViewById(R.id.sound_name)
        private val soundIcon: ImageView = itemView.findViewById(R.id.sound_icon)

        fun bind(sound: Sound) {
            songTitle.text = sound.name

            // Play button
            soundIcon.setOnClickListener {
                onSoundClicked(sound, playlistId)
            }

            // “Add to playlist” button (only once)
            val existing = itemView.findViewWithTag<ImageButton>("addButton")
            if (existing == null) {
                val addButton = ImageButton(itemView.context).apply {
                    tag = "addButton"
                    setImageResource(android.R.drawable.ic_input_add)
                    setBackgroundResource(android.R.color.transparent)
                    setPadding(8, 8, 8, 8)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    setOnClickListener {
                        onAddToPlaylistClicked(sound)
                    }
                }
                (itemView as ViewGroup).addView(addButton)
            }
        }
    }

    fun submitList(newSounds: List<Sound>) {
        sounds = newSounds
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SoundViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_playlist_sound, parent, false)
        return SoundViewHolder(view)
    }

    override fun onBindViewHolder(holder: SoundViewHolder, position: Int) {
        holder.bind(sounds[position])
    }

    override fun getItemCount(): Int = sounds.size
}


