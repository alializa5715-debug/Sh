package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.entity.MemoryEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class MemoryRepository(context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val dao = db.memoryDao()

    val allMemories: Flow<List<MemoryEntity>> = dao.getAllMemories()

    suspend fun addMemory(fact: String, category: String = "Preference") {
        if (fact.isBlank()) return
        val memory = MemoryEntity(
            id = UUID.randomUUID().toString(),
            fact = fact.trim(),
            category = category,
            createdAt = System.currentTimeMillis(),
            isEnabled = true
        )
        dao.insertMemory(memory)
    }

    suspend fun toggleMemory(id: String, isEnabled: Boolean) {
        val list = dao.getEnabledMemories()
        val found = list.find { it.id == id }
        if (found != null) {
            dao.updateMemory(found.copy(isEnabled = isEnabled))
        }
    }

    suspend fun deleteMemory(id: String) {
        dao.deleteMemory(id)
    }

    suspend fun clearAll() {
        dao.clearAllMemories()
    }

    suspend fun getMemoryContext(): String {
        val enabled = dao.getEnabledMemories()
        if (enabled.isEmpty()) return ""
        return enabled.joinToString("\n") { "- [${it.category}] ${it.fact}" }
    }
}
