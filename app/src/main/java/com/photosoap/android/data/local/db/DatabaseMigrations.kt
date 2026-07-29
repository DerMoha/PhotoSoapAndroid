package com.photosoap.android.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `processed_deletions` " +
                "(`requestId` TEXT NOT NULL, `processedAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`requestId`))",
        )
    }
}
