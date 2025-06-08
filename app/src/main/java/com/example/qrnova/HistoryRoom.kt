package com.example.qrnova

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Entity(tableName = "scanned_qr")
data class ScannedQrEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val content: String,
    val timestamp: Long
)

@Entity(tableName = "created_qr")
data class CreatedQrEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val imageUri: String,  // If you save the image to local storage
    val content: String,
    val timestamp: Long
)

@Dao
interface QrHistoryDao {
    @Insert
    suspend fun insertScanned(qr: ScannedQrEntity)

    @Insert
    suspend fun insertCreated(qr: CreatedQrEntity)

    @Query("SELECT * FROM scanned_qr ORDER BY timestamp DESC")
    fun getAllScanned(): Flow<List<ScannedQrEntity>>

    @Query("SELECT * FROM created_qr ORDER BY timestamp DESC")
    fun getAllCreated(): Flow<List<CreatedQrEntity>>
}

@Database(entities = [ScannedQrEntity::class, CreatedQrEntity::class], version = 1)
abstract class QrDatabase : RoomDatabase() {
    abstract fun qrHistoryDao(): QrHistoryDao

    companion object {
        @Volatile private var INSTANCE: QrDatabase? = null
        fun getInstance(context: Context): QrDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    QrDatabase::class.java,
                    "qr_history.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

class QrRepository(private val dao: QrHistoryDao) {
    suspend fun insertScanned(content: String) {
        dao.insertScanned(
            ScannedQrEntity(content = content, timestamp = System.currentTimeMillis())
        )
    }

    suspend fun insertCreated(content: String, imageUri: String) {
        dao.insertCreated(
            CreatedQrEntity(content = content, imageUri = imageUri, timestamp = System.currentTimeMillis())
        )
    }

    fun getScannedHistory(): Flow<List<ScannedQrEntity>> = dao.getAllScanned()
    fun getCreatedHistory(): Flow<List<CreatedQrEntity>> = dao.getAllCreated()
}

class QrHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val db = QrDatabase.getInstance(application)
    private val repo = QrRepository(db.qrHistoryDao())

    val scannedHistory: StateFlow<List<ScannedQrEntity>> =
        repo.getScannedHistory().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val createdHistory: StateFlow<List<CreatedQrEntity>> =
        repo.getCreatedHistory().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addScanned(content: String) {
        viewModelScope.launch { repo.insertScanned(content) }
    }

    fun addCreated(content: String, imageUri: String) {
        viewModelScope.launch { repo.insertCreated(content, imageUri) }
    }
}
