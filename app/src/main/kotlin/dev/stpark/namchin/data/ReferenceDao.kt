package dev.stpark.namchin.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReferenceDao {
    @Query("SELECT * FROM reference ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ReferenceEntity>>

    @Query("SELECT * FROM reference WHERE id = :id")
    suspend fun get(id: Long): ReferenceEntity?

    @Insert
    suspend fun insert(entity: ReferenceEntity): Long

    @Query("DELETE FROM reference WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM reference WHERE tags IS NULL")
    suspend fun withoutTags(): List<ReferenceEntity>

    @Query("SELECT * FROM reference WHERE embedding IS NULL")
    suspend fun withoutEmbedding(): List<ReferenceEntity>

    @Query("UPDATE reference SET embedding = :embedding WHERE id = :id")
    suspend fun updateEmbedding(id: Long, embedding: ByteArray)

    @Query("UPDATE reference SET tags = :tags, tagsConfirmed = :confirmed WHERE id = :id")
    suspend fun updateTags(id: Long, tags: String, confirmed: Boolean)
}
