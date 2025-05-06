package com.example.app.notesdata

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(viewModel: NoteViewModel) {
    val notes by viewModel.notes.collectAsState()

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<Note?>(null) }


    val gradientBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFFD8B1FF), Color.White)
    )
    val whiteFieldColors = TextFieldDefaults.colors(
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        focusedIndicatorColor = Color.LightGray,
        unfocusedIndicatorColor = Color.LightGray,
        focusedLabelColor = Color.Black,
        unfocusedLabelColor = Color.Black
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = gradientBrush)
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text("Нові нотатки", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))


        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Заголовок") },
            modifier = Modifier.fillMaxWidth(),
            colors = whiteFieldColors
        )
        Spacer(Modifier.height(4.dp))

        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("Текст нотатки") },
            modifier = Modifier.fillMaxWidth(),
            colors = whiteFieldColors
        )
        Spacer(Modifier.height(8.dp))


        Button(onClick = {
            if (isEditing && editingNote != null) {

                viewModel.updateNote(editingNote!!.copy(title = title, content = content))
                isEditing = false
                editingNote = null
            } else if (title.isNotBlank() || content.isNotBlank()) {

                viewModel.addNote(Note(title = title, content = content))
            }

            title = ""
            content = ""
        }) {
            Text(if (isEditing) "Оновити" else "Додати")
        }

        Spacer(Modifier.height(16.dp))
        Text("Список нотаток", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        LazyColumn {
            items(notes.size) { index ->
                val note = notes[index]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.Black
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            thickness = 1.dp,
                            color = Color.LightGray
                        )

                        Text(
                            text = note.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.DarkGray
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {

                            IconButton(onClick = {
                                title = note.title
                                content = note.content
                                editingNote = note
                                isEditing = true
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Редагувати",
                                    tint = Color.Gray
                                )
                            }


                            IconButton(onClick = { viewModel.deleteNote(note) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Видалити",
                                    tint = Color.Red
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}