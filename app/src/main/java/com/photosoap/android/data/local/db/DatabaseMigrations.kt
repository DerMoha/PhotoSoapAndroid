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

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_stats ADD COLUMN photosReviewed INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE user_stats ADD COLUMN photosKept INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE user_stats ADD COLUMN photosDeleted INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE user_stats ADD COLUMN photoStorageFreed INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE user_stats ADD COLUMN videosReviewed INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE user_stats ADD COLUMN videosKept INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE user_stats ADD COLUMN videosDeleted INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE user_stats ADD COLUMN videoStorageFreed INTEGER NOT NULL DEFAULT 0")
    }
}
