package ir.h3xnull.nerkh

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import java.util.concurrent.Executors

class MainActivity : Activity() {

    private val io = Executors.newSingleThreadExecutor()
    private val ui = Handler(Looper.getMainLooper())
    private lateinit var list: LinearLayout
    private lateinit var status: TextView

    // While the app is open: refresh every 30 seconds.
    private val tick = object : Runnable {
        override fun run() {
            refresh()
            ui.postDelayed(this, 30_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        list = findViewById(R.id.list)
        status = findViewById(R.id.status)
        findViewById<View>(R.id.refresh).setOnClickListener { refresh() }
        Scheduler.ensurePeriodic(this)
        show(null)
    }

    override fun onResume() {
        super.onResume()
        ui.post(tick)
    }

    override fun onPause() {
        super.onPause()
        ui.removeCallbacks(tick)
    }

    override fun onDestroy() {
        super.onDestroy()
        io.shutdown()
    }

    private fun refresh() {
        if (io.isShutdown) return
        status.text = "در حال بروزرسانی…"
        val app = applicationContext
        io.execute {
            val ok = try {
                PriceRepo.refresh(app)
                true
            } catch (e: Exception) {
                false
            }
            PriceWidget.updateAll(app)
            ui.post { if (!isFinishing) show(if (ok) null else "خطا در اتصال — اینترنت رو چک کن") }
        }
    }

    private fun show(error: String?) {
        list.removeAllViews()
        val quotes = PriceRepo.load(this, PriceRepo.ALL_ITEMS)
        PriceRepo.ALL_ITEMS.forEachIndexed { i, item ->
            val row = layoutInflater.inflate(R.layout.item_price, list, false)
            val q = quotes[i]
            row.findViewById<TextView>(R.id.name).text = item.title
            row.findViewById<TextView>(R.id.price).text = if (q != null) PriceRepo.priceText(q) else "—"
            val ch = row.findViewById<TextView>(R.id.change)
            ch.text = if (q != null) PriceRepo.changeText(q) else ""
            ch.setTextColor(if (q != null) PriceRepo.changeColor(q) else PriceRepo.NEUTRAL)
            list.addView(row)
        }
        status.text = error ?: PriceRepo.statusText(this)
    }
}
