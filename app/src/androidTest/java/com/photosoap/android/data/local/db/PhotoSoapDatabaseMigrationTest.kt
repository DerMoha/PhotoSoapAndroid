package com.photosoap.android.data.local.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhotoSoapDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        PhotoSoapDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate1To2PreservesStatsAndAddsDeletionLedger() {
        helper.createDatabase(TEST_DATABASE, 1).apply {
            execSQL(
                "INSERT INTO user_stats (id, totalReviewed, totalDeleted, totalKept, " +
                    "storageFreed, sessionReviewCount, currentStreak, bestStreak, dayStreak, " +
                    "lastReviewDate, todayReviewCount, todayDate, bestDayReviewCount, " +
                    "dailyChallengeProgress, dailyChallengeTarget, dailyChallengeType, " +
                    "dailyChallengeDate) VALUES (1, 7, 2, 5, 4096, 0, 0, 0, 0, NULL, " +
                    "0, NULL, 0, 0, 0, 'review', NULL)",
            )
            close()
        }

        helper.runMigrationsAndValidate(TEST_DATABASE, 2, true, MIGRATION_1_2).use { db ->
            db.query("SELECT totalReviewed, totalDeleted FROM user_stats WHERE id = 1").use {
                check(it.moveToFirst())
                assertEquals(7, it.getInt(0))
                assertEquals(2, it.getInt(1))
            }
            db.execSQL(
                "INSERT INTO processed_deletions (requestId, processedAt) " +
                    "VALUES ('request-1', 1)",
            )
            db.query("SELECT COUNT(*) FROM processed_deletions").use {
                check(it.moveToFirst())
                assertEquals(1, it.getInt(0))
            }
        }
    }

    @Test
    fun migrate2To3PreservesTotalsAndStartsMediaBreakdownAtZero() {
        helper.createDatabase("media-migration-test", 2).apply {
            execSQL("INSERT INTO user_stats (id, totalReviewed, totalDeleted, totalKept, storageFreed, sessionReviewCount, currentStreak, bestStreak, dayStreak, todayReviewCount, bestDayReviewCount, dailyChallengeProgress, dailyChallengeTarget, dailyChallengeType) VALUES (1, 7, 2, 5, 4096, 0, 0, 0, 0, 0, 0, 0, 0, 'review')")
            close()
        }
        helper.runMigrationsAndValidate("media-migration-test", 3, true, MIGRATION_2_3).use { db ->
            db.query("SELECT totalReviewed, storageFreed, photosReviewed, videosReviewed, photoStorageFreed, videoStorageFreed FROM user_stats WHERE id = 1").use {
                check(it.moveToFirst())
                assertEquals(7, it.getInt(0))
                assertEquals(4096L, it.getLong(1))
                for (column in 2..5) assertEquals(0L, it.getLong(column))
            }
        }
    }

    private companion object {
        const val TEST_DATABASE = "migration-test"
    }
}
