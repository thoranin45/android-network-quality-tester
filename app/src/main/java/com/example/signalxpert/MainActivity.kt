package com.example.signalxpert

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.dedotest.screen.VideoTestScreen
import com.example.signalxpert.data.TestResult
import com.example.signalxpert.nav.AboutScreen
import com.example.signalxpert.nav.ResultsScreen
import com.example.signalxpert.nav.SignalQualityScreenLTE
import com.example.signalxpert.nav.SignalQualityScreenNR
import com.example.signalxpert.nav.TestResultDetailScreen
import com.example.signalxpert.ui.theme.SignalXpertTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SignalXpertTheme {
                MainApp()
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    val bottomNavController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val speedTestViewModel: SpeedTestViewModel = viewModel()
    val cellularViewModel: CellularViewModel = viewModel()

    // Modal DrawerSheet
    ModalNavigationDrawer(
        drawerContent = { DrawerMenu(bottomNavController, drawerState, scope) },
        drawerState = drawerState,
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("SignalXpert") },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = null)
                        }
                    }
                )
            },
            bottomBar = { BottomNavigationBar(bottomNavController) }
        ) { innerPadding ->
            NavHost(
                navController = bottomNavController,
                startDestination = "home",
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                    composable("home") { SpeedTestScreen(viewModel = speedTestViewModel) }
                    composable("cellular") { CellularScreen(viewModel = cellularViewModel) }
                    composable("video") { VideoTestScreen() }
                    composable("browsing") {BrowsingTestScreen() }
                    composable("map") { /* Your Map Screen Composable */ }
                }
            }
        }
    }


@Composable
fun DrawerMenu(navController: NavHostController, drawerState: DrawerState, scope: CoroutineScope) {
    var showResultsScreen by remember { mutableStateOf(false) }
    var showAboutScreen by remember { mutableStateOf(false) }
    var selectedResult by remember { mutableStateOf<TestResult?>(null) }
    var selectedSimData by remember { mutableStateOf<SimData?>(null) }

    ModalDrawerSheet(
        modifier = Modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)) // ทำให้มุมโค้ง
            .background(Color.White)
    ) {
        // ส่วนของหน้าแรก
        AnimatedVisibility(
            visible = !showResultsScreen && !showAboutScreen,
            enter = fadeIn(animationSpec = tween(300)), // ใช้ fadeIn แบบสมูท
            exit = fadeOut(animationSpec = tween(300)),  // ใช้ fadeOut แบบสมูท
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White) // เพิ่มพื้นหลังสีเทาระหว่างการข้ามหน้า
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                // Items in the Drawer
                DrawerItem("ผลลัพธ์", onClick = { showResultsScreen = true })
                DrawerItem("การตั้งค่า", onClick = { /* เปิดหน้าการตั้งค่า */ })
                DrawerItem("เกี่ยวกับ", onClick = { showAboutScreen = true })
                DrawerItem("สนันสนุน", onClick = { /* เปิดหน้าสนับสนุน */ })

                Spacer(modifier = Modifier.weight(1f)) // ทำให้เว้นที่ด้านล่าง

                // Add images at the bottom of the drawer
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Row to arrange ZTE and AIS logos side by side
                    Row(
                        horizontalArrangement = Arrangement.spacedBy((-10).dp), // ตั้งค่าให้เกิดการซ้อนทับ
                        modifier = Modifier.padding(
                            horizontal = 0.dp,
                            vertical = 0.dp
                        ) // ลด padding รอบโลโก้
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.zte_logo_svg), // ใส่รูปภาพ ZTE
                            contentDescription = "ZTE",
                            modifier = Modifier
                                .size(80.dp)
                                .offset(x = (-10).dp) // เลื่อนให้ชิดด้านซ้าย
                        )
                        Image(
                            painter = painterResource(id = R.drawable.ais_logo), // ใส่รูปภาพ AIS
                            contentDescription = "AIS",
                            modifier = Modifier
                                .size(90.dp)
                                .offset(x = 10.dp) // เลื่อนให้ชิดด้านขวา
                        )
                    }

                    // SUT logo below the ZTE and AIS logos
                    Image(
                        painter = painterResource(id = R.drawable.sut_engineering_eng_removebg), // ใส่รูปภาพ SUT
                        contentDescription = "SUT",
                        modifier = Modifier
                            .size(150.dp)
                            .offset(y = (-60).dp) // ซ้อนทับกับโลโก้ด้านบน
                    )
                }
            }
        }

        // ส่วนของหน้าผลลัพธ์
        AnimatedVisibility(
            visible = showResultsScreen && selectedResult == null && selectedSimData == null,
            enter = fadeIn(animationSpec = tween(300)), // ใช้ fadeIn แบบสมูท
            exit = fadeOut(animationSpec = tween(300)),  // ใช้ fadeOut แบบสมูท
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White) // เพิ่มพื้นหลังสีเทาระหว่างการข้ามหน้า
        ) {
            ResultsScreen(
                navController = navController,
                onBackPressed = { showResultsScreen = false },
                onItemClick = { result ->
                    selectedResult = result
                },
                onSimDataClick = { simData ->
                    selectedSimData = simData
                }
            )
        }

// ส่วนของหน้าแสดงผลลัพธ์ที่เลือก
        AnimatedVisibility(
            visible = selectedResult != null,
            enter = fadeIn(animationSpec = tween(300)), // ใช้ fadeIn แบบสมูท
            exit = fadeOut(animationSpec = tween(300)),  // ใช้ fadeOut แบบสมูท
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White) // เพิ่มพื้นหลังสีขาวระหว่างการข้ามหน้า
        ) {
            selectedResult?.let { result ->
                TestResultDetailScreen(
                    result = result,
                    onBackPressed = { selectedResult = null }
                )
            }
        }

// ส่วนของการแสดง SimData
        AnimatedVisibility(
            visible = selectedSimData != null,
            enter = fadeIn(animationSpec = tween(300)), // ใช้ fadeIn แบบสมูท
            exit = fadeOut(animationSpec = tween(300)),  // ใช้ fadeOut แบบสมูท
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White) // เพิ่มพื้นหลังสีขาว
        ) {
            selectedSimData?.let { simData ->
                if (simData.is5G) {
                    SignalQualityScreenNR(
                        result = simData,
                        onBackPressed = { selectedSimData = null }
                    )
                } else {
                    SignalQualityScreenLTE(
                        result = simData,
                        onBackPressed = { selectedSimData = null }
                    )
                }
            }
        }

// ส่วนของหน้าเกี่ยวกับ
        AnimatedVisibility(
            visible = showAboutScreen,
            enter = fadeIn(animationSpec = tween(300)), // ใช้ fadeIn แบบสมูท
            exit = fadeOut(animationSpec = tween(300)),  // ใช้ fadeOut แบบสมูท
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White) // เพิ่มพื้นหลังสีขาว
        ) {
            AboutScreen(
                onBackPressed = { showAboutScreen = false }
            )
        }
    }
    LaunchedEffect(drawerState.isClosed) {
        if (drawerState.isClosed) {
            showResultsScreen = false
            showAboutScreen = false
            selectedResult = null
            selectedSimData = null
        }
    }
}

@Composable
fun DrawerItem(title: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(text = title, modifier = Modifier.padding(16.dp))
    }
    Divider()
}

// BottomNavigationBar for main screen navigation
@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        NavItem("home", "Speed Test", R.drawable.baseline_speed_24),
        NavItem("cellular", "Cellular", R.drawable.baseline_cell_tower_24),
        NavItem("video", "Video", R.drawable.baseline_slow_motion_video_24),
        NavItem("browsing", "Browsing", R.drawable.baseline_language_24),
    )

    val currentRoute = navController.currentBackStackEntryAsState()?.value?.destination?.route
    Column {
        // เพิ่มเส้น Divider ด้านบนของ BottomNavigationBar
        Divider(color = Color.Gray, thickness = 2.dp)
        NavigationBar(
            containerColor = Color(0xFFF8F9FA), // พื้นหลังสีอ่อน
            contentColor = Color(0xFF999999),   // สีเริ่มต้น
            modifier = Modifier.height(70.dp)   // ปรับขนาดความสูง
        ) {
            items.forEach { item ->
                NavigationBarItem(
                    icon = {
                        Icon(
                            painter = painterResource(id = item.icon),
                            contentDescription = item.title,
                            modifier = Modifier.size(24.dp), // ปรับขนาดไอคอน
                            tint = if (currentRoute == item.route) Color(0xFFFF6B00) else Color(
                                0xFFCCCCCC
                            ) // สีที่เลือกและสีที่ไม่ได้เลือก
                        )
                    },
                    label = {
                        Text(
                            text = item.title,
                            color = if (currentRoute == item.route) Color(0xFFFF6B00) else Color(
                                0xFFCCCCCC
                            ), // สีของข้อความ
                            fontWeight = if (currentRoute == item.route) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp // ปรับขนาดข้อความ
                        )
                    },
                    selected = currentRoute == item.route,
                    onClick = {
                        navController.navigate(item.route) {
                            navController.graph.startDestinationRoute?.let { route ->
                                popUpTo(route) { saveState = true }
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}
// Data class for navigation items
data class NavItem(val route: String, val title: String, val icon: Int)

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    SignalXpertTheme {
        MainApp()
    }
}
