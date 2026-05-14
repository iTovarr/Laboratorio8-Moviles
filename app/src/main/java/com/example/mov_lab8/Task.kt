package com.example.mov_lab8

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Priority(val color: Color, val icon: ImageVector, val containerColor: Color) {
    LOW(Color(0xFF4CAF50), Icons.Default.KeyboardArrowDown, Color(0xFFE8F5E9)),
    MEDIUM(Color(0xFFFF9800), Icons.Default.Remove, Color(0xFFFFF3E0)),
    HIGH(Color(0xFFF44336), Icons.Default.KeyboardArrowUp, Color(0xFFFFEBEE))
}

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val description: String,
    val isCompleted: Boolean = false,
    val priority: Priority = Priority.LOW,
    val dateLabel: String = "HOY",

    // --- NUEVOS CAMPOS PARA LA CONSIGNA ---

    // Requisito 5: Para poder ordenar por "Fecha de creación"
    val createdAt: Long = System.currentTimeMillis(),

    // Requisito 8: Para marcar si una tarea se repite (Tareas recurrentes)
    val isRecurring: Boolean = false
)