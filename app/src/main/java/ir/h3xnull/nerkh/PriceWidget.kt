package ir.h3xnull.nerkh

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class PriceWidget : AppWidgetProvider() {

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        render(ctx, mgr, ids, loading = false)
        Scheduler.ensurePeriodic(ctx)
        // Throttle: avoids a refresh loop and needless requests.
        if (System.currentTimeMillis() - PriceRepo.fetchedAt(ctx) > 2 * 60_000L) {
            Scheduler.refreshNow(ctx)
        }
    }

    override fun onEnabled(ctx: Context) {
        Scheduler.ensurePeriodic(ctx)
    }

    override fun onDisabled(ctx: Context) {
        Scheduler.cancel(ctx)
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        if (intent.action == ACTION_REFRESH) {
            val mgr = AppWidgetManager.getInstance(ctx)
            render(ctx, mgr, ids(ctx, mgr), loading = true)
            Scheduler.refreshNow(ctx)
        }
    }

    companion object {
        const val ACTION_REFRESH = "ir.h3xnull.nerkh.REFRESH"

        private val NAME = intArrayOf(R.id.n1, R.id.n2, R.id.n3, R.id.n4, R.id.n5, R.id.n6)
        private val PRICE = intArrayOf(R.id.p1, R.id.p2, R.id.p3, R.id.p4, R.id.p5, R.id.p6)
        private val CHANGE = intArrayOf(R.id.c1, R.id.c2, R.id.c3, R.id.c4, R.id.c5, R.id.c6)

        private fun ids(ctx: Context, mgr: AppWidgetManager): IntArray =
            mgr.getAppWidgetIds(ComponentName(ctx, PriceWidget::class.java))

        fun updateAll(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            render(ctx, mgr, ids(ctx, mgr), loading = false)
        }

        private fun render(ctx: Context, mgr: AppWidgetManager, ids: IntArray, loading: Boolean) {
            if (ids.isEmpty()) return
            val quotes = PriceRepo.load(ctx, PriceRepo.WIDGET_ITEMS)
            val status = if (loading) "در حال بروزرسانی…" else PriceRepo.statusText(ctx)

            val refreshPi = PendingIntent.getBroadcast(
                ctx, 1,
                Intent(ctx, PriceWidget::class.java).setAction(ACTION_REFRESH),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val openPi = PendingIntent.getActivity(
                ctx, 2,
                Intent(ctx, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            for (id in ids) {
                val v = RemoteViews(ctx.packageName, R.layout.widget)
                PriceRepo.WIDGET_ITEMS.forEachIndexed { i, item ->
                    val q = quotes[i]
                    v.setTextViewText(NAME[i], item.title)
                    v.setTextViewText(PRICE[i], if (q != null) PriceRepo.priceText(q) else "—")
                    v.setTextViewText(CHANGE[i], if (q != null) PriceRepo.changeText(q) else "")
                    v.setTextColor(CHANGE[i], if (q != null) PriceRepo.changeColor(q) else PriceRepo.NEUTRAL)
                }
                v.setTextViewText(R.id.status, status)
                v.setOnClickPendingIntent(R.id.refresh, refreshPi)
                v.setOnClickPendingIntent(R.id.root, openPi)
                mgr.updateAppWidget(id, v)
            }
        }
    }
}
