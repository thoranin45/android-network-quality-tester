package com.example.signalxpert.data

data class TestResult(
    val date: String,
    val download: Double,
    val upload: Double,
    val ping: Double,
    val jitter: Double,
    val serverLocation: String,  // เซิร์ฟเวอร์ที่เชื่อมต่อ
    val deviceModel: String,     // รุ่นโทรศัพท์
    val provider: String         // ผู้ให้บริการ
)
