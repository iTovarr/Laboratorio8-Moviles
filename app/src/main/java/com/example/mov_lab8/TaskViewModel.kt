package com.example.mov_lab8

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TaskViewModel(private val dao: TaskDao) : ViewModel() {

    // SOLUCIÓN AL "INPUT INVISIBLE": Usamos estado de Compose directo
    // Esto hace que el TextField se actualice al instante mientras escribes
    var searchQuery by mutableStateOf("")

    private val _filterMode = MutableStateFlow("Todas")
    private val _orderMode = MutableStateFlow("Fecha")

    var filterMode: String
        get() = _filterMode.value
        set(value) { _filterMode.value = value }

    var orderMode: String
        get() = _orderMode.value
        set(value) { _orderMode.value = value }

    // Flujo principal: combinamos el snapshotFlow de la búsqueda con los demás flujos
    val tasks = combine(
        dao.getAllTasks(),
        snapshotFlow { searchQuery }, // Convierte el estado de la barra de búsqueda en un flujo
        _filterMode,
        _orderMode
    ) { list, query, filter, order ->

        // 1. Buscamos (Punto 4)
        var result = if (query.isEmpty()) list
        else list.filter { it.description.contains(query, ignoreCase = true) }

        // 2. Filtramos (Punto 3)
        result = when (filter) {
            "Pendientes" -> result.filter { !it.isCompleted }
            "Completadas" -> result.filter { it.isCompleted }
            "Alta Prioridad" -> result.filter { it.priority == Priority.HIGH }
            else -> result
        }

        // 3. Ordenamos (Punto 5)
        when (order) {
            "Nombre" -> result.sortedBy { it.description }
            "Estado" -> result.sortedBy { it.isCompleted }
            "Fecha" -> result.sortedByDescending { it.createdAt }
            else -> result
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- Estadísticas en tiempo real (Punto 9) ---
    val statsHoy = tasks.map { list ->
        val hoy = list.filter { it.dateLabel.uppercase() == "HOY" }
        if (hoy.isEmpty()) "0/0" else "${hoy.count { it.isCompleted }}/${hoy.size}"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "0/0")

    val statsMañana = tasks.map { list ->
        val mañana = list.filter { it.dateLabel.uppercase() == "MAÑANA" }
        if (mañana.isEmpty()) "0/0" else "${mañana.count { it.isCompleted }}/${mañana.size}"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "0/0")

    val statsSemana = tasks.map { list ->
        val semana = list.filter { it.dateLabel.uppercase() == "SEMANA" }
        if (semana.isEmpty()) "0/0" else "${semana.count { it.isCompleted }}/${semana.size}"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "0/0")

    // --- Funciones de Base de Datos ---

    fun addTask(desc: String, priority: Priority, label: String) {
        viewModelScope.launch {
            val newTask = Task(description = desc, priority = priority, dateLabel = label)
            dao.insertTask(newTask)
            Log.d("CLOUD_SYNC", "Sincronizando nueva tarea: ${newTask.description}")
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            dao.updateTask(task)
            Log.d("CLOUD_SYNC", "Actualizando cambios en la nube para ID: ${task.id}")
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            dao.deleteTask(task)
            Log.d("CLOUD_SYNC", "Eliminando tarea de la nube...")
        }
    }
}