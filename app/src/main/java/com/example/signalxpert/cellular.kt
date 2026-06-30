@file:Suppress("DEPRECATION")

package com.example.signalxpert

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telephony.*
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.example.signalxpert.ui.theme.SignalXpertTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import androidx.compose.foundation.layout.fillMaxSize
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker


class CellularActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SignalXpertTheme {
                CellularScreen()
            }
        }
    }
}

@Suppress("DEPRECATION")
class CellularViewModel : ViewModel() {

    private var appContext: Context? = null
    val hasPermissions = mutableStateOf(false)

    // SIM data
    val sim1Data = mutableStateOf(SimData.empty())
    val sim2Data = mutableStateOf(SimData.empty())

    private val _history4G = MutableStateFlow<List<SimData>>(emptyList())
    val history4G: StateFlow<List<SimData>> = _history4G

    private val _history5G = MutableStateFlow<List<SimData>>(emptyList())
    val history5G: StateFlow<List<SimData>> = _history5G

    fun startFetching(context: Context) {
        appContext = context
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                fetchData(context)
                delay(4000) // ดึงข้อมูลเซลลูลาร์ทุก 2 วินาที
            }
        }
    }

    fun stopFetching() {
        viewModelScope.coroutineContext.cancelChildren()
    }

    fun onPermissionsResult(permissions: Map<String, Boolean>, context: Context) {
        hasPermissions.value = permissions.values.all { it }
        if (hasPermissions.value) {
            startFetching(context)
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchData(context: Context) {
        if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val sim1 = getSimData(context, 0)
        val sim2 = getSimData(context, 1)

        sim1Data.value = sim1 // Update SIM 1 data
        sim2Data.value = sim2 // Update SIM 2 data

        // Add to history
        if (sim1.networkType == "4G") {
            _history4G.value = _history4G.value + sim1
        } else if (sim1.networkType == "5G") {
            _history5G.value = _history5G.value + sim1
        }

        if (sim2.networkType == "4G") {
            _history4G.value = _history4G.value + sim2
        } else if (sim2.networkType == "5G") {
            _history5G.value = _history5G.value + sim2
        }

        // ตรวจสอบว่ามีผลลัพธ์หรือไม่
        if (_history4G.value.isEmpty()) {
            println("No results in history4G")
        } else {
            println("Results found in history4G: ${_history4G.value.size}")
        }

        if (_history5G.value.isEmpty()) {
            println("No results in history5G")
        } else {
            println("Results found in history5G: ${_history5G.value.size}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun getSimData(context: Context, simIndex: Int): SimData {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val subscriptionManager =
            context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        val subscriptionInfoList = subscriptionManager.activeSubscriptionInfoList

        if (subscriptionInfoList.size > simIndex) {
            val subscriptionId = subscriptionInfoList[simIndex].subscriptionId
            val telephonyManagerForSim = tm.createForSubscriptionId(subscriptionId)
            val cellInfoList = telephonyManagerForSim.allCellInfo

            val cellInfo = cellInfoList.firstOrNull()

            // ตรวจสอบข้อมูลของ cellInfo
            println("cellInfo: $cellInfo")

            return when (cellInfo) {
                is CellInfoLte -> {
                    val lte = cellInfo.cellIdentity as? CellIdentityLte
                    val strength = cellInfo.cellSignalStrength
                    val frequency = earfcnToFrequencyLte(lte!!.earfcn)
                    val bandwidth =
                        frequencyToBandwidthLte(frequency).replace(" MHz", "").toDoubleOrNull()
                            ?: 0.0

                    // ตรวจสอบข้อมูลของ LTE
                    println("LTE Info: ci=${lte.ci}, mcc=${lte.mccString}, mnc=${lte.mncString}, earfcn=${lte.earfcn}")
                    println("Signal Strength: dbm=${strength.dbm}, rsrp=${strength.rsrp}, rsrq=${strength.rsrq}, sinr=${strength.rssnr}")

                    SimData(
                        networkType = "4G",
                        cellId = lte.ci.toLong(),
                        plmn = "${lte.mccString}${lte.mncString}",
                        bandInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            lte.bands.joinToString()
                        } else {
                            "Unknown"
                        },
                        tac = lte.tac,
                        pci = lte.pci,
                        bandwidth = bandwidth,
                        frequency = frequency,
                        rssi = strength.dbm,
                        rsrp = strength.rsrp,
                        rsrq = strength.rsrq,
                        sinr = strength.rssnr,
                        cqi = strength.cqi.takeIf { it != Int.MAX_VALUE } ?: 0,
                        earfcn = lte.earfcn,
                        is5G = false
                    )
                }

                is CellInfoNr -> {
                    val nr = cellInfo.cellIdentity as CellIdentityNr
                    val strength = cellInfo.cellSignalStrength as CellSignalStrengthNr
                    val frequency = nrarfcnToFrequency(nr.nrarfcn)
                    val bandwidth = frequencyToBandwidth(frequency)

                    // ตรวจสอบข้อมูลของ NR
                    println("NR Info: nci=${nr.nci}, mcc=${nr.mccString}, mnc=${nr.mncString}, nrarfcn=${nr.nrarfcn}")
                    println("Signal Strength: ssRsrp=${strength.ssRsrp}, ssRsrq=${strength.ssRsrq}, ssSinr=${strength.ssSinr}")

                    SimData(
                        networkType = "5G",
                        cellId = nr.nci,
                        plmn = "${nr.mccString}${nr.mncString}",
                        bandInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            nr.bands.joinToString()
                        } else {
                            "Unknown"
                        },
                        tac = nr.tac,
                        pci = nr.pci,
                        bandwidth = bandwidth,
                        frequency = frequency,
                        rssi = 0,
                        rsrp = strength.ssRsrp,
                        rsrq = strength.ssRsrq,
                        sinr = strength.ssSinr,
                        cqi = 0,
                        is5G = true,
                        ssrsrp = strength.ssRsrp,
                        ssrsrq = strength.ssRsrq,
                        sssinr = strength.ssSinr,
                        arfcn = nr.nrarfcn
                    )
                }

                else -> SimData.empty()
            }
        } else {
            return SimData.empty()
        }
    }
}

private fun earfcnToFrequencyLte(earfcn: Int): Double {
    return when (earfcn) {
        in 0..599 -> 2110 + 0.1 * (earfcn - 0)        // Band 1
        in 600..1199 -> 1930 + 0.1 * (earfcn - 600)   // Band 2
        in 1200..1949 -> 1850 + 0.1 * (earfcn - 1200) // Band 3
        in 1950..2399 -> 2110 + 0.1 * (earfcn - 1950) // Band 4
        in 2400..2649 -> 869 + 0.1 * (earfcn - 2400)  // Band 5
        in 2650..2749 -> 875 + 0.1 * (earfcn - 2650)  // Band 6
        in 2750..3449 -> 2620 + 0.1 * (earfcn - 2750) // Band 7
        in 3450..3799 -> 925 + 0.1 * (earfcn - 3450)  // Band 8
        in 3800..4149 -> 1844.9 + 0.1 * (earfcn - 3800) // Band 9
        in 4150..4749 -> 2110 + 0.1 * (earfcn - 4150) // Band 10
        in 4750..4949 -> 1475.9 + 0.1 * (earfcn - 4750) // Band 11
        in 5010..5179 -> 729 + 0.1 * (earfcn - 5010)  // Band 12
        in 5180..5279 -> 746 + 0.1 * (earfcn - 5180)  // Band 13
        in 5280..5379 -> 758 + 0.1 * (earfcn - 5280)  // Band 14
        in 5730..5849 -> 734 + 0.1 * (earfcn - 5730)  // Band 17
        in 5850..5999 -> 734 + 0.1 * (earfcn - 5850)  // Band 18
        in 6000..6149 -> 734 + 0.1 * (earfcn - 6000)  // Band 19
        in 6150..6449 -> 734 + 0.1 * (earfcn - 6150)  // Band 20
        in 6450..6599 -> 734 + 0.1 * (earfcn - 6450)  // Band 21
        in 6600..7399 -> 734 + 0.1 * (earfcn - 6600)  // Band 22
        in 7500..7699 -> 1452.0 + 0.1 * (earfcn - 7500) // Band 23
        in 7700..8039 -> 3450 + 0.1 * (earfcn - 7700) // Band 24
        in 8040..8690 -> 462.5 + 0.1 * (earfcn - 8040) // Band 25
        in 8691..9341 -> 1475.0 + 0.1 * (earfcn - 8691) // Band 26
        in 9342..9990 -> 703 + 0.1 * (earfcn - 9342) // Band 27
        in 10000..10359 -> 2300 + 0.1 * (earfcn - 10000) // Band 28
        in 10360..10835 -> 2350 + 0.1 * (earfcn - 10360) // Band 29
        in 10836..11085 -> 2655 + 0.1 * (earfcn - 10836) // Band 30
        in 11086..11735 -> 5990 + 0.1 * (earfcn - 11086) // Band 31
        in 11736..12385 -> 734 + 0.1 * (earfcn - 11736) // Band 32
        in 12386..13035 -> 734 + 0.1 * (earfcn - 12386) // Band 33
        in 13036..13685 -> 734 + 0.1 * (earfcn - 13036) // Band 34
        in 13686..14335 -> 734 + 0.1 * (earfcn - 13686) // Band 35
        in 14336..14985 -> 734 + 0.1 * (earfcn - 14336) // Band 36
        in 14986..15635 -> 734 + 0.1 * (earfcn - 14986) // Band 37
        in 15636..16285 -> 734 + 0.1 * (earfcn - 15636) // Band 38
        in 16286..16935 -> 734 + 0.1 * (earfcn - 16286) // Band 39
        in 16936..17585 -> 734 + 0.1 * (earfcn - 16936) // Band 40
        in 17586..18235 -> 734 + 0.1 * (earfcn - 17586) // Band 41
        in 18236..18885 -> 734 + 0.1 * (earfcn - 18236) // Band 42
        in 18886..19535 -> 734 + 0.1 * (earfcn - 18886) // Band 43
        in 19536..20185 -> 734 + 0.1 * (earfcn - 19536) // Band 44
        in 20186..20835 -> 734 + 0.1 * (earfcn - 20186) // Band 45
        in 20836..21485 -> 734 + 0.1 * (earfcn - 20836) // Band 46
        in 21486..22135 -> 734 + 0.1 * (earfcn - 21486) // Band 47
        in 22136..22785 -> 734 + 0.1 * (earfcn - 22136) // Band 48
        in 22786..23435 -> 734 + 0.1 * (earfcn - 22786) // Band 49
        in 23436..24085 -> 734 + 0.1 * (earfcn - 23436) // Band 50
        in 24086..24735 -> 734 + 0.1 * (earfcn - 24086) // Band 51
        in 24736..25385 -> 734 + 0.1 * (earfcn - 24736) // Band 52
        in 25386..26035 -> 734 + 0.1 * (earfcn - 25386) // Band 53
        in 26036..26685 -> 734 + 0.1 * (earfcn - 26036) // Band 54
        in 26686..27335 -> 734 + 0.1 * (earfcn - 26686) // Band 55
        in 27336..27985 -> 734 + 0.1 * (earfcn - 27336) // Band 56
        in 27986..28635 -> 734 + 0.1 * (earfcn - 27986) // Band 57
        in 28636..29285 -> 734 + 0.1 * (earfcn - 28636) // Band 58
        in 29286..29935 -> 734 + 0.1 * (earfcn - 29286) // Band 59
        in 29936..30585 -> 734 + 0.1 * (earfcn - 29936) // Band 60
        in 30586..31235 -> 734 + 0.1 * (earfcn - 30586) // Band 61
        in 31236..31885 -> 734 + 0.1 * (earfcn - 31236) // Band 62
        in 31886..32535 -> 734 + 0.1 * (earfcn - 31886) // Band 63
        in 32536..33185 -> 734 + 0.1 * (earfcn - 32536) // Band 64
        in 33186..33835 -> 734 + 0.1 * (earfcn - 33186) // Band 65
        in 33836..34485 -> 734 + 0.1 * (earfcn - 33836) // Band 66
        in 34486..35135 -> 734 + 0.1 * (earfcn - 34486) // Band 67
        in 35136..35785 -> 734 + 0.1 * (earfcn - 35136) // Band 68
        in 35786..36435 -> 734 + 0.1 * (earfcn - 35786) // Band 69
        in 36436..37085 -> 734 + 0.1 * (earfcn - 36436) // Band 70
        in 37086..37735 -> 734 + 0.1 * (earfcn - 37086) // Band 71
        in 37736..38385 -> 734 + 0.1 * (earfcn - 37736) // Band 72
        in 38386..39035 -> 734 + 0.1 * (earfcn - 38386) // Band 73
        in 39036..39685 -> 734 + 0.1 * (earfcn - 39036) // Band 74
        in 39686..40335 -> 734 + 0.1 * (earfcn - 39686) // Band 75
        in 40336..40985 -> 734 + 0.1 * (earfcn - 40336) // Band 76
        in 40986..41635 -> 734 + 0.1 * (earfcn - 40986) // Band 77
        in 41636..42285 -> 734 + 0.1 * (earfcn - 41636) // Band 78
        in 42286..42935 -> 734 + 0.1 * (earfcn - 42286) // Band 79
        in 42936..43585 -> 734 + 0.1 * (earfcn - 42936) // Band 80
        in 43586..44235 -> 734 + 0.1 * (earfcn - 43586) // Band 81
        in 44236..44885 -> 734 + 0.1 * (earfcn - 44236) // Band 82
        in 44886..45535 -> 734 + 0.1 * (earfcn - 44886) // Band 83
        in 45536..46185 -> 734 + 0.1 * (earfcn - 45536) // Band 84
        in 46186..46835 -> 734 + 0.1 * (earfcn - 46186) // Band 85
        in 46836..47485 -> 734 + 0.1 * (earfcn - 46836) // Band 86
        in 47486..48135 -> 734 + 0.1 * (earfcn - 47486) // Band 87
        in 48136..48785 -> 734 + 0.1 * (earfcn - 48136) // Band 88
        in 48786..49435 -> 734 + 0.1 * (earfcn - 48786) // Band 89
        in 49436..50085 -> 734 + 0.1 * (earfcn - 49436) // Band 90
        in 50086..50735 -> 734 + 0.1 * (earfcn - 50086) // Band 91
        in 50736..51385 -> 734 + 0.1 * (earfcn - 50736) // Band 92
        else -> throw IllegalArgumentException("Invalid EARFCN value: $earfcn")
    }
}

private fun frequencyToBandwidthLte(frequency: Double): String {
    return when {
        frequency in 2110.0..2170.0 -> "60 MHz"   // LTE Band 1
        frequency in 1930.0..1990.0 -> "60 MHz"   // LTE Band 2
        frequency in 1805.0..1880.0 -> "75 MHz"   // LTE Band 3
        frequency in 2110.0..2155.0 -> "45 MHz"   // LTE Band 4
        frequency in 869.0..894.0 -> "25 MHz"    // LTE Band 5
        frequency in 875.0..885.0 -> "10 MHz"    // LTE Band 6
        frequency in 2620.0..2690.0 -> "70 MHz"   // LTE Band 7
        frequency in 925.0..960.0 -> "35 MHz"    // LTE Band 8
        frequency in 1844.9..1879.9 -> "35 MHz"   // LTE Band 9
        frequency in 2110.0..2170.0 -> "60 MHz"   // LTE Band 10
        frequency in 1475.9..1500.9 -> "20 MHz"   // LTE Band 11
        frequency in 728.0..746.0 -> "18 MHz"    // LTE Band 12
        frequency in 746.0..756.0 -> "10 MHz"    // LTE Band 13
        frequency in 758.0..768.0 -> "10 MHz"    // LTE Band 14
        frequency in 2600.0..2620.0 -> "20 MHz"   // LTE Band 15
        frequency in 2585.0..2600.0 -> "15 MHz"   // LTE Band 16
        frequency in 734.0..746.0 -> "12 MHz"    // LTE Band 17
        frequency in 860.0..875.0 -> "15 MHz"    // LTE Band 18
        frequency in 875.0..890.0 -> "15 MHz"    // LTE Band 19
        frequency in 791.0..821.0 -> "30 MHz"    // LTE Band 20
        frequency in 1495.5..1510.9 -> "15 MHz"   // LTE Band 21
        frequency in 3510.0..3600.0 -> "90 MHz"   // LTE Band 22
        frequency in 2180.0..2200.0 -> "20 MHz"   // LTE Band 23
        frequency in 1525.0..1559.0 -> "34 MHz"   // LTE Band 24
        frequency in 1930.0..1995.0 -> "65 MHz"   // LTE Band 25
        frequency in 859.0..894.0 -> "30/40 MHz"  // LTE Band 26
        frequency in 852.0..869.0 -> "17 MHz"    // LTE Band 27
        frequency in 758.0..803.0 -> "45 MHz"    // LTE Band 28
        frequency in 717.0..728.0 -> "11 MHz"    // LTE Band 29 (SDL)
        frequency in 2350.0..2360.0 -> "10 MHz"   // LTE Band 30
        frequency in 462.5..467.5 -> "5 MHz"     // LTE Band 31
        frequency in 1452.0..1496.0 -> "44 MHz"   // LTE Band 32 (SDL)
        frequency in 2110.0..2200.0 -> "90 MHz"   // LTE Band 65
        frequency in 2110.0..2200.0 -> "70/90 MHz" // LTE Band 66
        frequency in 738.0..758.0 -> "20 MHz"    // LTE Band 67 (SDL)
        frequency in 753.0..783.0 -> "30 MHz"    // LTE Band 68
        frequency in 2570.0..2620.0 -> "50 MHz"   // LTE Band 69 (SDL)
        frequency in 1995.0..2020.0 -> "15/25 MHz" // LTE Band 70
        frequency in 617.0..652.0 -> "35 MHz"    // LTE Band 71
        frequency in 461.0..466.0 -> "5 MHz"     // LTE Band 72
        frequency in 460.0..465.0 -> "5 MHz"     // LTE Band 73
        frequency in 1475.0..1518.0 -> "43 MHz"   // LTE Band 74
        frequency in 1432.0..1517.0 -> "85 MHz"   // LTE Band 75 (SDL)
        frequency in 1427.0..1432.0 -> "5 MHz"    // LTE Band 76 (SDL)
        frequency in 728.0..746.0 -> "18 MHz"    // LTE Band 85
        frequency in 420.0..425.0 -> "5 MHz"     // LTE Band 87
        frequency in 422.0..427.0 -> "5 MHz"     // LTE Band 88
        frequency in 757.0..758.0 -> "1 MHz"     // LTE Band 103
        frequency in 835.0..840.0 -> "5 MHz"     // LTE Band 106
        else -> "Unknown"
    }
}

private fun nrarfcnToFrequency(nrarfcn: Int): Double {
    return when (nrarfcn) {
        in 0..599999 -> 5 * nrarfcn / 1000.0 // FR1
        in 600000..2016666 -> 5 * nrarfcn / 1000.0 + 24250 // FR2
        else -> throw IllegalArgumentException("Invalid NRARFCN value: $nrarfcn")
    }
}

private fun frequencyToBandwidth(frequency: Double): Double {
    return when {
        frequency in 2500.0..2600.0 -> 60.0
        frequency in 700.0..800.0 -> 10.0
        else -> throw IllegalArgumentException("Unknown frequency: $frequency MHz")
    }
}

@Composable
fun CellularScreen(viewModel: CellularViewModel = viewModel()) {
    val context = LocalContext.current

    val hasPermissions by viewModel.hasPermissions
    val sim1Data by viewModel.sim1Data
    val sim2Data by viewModel.sim2Data

    var showDialog by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            viewModel.onPermissionsResult(permissions, context)
        }
    )

    LaunchedEffect(Unit) {
        if (!hasPermissions) {
            launcher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.READ_PHONE_STATE,
                )
            )
        } else {
            // เริ่มดึงค่าทุก 2 วินาทีเมื่อยังไม่มีการเปิด Dialog
            if (!showDialog) {
                viewModel.startFetching(context)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopFetching()
        }
    }

    // Layout หลัก
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            if (sim1Data.networkType != "N/A") {
                NetworkInfoCard(sim1Data)
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (sim2Data.networkType != "N/A") {
                NetworkInfoCard(sim2Data)
            }
        }

        // ปุ่มเปิดแมพจัดตำแหน่งอยู่มุมขวาล่าง
        Button(
            onClick = {
                showDialog = true
                // หยุดการดึงค่าทุก 2 วินาทีเมื่อเปิด Dialog
                viewModel.stopFetching()
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Text("Show Map")
        }

        // แสดง Dialog เมื่อ showDialog เป็น true
        if (showDialog) {
            MapDialog(
                onDismiss = {
                    showDialog = false
                    // กลับไปดึงค่าทุก 2 วินาทีเมื่อปิด Dialog
                    viewModel.startFetching(context)
                },
                sim1Data = sim1Data,
                sim2Data = sim2Data
            )
        }
    }
}

@Composable
fun NetworkInfoGridItem(label: String, value: String, backgroundColor: Color = Color.Transparent) {
    Row(
        verticalAlignment = Alignment.CenterVertically, // จัดแนวตรงกลางให้ข้อมูลตรงกัน
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )

        Box(
            modifier = Modifier
                .background(backgroundColor, shape = RoundedCornerShape(50)) // วงรี
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .align(Alignment.CenterVertically) // จัดตัวเลขให้อยู่ตรงกลาง
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White // สีตัวเลขเป็นสีขาว
            )
        }
    }
}

@Composable
fun NetworkInfoCard(data: SimData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F0F0)) // สีพื้นหลังของ Card ตามภาพ
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${data.networkType} ",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // ข้อมูลซิม เช่น PLMN, RSRP, SINR
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    NetworkInfoGridItem(label = "PLMN", value = data.plmn)
                    NetworkInfoGridItem(label = "Band Info", value = data.bandInfo)
                    NetworkInfoGridItem(label = "BW", value = "${data.bandwidth} MHz")
                    NetworkInfoGridItem(label = "TAC", value = data.tac.toString())
                    NetworkInfoGridItem(label = "PCI", value = data.pci.toString())
                    NetworkInfoGridItem(label = "Cell ID", value = data.cellId.toString())
                    NetworkInfoGridItem(label = "EARFCN/ARFCN", value = if (data.is5G) data.arfcn.toString() else data.earfcn.toString())
                    NetworkInfoGridItem(label = "Frequency", value = "${data.frequency} MHz")
                }

                Spacer(modifier = Modifier.width(16.dp)) // เพิ่มที่ว่างระหว่างคอลัมน์

                Column(modifier = Modifier.weight(1f)) {
                    if (data.is5G) {
                        NetworkInfoGridItem(label = "SS RSRP", value = data.ssrsrp.toString(), backgroundColor = getSSRsrpColor(data.ssrsrp))
                        NetworkInfoGridItem(label = "SS RSRQ", value = data.ssrsrq.toString())
                        NetworkInfoGridItem(label = "SS SNR", value = data.sssinr.toString(), backgroundColor = getSSSinrColor(data.sssinr))
                    } else {
                        NetworkInfoGridItem(label = "RSSI", value = data.rssi.toString())
                        NetworkInfoGridItem(label = "RSRP", value = data.rsrp.toString(), backgroundColor = getRsrpColor(data.rsrp))
                        NetworkInfoGridItem(label = "RSRQ", value = data.rsrq.toString())
                        NetworkInfoGridItem(label = "SNR", value = data.sinr.toString(), backgroundColor = getSinrColor(data.sinr))
                        NetworkInfoGridItem(label = "CQI", value = data.cqi.toString())
                    }
                }
            }
        }
    }
}



private fun getRsrpColor(rsrp: Int): Color {
    return when {
        rsrp >= -94 -> Color(0xFF006400) // Color2
        rsrp in -98 until -94 -> Color(0xFF00FF00) // Color6
        rsrp in -102 until -98 -> Color(0xFFFFFF00) // Color3
        rsrp in -105 until -102 -> Color(0xFF06ADF1) // Color7
        rsrp in -109 until -105 -> Color(0xFF1438FF) // Color8
        rsrp in -113 until -109 -> Color(0xFFFF00FF) // Color4
        rsrp < -113 -> Color(0xFFFF0000) // Color5
        else -> Color.Gray
    }
}

private fun getSinrColor(sinr: Int): Color {
    return when {
        sinr >= 20 -> Color(0xFF006400) // Color2
        sinr in 10 until 20 -> Color(0xFF00FF00) // Color6
        sinr in 3 until 10 -> Color(0xFFFFFF00) // Color3
        sinr in 0 until 3 -> Color(0xFF06ADF1) // Color7
        sinr in -3 until 0 -> Color(0xFFFF00FF) // Color4
        sinr < -3 -> Color(0xFFFF0000) // Color5
        else -> Color.Gray
    }
}

private fun getSSRsrpColor(ssrsrp: Int): Color {
    return when {
        ssrsrp >= -65 -> Color(0xFF1438FF) // Color8
        ssrsrp in -85 until -65 -> Color(0xFF006400) // Color2
        ssrsrp in -95 until -85 -> Color(0xFF00FF00) // Color6
        ssrsrp in -105 until -95 -> Color(0xFFFFFF00) // Color3
        ssrsrp in -115 until -105 -> Color(0xFFFF5722) // color10
        ssrsrp in -125 until -115 -> Color(0xFFFF0000) // Color5
        ssrsrp < -125 -> Color(0xFF671B1B) // color11
        else -> Color.Gray
    }
}

private fun getSSSinrColor(sssinr: Int): Color {
    return when {
        sssinr >= 25 -> Color(0xFF006400) // Color2
        sssinr in 16 until 25 -> Color(0xFF00FF00) // Color6
        sssinr in 10 until 16 -> Color(0xFFFFFF00) // Color3
        sssinr in 3 until 10 -> Color(0xFFFF5722) // color10
        sssinr in -23 until 3 -> Color(0xFFFF0000) // Color5
        else -> Color.Gray
    }
}

@Composable
fun NetworkInfoGridItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
    }
}

data class SimData(
    val networkType: String,
    val cellId: Long,
    val plmn: String,
    val bandInfo: String,
    val tac: Int,
    val pci: Int,
    val bandwidth: Double,
    val frequency: Double,
    val rssi: Int,
    val rsrp: Int,
    val rsrq: Int,
    val sinr: Int,
    val cqi: Int,
    val earfcn: Int = 0,
    val is5G: Boolean = false,
    val arfcn: Int = 0,
    val ssrsrp: Int = 0,
    val ssrsrq: Int = 0,
    val sssinr: Int = 0
) {
    companion object {
        fun empty() = SimData(
            networkType = "N/A",
            cellId = 0,
            plmn = "N/A",
            bandInfo = "N/A",
            tac = 0,
            pci = 0,
            bandwidth = 0.0,
            frequency = 0.0,
            rssi = 0,
            rsrp = 0,
            rsrq = 0,
            sinr = 0,
            cqi = 0
        )
    }
}

@Composable
fun MapDialog(
    onDismiss: () -> Unit,
    sim1Data: SimData,
    sim2Data: SimData
) {
    val context = LocalContext.current
    var latitude1 by remember { mutableStateOf(0.0) }
    var longitude1 by remember { mutableStateOf(0.0) }
    var latitude2 by remember { mutableStateOf(0.0) }
    var longitude2 by remember { mutableStateOf(0.0) }

    // Fetch the location for both SIMs
    LaunchedEffect(sim1Data, sim2Data) {
        // ตรวจสอบข้อมูล sim1Data
        if (sim1Data.plmn != "N/A" && sim1Data.tac > 0 && sim1Data.cellId > 0) {
            Log.d("MapDialog", "Fetching location for SIM1")
            Log.d("MapDialog", "SIM1 Data: plmn=${sim1Data.plmn}, tac=${sim1Data.tac}, cellId=${sim1Data.cellId}")

            fetchLocationFromCellInfo(
                sim1Data.plmn.take(3).toIntOrNull() ?: 0,
                sim1Data.plmn.drop(3).toIntOrNull() ?: 0,
                sim1Data.tac,
                sim1Data.cellId.toInt(),
                if (sim1Data.is5G) "nr" else "lte"
            ) { lat, lon ->
                latitude1 = lat
                longitude1 = lon
                Log.d("MapDialog", "SIM1 location: lat=$lat, lon=$lon")
            }
        } else {
            Log.e("MapDialog", "Invalid sim1Data for fetching location")
        }

        // ตรวจสอบข้อมูล sim2Data
        if (sim2Data.plmn != "N/A" && sim2Data.tac > 0 && sim2Data.cellId > 0) {
            Log.d("MapDialog", "Fetching location for SIM2")
            Log.d("MapDialog", "SIM2 Data: plmn=${sim2Data.plmn}, tac=${sim2Data.tac}, cellId=${sim2Data.cellId}")

            fetchLocationFromCellInfo(
                sim2Data.plmn.take(3).toIntOrNull() ?: 0,
                sim2Data.plmn.drop(3).toIntOrNull() ?: 0,
                sim2Data.tac,
                sim2Data.cellId.toInt(),
                if (sim2Data.is5G) "nr" else "lte"
            ) { lat, lon ->
                latitude2 = lat
                longitude2 = lon
                Log.d("MapDialog", "SIM2 location: lat=$lat, lon=$lon")
            }
        } else {
            Log.e("MapDialog", "Invalid sim2Data for fetching location")
            Log.d("MapDialog", "sim2Data details: plmn=${sim2Data.plmn}, tac=${sim2Data.tac}, cellId=${sim2Data.cellId}")
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.60f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.LightGray)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Cell Tower Locations",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .height(300.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    GoogleMapView(
                        latitude1 = if (latitude1 != 0.0) latitude1 else null,
                        longitude1 = if (longitude1 != 0.0) longitude1 else null,
                        latitude2 = if (latitude2 != 0.0) latitude2 else null,
                        longitude2 = if (longitude2 != 0.0) longitude2 else null
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun GoogleMapView(
    latitude1: Double?,
    longitude1: Double?,
    latitude2: Double?,
    longitude2: Double?
) {
    // Initialize the camera position state
    val cameraPositionState = rememberCameraPositionState {
        if (latitude1 != null && longitude1 != null) {
            position = CameraPosition(LatLng(latitude1, longitude1), 15f, 0f, 0f)
        }
    }

    // Render the GoogleMap Composable
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        // Add marker for SIM1 location
        if (latitude1 != null && longitude1 != null) {
            val position1 = LatLng(latitude1, longitude1)
            Marker(
                state = MarkerState(position = position1),
                title = "SIM1 Location"
            )
            // Animate the camera to SIM1's location
            LaunchedEffect(Unit) {
                cameraPositionState.animate(CameraUpdateFactory.newLatLng(position1))
            }
        }

        // Add marker for SIM2 location
        if (latitude2 != null && longitude2 != null) {
            val position2 = LatLng(latitude2, longitude2)
            Marker(
                state = MarkerState(position = position2),
                title = "SIM2 Location"
            )
        }
    }
}

// ปรับฟังก์ชัน fetchLocationFromCellInfo ให้รองรับ callback เพื่ออัปเดตตำแหน่ง
private fun fetchLocationFromCellInfo(
    mcc: Int,
    mnc: Int,
    tac: Int,
    cellId: Int,
    radioType: String,
    onLocationFetched: (Double, Double) -> Unit
) {
    val url = "https://us1.unwiredlabs.com/v2/process.php"

    val jsonBody = JSONObject().apply {
        val unwiredApiKey = "pk.ce9eb88eb6b84a873ff7de9fa6d16901"
        put("token", unwiredApiKey)
        put("radio", radioType)
        put("mcc", mcc)
        put("mnc", mnc)
        put("cells", JSONArray().apply {
            put(JSONObject().apply {
                put("tac", tac)
                put("cid", cellId)
            })
        })
        put("address", 1)
    }

    val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
    val request = Request.Builder()
        .url(url)
        .post(requestBody)
        .build()

    val client = OkHttpClient()
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("NetworkError", "Failed to fetch location: ${e.message}")
        }

        override fun onResponse(call: Call, response: Response) {
            if (!response.isSuccessful) {
                Log.e("NetworkError", "Response unsuccessful: ${response.code} - ${response.body?.string()}")
                return
            }

            response.body?.string()?.let {
                try {
                    val jsonResponse = JSONObject(it)
                    if (jsonResponse.getString("status") == "ok") {
                        val latitude = jsonResponse.getDouble("lat")
                        val longitude = jsonResponse.getDouble("lon")
                        onLocationFetched(latitude, longitude) // ส่งค่าพิกัดไปยัง callback
                    } else {
                        Log.e("ResponseError", "Status not OK: ${jsonResponse.getString("status")}")
                    }
                } catch (e: JSONException) {
                    Log.e("ResponseError", "Failed to parse location response", e)
                }
            }
        }
    })
}


@Preview(showBackground = true)
@Composable
fun CellularScreenPreview() {
    SignalXpertTheme {
        CellularScreen()
    }
}