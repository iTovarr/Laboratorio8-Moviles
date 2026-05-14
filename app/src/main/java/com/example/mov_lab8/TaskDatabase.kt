package com.example.mov_lab8

import androidx.room.Database
import androidx.room.RoomDatabase

// CAMBIO: Subimos la versión a 2 porque cambiamos la estructura de la tabla Task
@Database(entities = [Task::class], version = 2)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
}