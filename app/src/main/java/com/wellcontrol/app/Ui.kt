@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.wellcontrol.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.delay

private val HeroShape = RoundedCornerShape(34.dp)
private val TileShape = RoundedCornerShape(28.dp)
private val CardShape = RoundedCornerShape(26.dp)
private const val TWO_PI = (2.0 * PI).toFloat()

private fun kindIcon(kind: DeviceKind): Int = when (kind) {
    DeviceKind.WELL -> R.drawable.ic_drop
    DeviceKind.PUMP -> R.drawable.ic_waves
    DeviceKind.COMPRESSOR -> R.drawable.ic_wind
    DeviceKind.OTHER -> R.drawable.ic_cog
}

private fun amps(value: Float): String = String.format(Locale.US, "%.1f A", value)

/** "0:23", "12:05", "1:02:03" */
private fun clock(totalSecs: Int): String {
    val s = totalSecs.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) {
        String.format(Locale.US, "%d:%02d:%02d", h, m, sec)
    } else {
        String.format(Locale.US, "%d:%02d", m, sec)
    }
}

// ---------------------------------------------------------------- App shell

@Composable
fun WellApp(vm: MainViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var detailId by rememberSaveable { mutableStateOf<Int?>(null) }

    Box(
        Modifier
            .fillMaxSize()
            .background(AppBg)
    ) {
        if (tab == 0) HomeScreen(vm, onOpen = { detailId = it }) else SetupScreen(vm)

        FloatingNav(
            tab = tab,
            onTab = { tab = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
        )

        ToastHost(
            vm = vm,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 88.dp, start = 16.dp, end = 16.dp),
        )
    }

    val detail = vm.devices.firstOrNull { it.id == detailId }
    if (detail != null) {
        DeviceSheet(vm = vm, device = detail, onDismiss = { detailId = null })
    }
}

@Composable
fun FloatingNav(tab: Int, onTab: (Int) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.shadow(16.dp, CircleShape, spotColor = Navy.copy(alpha = 0.35f)),
        shape = CircleShape,
        color = Color.White,
    ) {
        Row(Modifier.padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            NavItem("Devices", Icons.Filled.Home, tab == 0) { onTab(0) }
            NavItem("Setup", Icons.Filled.Settings, tab == 1) { onTab(1) }
        }
    }
}

@Composable
private fun NavItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) Navy else Color.Transparent, label = "navBg")
    val fg = if (selected) Color.White else Muted
    Row(
        Modifier
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = label, tint = fg, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = fg, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

// ---------------------------------------------------------------- Toast

@Composable
fun ToastHost(vm: MainViewModel, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    var last by remember { mutableStateOf<Toast?>(null) }
    val current = vm.toast

    LaunchedEffect(current) {
        if (current != null) {
            last = current
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(3500)
            if (vm.toast == current) vm.toast = null
        }
    }

    AnimatedVisibility(
        visible = current != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier,
    ) {
        val t = last
        if (t != null) {
            val dot = when (t.kind) {
                ToastKind.ON -> IosGreen
                ToastKind.OFF -> Color(0xFF9AA6B8)
                ToastKind.ERROR -> RedOff
                ToastKind.INFO -> SkyLight
            }
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Navy,
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(14.dp, RoundedCornerShape(24.dp)),
            ) {
                Row(
                    Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(12.dp)
                            .background(dot, CircleShape)
                    )
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(t.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Text(t.sub, color = Color(0xFFB4C0D4), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Small building blocks

@Composable
fun IosSwitch(
    checked: Boolean,
    busy: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val track by animateColorAsState(if (checked) IosGreen else IosGrey, tween(250), label = "track")
    val knobX by animateDpAsState(
        if (checked) 20.dp else 0.dp,
        spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "knob",
    )
    val source = remember { MutableInteractionSource() }

    Box(
        Modifier
            .graphicsLayer { alpha = if (enabled || busy) 1f else 0.4f }
            .width(51.dp)
            .height(31.dp)
            .clip(CircleShape)
            .background(track)
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = enabled && !busy,
            ) { onChange(!checked) }
            .padding(2.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset(x = knobX)
                .size(27.dp)
                .shadow(3.dp, CircleShape)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(15.dp),
                    strokeWidth = 2.dp,
                    color = Amber,
                )
            }
        }
    }
}

/** Big dark rounded button, the main action style of the app. */
@Composable
fun PillButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    busy: Boolean = false,
    color: Color = Navy,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = Color.White,
            disabledContainerColor = Chip,
            disabledContentColor = Muted,
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        if (busy) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = Amber,
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(label, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** One filled wave shape. `level` is where the water surface sits (0 = top, 1 = bottom). */
private fun DrawScope.wave(phase: Float, level: Float, amplitude: Float, cycles: Float, color: Color) {
    val w = size.width
    val h = size.height
    val base = h * level
    val step = 5.dp.toPx()
    val path = Path()
    path.moveTo(0f, h)
    var x = 0f
    while (x <= w + step) {
        val y = base + amplitude * sin((x / w) * cycles * TWO_PI + phase)
        path.lineTo(x, y)
        x += step
    }
    path.lineTo(w, h)
    path.close()
    drawPath(path, color)
}

/**
 * A canvas whose drawing gets a wave phase (0..2π). While `animate` is true the phase loops
 * for ever and only the drawing is redone each frame; otherwise the picture stays still.
 */
@Composable
private fun WaveCanvas(
    animate: Boolean,
    millis: Int,
    modifier: Modifier = Modifier,
    draw: DrawScope.(Float) -> Unit,
) {
    if (animate) {
        val transition = rememberInfiniteTransition(label = "water")
        val phase = transition.animateFloat(
            initialValue = 0f,
            targetValue = TWO_PI,
            animationSpec = infiniteRepeatable(tween(millis, easing = LinearEasing), RepeatMode.Restart),
            label = "phase",
        )
        Canvas(modifier) { draw(phase.value) }
    } else {
        Canvas(modifier) { draw(0.9f) }
    }
}

// ---------------------------------------------------------------- Home

@Composable
fun HomeScreen(vm: MainViewModel, onOpen: (Int) -> Unit) {
    val running = vm.devices.filter { d ->
        val s = vm.statuses[d.id]
        s != null && s.online && s.running
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 120.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) { TopBar(vm) }
        item(span = { GridItemSpan(maxLineSpan) }) { Hero(vm, running) }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 0.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Your motors",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy,
                    modifier = Modifier.weight(1f),
                )
                Text("Tap a motor for details", fontSize = 13.sp, color = Muted)
            }
        }
        gridItems(items = vm.devices, key = { it.id }) { d ->
            DeviceTile(
                device = d,
                status = vm.statuses[d.id],
                pending = vm.pending[d.id],
                outcome = vm.outcomes[d.id],
                onToggle = { on -> vm.setRunning(d, on) },
                onOpen = { onOpen(d.id) },
            )
        }
    }
}

@Composable
fun TopBar(vm: MainViewModel) {
    val (text, dot) = when (vm.connected) {
        null -> "Checking…" to Amber
        true -> (if (vm.settings.demoMode) "Demo mode" else "Connected") to IosGreen
        false -> "Not connected" to RedOff
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .background(Navy, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_drop),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            "Well Control",
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            color = Navy,
            modifier = Modifier.weight(1f),
        )
        Row(
            Modifier
                .shadow(2.dp, CircleShape)
                .background(Color.White, CircleShape)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(9.dp)
                    .background(dot, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Navy)
        }
    }
}

/** The water picture behind the hero. The waves only move while a motor is running. */
@Composable
fun WaterBackground(animate: Boolean, modifier: Modifier = Modifier) {
    Box(modifier) {
        Image(
            painter = painterResource(R.drawable.hero_water),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
        WaveCanvas(animate = animate, millis = 7000, modifier = Modifier.matchParentSize()) { phase ->
            wave(phase, 0.50f, 12.dp.toPx(), 1.1f, Color.White.copy(alpha = 0.10f))
            wave(-phase + 2f, 0.56f, 9.dp.toPx(), 1.7f, Color.White.copy(alpha = 0.10f))
        }
    }
}

@Composable
fun Hero(vm: MainViewModel, running: List<Device>) {
    val count = running.size
    val total = vm.devices.size
    val load = running.sumOf { (vm.statuses[it.id]?.amps ?: 0f).toDouble() }.toFloat()
    val longest = running.maxOfOrNull { vm.statuses[it.id]?.runSecs ?: 0 }
    val nextOff = running
        .filter { it.autoOffMin > 0 }
        .minOfOrNull { it.autoOffMin * 60 - (vm.statuses[it.id]?.runSecs ?: 0) }

    val headline = when {
        vm.connected == false -> "Home box not reachable"
        count == 0 -> "All motors are off"
        count == 1 -> "${running[0].name} is running"
        else -> "$count motors are running"
    }

    Box(
        Modifier
            .fillMaxWidth()
            .shadow(14.dp, HeroShape, spotColor = Sky.copy(alpha = 0.5f))
            .clip(HeroShape)
    ) {
        WaterBackground(animate = count > 0, modifier = Modifier.matchParentSize())

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier
                    .size(50.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.40f), Color.White.copy(alpha = 0.14f))
                        ),
                        CircleShape,
                    )
                    .border(1.5.dp, Color.White.copy(alpha = 0.55f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_drop),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(25.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("Motors running", color = Color.White.copy(alpha = 0.92f), fontSize = 16.sp)
            Row {
                Text(
                    "$count",
                    color = Color.White,
                    fontSize = 74.sp,
                    lineHeight = 78.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    "/$total",
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 28.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .alignByBaseline(),
                )
            }
            Row(
                Modifier
                    .background(Color.White.copy(alpha = 0.20f), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.40f), CircleShape)
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(if (count > 0) Color(0xFF8DF5A9) else Color.White.copy(alpha = 0.8f), CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Text(headline, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(16.dp))

            // White panel sitting inside the hero.
            Column(
                Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
                    .background(Color.White, CardShape)
                    .padding(horizontal = 12.dp, vertical = 16.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    HeroStat("Total load", if (count > 0) amps(load) else "0 A", Modifier.weight(1f))
                    VLine()
                    HeroStat("Longest run", longest?.let { formatDuration(it) } ?: "–", Modifier.weight(1f))
                    VLine()
                    HeroStat("Next auto-off", nextOff?.let { formatDuration(it) } ?: "–", Modifier.weight(1f))
                }
                Spacer(Modifier.height(14.dp))
                PillButton("Stop all motors", onClick = { vm.stopAll() })
            }
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 13.sp, color = Muted, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Navy, maxLines = 1)
    }
}

@Composable
private fun VLine() {
    Box(
        Modifier
            .width(1.dp)
            .height(30.dp)
            .background(Line)
    )
}

/** Square tile for one motor: icon, switch, name and what it is doing right now. */
@Composable
fun DeviceTile(
    device: Device,
    status: DeviceStatus?,
    pending: Pending?,
    outcome: Outcome?,
    onToggle: (Boolean) -> Unit,
    onOpen: () -> Unit,
) {
    val online = status?.online == true
    val isRunning = online && status?.running == true
    val live = isRunning && pending == null
    val fault = status?.fault.orEmpty()
    val switchOn = pending?.target ?: isRunning

    val nameColor = if (live) Color.White else Navy
    val soft = if (live) Color.White.copy(alpha = 0.95f) else Muted

    val line1: String
    val line1Color: Color
    val line2: String
    when {
        pending != null && pending.stage == Stage.SENDING -> {
            line1 = if (pending.target) "Starting…" else "Stopping…"
            line1Color = Amber
            line2 = "Sending command"
        }
        pending != null -> {
            line1 = if (pending.target) "Starting…" else "Stopping…"
            line1Color = Amber
            line2 = "Waiting for sensor"
        }
        outcome != null && !outcome.ok -> {
            line1 = "No response"
            line1Color = RedOff
            line2 = "Check the starter"
        }
        !online -> {
            line1 = "Offline"
            line1Color = Muted
            line2 = "Check box power"
        }
        fault.isNotEmpty() -> {
            line1 = "Fault"
            line1Color = RedOff
            line2 = fault
        }
        isRunning -> {
            line1 = if (outcome != null) "✓ Confirmed ON" else "Running  ${clock(status?.runSecs ?: 0)}"
            line1Color = Color.White
            val off = if (device.autoOffMin > 0) {
                "off in ${formatDuration(device.autoOffMin * 60 - (status?.runSecs ?: 0))}"
            } else {
                "no auto-off"
            }
            line2 = "${amps(status?.amps ?: 0f)}  ·  $off"
        }
        else -> {
            line1 = if (outcome != null) "✓ Confirmed OFF" else "Off"
            line1Color = if (outcome != null) GreenDark else Muted
            line2 = if (device.autoOffMin > 0) "Auto-off ${device.autoOffMin} min" else "No auto-off"
        }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .height(176.dp)
            .shadow(
                elevation = if (live) 12.dp else 2.dp,
                shape = TileShape,
                spotColor = if (live) Sky.copy(alpha = 0.6f) else Navy.copy(alpha = 0.25f),
            )
            .clip(TileShape)
            .background(Color.White)
            .clickable(onClick = onOpen)
    ) {
        if (live) {
            Image(
                painter = painterResource(R.drawable.tile_water),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
            WaveCanvas(animate = true, millis = 4200, modifier = Modifier.matchParentSize()) { phase ->
                wave(phase, 0.50f, 6.dp.toPx(), 1.2f, Color(0xFF0B46B8).copy(alpha = 0.22f))
                wave(-phase + 1.5f, 0.60f, 5.dp.toPx(), 1.8f, Color(0xFF0B46B8).copy(alpha = 0.26f))
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 14.dp, top = 12.dp, bottom = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .background(if (live) Color.White.copy(alpha = 0.24f) else Chip, RoundedCornerShape(15.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(kindIcon(device.kind)),
                        contentDescription = null,
                        tint = if (live) Color.White else Navy,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                IosSwitch(
                    checked = switchOn,
                    busy = pending != null,
                    enabled = online,
                    onChange = onToggle,
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                device.name,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                color = nameColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                line1,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = line1Color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                line2,
                fontSize = 13.sp,
                color = soft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (pending != null) {
            LinearProgressIndicator(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(5.dp),
                color = Amber,
                trackColor = TintAmber,
            )
        }
    }
}

// ---------------------------------------------------------------- Device details (bottom sheet)

@Composable
fun DeviceSheet(vm: MainViewModel, device: Device, onDismiss: () -> Unit) {
    val status = vm.statuses[device.id]
    val pending = vm.pending[device.id]
    val outcome = vm.outcomes[device.id]
    val online = status?.online == true
    val isRunning = online && status?.running == true
    val live = isRunning && pending == null
    val runSecs = status?.runSecs ?: 0

    val fraction = when {
        !live -> 0f
        device.autoOffMin > 0 -> (1f - runSecs / (device.autoOffMin * 60f)).coerceIn(0f, 1f)
        else -> 1f
    }
    val (centerText, caption) = when {
        pending != null -> (if (pending.target) "Starting" else "Stopping") to "please wait"
        !online -> "Offline" to "not reachable"
        isRunning -> clock(runSecs) to "running"
        else -> "Off" to "motor stopped"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp),
        containerColor = Color.White,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .size(width = 46.dp, height = 5.dp)
                    .background(Line, CircleShape)
            )
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(device.name, fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, color = Navy)
            Text("${device.kind.label}  ·  Box ${device.boxId}", fontSize = 15.sp, color = Muted)
            Spacer(Modifier.height(22.dp))

            RunGauge(
                live = live,
                busy = pending != null,
                fraction = fraction,
                text = centerText,
                caption = caption,
            )

            Spacer(Modifier.height(22.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                HeroStat("Current", if (isRunning) amps(status?.amps ?: 0f) else "0 A", Modifier.weight(1f))
                VLine()
                val autoLabel = if (isRunning) "Auto-off in" else "Auto-off after"
                val autoValue = when {
                    device.autoOffMin <= 0 -> "Never"
                    isRunning -> formatDuration(device.autoOffMin * 60 - runSecs)
                    else -> "${device.autoOffMin} min"
                }
                HeroStat(autoLabel, autoValue, Modifier.weight(1f))
            }

            Spacer(Modifier.height(18.dp))
            NoteBox(device, status, pending, outcome)
            Spacer(Modifier.height(14.dp))

            val label = when {
                pending != null && pending.stage == Stage.SENDING -> "Sending command…"
                pending != null -> "Waiting for sensor…"
                !online -> "Not reachable"
                isRunning -> "Turn off"
                else -> "Turn on"
            }
            PillButton(
                label = label,
                onClick = { vm.setRunning(device, !isRunning) },
                enabled = online && pending == null,
                busy = pending != null,
                color = if (isRunning) Navy else Sky,
            )
        }
    }
}

/** Round gauge: the ring is the auto-off countdown, the water inside moves while the motor runs. */
@Composable
fun RunGauge(live: Boolean, busy: Boolean, fraction: Float, text: String, caption: String) {
    val sweep by animateFloatAsState(fraction, tween(600), label = "sweep")
    val ringColor = if (busy) Amber else Sky

    Box(Modifier.size(216.dp), contentAlignment = Alignment.Center) {
        WaveCanvas(animate = live, millis = 3600, modifier = Modifier.matchParentSize()) { phase ->
            val stroke = 13.dp.toPx()
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val arcTopLeft = Offset(stroke / 2f, stroke / 2f)
            drawArc(
                color = Line,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(stroke),
            )
            if (sweep > 0.001f) {
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = 360f * sweep,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
            val radius = size.minDimension / 2f - stroke - 9.dp.toPx()
            val porthole = Path().apply { addOval(Rect(center = center, radius = radius)) }
            clipPath(porthole) {
                drawRect(Chip)
                if (live) {
                    wave(phase, 0.60f, 7.dp.toPx(), 1.3f, SkyLight.copy(alpha = 0.45f))
                    wave(-phase + 2f, 0.66f, 6.dp.toPx(), 1.9f, Sky.copy(alpha = 0.35f))
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text,
                fontSize = if (text.length > 6) 32.sp else 46.sp,
                lineHeight = 50.sp,
                fontWeight = FontWeight.Bold,
                color = if (busy) Amber else Navy,
            )
            Text(caption, fontSize = 15.sp, color = if (live) Navy.copy(alpha = 0.7f) else Muted)
        }
    }
}

/** The coloured message that tells the person exactly what happened. */
@Composable
fun NoteBox(device: Device, status: DeviceStatus?, pending: Pending?, outcome: Outcome?) {
    val online = status?.online == true
    val fault = status?.fault.orEmpty()

    val (text, bg, fg) = when {
        pending != null && pending.stage == Stage.SENDING ->
            Triple("Sending command to ${device.name}…", TintAmber, Amber)
        pending != null ->
            Triple(
                "Command sent. Waiting for the motor sensor to confirm it ${if (pending.target) "started" else "stopped"}…",
                TintAmber, Amber,
            )
        outcome != null && outcome.ok && outcome.target ->
            Triple("✓ Confirmed: ${device.name} is ON", TintOn, GreenDark)
        outcome != null && outcome.ok ->
            Triple("✓ Confirmed: ${device.name} is OFF", Chip, Navy)
        outcome != null ->
            Triple("No response. The motor did not ${if (outcome.target) "start" else "stop"}. Check the starter.", TintRed, RedOff)
        !online ->
            Triple("Not reachable. Check the box power and radio range.", Chip, Muted)
        fault.isNotEmpty() ->
            Triple(fault, TintRed, RedOff)
        status?.running == true ->
            Triple("Motor is running (confirmed by the sensor)", TintOn, GreenDark)
        else ->
            Triple("Motor is off", Chip, Muted)
    }

    Box(
        Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = fg, fontSize = 15.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
    }
}

// ---------------------------------------------------------------- Setup

@Composable
fun SetupScreen(vm: MainViewModel) {
    var editing by remember { mutableStateOf<Device?>(null) }
    var hostText by remember(vm.settings.host) { mutableStateOf(vm.settings.host) }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        item {
            Column(Modifier.padding(horizontal = 6.dp, vertical = 8.dp)) {
                Text("Setup", fontSize = 36.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold, color = Navy)
                Text("Connection and devices", fontSize = 15.sp, color = Muted)
            }
        }

        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, CardShape, spotColor = Navy.copy(alpha = 0.25f))
                    .background(Color.White, CardShape)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Connection", fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = Navy)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Demo mode", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Navy)
                        Text("Try the app without any hardware", fontSize = 13.sp, color = Muted)
                    }
                    IosSwitch(
                        checked = vm.settings.demoMode,
                        busy = false,
                        enabled = true,
                        onChange = { vm.setDemo(it) },
                    )
                }
                if (!vm.settings.demoMode) {
                    OutlinedTextField(
                        value = hostText,
                        onValueChange = { hostText = it },
                        label = { Text("Home box address") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { vm.setHost(hostText.trim()) }, shape = CircleShape) { Text("Save") }
                        OutlinedButton(onClick = { vm.syncDevices() }, shape = CircleShape) {
                            Text("Send setup to box")
                        }
                    }
                    Text(
                        "First connect this phone to the home box Wi-Fi (for example \"WellControl\"). " +
                            "The address is shown on the home box screen.",
                        fontSize = 13.sp,
                        color = Muted,
                    )
                }
            }
        }

        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, top = 8.dp),
            ) {
                Text(
                    "Devices (${vm.devices.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = Navy,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = { editing = vm.newDevice() },
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add device", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        items(items = vm.devices, key = { it.id }) { d ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, CardShape, spotColor = Navy.copy(alpha = 0.25f))
                    .clip(CardShape)
                    .background(Color.White)
                    .clickable { editing = d }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(46.dp)
                        .background(Chip, RoundedCornerShape(15.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(kindIcon(d.kind)),
                        contentDescription = null,
                        tint = Navy,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(d.name, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = Navy)
                    val relays = if (d.type == DeviceType.STARTER) "relays ${d.channel}+${d.channel + 1}" else "relay ${d.channel}"
                    val auto = if (d.autoOffMin > 0) "${d.autoOffMin} min" else "off"
                    Text("Box ${d.boxId}, $relays  ·  auto-off $auto", fontSize = 13.sp, color = Muted)
                }
                Box(
                    Modifier
                        .size(38.dp)
                        .background(Chip, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = Navy, modifier = Modifier.size(18.dp))
                }
            }
        }

        item {
            Text(
                "Well Control 1.0  ·  Works without internet or SIM",
                fontSize = 13.sp,
                color = Muted,
                modifier = Modifier.padding(8.dp),
            )
        }
    }

    val e = editing
    if (e != null) {
        DeviceEditor(
            device = e,
            isNew = vm.devices.none { it.id == e.id },
            onSave = {
                vm.saveDevice(it)
                editing = null
            },
            onDelete = {
                vm.deleteDevice(e.id)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

@Composable
fun DeviceEditor(
    device: Device,
    isNew: Boolean,
    onSave: (Device) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(device.name) }
    var kind by remember { mutableStateOf(device.kind) }
    var type by remember { mutableStateOf(device.type) }
    var box by remember { mutableStateOf(device.boxId.toString()) }
    var channel by remember { mutableStateOf(device.channel.toString()) }
    var autoOff by remember { mutableStateOf(device.autoOffMin.toString()) }
    var askDelete by remember { mutableStateOf(false) }
    val fieldShape = RoundedCornerShape(16.dp)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(32.dp),
        containerColor = Color.White,
        title = {
            Text(
                if (isNew) "Add device" else "Edit device",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Navy,
            )
        },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    shape = fieldShape,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text("What is it?", fontSize = 14.sp, color = Muted)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DeviceKind.entries.forEach { k ->
                        FilterChip(
                            selected = kind == k,
                            onClick = { kind = k },
                            label = { Text(k.label) },
                            shape = CircleShape,
                        )
                    }
                }

                Text("How is it switched?", fontSize = 14.sp, color = Muted)
                DeviceType.entries.forEach { t ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { type = t },
                    ) {
                        RadioButton(selected = type == t, onClick = { type = t })
                        Column {
                            Text(t.label, fontSize = 15.sp, color = Navy)
                            Text(t.help, fontSize = 12.sp, color = Muted)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = box,
                        onValueChange = { v -> box = v.filter { it.isDigit() } },
                        label = { Text("Box no.") },
                        singleLine = true,
                        shape = fieldShape,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = channel,
                        onValueChange = { v -> channel = v.filter { it.isDigit() } },
                        label = { Text("Relay no.") },
                        singleLine = true,
                        shape = fieldShape,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }

                OutlinedTextField(
                    value = autoOff,
                    onValueChange = { v -> autoOff = v.filter { it.isDigit() } },
                    label = { Text("Auto-off after (minutes, 0 = never)") },
                    singleLine = true,
                    shape = fieldShape,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                shape = CircleShape,
                onClick = {
                    onSave(
                        device.copy(
                            name = name.trim(),
                            kind = kind,
                            type = type,
                            boxId = box.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                            channel = channel.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                            autoOffMin = autoOff.toIntOrNull() ?: 0,
                        )
                    )
                },
            ) { Text("Save", fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            Row {
                if (!isNew) {
                    TextButton(
                        onClick = { askDelete = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = RedOff),
                    ) { Text("Delete") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )

    if (askDelete) {
        AlertDialog(
            onDismissRequest = { askDelete = false },
            shape = RoundedCornerShape(32.dp),
            containerColor = Color.White,
            title = { Text("Delete ${device.name}?", fontWeight = FontWeight.Bold, color = Navy) },
            text = { Text("It will be removed from the app. You can add it again later.") },
            confirmButton = {
                Button(
                    onClick = {
                        askDelete = false
                        onDelete()
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = RedOff),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { askDelete = false }) { Text("Cancel") } },
        )
    }
}
