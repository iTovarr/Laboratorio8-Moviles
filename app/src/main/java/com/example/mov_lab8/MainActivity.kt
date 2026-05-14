package com.example.mov_lab8

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
        setContent { MovLab8Theme { TaskScreen(viewModel) } }
    }
}

@Composable
fun TaskScreen(viewModel: TaskViewModel) {
    val context = LocalContext.current // NECESARIO PARA EL TOAST
    val tasks by viewModel.tasks.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }

    Scaffold(
        containerColor = Color(0xFFF5F9FF),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showAddDialog = true
                    // REQUISITO 6: NOTIFICACIÓN VISUAL (TOAST)
                    Toast.makeText(context, "🔔 Recordatorio: ¡Revisa tus tareas pendientes!", Toast.LENGTH_SHORT).show()
                    Log.d("NOTIFICACION", "Evento de recordatorio disparado")
                },
                containerColor = Color(0xFFFF8A80),
                shape = CircleShape
            ) { Icon(Icons.Default.Add, null, tint = Color.White) }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            SearchBar(
                query = viewModel.searchQuery,
                onQueryChange = { viewModel.searchQuery = it }
            )

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChips(viewModel.filterMode) { viewModel.filterMode = it }
                Spacer(Modifier.weight(1f))

                IconButton(onClick = {
                    viewModel.orderMode = when(viewModel.orderMode) {
                        "Fecha" -> "Nombre"
                        "Nombre" -> "Estado"
                        else -> "Fecha"
                    }
                    Toast.makeText(context, "Ordenado por: ${viewModel.orderMode}", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.Sort, null, tint = Color.Gray)
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                item { HeaderSummary(viewModel) }

                val groupedTasks = tasks.groupBy { it.dateLabel }
                groupedTasks.forEach { (label, list) ->
                    item { SectionHeader(label) }
                    items(list, key = { it.id }) { task ->
                        ProfessionalTaskCard(
                            task = task,
                            onToggle = { viewModel.updateTask(task.copy(isCompleted = !task.isCompleted)) },
                            onDelete = {
                                viewModel.deleteTask(task)
                                Toast.makeText(context, "Tarea eliminada", Toast.LENGTH_SHORT).show()
                            },
                            onEdit = { taskToEdit = task }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog || taskToEdit != null) {
        ProfessionalTaskDialog(
            task = taskToEdit,
            onDismiss = { showAddDialog = false; taskToEdit = null },
            onConfirm = { desc, priority, label ->
                if (taskToEdit != null) {
                    viewModel.updateTask(taskToEdit!!.copy(description = desc, priority = priority, dateLabel = label))
                    Toast.makeText(context, "Tarea actualizada", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.addTask(desc, priority, label)
                    Toast.makeText(context, "Tarea guardada y sincronizada", Toast.LENGTH_SHORT).show()
                }
                showAddDialog = false; taskToEdit = null
            }
        )
    }
}

// --- COMPONENTES ---

@Composable
fun ProfessionalTaskCard(task: Task, onToggle: () -> Unit, onDelete: () -> Unit, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onEdit() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(26.dp).clip(CircleShape)
                    .background(if (task.isCompleted) Color(0xFF81C784) else Color.Transparent)
                    .border(2.dp, if (task.isCompleted) Color(0xFF81C784) else Color(0xFFE0E0E0), CircleShape)
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                if (task.isCompleted) Icon(Icons.Default.Check, null, Modifier.size(18.dp), Color.White)
            }

            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    color = if (task.isCompleted) Color.LightGray else Color.Black
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Prioridad: ${task.priority.name}", style = MaterialTheme.typography.labelSmall, color = task.priority.color)

                    // REQUISITO 8: ICONO DE RECURRENCIA VISIBLE
                    if (task.dateLabel == "SEMANA") {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.Refresh, null, Modifier.size(12.dp), Color.Gray)
                        Text(" Recurrente", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, null, tint = Color.LightGray)
            }
        }
    }
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(25.dp)),
        placeholder = { Text("Buscar tarea...") },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, null)
                }
            }
        },
        shape = RoundedCornerShape(25.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFFFF8A80),
            unfocusedBorderColor = Color.LightGray
        )
    )
}

@Composable
fun FilterChips(selectedFilter: String, onFilterSelected: (String) -> Unit) {
    val filters = listOf("Todas", "Pendientes", "Completadas")
    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        filters.forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter) }
            )
        }
    }
}

@Composable
fun HeaderSummary(viewModel: TaskViewModel) {
    val hoy by viewModel.statsHoy.collectAsState()
    val mañana by viewModel.statsMañana.collectAsState()
    val semana by viewModel.statsSemana.collectAsState()

    // Contenedor principal de los contadores
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ProgressCircle(hoy, "HOY", Color(0xFFF06292))
            ProgressCircle(mañana, "MAÑANA", Color(0xFF4FC3F7))
            ProgressCircle(semana, "SEMANA", Color(0xFF9575CD))
        }
    }
}

@Composable
fun ProgressCircle(progress: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(54.dp)
                .border(3.dp, color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = progress,
                fontWeight = FontWeight.Bold,
                color = color,
                style = MaterialTheme.typography.titleMedium
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        title, Modifier.fillMaxWidth().padding(vertical = 8.dp)
            .background(Color(0xFFE3F2FD), RoundedCornerShape(4.dp)).padding(8.dp),
        fontWeight = FontWeight.Bold, color = Color.DarkGray
    )
}

@Composable
fun ProfessionalTaskDialog(task: Task?, onDismiss: () -> Unit, onConfirm: (String, Priority, String) -> Unit) {
    var desc by remember { mutableStateOf(task?.description ?: "") }
    var selectedPriority by remember { mutableStateOf(task?.priority ?: Priority.LOW) }
    var selectedLabel by remember { mutableStateOf(task?.dateLabel ?: "HOY") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (task == null) "Nueva Tarea" else "Editar Tarea") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Descripción") })
                Text("Categoría / Frecuencia:")
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    listOf("HOY", "MAÑANA", "SEMANA").forEach { label ->
                        FilterChip(selected = selectedLabel == label, onClick = { selectedLabel = label }, label = { Text(label) })
                        Spacer(Modifier.width(4.dp))
                    }
                }
                Text("Prioridad:")
                Row {
                    Priority.entries.forEach { p ->
                        IconButton(onClick = { selectedPriority = p }) {
                            Icon(p.icon, null, tint = if(selectedPriority == p) p.color else Color.Gray)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (desc.isNotBlank()) onConfirm(desc, selectedPriority, selectedLabel) }) {
                Text("Guardar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}