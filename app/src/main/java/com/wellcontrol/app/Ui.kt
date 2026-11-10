@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.wellcontrol.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val CardShape = RoundedCornerShape(24.dp)

private fun kindIcon(kind: DeviceKind): Int = when (kind) {
    DeviceKind.WELL -> R.drawable.ic_drop
    DeviceKind.PUMP -> R.drawable.ic_waves
    DeviceKind.COMPRESSOR -> R.drawable.ic_wind
    DeviceKind.OTHER -> R.drawable.ic_cog
}

// ---------------------------------------------------------------- App shell

@Composable
fun WellApp(vm: MainViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Box(
        Modifier
            .fillMaxSize()
            .background(AppBg)
    ) {
        if (tab == 0) DevicesScreen(vm) else SetupScreen(vm)

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
}

@Composable
fun FloatingNav(tab: Int, onTab: (Int) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.shadow(12.dp, RoundedCornerShape(32.dp)),
        shape = RoundedCornerShape(32.dp),
        color = Color.White,
    ) {
        Row(Modifier.padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            NavItem("Devices", Icons.Filled.Home, tab == 0) { onTab(0) }
            NavItem("Setup", Icons.Filled.Settings, tab == 1) { onTab(1) }
        }
    }
}

@Composable
private fun NavItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bg by animateColorAsState(if (selected) Brand else Color.Transparent, label = "navBg")
    val fg = if (selected) Color.White else Muted
    Row(
        Modifier
            .clip(RoundedCornerShape(26.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = label, tint = fg, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = fg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
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
                ToastKind.OFF -> Slate
                ToastKind.ERROR -> RedOff
                ToastKind.INFO -> Accent
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Ink,
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(20.dp)),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(12.dp)
                            .background(dot, CircleShape)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(t.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(t.sub, color = Color(0xFFB8C4CE), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- iOS style switch

@Composable
fun IosSwitch(
    checked: Boolean,
    busy: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val track by animateColorAsState(if (checked) IosGreen else IosGrey, tween(250), label = "track")
    val knobX by animateDpAsState(if (checked) 20.dp else 0.dp, spring(dampingRatio = 0.7f, stiffness = 500f), label = "knob")
    val source = remember { MutableInteractionSource() }

    Box(
        Modifier
            .graphicsLayer { alpha = if (enabled || busy) 1f else 0.4f }
            .width(51.dp)
            .height(31.dp)
            .clip(RoundedCornerShape(16.dp))
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

@Composable
fun PulseDot(color: Color, pulse: Boolean) {
    Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) {
        if (pulse) {
            val transition = rememberInfiniteTransition(label = "pulse")
            val p by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Restart),
                label = "p",
            )
            Box(
                Modifier
                    .size(18.dp)
                    .graphicsLayer {
                        scaleX = 0.5f + p * 0.5f
                        scaleY = 0.5f + p * 0.5f
                        alpha = 0.6f * (1f - p)
                    }
                    .background(color, CircleShape)
            )
        }
        Box(
            Modifier
                .size(9.dp)
                .background(color, CircleShape)
        )
    }
}

// ---------------------------------------------------------------- Devices

@Composable
fun DevicesScreen(vm: MainViewModel) {
    val running = vm.devices.count { d ->
        val s = vm.statuses[d.id]
        s != null && s.online && s.running
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        item {
            Column(Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                Text("Well Control", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("Switch your motors, no internet needed", fontSize = 14.sp, color = Muted)
            }
        }
        item { Hero(vm, running) }
        items(items = vm.devices, key = { it.id }) { d ->
            DeviceCard(
                device = d,
                status = vm.statuses[d.id],
                pending = vm.pending[d.id],
                outcome = vm.outcomes[d.id],
                onToggle = { on -> vm.setRunning(d, on) },
            )
        }
    }
}

@Composable
fun Hero(vm: MainViewModel, running: Int) {
    val total = vm.devices.size
    val (connText, connColor) = when (vm.connected) {
        null -> "Checking…" to Color(0xFFFFD27A)
        true -> (if (vm.settings.demoMode) "Demo mode" else "Home box connected") to Color(0xFF7CE79B)
        false -> "Not connected" to Color(0xFFFF9A9D)
    }
    Box(
        Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Brush.linearGradient(listOf(Color(0xFF0D4F6B), Color(0xFF1B7BA3))))
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(9.dp)
                        .background(connColor, CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Text(connText, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$running", color = Color.White, fontSize = 52.sp, fontWeight = FontWeight.Bold)
                Text(
                    " of $total running",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = { vm.stopAll() },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.18f),
                    contentColor = Color.White,
                ),
            ) {
                Text("Stop all motors", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun DeviceCard(
    device: Device,
    status: DeviceStatus?,
    pending: Pending?,
    outcome: Outcome?,
    onToggle: (Boolean) -> Unit,
) {
    val online = status?.online == true
    val isRunning = online && status?.running == true
    val fault = status?.fault.orEmpty()
    val switchOn = pending?.target ?: isRunning
    val glow by animateColorAsState(
        if (isRunning && pending == null) IosGreen.copy(alpha = 0.55f) else Color.Transparent,
        tween(300),
        label = "glow",
    )

    Card(
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isRunning) 6.dp else 2.dp),
        border = BorderStroke(2.dp, glow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(48.dp)
                        .background(if (isRunning) TintOn else TintOff, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(kindIcon(device.kind)),
                        contentDescription = null,
                        tint = if (isRunning) GreenDark else Brand,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(device.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink)
                    Spacer(Modifier.height(4.dp))
                    StateBadge(online, isRunning, fault.isNotEmpty(), pending)
                }
                IosSwitch(
                    checked = switchOn,
                    busy = pending != null,
                    enabled = online,
                    onChange = onToggle,
                )
            }

            if (pending != null) {
                Spacer(Modifier.height(14.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape),
                    color = Amber,
                    trackColor = TintAmber,
                )
            }

            Spacer(Modifier.height(14.dp))
            NoteBox(device, status, pending, outcome)

            if (isRunning && pending == null) {
                Spacer(Modifier.height(12.dp))
                StatsRow(device, status)
            }
        }
    }
}

@Composable
fun StateBadge(online: Boolean, running: Boolean, hasFault: Boolean, pending: Pending?) {
    val (text, color) = when {
        pending != null -> (if (pending.target) "Starting…" else "Stopping…") to Amber
        !online -> "Offline" to Slate
        hasFault -> "Fault" to RedOff
        running -> "RUNNING" to GreenDark
        else -> "STOPPED" to Slate
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        PulseDot(if (running && pending == null) IosGreen else color, pulse = running && pending == null)
        Spacer(Modifier.width(4.dp))
        Text(text, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

/** The coloured message under the switch: tells the person exactly what happened. */
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
            Triple("✓ Confirmed: ${device.name} is OFF", TintSlate, Slate)
        outcome != null ->
            Triple("No response. The motor did not ${if (outcome.target) "start" else "stop"}. Check the starter.", TintRed, RedOff)
        !online ->
            Triple("Not reachable. Check the box power and radio range.", TintSlate, Slate)
        fault.isNotEmpty() ->
            Triple(fault, TintRed, RedOff)
        status?.running == true ->
            Triple("Motor is running (sensor confirmed)", TintOn, GreenDark)
        else ->
            Triple(detailLine(device), TintSlate, Slate)
    }

    Box(
        Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        Text(text, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

fun detailLine(d: Device): String {
    val auto = if (d.autoOffMin > 0) "auto-off ${d.autoOffMin} min" else "no auto-off"
    return "Motor is off • $auto"
}

@Composable
fun StatsRow(device: Device, status: DeviceStatus?) {
    val s = status ?: return
    val left = if (device.autoOffMin > 0) {
        formatDuration(device.autoOffMin * 60 - s.runSecs)
    } else {
        "Never"
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile("Running", formatDuration(s.runSecs), Modifier.weight(1f))
        StatTile("Current", String.format("%.1f A", s.amps), Modifier.weight(1f))
        StatTile("Auto-off in", left, Modifier.weight(1f))
    }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(AppBg, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(label, fontSize = 11.sp, color = Muted)
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink)
    }
}

// ---------------------------------------------------------------- Setup

@Composable
fun SetupScreen(vm: MainViewModel) {
    var editing by remember { mutableStateOf<Device?>(null) }
    var hostText by remember(vm.settings.host) { mutableStateOf(vm.settings.host) }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        item {
            Column(Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                Text("Setup", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("Connection and devices", fontSize = 14.sp, color = Muted)
            }
        }

        item {
            Card(
                shape = CardShape,
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Connection", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Ink)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Demo mode", fontSize = 15.sp, color = Ink)
                            Text("Try the app without any hardware", fontSize = 12.sp, color = Muted)
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
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { vm.setHost(hostText.trim()) },
                                shape = RoundedCornerShape(14.dp),
                            ) { Text("Save") }
                            OutlinedButton(
                                onClick = { vm.syncDevices() },
                                shape = RoundedCornerShape(14.dp),
                            ) { Text("Send setup to box") }
                        }
                        Text(
                            "First connect this phone to the home box Wi-Fi (for example \"WellControl\"). " +
                                "The address is shown on the home box screen.",
                            fontSize = 12.sp,
                            color = Muted,
                        )
                    }
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Devices (${vm.devices.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Ink,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = { editing = vm.newDevice() },
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add device")
                }
            }
        }

        items(items = vm.devices, key = { it.id }) { d ->
            Card(
                shape = CardShape,
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardShape)
                    .clickable { editing = d },
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .background(TintOff, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(kindIcon(d.kind)),
                            contentDescription = null,
                            tint = Brand,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(d.name, fontWeight = FontWeight.Bold, color = Ink)
                        val relays = if (d.type == DeviceType.STARTER) "relays ${d.channel}+${d.channel + 1}" else "relay ${d.channel}"
                        val auto = if (d.autoOffMin > 0) "${d.autoOffMin} min" else "off"
                        Text(
                            "Box ${d.boxId}, $relays • auto-off $auto",
                            fontSize = 12.sp,
                            color = Muted,
                        )
                    }
                    IconButton(onClick = { editing = d }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = Muted)
                    }
                }
            }
        }

        item {
            Text(
                "Well Control 1.0 • Works without internet or SIM",
                fontSize = 12.sp,
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

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = CardShape,
        containerColor = Color.White,
        title = { Text(if (isNew) "Add device" else "Edit device") },
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
                    modifier = Modifier.fillMaxWidth(),
                )

                Text("What is it?", fontSize = 13.sp, color = Muted)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DeviceKind.entries.forEach { k ->
                        FilterChip(
                            selected = kind == k,
                            onClick = { kind = k },
                            label = { Text(k.label) },
                        )
                    }
                }

                Text("How is it switched?", fontSize = 13.sp, color = Muted)
                DeviceType.entries.forEach { t ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { type = t },
                    ) {
                        RadioButton(selected = type == t, onClick = { type = t })
                        Column {
                            Text(t.label, fontSize = 14.sp)
                            Text(t.help, fontSize = 11.sp, color = Muted)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = box,
                        onValueChange = { v -> box = v.filter { it.isDigit() } },
                        label = { Text("Box no.") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = channel,
                        onValueChange = { v -> channel = v.filter { it.isDigit() } },
                        label = { Text("Relay no.") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }

                OutlinedTextField(
                    value = autoOff,
                    onValueChange = { v -> autoOff = v.filter { it.isDigit() } },
                    label = { Text("Auto-off after (minutes, 0 = never)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
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
            ) { Text("Save") }
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
            title = { Text("Delete ${device.name}?") },
            text = { Text("It will be removed from the app. You can add it again later.") },
            confirmButton = {
                Button(
                    onClick = {
                        askDelete = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedOff),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { askDelete = false }) { Text("Cancel") } },
        )
    }
}
