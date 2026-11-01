package com.familyrecipes.android.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.familyrecipes.android.FamilyRecipesApp

/** Account/server-scoped durable records: drafts, last search, chat and outbox. */
@Entity(tableName = "records", primaryKeys = ["account", "kind", "key"])
data class LocalRecord(val account: String, val kind: String, val key: String, val json: String)
@Dao
interface RecordDao {
    @Query("SELECT * FROM records WHERE account=:account AND kind=:kind ORDER BY rowid")
    suspend fun list(account: String, kind: String): List<LocalRecord>
    @Query("SELECT * FROM records WHERE account=:account AND kind=:kind ORDER BY rowid")
    fun observe(account: String, kind: String): Flow<List<LocalRecord>>
    @Query("SELECT json FROM records WHERE account=:account AND kind=:kind AND `key`=:key")
    suspend fun get(account: String, kind: String, key: String): String?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun put(record: LocalRecord)
    @Query("DELETE FROM records WHERE account=:account AND kind=:kind AND `key`=:key")
    suspend fun delete(account: String, kind: String, key: String)
}
@Database(entities = [LocalRecord::class], version = 1, exportSchema = false)
abstract class LocalStore : RoomDatabase() {
    abstract fun records(): RecordDao
    companion object {
        val instance: LocalStore by lazy {
            Room.databaseBuilder(FamilyRecipesApp.instance, LocalStore::class.java, "local-state.db").build()
        }
        val account: String get() = "${PreferenceManager.baseUrl}|${PreferenceManager.userId}"
    }
}
