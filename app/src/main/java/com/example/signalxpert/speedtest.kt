@file:Suppress("DEPRECATION")

package com.example.signalxpert

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.telephony.TelephonyManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.example.signalxpert.data.TestResult
import com.example.signalxpert.servers.GetSpeedTestHostsHandler
import com.example.signalxpert.servers.HttpDownloadTest
import com.example.signalxpert.servers.HttpUploadTest
import com.example.signalxpert.servers.PingTest
import com.example.signalxpert.ui.theme.SignalXpertTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.floor
import kotlin.math.min

val Green200 = Color(0xFFA5D6A7)
val Green500 = Color(0xFF4CAF50)
val LightColor = Color(0xFFD3D3D3)
val GreenGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF81C784), Color(0xFF388E3C))
)

class SpeedTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SignalXpertTheme {
                SpeedTestScreen()
            }
        }
    }
}

class SpeedTestViewModel : ViewModel() {
    var downloadThreshold by mutableStateOf(2.5)
    var uploadThreshold by mutableStateOf(0.5)
    var pingThreshold by mutableStateOf(150.0)

    var isDownloadAboveThreshold by mutableStateOf(false)
        private set
    var isUploadAboveThreshold by mutableStateOf(false)
        private set
    var isPingBelowThreshold by mutableStateOf(false)
        private set

    private val _testHistory = MutableStateFlow<List<TestResult>>(emptyList())
    val testHistory: StateFlow<List<TestResult>> = _testHistory
    var selectedServer by mutableStateOf<List<String>?>(null)
    var serversList by mutableStateOf<List<List<String>>>(emptyList())
    var showBottomSheet by mutableStateOf(false)
    var downloadSpeed by mutableStateOf(0.0)
        private set
    var uploadSpeed by mutableStateOf(0.0)
        private set
    var avgDownloadSpeed by mutableStateOf(0.0)
        private set
    var avgUploadSpeed by mutableStateOf(0.0)
        private set
    var maxDownloadSpeed by mutableStateOf(0.0)
        private set
    var maxUploadSpeed by mutableStateOf(0.0)
        private set
    var ping by mutableStateOf(0.0)
        private set
    var jitter by mutableStateOf(0.0)
        private set
    var serverLocation by mutableStateOf("")
        private set
    var showUploadGraph by mutableStateOf(false)
        private set
    var isTestRunning by mutableStateOf(false)
        private set
    var isTestCompleted by mutableStateOf(false)
        private set

    var graphRange by mutableStateOf(100f)

    private val getSpeedTestHostsHandler = GetSpeedTestHostsHandler()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            getSpeedTestHostsHandler.start()
            getSpeedTestHostsHandler.join()

            serversList = getSpeedTestHostsHandler.mapValue.values.toList()
            if (serversList.isNotEmpty()) {
                selectedServer = serversList[0]
                withContext(Dispatchers.Main) {
                    serverLocation = "${selectedServer?.get(2)}, ${selectedServer?.get(3)}"
                }
            }
        }
    }

    fun selectServer(server: List<String>) {
        selectedServer = server
        serverLocation = "${server[2]}, ${server[3]}"
    }

    fun saveTestResult(
        context: Context,
        downloadSpeed: Double,
        uploadSpeed: Double,
        ping: Double,
        jitter: Double
    ) {
        val serverLocation = selectedServer?.let { "${it[2]}, ${it[3]}" } ?: "Unknown"
        val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
        val provider = getProviderName(context)

        val newResult = TestResult(
            date = getCurrentDate(),
            download = downloadSpeed,
            upload = uploadSpeed,
            ping = ping,
            jitter = jitter,
            serverLocation = serverLocation,
            deviceModel = deviceModel,
            provider = provider
        )
        _testHistory.value = _testHistory.value + newResult
        println("TestResult added: $newResult")
    }

    private fun getCurrentDate(): String {
        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        return current.format(formatter)
    }

    private fun getProviderName(context: Context): String {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return telephonyManager.networkOperatorName
    }

    private fun onTestCompleted(context: Context) {
        saveTestResult(
            context = context,
            downloadSpeed = avgDownloadSpeed,
            uploadSpeed = avgUploadSpeed,
            ping = ping,
            jitter = jitter
        )
    }

    private fun checkThresholds() {
        isDownloadAboveThreshold = avgDownloadSpeed >= downloadThreshold
        isUploadAboveThreshold = avgUploadSpeed >= uploadThreshold
        isPingBelowThreshold = ping <= pingThreshold
    }

    fun startSpeedTest(context: Context) {
        if (isTestRunning) return

        isTestRunning = true
        isTestCompleted = false
        resetTestValues()

        viewModelScope.launch(Dispatchers.Default) {
            val bestServerIndex = selectBestServer()
            val bestServerUrl = getSpeedTestHostsHandler.mapKey[bestServerIndex]

            if (bestServerUrl != null) {
                val pingTest = PingTest(bestServerUrl, 3) // Only one instance of PingTest
                pingTest.start()
                pingTest.join()

                withContext(Dispatchers.Main) {
                    ping = pingTest.avgRtt
                    jitter = pingTest.jitter
                }

                // Update UI in loop for ping test progress
                while (!pingTest.isFinished) {
                    val currentRtt = pingTest.instantRtt
                    if (currentRtt > 0.0) {
                        ping = ping
                        jitter = jitter
                    }
                    delay(100)
                }

                val downloadTest = HttpDownloadTest(bestServerUrl)
                downloadTest.start()

                while (!downloadTest.isFinished) {
                    val downloadRate = downloadTest.instantDownloadRate
                    if (downloadRate > 0.0) {
                        withContext(Dispatchers.Main) {
                            downloadSpeed = downloadRate
                            maxDownloadSpeed = maxOf(maxDownloadSpeed, downloadRate)
                        }
                    }
                    delay(100)
                }
                downloadTest.join()
                withContext(Dispatchers.Main) {
                    downloadSpeed = downloadTest.finalDownloadRate
                    avgDownloadSpeed = downloadTest.averageDownloadRate
                }

                delay(1000)
                resetGraph()

                val uploadTest = HttpUploadTest(bestServerUrl)
                uploadTest.start()

                while (!uploadTest.isFinished) {
                    val uploadRate = uploadTest.instantUploadRate
                    if (uploadRate > 0.0) {
                        withContext(Dispatchers.Main) {
                            uploadSpeed = uploadRate
                            maxUploadSpeed = maxOf(maxUploadSpeed, uploadRate)
                            showUploadGraph = true
                        }
                    }
                    delay(100)
                }
                uploadTest.join()
                withContext(Dispatchers.Main) {
                    uploadSpeed = uploadTest.finalUploadRate
                    avgUploadSpeed = uploadTest.averageUploadRate

                    delay(1000)
                    resetGraph()

                    isTestRunning = false
                    isTestCompleted = true

                    checkThresholds()
                    onTestCompleted(context) // Called when the test is completed
                }
            }
        }
    }

    fun resetTestValues() {
        downloadSpeed = 0.0
        uploadSpeed = 0.0
        maxDownloadSpeed = 0.0
        maxUploadSpeed = 0.0
        avgDownloadSpeed = 0.0
        avgUploadSpeed = 0.0
        ping = 0.0
        jitter = 0.0
        showUploadGraph = false
    }

    private fun resetGraph() {
        downloadSpeed = 0.0
        uploadSpeed = 0.0
    }

    private fun selectBestServer(): Int {
        val pingResults = mutableMapOf<Int, Double>()
        val mapKey = getSpeedTestHostsHandler.mapKey

        runBlocking {
            mapKey.forEach { (index, url) ->
                val pingTest = PingTest(url.replace(":8080", ""), 3)
                pingTest.start()
                pingTest.join()
                pingResults[index] = pingTest.avgRtt
            }
        }

        return pingResults.minByOrNull { it.value }?.key ?: 0
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedTestScreen(viewModel: SpeedTestViewModel = viewModel()) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Speedometer Section
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(300.dp)
        ) {
            Speedometer(
                speed = if (viewModel.showUploadGraph) viewModel.uploadSpeed.toFloat() else viewModel.downloadSpeed.toFloat(),
                label = if (viewModel.showUploadGraph) "Upload" else "Download",
                graphRange = viewModel.graphRange
            )
        }

        Spacer(modifier = Modifier.height(5.dp))

        // Speed Metrics Section
        SpeedMetricsDisplay(
            avgDownloadSpeed = viewModel.avgDownloadSpeed,
            maxDownloadSpeed = viewModel.maxDownloadSpeed,
            isDownloadAboveThreshold = viewModel.isDownloadAboveThreshold,
            avgUploadSpeed = viewModel.avgUploadSpeed,
            maxUploadSpeed = viewModel.maxUploadSpeed,
            isUploadAboveThreshold = viewModel.isUploadAboveThreshold,
            ping = viewModel.ping,
            isPingBelowThreshold = viewModel.isPingBelowThreshold,
            jitter = viewModel.jitter,
            isTestCompleted = viewModel.isTestCompleted
        )

        Spacer(modifier = Modifier.height(1.dp))

        // Network Card Section
        NetworkCard(
            serverLocation = viewModel.serverLocation,
            onServerClick = {
                viewModel.showBottomSheet = true
                scope.launch { sheetState.show() }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Start Test Button
        Button(
            onClick = {
                viewModel.resetTestValues()
                viewModel.startSpeedTest(context)
            },
            enabled = !viewModel.isTestRunning,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000)) // เปลี่ยนสีปุ่ม
        ) {
            Text(text = if (viewModel.isTestCompleted) "TEST AGAIN" else "START")
        }
    }

    if (viewModel.showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.showBottomSheet = false },
            sheetState = sheetState
        ) {
            CombinedBottomSheet(viewModel = viewModel) {
                viewModel.showBottomSheet = false
                scope.launch { sheetState.hide() }
            }
        }
    }
}

@Composable
fun SpeedMetricsDisplay(
    avgDownloadSpeed: Double,
    maxDownloadSpeed: Double,
    isDownloadAboveThreshold: Boolean,
    avgUploadSpeed: Double,
    maxUploadSpeed: Double,
    isUploadAboveThreshold: Boolean,
    ping: Double,
    isPingBelowThreshold: Boolean,
    jitter: Double,
    isTestCompleted: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Download and Upload Metrics
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            MetricDisplay(
                label = "DOWNLOAD", unit = "Mbps", value = avgDownloadSpeed,
                additionalValue = maxDownloadSpeed, status = if (isTestCompleted) if (isDownloadAboveThreshold) "Pass" else "Fail" else null,
                statusColor = if (isTestCompleted) if (isDownloadAboveThreshold) Color.Green else Color.Red else Color.Gray
            )
            MetricDisplay(
                label = "UPLOAD", unit = "Mbps", value = avgUploadSpeed,
                additionalValue = maxUploadSpeed, status = if (isTestCompleted) if (isUploadAboveThreshold) "Pass" else "Fail" else null,
                statusColor = if (isTestCompleted) if (isUploadAboveThreshold) Color.Green else Color.Red else Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Ping and Jitter Metrics
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            MetricDisplay(
                label = "PING", unit = "ms", value = ping,
                status = if (isTestCompleted) if (isPingBelowThreshold) "Pass" else "Fail" else null,
                statusColor = if (isTestCompleted) if (isPingBelowThreshold) Color.Green else Color.Red else Color.Gray,
                isHorizontal = true
            )
            MetricDisplay(
                label = "JITTER", unit = "ms", value = jitter,
                isHorizontal = true
            )
        }
    }
}

@Composable
fun MetricDisplay(
    label: String,
    unit: String,
    value: Double,
    additionalValue: Double? = null,
    status: String? = null,
    statusColor: Color = Color.Gray,
    isHorizontal: Boolean = false
) {
    if (isHorizontal) {
        // For Ping and Jitter
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = label , fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(text = unit, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, bottom = 15.dp), color = Color.Gray)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = String.format("%.1f", value),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            status?.let {
                Text(
                    text = it,
                    fontSize = 14.sp,
                    color = statusColor,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    } else {
        // For Download and Upload
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = label, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(text = unit, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, bottom = 15.dp), color = Color.Gray)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = String.format("%.1f", value), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                status?.let {
                    Text(
                        text = it,
                        fontSize = 14.sp,
                        color = statusColor,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            additionalValue?.let {
                Text(text = String.format("%.1f", it), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun NetworkCard(serverLocation: String, onServerClick: () -> Unit) {
    val context = LocalContext.current
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkInfo = connectivityManager.activeNetworkInfo

    val isWiFi = networkInfo?.type == ConnectivityManager.TYPE_WIFI
    val connectionName = remember { mutableStateOf("") }
    val connectionIcon = remember { mutableStateOf(R.drawable.ic_wifi) } // Default to Wi-Fi icon

    // Get the actual device name
    val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"

    // Request permissions
    val permissionState = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                // Access Wi-Fi or mobile info based on the connection type
                if (isWiFi == true) {
                    val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    val wifiInfo = wifiManager.connectionInfo
                    connectionName.value = wifiInfo.ssid.trim('"')
                    connectionIcon.value = R.drawable.ic_wifi
                } else if (networkInfo?.type == ConnectivityManager.TYPE_MOBILE) {
                    val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                    connectionName.value = telephonyManager.networkOperatorName
                    connectionIcon.value = when (networkInfo.subtype) {
                        TelephonyManager.NETWORK_TYPE_NR -> R.drawable.baseline_5g_24 // 5G
                        TelephonyManager.NETWORK_TYPE_LTE -> R.drawable.baseline_4g_mobiledata_24 // 4G
                        else -> R.drawable.baseline_4g_mobiledata_24 // Default for other mobile networks
                    }
                } else {
                    connectionName.value = "Unknown"
                }
            }
        }
    )

    LaunchedEffect(Unit) {
        permissionState.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.LightGray)
            .padding(16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(16.dp) // Padding ปรับตามขนาดหน้าจอ
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = connectionIcon.value),
                        contentDescription = null,
                        modifier = Modifier.size(30.dp).clip(CircleShape).background(Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(connectionName.value, fontSize = 16.sp, color = Color.Black)
                        Text(deviceName, fontSize = 14.sp, color = Color.Black)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Server: $serverLocation", fontSize = 14.sp, color = Color.Black)
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
                    .clickable(onClick = onServerClick),
                contentAlignment = Alignment.Center
            ) {
                Text("!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CombinedBottomSheet(viewModel: SpeedTestViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val graphRange = viewModel.graphRange
    val isTestRunning = viewModel.isTestRunning
    var isServerSelection by remember { mutableStateOf(false) } // State to toggle between views

    var downloadInput by remember { mutableStateOf(viewModel.downloadThreshold.toString()) }
    var uploadInput by remember { mutableStateOf(viewModel.uploadThreshold.toString()) }
    var pingInput by remember { mutableStateOf(viewModel.pingThreshold.toString()) }

    if (isServerSelection) {
        // Server Selection UI
        LazyColumn(modifier = Modifier.padding(16.dp)) {
            item {
                Text("เซิร์ฟเวอร์", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
            }
            viewModel.serversList.forEach { server ->
                item {
                    ServerItem(
                        city = server[2],
                        provider = server[5],
                        isSelected = viewModel.selectedServer == server,
                        onClick = {
                            viewModel.selectServer(server)
                            isServerSelection = false // Return to main settings view
                        }
                    )
                }
            }
        }
    } else {
        // Main Settings UI
        LazyColumn(modifier = Modifier.padding(16.dp)) {
            // Graph Range Section
            item {
                Text("สเกล", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val ranges = listOf(100f, 500f, 1000f)
                    ranges.forEach { range ->
                        Text(
                            text = range.toInt().toString(),
                            color = if (graphRange == range) Color.Blue else Color.Gray,
                            modifier = Modifier
                                .clickable(enabled = !isTestRunning) {
                                    viewModel.graphRange = range
                                }
                                .padding(8.dp),
                            fontSize = 18.sp,
                            fontWeight = if (graphRange == range) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Divider(color = Color.Gray, thickness = 1.dp)
            }

            // Test Thresholds Section
            item {
                Text("กำหนดค่า", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    ThresholdInputField(label = "Download", value = downloadInput, onValueChange = { downloadInput = it }, unitLabel = "Mbps")
                    Spacer(modifier = Modifier.height(8.dp))
                    ThresholdInputField(label = "Upload", value = uploadInput, onValueChange = { uploadInput = it }, unitLabel = "Mbps")
                    Spacer(modifier = Modifier.height(8.dp))
                    ThresholdInputField(label = "Ping", value = pingInput, onValueChange = { pingInput = it }, unitLabel = "ms")
                }

                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = Color.Gray, thickness = 1.dp)
            }

            // Server Selection Section
            item {
                var isPressed by remember { mutableStateOf(false) }
                val scale by animateFloatAsState(targetValue = if (isPressed) 1.05f else 1f)

                // Wrap Text with a Box to apply the scale animation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(scale) // Apply scaling based on the pressed state
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    isPressed = true
                                    tryAwaitRelease()  // Wait until the tap is released
                                    isPressed = false
                                }
                            )
                        }
                        .clickable {
                            isServerSelection = true // Switch to server selection view
                        }
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "เปลี่ยนเซิร์ฟเวอร์ทดสอบ",
                        fontSize = 16.sp,
                        color = Color.Blue,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThresholdInputField(label: String, value: String, onValueChange: (String) -> Unit, unitLabel: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp) // เพิ่มความสูงให้พอดีกับการแสดง TextField
            .clip(RoundedCornerShape(12.dp)) // ปรับให้ขอบโค้งมน
            .background(Color.LightGray) // สีพื้นหลังของกล่องกรอกข้อมูล
            .padding(horizontal = 16.dp), // Padding รอบด้านของกล่อง
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Label ด้านซ้าย
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.DarkGray,
                modifier = Modifier.padding(start = 8.dp)
            )

            Spacer(modifier = Modifier.width(16.dp)) // เพิ่มระยะห่างระหว่าง Label กับ TextField

            // TextField สำหรับกรอกข้อมูล
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f), // กำหนดให้ TextField ขยายได้เต็มที่
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp),
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = Color.Transparent, // ตั้งให้พื้นหลังเป็นโปร่งใส
                    cursorColor = Color.Black, // สีของเคอร์เซอร์
                    focusedIndicatorColor = Color.Transparent, // ขอบเมื่อโฟกัส
                    unfocusedIndicatorColor = Color.Transparent // ขอบเมื่อไม่ได้โฟกัส
                ),
                singleLine = true // จำกัดให้แสดงผลเพียงบรรทัดเดียว
            )

            Spacer(modifier = Modifier.width(8.dp)) // เพิ่มระยะห่างระหว่าง TextField กับ Unit

            // Unit Label ทางขวา
            Text(
                text = unitLabel,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = Color.DarkGray
            )
        }
    }
}



@Composable
fun ServerItem(city: String, provider: String, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isSelected) Color(0xFF003366) else Color.Transparent
    val textColor = if (isSelected) Color.Cyan else Color.Black
    val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .background(backgroundColor)
            .padding(8.dp)
    ) {
        Text(
            text = "$city - $provider",
            fontSize = 16.sp,
            fontWeight = fontWeight,
            color = textColor,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
fun Speedometer(speed: Float, label: String, graphRange: Float) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(300.dp).padding(16.dp) // Padding ปรับตามขนาดหน้าจอ
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val progress = min(speed / graphRange, 1f)
            drawArcs(progress, 270f)
            drawLines(progress, 270f)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = label, color = Color.Gray, fontSize = 16.sp)
            Text(text = String.format("%.1f Mbps", speed), color = Color.Gray, fontSize = 24.sp)
        }
    }
}

fun DrawScope.drawArcs(progress: Float, maxValue: Float) {
    val startAngle = 270 - maxValue / 2
    val sweepAngle = maxValue * progress

    val topLeft = Offset(50f, 50f)
    val size = Size(size.width - 100f, size.height - 100f)

    fun drawBlur() {
        for (i in 0..20) {
            drawArc(
                color = Green200.copy(alpha = i / 900f),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = size,
                style = Stroke(width = 80f + (20 - i) * 20, cap = StrokeCap.Round)
            )
        }
    }

    fun drawStroke() {
        drawArc(
            color = Green500,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = topLeft,
            size = size,
            style = Stroke(width = 86f, cap = StrokeCap.Round)
        )
    }

    fun drawGradient() {
        drawArc(
            brush = GreenGradient,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = topLeft,
            size = size,
            style = Stroke(width = 80f, cap = StrokeCap.Round)
        )
    }

    drawBlur()
    drawStroke()
    drawGradient()
}

fun DrawScope.drawLines(progress: Float, maxValue: Float, numberOfLines: Int = 40) {
    val oneRotation = maxValue / numberOfLines
    val startValue = if (progress == 0f) 0 else floor(progress * numberOfLines).toInt() + 1

    for (i in startValue..numberOfLines) {
        rotate(i * oneRotation + (180 - maxValue) / 2) {
            drawLine(
                LightColor,
                Offset(if (i % 5 == 0) 80f else 30f, size.height / 2),
                Offset(0f, size.height / 2),
                8f,
                StrokeCap.Round
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SpeedTestScreenPreview() {
    SignalXpertTheme {
        SpeedTestScreen(viewModel = SpeedTestViewModel().apply {
            showBottomSheet = true
        })
    }
}
