package com.example.mov_lab8

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.example.mov_lab8.ui.theme.MovLab8Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = Room.databaseBuilder(applicationContext, TaskDatabase::class.java, "task_db")
            .fallbackToDestructiveMigration().build()
        val viewModel = TaskViewModel(db.taskDao())

        enableEdgeToEdge()
        setContent {
            MovLab8Theme {
                Scaffold(
                    floatingActionButton = {
                        // Botón para borrar todo (opcional, para mantener la función)
                        FloatingActionButton(
                            onClick = { viewModel.deleteAllTasks() },
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ) { Icon(Icons.Default.Delete, "Borrar todo") }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        TaskScreen(viewModel)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(viewModel: TaskViewModel) {
    val coroutineScope = rememberCoroutineScope()
    var newTaskDescription by remember { mutableStateOf("") }
    val tasks by viewModel.filteredTasks.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Tareas", style = MaterialTheme.typography.headlineLarge)

        OutlinedTextField(
            value = newTaskDescription,
            onValueChange = { newTaskDescription = it },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            placeholder = { Text("Añadir una tarea") },
            trailingIcon = {
                IconButton(onClick = {
                    if (newTaskDescription.isNotBlank()) {
                        viewModel.addTask(newTaskDescription)
                        newTaskDescription = ""
                    }
                }) { Icon(Icons.Default.Add, null) }
            },
            shape = MaterialTheme.shapes.medium
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            listOf("Todas", "Pendientes", "Completadas").forEach { mode ->
                FilterChip(
                    selected = viewModel.filterMode == mode,
                    onClick = { viewModel.filterMode = mode },
                    label = { Text(mode) }
                )
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(tasks) { task ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = task.isCompleted,
                            onCheckedChange = { viewModel.toggleTaskCompletion(task) }
                        )
                        Text(
                            text = task.description,
                            modifier = Modifier.weight(1f),
                            style = if (task.isCompleted)
                                MaterialTheme.typography.bodyLarge.copy(textDecoration = TextDecoration.LineThrough, color = Color.Gray)
                            else MaterialTheme.typography.bodyLarge
                        )
                        IconButton(onClick = { viewModel.deleteTask(task) }) {
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}