package dev.stpark.namchin.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

@Database(entities = [ReferenceEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun referenceDao(): ReferenceDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        /** v2: 장면 임베딩과 태그. 기존 레퍼런스는 그대로 두고 열만 추가한다. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE `reference` ADD COLUMN `embedding` BLOB")
                connection.execSQL("ALTER TABLE `reference` ADD COLUMN `tags` TEXT")
                connection.execSQL("ALTER TABLE `reference` ADD COLUMN `tagsConfirmed` INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "namchin.db")
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { instance = it }
        }
    }
}
