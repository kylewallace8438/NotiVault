package com.notivault.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    /** Returns -1 when the row already exists (same dedupHash). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: NotificationEntity): Long

    @Update
    suspend fun updateAll(entities: List<NotificationEntity>)

    @Query(
        """
        SELECT * FROM notifications
        WHERE (:pkg IS NULL OR packageName = :pkg)
          AND (:query IS NULL OR fullText LIKE '%' || :query || '%')
        ORDER BY postedAt DESC
        LIMIT :limit
        """
    )
    fun observe(pkg: String?, query: String?, limit: Int): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE id = :id")
    fun observeById(id: Long): Flow<NotificationEntity?>

    @Query("SELECT * FROM notifications ORDER BY postedAt ASC")
    suspend fun getAll(): List<NotificationEntity>

    @Query(
        """
        SELECT packageName, MAX(appLabel) AS appLabel, COUNT(*) AS count
        FROM notifications GROUP BY packageName ORDER BY count DESC
        """
    )
    fun observeSources(): Flow<List<SourceCount>>

    @Query("SELECT COUNT(*) FROM notifications")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notifications")
    suspend fun count(): Int

    @Query("SELECT * FROM notifications WHERE amount IS NOT NULL ORDER BY postedAt ASC")
    fun observeWithAmount(): Flow<List<NotificationEntity>>

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM notifications")
    suspend fun deleteAll()
}
