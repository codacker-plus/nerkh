package ir.h3xnull.nerkh

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/** One market item. [key] is the symbol in the tgju live feed. [inRial] = price comes in rial (shown in toman). */
data class Item(val key: String, val title: String, val inRial: Boolean)

data class Quote(val item: Item, val price: Double, val changePct: Double, val up: Boolean)

object PriceRepo {

    // The 6 items on the widget (order = order on screen).
    val WIDGET_ITEMS = listOf(
        Item("price_dollar_rl", "دلار", true),
        Item("price_eur", "یورو", true),
        Item("crypto-tether-irr", "تتر", true),
        Item("geram18", "طلای ۱۸ عیار", true),
        Item("sekee", "سکه امامی", true),
        Item("ons", "انس طلا", false),
    )

    // Everything shown inside the app.
    val ALL_ITEMS = WIDGET_ITEMS + listOf(
        Item("geram24", "طلای ۲۴ عیار", true),
        Item("mesghal", "مثقال طلا", true),
        Item("sekeb", "سکه بهار آزادی", true),
        Item("nim", "نیم سکه", true),
        Item("rob", "ربع سکه", true),
        Item("gerami", "سکه گرمی", true),
    )

    val UP = 0xFF22C55E.toInt()
    val DOWN = 0xFFEF4444.toInt()
    val NEUTRAL = 0xFF9CA3AF.toInt()

    // call1..call5 serve the same feed; try them in order.
    private val HOSTS = listOf("call4", "call1", "call2", "call3", "call5")

    private const val PREFS = "nerkh"
    private const val K_JSON = "json"
    private const val K_TIME = "fetched_at"
    private const val K_ERR = "error"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Downloads fresh prices and saves them. Blocking: call off the main thread. */
    fun refresh(ctx: Context) {
        val p = prefs(ctx)
        try {
            val current = download()
            val keep = JSONObject()
            for (item in ALL_ITEMS) {
                val o = current.optJSONObject(item.key)
                if (o != null) keep.put(item.key, o)
            }
            p.edit()
                .putString(K_JSON, keep.toString())
                .putLong(K_TIME, System.currentTimeMillis())
                .putBoolean(K_ERR, false)
                .commit()
        } catch (e: Exception) {
            p.edit().putBoolean(K_ERR, true).commit()
            throw e
        }
    }

    private fun download(): JSONObject {
        var last: Exception = Exception("no host")
        for (h in HOSTS) {
            try {
                return JSONObject(httpGet("https://$h.tgju.org/ajax.json")).getJSONObject("current")
            } catch (e: Exception) {
                last = e
            }
        }
        throw last
    }

    private fun httpGet(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 10_000
            c.readTimeout = 15_000
            c.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"
            )
            c.setRequestProperty("Accept", "application/json, text/plain, */*")
            c.setRequestProperty("Accept-Language", "fa-IR,fa;q=0.9,en;q=0.8")
            c.setRequestProperty("Referer", "https://www.tgju.org/")
            c.setRequestProperty("Origin", "https://www.tgju.org")
            val code = c.responseCode
            if (code != 200) throw Exception("HTTP $code")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    /** Saved prices for [items]; null where there is no data yet. */
    fun load(ctx: Context, items: List<Item>): List<Quote?> {
        val s = prefs(ctx).getString(K_JSON, null) ?: return items.map { null }
        val j = try {
            JSONObject(s)
        } catch (e: Exception) {
            return items.map { null }
        }
        return items.map { item ->
            val o = j.optJSONObject(item.key)
            if (o == null) null else parse(item, o)
        }
    }

    private fun parse(item: Item, o: JSONObject): Quote? {
        val price = o.optString("p").replace(",", "").toDoubleOrNull() ?: return null
        val dp = abs(o.optDouble("dp", 0.0)).let { if (it.isNaN()) 0.0 else it }
        return Quote(item, price, dp, o.optString("dt") != "low")
    }

    // ---------- text helpers ----------

    private val SYM = DecimalFormatSymbols(Locale.US)

    fun priceText(q: Quote): String {
        val s = if (q.item.inRial) {
            DecimalFormat("#,##0", SYM).format(q.price / 10) // rial -> toman
        } else {
            DecimalFormat("#,##0.##", SYM).format(q.price) + " $"
        }
        return fa(s)
    }

    fun changeText(q: Quote): String {
        if (q.changePct == 0.0) return fa("0%")
        val arrow = if (q.up) "▲" else "▼"
        return fa("$arrow " + DecimalFormat("0.##", SYM).format(q.changePct) + "%")
    }

    fun changeColor(q: Quote): Int = when {
        q.changePct == 0.0 -> NEUTRAL
        q.up -> UP
        else -> DOWN
    }

    fun statusText(ctx: Context): String {
        val t = prefs(ctx).getLong(K_TIME, 0L)
        if (t == 0L) return if (hasError(ctx)) "خطا در اتصال" else "هنوز داده‌ای نیست"
        val hm = fa(SimpleDateFormat("HH:mm", Locale.US).format(Date(t)))
        return if (hasError(ctx)) "خطا · آخرین: $hm" else "بروز: $hm"
    }

    fun fetchedAt(ctx: Context): Long = prefs(ctx).getLong(K_TIME, 0L)

    private fun hasError(ctx: Context): Boolean = prefs(ctx).getBoolean(K_ERR, false)

    /** Latin digits/separators -> Persian. */
    fun fa(s: String): String {
        val sb = StringBuilder(s.length)
        for (ch in s) {
            sb.append(
                when (ch) {
                    in '0'..'9' -> '۰' + (ch - '0')
                    ',' -> '٬'
                    '.' -> '٫'
                    '%' -> '٪'
                    else -> ch
                }
            )
        }
        return sb.toString()
    }
}
