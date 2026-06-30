package com.example.signalxpert.servers

import java.io.DataOutputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.HttpsURLConnection

class HttpUploadTest(private val fileURL: String) : Thread() {
    private var uploadElapsedTime = 0.0
    var isFinished = false
    var finalUploadRate = 0.0
    private var startTime: Long = 0
    private var firstNonZeroRateTime: Long = 0
    private val uploadRates: MutableList<Double> = ArrayList()
    private var isCounting = false
    private fun round(value: Double, places: Int): Double {
        require(places >= 0)
        return try {
            BigDecimal(value).setScale(places, RoundingMode.HALF_UP).toDouble()
        } catch (ex: Exception) {
            0.0
        }
    }

    val instantUploadRate: Double
        get() {
            if (uploadedKByte.get() >= 0) {
                val now = System.currentTimeMillis()
                val elapsedTime = (now - startTime) / 1000.0
                if (elapsedTime > 0) {
                    val rate = round(uploadedKByte.get() / 1000.0 * 8 / elapsedTime, 2)
                    if (rate > 0.0 && !isCounting) {
                        firstNonZeroRateTime = now
                        isCounting = true
                    }
                    if (isCounting && (now - firstNonZeroRateTime) / 1000.0 <= 8) {
                        uploadRates.add(rate)
                    }
                    return rate
                }
            }
            return 0.0
        }
    val averageUploadRate: Double
        get() = if (uploadRates.isEmpty()) 0.0 else round(
            uploadRates.stream().mapToDouble { obj: Double -> obj }
                .average().orElse(0.0), 2)

    override fun run() {
        try {
            val url = URL(fileURL)
            uploadedKByte.set(0)
            startTime = System.currentTimeMillis()
            val executor = Executors.newFixedThreadPool(4)
            for (i in 0..3) {
                executor.execute(HandlerUpload(url))
            }
            executor.shutdown()
            while (!executor.isTerminated) {
                sleep(100)
            }
            uploadElapsedTime = (System.currentTimeMillis() - startTime) / 1000.0
            finalUploadRate = uploadedKByte.get() / 1000.0 * 8 / uploadElapsedTime
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        isFinished = true
    }

    private class HandlerUpload(private val url: URL) : Thread() {
        override fun run() {
            val buffer = ByteArray(1024 * 1024)
            val startTime = System.currentTimeMillis()
            val timeout = 8
            while (true) {
                try {
                    val conn = if (url.protocol == "https") {
                        url.openConnection() as HttpsURLConnection
                    } else {
                        url.openConnection() as HttpURLConnection
                    }
                    conn.doOutput = true
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Connection", "Keep-Alive")
                    conn.connect()
                    DataOutputStream(conn.outputStream).use { dos ->
                        dos.write(buffer, 0, buffer.size)
                        dos.flush()
                    }
                    if (conn.responseCode != HttpsURLConnection.HTTP_OK) {
                        println("Upload error, response code: " + conn.responseCode)
                        break
                    }
                    uploadedKByte.addAndGet(buffer.size / 1024)
                    if ((System.currentTimeMillis() - startTime) / 1000.0 >= timeout) {
                        break
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    break
                }
            }
        }
    }

    companion object {
        private val uploadedKByte = AtomicInteger(0)
    }
}
