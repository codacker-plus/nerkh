package ir.h3xnull.nerkh

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class RefreshWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        try {
            PriceRepo.refresh(applicationContext)
        } catch (e: Exception) {
            // error flag is saved by PriceRepo; the widget shows it
        }
        PriceWidget.updateAll(applicationContext)
        return Result.success()
    }
}
