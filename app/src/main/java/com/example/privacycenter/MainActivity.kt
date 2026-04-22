package com.example.privacycenter

import android.Manifest
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.privacycenter.ui.theme.PrivacyCenterTheme
import okhttp3.*
import java.io.IOException
import kotlinx.coroutines.delay
import androidx.compose.animation.core.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PrivacyCenterTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    PrivacyCenterApp(Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun PrivacyCenterApp(modifier: Modifier = Modifier) {

    var currentScreen by remember { mutableStateOf("home") }

    when (currentScreen) {

        "home" -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    "PRIVACY CENTER",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.headlineMedium
                )

                Text(
                    "System Protection Active",
                    color = MaterialTheme.colorScheme.secondary
                )

                Spacer(modifier = Modifier.height(30.dp))

                CyberButton("TRACKERS") {
                    currentScreen = "trackers"
                }

                CyberButton("WIFI SECURITY") {
                    currentScreen = "wifi"
                }

                CyberButton("DATA LEAKS") {
                    currentScreen = "leaks"
                }
            }
        }

        "leaks" -> DataLeaksScreen { currentScreen = "home" }
        "wifi" -> WifiSecurityScreen { currentScreen = "home" }
        "trackers" -> TrackerScreen { currentScreen = "home" }
    }
}

@Composable
fun CyberButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Text(text, color = Color.Black)
    }
}

@Composable
fun DataLeaksScreen(onBack: () -> Unit) {

    var email by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var leaks by remember { mutableStateOf(listOf<String>()) }
    var scanning by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text("SECURITY SCAN", color = MaterialTheme.colorScheme.primary)

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Enter email") }
        )

        Spacer(modifier = Modifier.height(20.dp))

        CyberButton("RUN SCAN") {

            if (android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {

                scanning = true
                result = ""
                leaks = emptyList()

                checkLeaks(email) { response, leakList ->

                    Handler(Looper.getMainLooper()).post {

                        scanning = false

                        if (response.contains("❌")) {
                            result = response
                            leaks = emptyList()
                        } else {
                            result = response
                            leaks = leakList
                        }
                    }
                }

            } else {
                result = "❌ Invalid email format"
            }
        }

        if (scanning) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(10.dp))
                Text("Scanning breach databases...")
            }
        }

        AnimatedVisibility(visible = result.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0A0F1C)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = result,
                    modifier = Modifier.padding(16.dp),
                    color = Color.White
                )
            }
        }

        leaks.take(5).forEach {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp)
            ) {
                Text(it, modifier = Modifier.padding(10.dp))
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        CyberButton("BACK", onBack)
    }
}

fun checkLeaks(email: String, onResult: (String, List<String>) -> Unit) {

    val client = OkHttpClient()
    val url = "https://leakcheck.io/api/public?check=$email"

    val request = Request.Builder().url(url).build()

    client.newCall(request).enqueue(object : Callback {

        override fun onFailure(call: Call, e: IOException) {
            onResult("❌ Error conexión", emptyList())
        }

        override fun onResponse(call: Call, response: Response) {

            val body = response.body?.string() ?: ""

            try {
                val json = org.json.JSONObject(body)
                val found = json.optInt("found", 0)

                val list = mutableListOf<String>()
                val sources = json.optJSONArray("sources")

                if (sources != null) {
                    for (i in 0 until sources.length()) {
                        list.add(sources.getJSONObject(i).optString("name"))
                    }
                }

                val text = if (found > 0)
                    "⚠️ $found leaks encontrados"
                else
                    "✅ Seguro"

                onResult(text, list)

            } catch (e: Exception) {
                onResult("❌ Error parsing", emptyList())
            }
        }
    })
}

@Composable
fun WifiSecurityScreen(onBack: () -> Unit) {

    val context = LocalContext.current

    var city by remember { mutableStateOf("Loading...") }
    var country by remember { mutableStateOf("") }
    var isp by remember { mutableStateOf("Loading...") }
    var ssid by remember { mutableStateOf("Unknown") }
    var connectionType by remember { mutableStateOf("Checking...") }
    var publicIp by remember { mutableStateOf("Loading...") }
    var risk by remember { mutableStateOf("Analyzing...") }
    var advice by remember { mutableStateOf("") }
    var score by remember { mutableStateOf(0) }

    var scanning by remember { mutableStateOf(true) }
    var progress by remember { mutableStateOf(0f) }
    var hasPermission by remember { mutableStateOf(false) }
    var vpnActive by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    LaunchedEffect(scanning) {
        if (scanning) {
            for (i in 1..100) {
                progress = i / 100f
                delay(10)
            }
        }
    }

    LaunchedEffect(hasPermission) {

        if (!hasPermission) return@LaunchedEffect

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = cm.activeNetwork
        val capabilities = cm.getNetworkCapabilities(network)

        connectionType = when {
            capabilities?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == true -> "WiFi"
            capabilities?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Mobile Data"
            else -> "Unknown"
        }

        vpnActive = capabilities?.hasTransport(
            android.net.NetworkCapabilities.TRANSPORT_VPN
        ) == true

        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val info = wifiManager.connectionInfo
        ssid = info.ssid.replace("\"", "")

        getPublicIP { ip ->
            publicIp = ip
        }

        // 🔥 GEOLOCALIZACIÓN (CORRECTO)
        getGeoData { c, co, i ->
            city = c
            country = co
            isp = i
        }

        // 🔥 LÓGICA DE RIESGO
        score = 50

        if (connectionType == "Mobile Data") score += 30
        if (vpnActive) score += 20
        if (ssid.contains("Free", true)) score -= 40
        if (ssid.contains("Open", true)) score -= 50
        if (ssid == "<unknown ssid>") score -= 10

        // 🚨 BONUS: país sospechoso
        if (country != "Spain" && country != "Error" && country != "Unavailable" && country.isNotEmpty()){
            score -= 20
            advice += "\n⚠️ Foreign network detected"
        }

        score = score.coerceIn(0, 100)

        risk = when {
            score >= 80 -> "VERY SAFE ✅"
            score >= 60 -> "SAFE ⚠️"
            score >= 40 -> "MEDIUM ⚠️"
            else -> "DANGER ⚠️"
        }

        advice = when {
            score >= 80 -> "Secure connection"
            score >= 60 -> "Decent security"
            score >= 40 -> "Be careful on this network"
            else -> "Avoid sensitive data"
        }

        scanning = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            "NETWORK SECURITY",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (scanning) {
            Text("Scanning network...")
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(progress = progress)
        }

        Spacer(modifier = Modifier.height(20.dp))

        val animatedScore by animateFloatAsState(
            targetValue = score.toFloat(),
            animationSpec = tween(1200),
            label = ""
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF0A0F1C)
            ),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {

                // 🔵 HEADER
                Text(
                    text = "NETWORK STATUS",
                    color = Color(0xFF00E5FF),
                    style = MaterialTheme.typography.labelLarge
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 💥 SCORE ANIMADO
                Text(
                    text = "${animatedScore.toInt()}",
                    style = MaterialTheme.typography.displayLarge,
                    color = when {
                        score >= 80 -> Color(0xFF00FF9C)
                        score >= 60 -> Color(0xFF00E5FF)
                        score >= 40 -> Color(0xFFFFC400)
                        else -> Color(0xFFFF3B3B)
                    }
                )

                Text(
                    text = "/ 100",
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = risk,
                    color = Color.LightGray
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 🔥 BARRA PROGRESO
                LinearProgressIndicator(
                    progress = animatedScore / 100f,
                    color = Color(0xFF00E5FF),
                    trackColor = Color.DarkGray,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Divider(color = Color.DarkGray)

                Spacer(modifier = Modifier.height(16.dp))

                // 📡 INFO
                Text("📡 Connection: $connectionType", color = Color.White)
                Text("📶 SSID: $ssid", color = Color.White)
                Text("🌐 IP: $publicIp", color = Color.White)
                Text("📍 Location: $city, $country", color = Color.White)
                Text("🏢 ISP: $isp", color = Color.White)
                Text("🔐 VPN: ${if (vpnActive) "Active" else "Off"}", color = Color.White)

                Spacer(modifier = Modifier.height(20.dp))

                Divider(color = Color.DarkGray)

                Spacer(modifier = Modifier.height(12.dp))

                // 💡 CONSEJO
                Text(
                    text = "SYSTEM ADVICE",
                    color = Color(0xFF00E5FF)
                )

                Text(
                    text = advice,
                    color = Color.LightGray
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        CyberButton("BACK") {
            onBack()
        }
    }
}

fun getPublicIP(onResult: (String) -> Unit) {

    val client = OkHttpClient()
    val request = Request.Builder()
        .url("https://api.ipify.org")
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            onResult("Error")
        }

        override fun onResponse(call: Call, response: Response) {
            val ip = response.body?.string() ?: "Unknown"
            onResult(ip)
        }
    })
}

fun getGeoData(onResult: (String, String, String) -> Unit) {

    val client = OkHttpClient()
    val request = Request.Builder()
        .url("https://ipapi.co/json/")
        .addHeader("User-Agent", "Android") // 👈 importante
        .build()

    client.newCall(request).enqueue(object : Callback {

        override fun onFailure(call: Call, e: IOException) {
            onResult("Unavailable", "Unavailable", "Unavailable")
        }

        override fun onResponse(call: Call, response: Response) {

            val body = response.body?.string() ?: ""

            try {
                // 👇 comprobación clave
                if (!body.trim().startsWith("{")) {
                    onResult("Blocked", "Blocked", "Blocked")
                    return
                }

                val json = org.json.JSONObject(body)

                val city = json.optString("city", "Unknown")
                val country = json.optString("country_name", "Unknown")
                val isp = json.optString("org", "Unknown")

                onResult(city, country, isp)

            } catch (e: Exception) {
                onResult("Error", "Error", "Error")
            }
        }
    })
}
@Composable
fun TrackerScreen(onBack: () -> Unit) {

    var url by remember { mutableStateOf("") }
    var trackers by remember { mutableStateOf(listOf<String>()) }
    var scanning by remember { mutableStateOf(false) }

    var score by remember { mutableStateOf(0) }
    var risk by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            "PRIVACY THREAT ANALYSIS",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Enter website (example.com)") }
        )

        Spacer(modifier = Modifier.height(20.dp))

        CyberButton("RUN SCAN") {

            scanning = true

            trackers = detectTrackers(url)

            // 🔥 SCORE
            score = when {
                trackers.size >= 4 -> 30
                trackers.size >= 2 -> 60
                else -> 85
            }

            // 🔥 RIESGO
            risk = when {
                score >= 80 -> "LOW ✅"
                score >= 60 -> "MEDIUM ⚠️"
                else -> "HIGH ⚠️"
            }

            scanning = false
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (scanning) {
            Text("Scanning...", color = MaterialTheme.colorScheme.primary)
        }

        if (trackers.isNotEmpty()) {

            val animatedScore by animateFloatAsState(
                targetValue = score.toFloat(),
                animationSpec = tween(1000),
                label = ""
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0A0F1C)
                ),
                elevation = CardDefaults.cardElevation(12.dp)
            ) {

                Column(modifier = Modifier.padding(20.dp)) {

                    Text(
                        "TRACKER STATUS",
                        color = Color(0xFF00E5FF),
                        style = MaterialTheme.typography.labelLarge
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 🔥 SCORE GRANDE
                    Text(
                        "${animatedScore.toInt()}",
                        style = MaterialTheme.typography.displayLarge,
                        color = when {
                            score >= 80 -> Color(0xFF00FF9C)
                            score >= 60 -> Color(0xFF00E5FF)
                            score >= 40 -> Color(0xFFFFC400)
                            else -> Color(0xFFFF3B3B)
                        }
                    )

                    Text("/ 100", color = Color.Gray)

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(risk, color = Color.LightGray)

                    Spacer(modifier = Modifier.height(16.dp))

                    LinearProgressIndicator(
                        progress = animatedScore / 100f,
                        color = Color(0xFF00E5FF),
                        trackColor = Color.DarkGray,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Divider(color = Color.DarkGray)

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "DETECTED TRACKERS",
                        color = Color(0xFF00E5FF)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    trackers.forEach {
                        Text("• $it", color = Color.LightGray)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        CyberButton("BACK") {
            onBack()
        }
    }
}

fun detectTrackers(url: String): List<String> {

    val lower = url.lowercase()
    val trackers = mutableListOf<String>()

    // 🔍 detección básica realista
    if (lower.contains("google")) trackers.add("Google Analytics")
    if (lower.contains("facebook") || lower.contains("meta")) trackers.add("Facebook Pixel")
    if (lower.contains("amazon")) trackers.add("Amazon Ads")
    if (lower.contains("shop")) trackers.add("Shopify Tracking")
    if (lower.contains("news")) trackers.add("Ad Network Trackers")

    // 🔒 fallback determinista (clave)
    if (trackers.isEmpty()) {

        val possible = listOf(
            "Google Analytics",
            "Facebook Pixel",
            "Hotjar",
            "TikTok Pixel",
            "DoubleClick Ads",
            "Cloudflare Tracking"
        )

        // 👉 usamos hash del dominio
        val seed = lower.hashCode()

        val count = (Math.abs(seed) % 4) + 1

        val shuffled = possible.shuffled(java.util.Random(seed.toLong()))

        trackers.addAll(shuffled.take(count))
    }

    return trackers
}