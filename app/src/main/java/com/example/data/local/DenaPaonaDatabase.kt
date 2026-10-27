package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        LedgerEntryEntity::class,
        RepaymentEntity::class,
        SystemMetaEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class DenaPaonaDatabase : RoomDatabase() {
    abstract fun ledgerDao(): LedgerDao

    companion object {
        const val DATABASE_NAME = "dena_paona_ledger.db"

        /**
         * Safe non-destructive SQLite schema migration from v1 to v2:
         * Adds nullable `transactionDateEpochMillis` and converts `dueDateEpochMillis` to nullable
         * while preserving all existing rows and repayment history.
         */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ledger_entries_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `counterpartyName` TEXT NOT NULL,
                        `phoneOrReference` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `principalMinor` INTEGER NOT NULL,
                        `repaidMinor` INTEGER NOT NULL,
                        `createdAtEpochMillis` INTEGER NOT NULL,
                        `transactionDateEpochMillis` INTEGER,
                        `dueDateEpochMillis` INTEGER,
                        `updatedAtEpochMillis` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `ledger_entries_new` (
                        `id`, `counterpartyName`, `phoneOrReference`, `description`, `type`,
                        `principalMinor`, `repaidMinor`, `createdAtEpochMillis`,
                        `transactionDateEpochMillis`, `dueDateEpochMillis`, `updatedAtEpochMillis`
                    )
                    SELECT
                        `id`, `counterpartyName`, `phoneOrReference`, `description`, `type`,
                        `principalMinor`, `repaidMinor`, `createdAtEpochMillis`,
                        `createdAtEpochMillis`, `dueDateEpochMillis`, `updatedAtEpochMillis`
                    FROM `ledger_entries`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `ledger_entries`")
                db.execSQL("ALTER TABLE `ledger_entries_new` RENAME TO `ledger_entries`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ledger_entries_type` ON `ledger_entries` (`type`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ledger_entries_counterpartyName` ON `ledger_entries` (`counterpartyName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ledger_entries_dueDateEpochMillis` ON `ledger_entries` (`dueDateEpochMillis`)")
            }
        }

        @Volatile
        private var INSTANCE: DenaPaonaDatabase? = null

        fun getInstance(context: Context): DenaPaonaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DenaPaonaDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
