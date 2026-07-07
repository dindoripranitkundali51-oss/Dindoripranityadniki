package com.example.dindoripranityadnyiki.core.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database for cached app data.
 * Optimized for offline-first capabilities and data integrity.
 */

@Entity(tableName = "panchang_cache")
data class PanchangEntity(
    @PrimaryKey val date: String,
    val tithi: String,
    val nakshatra: String,
    val agniVas: String,
    val shubhMuhurat: String,
    val description: String,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "booking_cache")
data class BookingEntity(
    @PrimaryKey val id: String,
    val poojaName: String,
    val contactName: String,
    val contactPhone: String,
    val address: String,
    val date: String,
    val status: String,
    val totalAmount: Double,
    val gurujiName: String?,
    val lat: Double,
    val lng: Double,
    val lastSyncAt: Long = System.currentTimeMillis()
)

@Dao
interface DivineDao {
    // Panchang Operations
    @Query("SELECT * FROM panchang_cache WHERE date = :date")
    suspend fun getPanchang(date: String): PanchangEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPanchang(panchang: PanchangEntity)

    @Query("DELETE FROM panchang_cache WHERE date < :thresholdDate")
    suspend fun clearOldPanchang(thresholdDate: String)

    // Booking Operations (Offline Mirror)
    @Query("SELECT * FROM booking_cache ORDER BY date DESC")
    suspend fun getAllBookings(): List<BookingEntity>

    @Query("SELECT * FROM booking_cache WHERE id = :bookingId")
    suspend fun getBookingById(bookingId: String): BookingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookings(bookings: List<BookingEntity>)

    @Query("DELETE FROM booking_cache WHERE id = :bookingId")
    suspend fun deleteBooking(bookingId: String)

    @Query("DELETE FROM booking_cache")
    suspend fun clearAllBookings()
}

@Database(
    entities = [PanchangEntity::class, BookingEntity::class], 
    version = 3,
    exportSchema = false
)
abstract class DivineDatabase : RoomDatabase() {
    abstract fun divineDao(): DivineDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `booking_cache` (
                        `id` TEXT NOT NULL,
                        `poojaName` TEXT NOT NULL,
                        `contactName` TEXT NOT NULL,
                        `contactPhone` TEXT NOT NULL,
                        `address` TEXT NOT NULL,
                        `date` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `totalAmount` REAL NOT NULL,
                        `gurujiName` TEXT,
                        `lat` REAL NOT NULL,
                        `lng` REAL NOT NULL,
                        `lastSyncAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        @Volatile 
        private var instance: DivineDatabase? = null

        fun getInstance(context: Context): DivineDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    DivineDatabase::class.java, 
                    "divine_cache.db"
                )
                .addMigrations(MIGRATION_2_3)
                .fallbackToDestructiveMigrationOnDowngrade() 
                .build()
                .also { instance = it }
            }
        }
    }
}
