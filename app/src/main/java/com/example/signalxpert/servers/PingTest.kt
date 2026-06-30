package com.example.signalxpert.servers

import java.io.BufferedReader
import java.io.InputStreamReader

// PingTest class:
class PingTest(private val server: String?, private val count: Int) : Thread() {
    private val rtts: MutableList<Double> = ArrayList()
    var isFinished = false
        private set
    var avgRtt = 0.0
        private set
    var instantRtt = 0.0
        private set
    var jitter = 0.0
        private set

    override fun run() {
        if (server == null) {
            isFinished = true
            return
        }

        try {
            val pb = ProcessBuilder("ping", "-c", count.toString(), server)
            pb.redirectErrorStream(true)
            val process = pb.start()
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                var line: String
                var prevRtt = 0.0
                var firstRtt = true
                while (reader.readLine().also { line = it } != null) {
                    if (line.contains("icmp_seq")) {
                        instantRtt = line.split("time=".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[1].split(" ".toRegex())
                            .dropLastWhile { it.isEmpty() }
                            .toTypedArray()[0].toDouble()
                        rtts.add(instantRtt)
                        if (!firstRtt) {
                            jitter += Math.abs(instantRtt - prevRtt)
                        } else {
                            firstRtt = false
                        }
                        prevRtt = instantRtt
                    }
                    if (line.startsWith("rtt")) {
                        avgRtt =
                            line.split("/".toRegex()).dropLastWhile { it.isEmpty() }
                                .toTypedArray()[4].toDouble()
                        break
                    }
                }
            }
            process.waitFor()
            jitter = if (rtts.size > 1) jitter / (rtts.size - 1) else 0.0
        } catch (e: Exception) {
            e.printStackTrace()
        }
        isFinished = true
    }
}
