package com.example.signalxpert.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.signalxpert.CellularViewModel
import com.example.signalxpert.SimData
import com.example.signalxpert.SpeedTestViewModel
import com.example.signalxpert.data.TestResult

@Composable
fun ResultsScreen(
    navController: NavHostController,
    viewModel: SpeedTestViewModel = viewModel(),
    cellularViewModel: CellularViewModel = viewModel(),
    onBackPressed: () -> Unit,
    onItemClick: (TestResult) -> Unit,
    onSimDataClick: (SimData) -> Unit
) {
    var selectedTab by remember { mutableStateOf("speed") }

    val testResultsSpeed by viewModel.testHistory.collectAsState()
    val history4G by cellularViewModel.history4G.collectAsState()
    val history5G by cellularViewModel.history5G.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        IconButton(onClick = onBackPressed) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }

        // Tab Row
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .background(Color.LightGray, shape = RoundedCornerShape(16.dp)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Row ที่มีคำว่า "Speed" และ "Cellular"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // คำว่า "Speed"
                Text(
                    text = "Speed",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                // เส้นแบ่ง


                // คำว่า "Cellular"
                Text(
                    text = "Cellular",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Row ปุ่ม Speed, 4G, และ 5G
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // ปุ่ม "Speed"
                TabButton(
                    title = "Speed",
                    isSelected = selectedTab == "speed",
                    onClick = { selectedTab = "speed" }
                )

                Divider(
                    modifier = Modifier
                        .height(40.dp)
                        .width(1.dp), color = Color.Gray
                ) // เส้นแบ่งระหว่างปุ่ม

                // ปุ่ม "4G"
                TabButton(
                    title = "4G",
                    isSelected = selectedTab == "4g",
                    onClick = { selectedTab = "4g" }
                )

                Divider(
                    modifier = Modifier
                        .height(40.dp)
                        .width(1.dp), color = Color.Gray
                ) // เส้นแบ่งระหว่างปุ่ม

                // ปุ่ม "5G"
                TabButton(
                    title = "5G",
                    isSelected = selectedTab == "5g",
                    onClick = { selectedTab = "5g" }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Row สำหรับหัวข้อ "ประเภท", "วันที่", "Download", "Upload"
        Row(
            modifier = Modifier
                .fillMaxWidth() // กำหนดความกว้างของกรอบให้เต็มความกว้างของหน้าจอ
                .background(Color.LightGray, shape = RoundedCornerShape(8.dp)) // กำหนดสีพื้นหลังและทำให้มุมโค้ง
                .padding(8.dp), // Padding ภายในกรอบ
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("วันที่", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(20.dp))
            when (selectedTab) {
                "speed" -> {
                    Text("Download", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Upload", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                "4g" -> {
                    Text("PCI", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("RSRP", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("SINR", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                "5g" -> {
                    Text("PCI", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("SS RSRP", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("SS SINR", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // แสดงรายการ TestResults ตามแท็บที่เลือก
        when (selectedTab) {
            "speed" -> {
                TestHistoryScreen(testResultsSpeed, "No speed results available") { result ->
                    onItemClick(result) // ส่งผลลัพธ์ไปที่ onItemClick
                }
            }
            "4g" -> {
                TestHistoryScreenLTE(navController, history4G, "No 4G results available") { simData ->
                    onSimDataClick(simData)
                }
            }
            "5g" -> {
                TestHistoryScreenNR(navController, history5G, "No 5G results available") { simData ->
                    onSimDataClick(simData)
                }
            }
        }
    }
}

@Composable
fun TabButton(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.Black else Color.Gray
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (isSelected) {
            Box(
                modifier = Modifier
                    .height(3.dp)
                    .width(40.dp)
                    .background(Color.Red)
            )
        }
    }
}

@Composable
fun TestHistoryScreen(results: List<TestResult>, emptyMessage: String, onItemClick: (TestResult) -> Unit) {
    if (results.isEmpty()) {
        Text(
            text = emptyMessage,
            modifier = Modifier.fillMaxSize(),
            textAlign = TextAlign.Center
        )
    } else {
        LazyColumn {
            items(results) { result ->
                TestResultItem(result = result, onClick = { onItemClick(result) })
            }
        }
    }
}

@Composable
fun TestResultItem(result: TestResult, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ไอคอน LTE
      //  Icon(
      //      painter = painterResource(id = R.drawable.ic_lte), // แทนที่ด้วยไอคอนของคุณเอง
      //      contentDescription = null,
      //      modifier = Modifier.size(24.dp),
      //      tint = Color.Gray
      //  )

        Spacer(modifier = Modifier.width(8.dp))

        // ไอคอนโทรศัพท์
       // Icon(
       //     painter = painterResource(id = R.drawable.ic_phone), // แทนที่ด้วยไอคอนของคุณเอง
       //     contentDescription = null,
      //      modifier = Modifier.size(24.dp),
      //      tint = Color.Gray
     //   )

        Spacer(modifier = Modifier.width(8.dp))

        // วันที่และเวลา
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = result.date,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Gray
            )
        }

        // Download Speed
        Text(
            text = "${result.download}",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        // Upload Speed
        Text(
            text = "${result.upload}",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
fun TestResultDetailScreen(result: TestResult, onBackPressed: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1C1E)) // สีพื้นหลังที่เข้มขึ้นเล็กน้อย
            .padding(16.dp)
    ) {
        // ส่วนหัวพร้อมปุ่มกลับ
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackPressed) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "ผลลัพธ์",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.weight(1f)) // ช่องว่างที่อยู่สองด้านเพื่อทำให้หัวเรื่องอยู่ตรงกลาง
        }

        Spacer(modifier = Modifier.height(16.dp))

        // วันที่ของการทดสอบ
        Text(
            text = "วันที่: ${result.date}",
            color = Color(0xFFBDBDBD), // สีเทาอ่อน
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ส่วนความเร็ว
        SectionTitle(title = "ความเร็ว")
        SpeedRow(label = "ดาวน์โหลด", speed = "${result.download} Mbps", usedData = "ใช้ข้อมูลแล้ว 90.5MB")
        Spacer(modifier = Modifier.height(8.dp))
        SpeedRow(label = "อัปโหลด", speed = "${result.upload} Mbps", usedData = "ใช้ข้อมูลแล้ว 0.0MB")

        Spacer(modifier = Modifier.height(16.dp))

        // ส่วนการตอบสนอง
        SectionTitle(title = "การตอบสนอง")
        PingRow(label = "Ping", ping = "${result.ping}", jitter = "${result.jitter}", min = "-", max = "-")

        Spacer(modifier = Modifier.height(16.dp))

        // ส่วนการเชื่อมต่อ
        SectionTitle(title = "การเชื่อมต่อ")
        NetworkInfoRow(label = "ประเภทการเชื่อมต่อ", value = "${result.provider}")
        Spacer(modifier = Modifier.height(8.dp))
        NetworkInfoRow(label = "อุปกรณ์", value = "${result.deviceModel}")
        Spacer(modifier = Modifier.height(8.dp))
        NetworkInfoRow(label = "เซิร์ฟเวอร์", value = " ${result.serverLocation}")
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        color = Color(0xFF64B5F6), // สีฟ้าอ่อน
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun SpeedRow(label: String, speed: String, usedData: String) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = Color(0xFFBDBDBD), fontSize = 14.sp) // สีเทาอ่อน
            Text(text = speed, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) // ตัวหนาสำหรับความเร็ว
        }
        Text(text = usedData, color = Color(0xFFBDBDBD), fontSize = 12.sp) // สีเทาอ่อนสำหรับข้อมูลที่ใช้
    }
}

@Composable
fun PingRow(label: String, ping: String, jitter: String, min: String, max: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(text = label, color = Color(0xFFFFC107), fontSize = 14.sp) // สีเหลืองสำหรับ Ping
            Text(text = ping, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold) // ขนาดใหญ่สำหรับ Ping
            Text(text = "Jitter $jitter", color = Color(0xFFBDBDBD), fontSize = 12.sp) // สีเทาสำหรับ Jitter
        }
    }
}

@Composable
fun NetworkInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color(0xFFBDBDBD), fontSize = 14.sp) // สีเทาอ่อนสำหรับ Label
        Text(text = value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) // ตัวหนาสำหรับข้อมูล
    }
}

@Composable
fun TestHistoryScreenLTE(
    navController: NavHostController,
    results: List<SimData>,
    emptyMessage: String,
    onItemClick: (SimData) -> Unit // ใช้ SimData เป็นพารามิเตอร์ของ onItemClick
) {
    if (results.isEmpty()) {
        Text(
            text = emptyMessage,
            modifier = Modifier.fillMaxSize(),
            textAlign = TextAlign.Center
        )
    }  else {
        LazyColumn {
            items(results) { result ->
                TestResultItemLTE(result = result, onClick = {onItemClick (result) })
            }
        }
    }
}

@Composable
fun TestResultItemLTE(result: SimData, onClick: () -> Unit) { // รับ `SimData` และ `onClick`
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Spacer(modifier = Modifier.width(8.dp))

        // วันที่และเวลา
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text =  "${result.pci}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Gray
            )
        }

        // RSRP
        Text(
            text = "${result.rsrp}",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        // SINR
        Text(
            text = "${result.sinr}",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
fun SignalQualityScreenLTE(result: SimData, onBackPressed: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        IconButton(onClick = onBackPressed) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        // Section 1: คุณภาพสัญญาณ
        Text(
            text = "คุณภาพสัญญาณ",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Column(
            modifier = Modifier.padding(start = 16.dp)
        ) {
            NetworkInfoRowLTE(label = "RSSI", value = "${result.rssi}")
            Spacer(modifier = Modifier.height(8.dp))
            NetworkInfoRowLTE(label = "RSRQ", value = "${result.rsrq}")
            Spacer(modifier = Modifier.height(8.dp))
            NetworkInfoRowLTE(label = "RSRP", value = "${result.rsrp}")
            Spacer(modifier = Modifier.height(8.dp))
            NetworkInfoRowLTE(label = "SINR", value = "${result.sinr}")
            Spacer(modifier = Modifier.height(8.dp))
            NetworkInfoRowLTE(label = "CQI", value = "${result.cqi}")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section 2: ข้อมูลเพิ่มเติม
        Text(
            text = "ข้อมูลเพิ่มเติม",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Column(
            modifier = Modifier.padding(start = 16.dp)
        ) {
            NetworkInfoRowLTE(label = "PLMN", value = "${result.plmn}")
            Spacer(modifier = Modifier.height(8.dp))
            NetworkInfoRowLTE(label = "Band info", value = "${result.bandInfo}")
            Spacer(modifier = Modifier.height(8.dp))
            NetworkInfoRowLTE(label = "BW", value = "${result.bandwidth}")
            Spacer(modifier = Modifier.height(8.dp))
            NetworkInfoRowLTE(label = "TAC", value = "${result.tac}")
            Spacer(modifier = Modifier.height(8.dp))
            NetworkInfoRowLTE(label = "PCI", value = "${result.pci}")
            Spacer(modifier = Modifier.height(8.dp))
            NetworkInfoRowLTE(label = "Call ID", value = "${result.cellId}")
            Spacer(modifier = Modifier.height(8.dp))
            NetworkInfoRowLTE(label = "EARFCN", value = "${result.earfcn}")
            Spacer(modifier = Modifier.height(8.dp))
            NetworkInfoRowLTE(label = "FREQUENCY", value = "${result.frequency}")
        }
    }
}
@Composable
fun NetworkInfoRowLTE(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color(0xFFBDBDBD), fontSize = 14.sp) // สีเทาอ่อนสำหรับ Label
        Text(text = value, color = Color.LightGray, fontSize = 14.sp, fontWeight = FontWeight.Bold) // ตัวหนาสำหรับข้อมูล
    }
}
@Composable
fun TestHistoryScreenNR(
    navController: NavHostController,
    results: List<SimData>,
    emptyMessage: String,
    onItemClick: (SimData) -> Unit // ใช้ SimData เป็นพารามิเตอร์ของ onItemClick
) {
    if (results.isEmpty()) {
        Text(
            text = emptyMessage,
            modifier = Modifier.fillMaxSize(),
            textAlign = TextAlign.Center
        )
    } else {
        LazyColumn {
            items(results) { result ->
                TestResultItemNR(result = result, onClick = { onItemClick(result) }) // ส่ง result ไปยัง onItemClick
            }
        }
    }
}
@Composable
fun TestResultItemNR(result: SimData, onClick: () -> Unit) { // รับ `SimData` และ `onClick`
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Spacer(modifier = Modifier.width(8.dp))

        // วันที่และเวลา
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text =  "${result.pci}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Gray
            )
        }

        // RSRP
        Text(
            text = "${result.ssrsrp}",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        // SINR
        Text(
            text = "${result.sssinr}",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
fun SignalQualityScreenNR(result: SimData, onBackPressed: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        IconButton(onClick = onBackPressed) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        // Section 1: คุณภาพสัญญาณ
        Text(
            text = "คุณภาพสัญญาณ",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Column(
            modifier = Modifier.padding(start = 16.dp)
        ) {
            NetworkInfoRowLTE(label = "SS RSRQ", value = "${result.ssrsrq}")
            Spacer(modifier = Modifier.height(8.dp))
            NetworkInfoRowLTE(label = "SS RSRP", value = "${result.ssrsrp}")
            Spacer(modifier = Modifier.height(8.dp))
            NetworkInfoRowLTE(label = "SS SINR", value = "${result.sinr}")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section 2: ข้อมูลเพิ่มเติม
        Text(
            text = "ข้อมูลเพิ่มเติม",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Column(
            modifier = Modifier.padding(start = 16.dp)
        ) {
            NetworkInfoRowLTE(label = "PLMN", value = "${result.plmn}")
            Spacer(modifier = Modifier.height(8.dp))
            NetworkInfoRowLTE(label = "Band info", value = "${result.bandInfo}")
            Spacer(modifier = Modifier.height(8.dp))
            NetworkInfoRowLTE(label = "BW", value = "${result.bandwidth}")
            Spacer(modifier = Modifier.height(8.dp))
            NetworkInfoRowLTE(label = "TAC", value = "${result.tac}")
            Spacer(modifier = Modifier.height(8.dp))
            NetworkInfoRowLTE(label = "PCI", value = "${result.pci}")
            Spacer(modifier = Modifier.height(8.dp))
            NetworkInfoRowLTE(label = "Call ID", value = "${result.cellId}")
            Spacer(modifier = Modifier.height(8.dp))
            NetworkInfoRowLTE(label = "EARFCN", value = "${result.arfcn}")
            Spacer(modifier = Modifier.height(8.dp))
            NetworkInfoRowLTE(label = "FREQUENCY", value = "${result.frequency}")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ResultsScreenPreview() {
    // สร้าง NavController จำลองเพื่อใช้ใน Preview
    val navController = rememberNavController()

    ResultsScreen(
        navController = navController,
        onBackPressed = {},
        onItemClick = { },
        onSimDataClick = { }
    )
}