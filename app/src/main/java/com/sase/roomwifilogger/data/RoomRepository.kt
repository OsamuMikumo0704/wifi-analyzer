package com.sase.roomwifilogger.data

import com.sase.roomwifilogger.data.db.RoomDao
import com.sase.roomwifilogger.data.db.RoomEntity
import kotlinx.coroutines.flow.Flow

sealed class RoomNameError(message: String) : IllegalArgumentException(message) {
    data object Empty : RoomNameError("Room name must not be empty.")
    data object Duplicate : RoomNameError("Room name must be unique.")
}

interface RoomRepository {
    fun observeRooms(): Flow<List<RoomEntity>>
    suspend fun addRoom(name: String): Result<Long>
    suspend fun renameRoom(id: Long, name: String): Result<Unit>
    suspend fun deleteRoom(id: Long)
}

class DefaultRoomRepository(
    private val roomDao: RoomDao,
    private val clockMillis: () -> Long = System::currentTimeMillis,
) : RoomRepository {
    override fun observeRooms(): Flow<List<RoomEntity>> = roomDao.observeRooms()

    override suspend fun addRoom(name: String): Result<Long> {
        val trimmedName = name.trim()
        validateName(trimmedName)?.let { return Result.failure(it) }

        return Result.success(
            roomDao.insert(
                RoomEntity(
                    name = trimmedName,
                    createdAt = clockMillis(),
                ),
            ),
        )
    }

    override suspend fun renameRoom(id: Long, name: String): Result<Unit> {
        val trimmedName = name.trim()
        validateName(trimmedName, ignoredRoomId = id)?.let { return Result.failure(it) }

        val room = requireNotNull(roomDao.getById(id)) {
            "Room not found: $id"
        }
        roomDao.update(room.copy(name = trimmedName))
        return Result.success(Unit)
    }

    override suspend fun deleteRoom(id: Long) {
        roomDao.getById(id)?.let { roomDao.delete(it) }
    }

    private suspend fun validateName(
        trimmedName: String,
        ignoredRoomId: Long? = null,
    ): RoomNameError? {
        if (trimmedName.isEmpty()) {
            return RoomNameError.Empty
        }

        val duplicateExists = roomDao.getAll().any { room ->
            room.id != ignoredRoomId && room.name.equals(trimmedName, ignoreCase = true)
        }
        return if (duplicateExists) RoomNameError.Duplicate else null
    }
}
