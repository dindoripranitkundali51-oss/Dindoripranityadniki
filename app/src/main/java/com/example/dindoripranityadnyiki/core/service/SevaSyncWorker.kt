package com.example.dindoripranityadnyiki.core.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Placeholder offline sync worker.
 * Firebase dependencies removed.
 */
class SevaSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return Result.success()
    }
}
