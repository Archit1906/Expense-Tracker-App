package com.example.espensestracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Transaction::class, UnparsedSms::class, MerchantRule::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun unparsedSmsDao(): UnparsedSmsDao
    abstract fun merchantRuleDao(): MerchantRuleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `unparsed_sms` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sender` TEXT NOT NULL, " +
                        "`body` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `amount` REAL NOT NULL, " +
                        "`account` TEXT, `refNo` TEXT, `reason` TEXT NOT NULL, " +
                        "`smsHash` TEXT NOT NULL, `dismissed` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_unparsed_sms_smsHash` ON `unparsed_sms` (`smsHash`)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `merchantKey` TEXT")
                db.execSQL("ALTER TABLE `unparsed_sms` ADD COLUMN `merchantKey` TEXT")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `merchant_rules` (`merchantKey` TEXT NOT NULL, " +
                        "`category` TEXT NOT NULL, `displayName` TEXT, PRIMARY KEY(`merchantKey`))"
                )
            }
        }

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "expenses.db")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build().also { INSTANCE = it }
            }
    }
}
