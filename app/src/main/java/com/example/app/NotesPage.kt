package com.example.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app.notesdata.NoteDatabase
import com.example.app.notesdata.NoteRepository
import com.example.app.notesdata.NotesScreen
import com.example.app.notesdata.NoteViewModel
import com.example.app.notesdata.NoteViewModelFactory

class NotesPage : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notes_page)
        val dao = NoteDatabase.getDatabase(this).noteDao()
        val repository = NoteRepository(dao)
        val factory = NoteViewModelFactory(repository)

        setContent {
            val viewModel: NoteViewModel = viewModel(factory = factory)
            NotesScreen(viewModel = viewModel)
        }

    }

}
