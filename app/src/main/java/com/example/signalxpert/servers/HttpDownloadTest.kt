package com.example.signalxpert.servers

import java.math.BigDecimal
import java.math.RoundingMode
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection

class HttpDownloadTest(private val fileURL: String) : Thread() {
    private var downloadElapsedTime = 0.0
    var isFinished = false
    var finalDownloadRate = 0.0
    private var startTime: Long = 0
    private var elapsedTime = 0.0
    private var firstNonZeroRateTime: Long = 0
    private val downloadRates: MutableList<Double> = CopyOnWriteArrayList()
    private var isCounting = false
    private var downloadedByte = 0
    private val timeout = 8

    private fun round(value: Double, places: Int): Double {
        require(places >= 0)
        return try {
            BigDecimal(value).setScale(places, RoundingMode.HALF_UP).toDouble()
        } catch (ex: Exception) {
            0.0
        }
    }

    val instantDownloadRate: Double
        get() {
            val now = System.currentTimeMillis()
            if (downloadedByte > 0) {
                elapsedTime = (now - if (firstNonZeroRateTime != 0L) firstNonZeroRateTime else startTime) / 1000.0
                if (elapsedTime > 0) {
                    val rate = round(downloadedByte * 8 / (1000 * 1000.0) / elapsedTime, 2)
                    if (rate > 0.0 && !isCounting) {
                        firstNonZeroRateTime = now
                        isCounting = true
                        println("Start Counting at: $firstNonZeroRateTime")
                    }
                    if (isCounting && (now - firstNonZeroRateTime) / 1000.0 <= timeout) {
                        downloadRates.add(rate)
                    }
                    return rate
                }
            }
            return 0.0
        }

    val averageDownloadRate: Double
        get() = synchronized(downloadRates) {
            if (downloadRates.isEmpty()) 0.0 else round(
                downloadRates.stream().mapToDouble { it }.average().orElse(0.0), 2)
        }

    override fun run() {
        try {
            val executor = Executors.newFixedThreadPool(4) // ใช้ 4 เชื่อมต่อพร้อมกัน
            val fileUrls = listOf(
                "$fileURL/random4000x4000.jpg",
                "$fileURL/random3000x3000.jpg",
                "$fileURL/random2000x2000.jpg",
                "$fileURL/random1000x1000.jpg"
            )

            startTime = System.currentTimeMillis()
            for (link in fileUrls) {
                executor.execute {
                    val url = URL(link)
                    try {
                        val conn = if (link.startsWith("https")) {
                            url.openConnection() as HttpsURLConnection
                        } else {
                            url.openConnection() as HttpURLConnection
                        }
                        conn.connect()
                        if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                            val buffer = ByteArray(4 * 1024 * 1024)
                            conn.inputStream.use { inputStream ->
                                var len: Int
                                while (inputStream.read(buffer).also { len = it } != -1) {
                                    downloadedByte += len
                                    instantDownloadRate // Calculate rate and trigger counting
                                    if (isCounting && (System.currentTimeMillis() - firstNonZeroRateTime) / 1000.0 >= timeout) {
                                        break
                                    }
                                }
                            }
                        } else {
                            println("Link not found or invalid: $link")
                        }
                        conn.disconnect()
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                        println("Error during download: ${ex.message}")
                    }
                }
            }
            executor.shutdown()
            executor.awaitTermination(timeout.toLong(), TimeUnit.SECONDS)
            downloadElapsedTime = if (isCounting) (System.currentTimeMillis() - firstNonZeroRateTime) / 1000.0 else (System.currentTimeMillis() - startTime) / 1000.0
            finalDownloadRate = downloadedByte * 8 / (1000 * 1000.0) / downloadElapsedTime
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        isFinished = true
    }
}