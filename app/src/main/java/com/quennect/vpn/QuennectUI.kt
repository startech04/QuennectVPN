package com.quennect.vpn

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.random.Random

val ShipHull = Color(0xFF020205)
val ShipDeepSpace = Color(0xFF050510)
val NeonCyan = Color(0xFF00FFFF)
val NeonMagenta = Color(0xFFB026FF)
val ShipGrey = Color(0xFF555566)
val ShipGlass = Color(0x33FFFFFF)
val HUDGreen = Color(0xFF00FF41)
val HUDYellow = Color(0xFFFFD700)
val HUDRed = Color(0xFFFF0033)

val SciFiFont = FontFamily.Monospace

enum class ConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED
}

@Composable
fun UpdateRequiredScreen(
    currentVersionName: String,
    remoteVersionName: String,
    progress: Int,
    error: String?,
    onUpdateRequested: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ShipHull
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            StarryBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Warning icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .border(2.dp, HUDYellow, CircleShape)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Update",
                        tint = HUDYellow,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "UPDATE REQUIRED",
                    color = HUDRed,
                    fontFamily = SciFiFont,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Ship-OS v$currentVersionName is outdated",
                    color = ShipGrey,
                    fontFamily = SciFiFont,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "New version: v$remoteVersionName",
                    color = HUDYellow,
                    fontFamily = SciFiFont,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(40.dp))

                if (progress >= 0) {
                    // Downloading
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (progress < 100) "DOWNLOADING..." else "READY TO INSTALL",
                            color = if (progress < 100) NeonCyan else HUDGreen,
                            fontFamily = SciFiFont,
                            fontSize = 12.sp,
                            letterSpacing = 2.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = if (progress < 100) NeonCyan else HUDGreen,
                            trackColor = ShipGlass,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "$progress%",
                            color = NeonCyan,
                            fontFamily = SciFiFont,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    // Download button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(NeonCyan.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .border(1.dp, NeonCyan, RoundedCornerShape(4.dp))
                            .clickable { onUpdateRequested() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = NeonCyan
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "DOWNLOAD & INSTALL",
                                color = NeonCyan,
                                fontFamily = SciFiFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                if (error != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "[ERROR] $error",
                        color = HUDRed,
                        fontFamily = SciFiFont,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Terminal style footer
                Text(
                    text = "App cannot be used until updated",
                    color = HUDRed.copy(alpha = 0.6f),
                    fontFamily = SciFiFont,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun QuennectApp(
    logs: List<String>,
    versionName: String = "1.0.0.2",
    userName: String? = null,
    signInLoading: Boolean = false,
    signInError: String? = null,
    onConnectRequested: () -> Unit,
    onDisconnectRequested: () -> Unit,
    onSignInRequested: () -> Unit = {}
) {
    var connectionState by remember { mutableStateOf(ConnectionState.DISCONNECTED) }
    var downloadSpeed by remember { mutableStateOf(0.0f) }
    var uploadSpeed by remember { mutableStateOf(0.0f) }
    var connectionStartTime by remember { mutableLongStateOf(0L) }
    var currentTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(logs.size) {
        if (logs.any { it.contains("LIVE") || it.contains("Protected") } && connectionState == ConnectionState.CONNECTING) {
            connectionState = ConnectionState.CONNECTED
            connectionStartTime = System.currentTimeMillis()
        }
    }

    LaunchedEffect(connectionState) {
        if (connectionState == ConnectionState.CONNECTED) {
            while (true) {
                currentTime = System.currentTimeMillis()
                downloadSpeed = Random.nextFloat() * 14.0f + 1.0f
                uploadSpeed = Random.nextFloat() * 4.5f + 0.5f
                delay(1000)
            }
        } else {
            downloadSpeed = 0f
            uploadSpeed = 0f
            currentTime = 0L
            if (connectionState == ConnectionState.DISCONNECTED) {
                connectionStartTime = 0L
            }
        }
    }

    val duration = if (connectionStartTime > 0 && connectionState == ConnectionState.CONNECTED) currentTime - connectionStartTime else 0L
    val hours = (duration / 3600000)
    val minutes = (duration % 3600000) / 60000
    val seconds = (duration % 60000) / 1000
    val timeString = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ShipHull
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            StarryBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier
                        .size(44.dp)
                        .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(2.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher),
                            contentDescription = "Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(2.dp))
                        )
                    }

                    Text(
                        text = if (userName != null) "LOGGED IN: $userName" else "SHIP-OS v$versionName",
                        color = NeonCyan,
                        fontFamily = SciFiFont,
                        fontSize = 10.sp,
                        letterSpacing = 2.sp
                    )

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            .clickable { if (userName == null) onSignInRequested() }
                    ) {
                        if (userName != null) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = "User", tint = HUDGreen, modifier = Modifier.align(Alignment.Center).size(20.dp))
                        } else {
                            Icon(imageVector = Icons.Default.Person, contentDescription = "User", tint = NeonCyan, modifier = Modifier.align(Alignment.Center).size(20.dp))
                        }
                    }
                }

                MarsPowerButton(connectionState) {
                    when (connectionState) {
                        ConnectionState.DISCONNECTED -> {
                            onConnectRequested()
                            connectionState = ConnectionState.CONNECTING
                        }
                        ConnectionState.CONNECTING, ConnectionState.CONNECTED -> {
                            onDisconnectRequested()
                            connectionState = ConnectionState.DISCONNECTED
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "[SYSTEM_LINK]: ",
                            color = ShipGrey,
                            fontFamily = SciFiFont,
                            fontSize = 14.sp
                        )
                        Text(
                            text = when (connectionState) {
                                ConnectionState.CONNECTED -> "ESTABLISHED"
                                ConnectionState.CONNECTING -> "SYNCING..."
                                else -> "OFFLINE"
                            },
                            color = when (connectionState) {
                                ConnectionState.CONNECTED -> NeonCyan
                                ConnectionState.CONNECTING -> HUDYellow
                                else -> HUDRed
                            },
                            fontFamily = SciFiFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    if (connectionState == ConnectionState.CONNECTED) {
                        Text(
                            text = "UPTIME: $timeString",
                            color = HUDGreen,
                            fontFamily = SciFiFont,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ShipGlass, RoundedCornerShape(2.dp))
                        .border(1.dp, NeonCyan.copy(alpha = 0.1f), RoundedCornerShape(2.dp))
                        .padding(16.dp)
                ) {
                    NetworkStatsRow(connectionState == ConnectionState.CONNECTED, downloadSpeed, uploadSpeed)
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (userName == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .background(NeonCyan.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .border(1.dp, NeonCyan, RoundedCornerShape(4.dp))
                            .then(if (!signInLoading) Modifier.clickable { onSignInRequested() } else Modifier),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Login, contentDescription = null, tint = NeonCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (signInLoading) "AUTHENTICATING..." else "AUTHORIZE WITH GOOGLE",
                                color = if (signInLoading) HUDYellow else NeonCyan,
                                fontFamily = SciFiFont,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (signInError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "[AUTH ERROR] $signInError",
                            color = HUDRed,
                            fontFamily = SciFiFont,
                            fontSize = 9.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                TerminalLogs(logs, modifier = Modifier.height(120.dp).fillMaxWidth())

                Spacer(modifier = Modifier.height(10.dp))

                Canvas(modifier = Modifier.fillMaxWidth().height(2.dp)) {
                    drawLine(
                        color = NeonCyan.copy(alpha = 0.3f),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
        }
    }
}

@Composable
fun MarsPowerButton(state: ConnectionState, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "hudRotate")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(220.dp)) {
            drawArc(
                color = NeonCyan.copy(alpha = 0.2f),
                startAngle = 45f,
                sweepAngle = 90f,
                useCenter = false,
                style = Stroke(width = 2.dp.toPx())
            )
            drawArc(
                color = NeonCyan.copy(alpha = 0.2f),
                startAngle = 225f,
                sweepAngle = 90f,
                useCenter = false,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        if (state != ConnectionState.DISCONNECTED) {
            Canvas(modifier = Modifier.size(220.dp)) {
                drawArc(
                    color = if (state == ConnectionState.CONNECTED) NeonCyan else HUDYellow,
                    startAngle = rotation,
                    sweepAngle = 40f,
                    useCenter = false,
                    style = Stroke(width = 2.dp.toPx())
                )
                drawArc(
                    color = if (state == ConnectionState.CONNECTED) NeonCyan else HUDYellow,
                    startAngle = rotation + 180f,
                    sweepAngle = 40f,
                    useCenter = false,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .border(1.dp, NeonCyan.copy(alpha = 0.3f), CircleShape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.planet_mars),
                contentDescription = "Engine Core",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(if (state == ConnectionState.CONNECTED) 0.dp else 2.dp)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), CircleShape).padding(10.dp)
            ) {
                Text(
                    text = when (state) {
                        ConnectionState.CONNECTED -> "DE-ACTIVATE"
                        ConnectionState.CONNECTING -> "SYNCING"
                        else -> "ENGAGE"
                    },
                    color = Color.White,
                    fontFamily = SciFiFont,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun NetworkStatsRow(active: Boolean, down: Float, up: Float) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        StatItem("RX_DATA", String.format(Locale.getDefault(), "%.1f", down), NeonCyan, active)
        StatItem("TX_DATA", String.format(Locale.getDefault(), "%.1f", up), NeonMagenta, active)
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color, active: Boolean) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(label, color = color.copy(alpha = 0.7f), fontFamily = SciFiFont, fontSize = 9.sp, letterSpacing = 1.sp)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = Color.White, fontFamily = SciFiFont, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(" MBPS", color = ShipGrey, fontFamily = SciFiFont, fontSize = 10.sp, modifier = Modifier.padding(bottom = 4.dp))
        }
        ConnectionGraph(active, color, modifier = Modifier.size(140.dp, 30.dp))
    }
}

@Composable
fun StarryBackground() {
    val starPositions = remember {
        List(120) { Offset(Random.nextFloat(), Random.nextFloat()) to (Random.nextFloat() * 1.5f + 0.2f) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ShipHull, ShipDeepSpace)))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            starPositions.forEach { (pos, starSize) ->
                drawCircle(
                    color = Color.White.copy(alpha = Random.nextFloat() * 0.5f + 0.3f),
                    radius = starSize.dp.toPx(),
                    center = Offset(pos.x * size.width, pos.y * size.height)
                )
            }
        }
    }
}

@Composable
fun ConnectionGraph(active: Boolean, color: Color, modifier: Modifier = Modifier) {
    val points = remember { mutableStateListOf<Float>() }
    LaunchedEffect(active) {
        while (true) {
            points.add(if (active) (20..80).random().toFloat() else 0f)
            if (points.size > 20) points.removeAt(0)
            delay(400)
        }
    }
    Canvas(modifier = modifier) {
        val path = Path()
        if (points.isNotEmpty()) {
            val stepX = size.width / 20
            val scaleY = size.height / 100
            path.moveTo(0f, size.height - (points[0] * scaleY))
            points.forEachIndexed { index, value ->
                path.lineTo(index * stepX, size.height - (value * scaleY))
            }
        }
        drawPath(path = path, color = color, style = Stroke(width = 1.dp.toPx()))
    }
}

@Composable
fun TerminalLogs(logs: List<String>, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.5f))
            .border(1.dp, NeonCyan.copy(alpha = 0.2f))
            .padding(10.dp)
    ) {
        LazyColumn(reverseLayout = true) {
            items(logs.asReversed()) { log ->
                val textColor = when {
                    log.contains("LIVE") || log.contains("success") -> HUDGreen
                    log.contains("ERROR") -> HUDRed
                    else -> NeonCyan.copy(alpha = 0.6f)
                }
                Text(
                    text = "> $log",
                    color = textColor,
                    fontFamily = SciFiFont,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
    }
}
