package ir.h3xnull.nerkh

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Background refresh. Android allows at most one periodic run every 15 minutes. */
object Scheduler {
    private const val PERIODIC = "nerkh_periodic"
    private const val NOW = "nerkh_now"

    fun ensurePeriodic(ctx: Context) {
        val net = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val req = PeriodicWorkRequest.Builder(RefreshWorker::class.java, 15, TimeUnit.MINUTES)
            .setConstraints(net)
            .build()
        WorkManager.getInstance(ctx)
            .enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, req)
    }

    /** Runs right away, even offline (so the widget can show an error instead of waiting). */
    fun refreshNow(ctx: Context) {
        val req = OneTimeWorkRequest.Builder(RefreshWorker::class.java).build()
        WorkManager.getInstance(ctx).enqueueUniqueWork(NOW, ExistingWorkPolicy.REPLACE, req)
    }

    fun cancel(ctx: Context) {
        WorkManager.getInstance(ctx).cancelUniqueWork(PERIODIC)
    }
}
