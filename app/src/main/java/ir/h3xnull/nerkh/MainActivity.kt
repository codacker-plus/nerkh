package ir.h3xnull.nerkh

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.util.concurrent.Executors

class MainActivity : Activity() {

    private val io = Executors.newSingleThreadExecutor()
    private val ui = Handler(Looper.getMainLooper())
    private lateinit var grid: LinearLayout
    private lateinit var status: TextView
    private lateinit var dot: View
    private lateinit var refreshBtn: View

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
        grid = findViewById(R.id.grid)
        status = findViewById(R.id.status)
        dot = findViewById(R.id.dot)
        refreshBtn = findViewById(R.id.refresh)
        refreshBtn.setOnClickListener { refresh() }
        Scheduler.ensurePeriodic(this)
        show(false)
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
        setDot(PriceRepo.NEUTRAL)
        refreshBtn.animate().rotationBy(360f).setDuration(600).start()
        val app = applicationContext
        io.execute {
            val ok = try {
                PriceRepo.refresh(app)
                true
            } catch (e: Exception) {
                false
            }
            PriceWidget.updateAll(app)
            ui.post { if (!isFinishing) show(!ok) }
        }
    }

    private fun setDot(color: Int) {
        dot.background.mutate().setTint(color)
    }

    private fun show(failed: Boolean) {
        grid.removeAllViews()
        val items = PriceRepo.ALL_ITEMS
        val quotes = PriceRepo.load(this, items)
        var row: LinearLayout? = null
        items.forEachIndexed { i, item ->
            if (i % 2 == 0) {
                val newRow = LinearLayout(this)
                newRow.orientation = LinearLayout.HORIZONTAL
                newRow.layoutDirection = View.LAYOUT_DIRECTION_RTL
                grid.addView(newRow)
                row = newRow
            }
            val r = row!!
            val card = layoutInflater.inflate(R.layout.item_card, r, false)
            val q = quotes[i]
            card.findViewById<ImageView>(R.id.icon).setImageResource(item.icon)
            card.findViewById<TextView>(R.id.name).text = item.title
            card.findViewById<TextView>(R.id.unit).text = PriceRepo.unitText(item)
            card.findViewById<TextView>(R.id.price).text = if (q != null) PriceRepo.priceText(q) else "—"
            val ch = card.findViewById<TextView>(R.id.change)
            if (q != null) {
                ch.visibility = View.VISIBLE
                ch.text = PriceRepo.changeText(q)
                ch.setTextColor(PriceRepo.changeColor(q))
                ch.setBackgroundResource(PriceRepo.badgeRes(q))
            } else {
                ch.visibility = View.GONE
            }
            r.addView(card)
        }
        val error = failed || PriceRepo.hasError(this)
        status.text = if (failed) "خطا در اتصال — اینترنت رو چک کن" else PriceRepo.statusText(this)
        setDot(if (error) PriceRepo.DOWN else PriceRepo.UP)
    }
}
