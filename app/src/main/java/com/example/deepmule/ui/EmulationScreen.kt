package com.example.deepmule.ui

import android.graphics.SurfaceTexture
import android.view.KeyEvent
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.example.deepmule.R
import com.example.deepmule.data.GameEntity
import com.example.deepmule.emu.EmulationViewModel
import com.example.deepmule.emu.EmulationViewModel.VirtualButton
import com.example.deepmule.ui.theme.DeepMuleTheme
import kotlin.math.min

@Composable
fun EmulationScreen(
    viewModel: EmulationViewModel,
    onExit: () -> Unit
) {
    val currentGame by viewModel.currentGame.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    EmulationContent(
        currentGame = currentGame,
        isPaused = isPaused,
        syncStatus = syncStatus,
        onExit = { viewModel.onExit(); onExit() },
        onTogglePause = viewModel::togglePause,
        onVirtualButtonPressed = viewModel::setVirtualButton,
        onSaveState = viewModel::saveState,
        onLoadState = viewModel::loadState,
        onSurfaceReady = viewModel::onSurfaceReady,
        onSurfaceDestroyed = viewModel::onSurfaceDestroyed,
        onGamepadKeyEvent = viewModel::onGamepadKeyEvent
    )
}

@Composable
fun EmulationContent(
    currentGame: GameEntity?,
    isPaused: Boolean,
    syncStatus: String,
    onExit: () -> Unit,
    onTogglePause: () -> Unit,
    onVirtualButtonPressed: (VirtualButton, Boolean) -> Unit,
    onSaveState: (Int) -> Unit,
    onLoadState: (Int) -> Unit,
    onSurfaceReady: (android.view.Surface) -> Unit,
    onSurfaceDestroyed: () -> Unit,
    onGamepadKeyEvent: (Int, Boolean) -> Boolean,
    modifier: Modifier = Modifier
) {
    var selectedSlot by remember { mutableIntStateOf(1) }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(Color.Black)) {
        val metrics = remember(maxWidth, maxHeight, currentGame?.system) {
            emulationLayoutMetrics(maxWidth, maxHeight, currentGame?.system)
        }

        // 1 ── Game surface
        AndroidView(
            factory = { ctx ->
                TextureView(ctx).apply {
                    isFocusable = true
                    isFocusableInTouchMode = true
                    requestFocus()
                    var rs: android.view.Surface? = null
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                            rs?.release()
                            val runtimeSurface = android.view.Surface(st)
                            rs = runtimeSurface
                            onSurfaceReady(runtimeSurface)
                        }
                        override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {
                            val runtimeSurface = rs
                            if (runtimeSurface != null) {
                                onSurfaceReady(runtimeSurface)
                            }
                        }
                        override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                            onSurfaceDestroyed(); rs?.release(); rs = null; return true
                        }
                        override fun onSurfaceTextureUpdated(st: SurfaceTexture) = Unit
                    }
                    setOnKeyListener { _, keyCode, event ->
                        when (event.action) {
                            KeyEvent.ACTION_DOWN -> onGamepadKeyEvent(keyCode, true)
                            KeyEvent.ACTION_UP   -> onGamepadKeyEvent(keyCode, false)
                            else                 -> false
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize().zIndex(0f)
        )

        // 2 ── Sync status
        Surface(
            modifier = Modifier.zIndex(1f).align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = metrics.statusBottomPadding),
            color = Color.Black.copy(alpha = 0.65f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = syncStatus,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // 3 ── HUD overlay
        Column(
            modifier = Modifier.fillMaxSize().zIndex(2f)
                .statusBarsPadding().navigationBarsPadding()
        ) {
            EmulationTopBar(
                currentGame = currentGame,
                isPaused = isPaused,
                onExit = onExit,
                onTogglePause = onTogglePause
            )

            Spacer(Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color.Black.copy(alpha = 0.78f),
                        RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    )
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.14f),
                        RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    )
                    .padding(
                        start = metrics.panelHorizontalPadding,
                        end = metrics.panelHorizontalPadding,
                        top = metrics.panelVerticalPadding,
                        bottom = metrics.panelVerticalPadding
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ShoulderButton(
                        label = "L",
                        button = VirtualButton.L,
                        width = metrics.shoulderWidth,
                        height = metrics.shoulderHeight,
                        onButtonPressed = onVirtualButtonPressed
                    )
                    ShoulderButton(
                        label = "R",
                        button = VirtualButton.R,
                        width = metrics.shoulderWidth,
                        height = metrics.shoulderHeight,
                        onButtonPressed = onVirtualButtonPressed
                    )
                }

                Spacer(modifier = Modifier.height(metrics.clusterSpacing))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    VirtualDPad(
                        modifier = Modifier.size(metrics.dpadSize),
                        arrowSize = metrics.dpadArrowSize,
                        centerSize = metrics.dpadCenterSize,
                        onButtonPressed = onVirtualButtonPressed
                    )

                    Column(
                        modifier = Modifier.padding(bottom = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(metrics.centerSpacing)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(metrics.centerSpacing)) {
                            MiniButton(
                                label = "SEL",
                                button = VirtualButton.SELECT,
                                width = metrics.miniButtonWidth,
                                height = metrics.miniButtonHeight,
                                onButtonPressed = onVirtualButtonPressed
                            )
                            MiniButton(
                                label = "STA",
                                button = VirtualButton.START,
                                width = metrics.miniButtonWidth,
                                height = metrics.miniButtonHeight,
                                onButtonPressed = onVirtualButtonPressed
                            )
                        }
                        Surface(
                            onClick = { selectedSlot = (selectedSlot % 5) + 1 },
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.18f),
                            contentColor = Color.White
                        ) {
                            Text("Slot $selectedSlot",
                                modifier = Modifier.padding(
                                    horizontal = metrics.slotHorizontalPadding,
                                    vertical = metrics.slotVerticalPadding
                                ),
                                style = MaterialTheme.typography.labelMedium
                                    .copy(fontWeight = FontWeight.Bold))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(metrics.centerSpacing)) {
                            ControlAction(
                                icon = Icons.Rounded.Save,
                                label = stringResource(R.string.save_state),
                                containerSize = metrics.utilityButtonSize,
                                onClick = { onSaveState(selectedSlot) }
                            )
                            ControlAction(
                                icon = Icons.Rounded.SettingsBackupRestore,
                                label = stringResource(R.string.load_state),
                                containerSize = metrics.utilityButtonSize,
                                onClick = { onLoadState(selectedSlot) }
                            )
                        }
                    }

                    VirtualActionButtons(
                        modifier = Modifier.size(metrics.actionClusterSize),
                        buttonSize = metrics.actionButtonSize,
                        isArcadeLayout = metrics.isArcadeLayout,
                        onButtonPressed = onVirtualButtonPressed
                    )
                }
            }
        }
    }
}

@Composable
private fun EmulationTopBar(
    currentGame: GameEntity?,
    isPaused: Boolean,
    onExit: () -> Unit,
    onTogglePause: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .background(Color.Black.copy(alpha = 0.62f), RoundedCornerShape(18.dp))
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(18.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onExit) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.exit_emulation),
                tint = Color.White
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = currentGame?.title ?: "",
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = currentGame?.system ?: "",
                color = Color.White.copy(alpha = 0.78f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium
            )
        }

        IconButton(onClick = onTogglePause) {
            Icon(
                imageVector = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                contentDescription = if (isPaused) {
                    stringResource(R.string.resume_emulation)
                } else {
                    stringResource(R.string.pause_emulation)
                },
                tint = Color.White
            )
        }
    }
}

// ── Shoulder button ──────────────────────────────────────────────────────────

@Composable
fun ShoulderButton(
    label: String,
    button: VirtualButton,
    width: Dp = 64.dp,
    height: Dp = 28.dp,
    onButtonPressed: (VirtualButton, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(6.dp))
            .graphicsLayer {
                alpha = if (isPressed) 0.8f else 0.45f
                scaleX = if (isPressed) 1.1f else 1f
                scaleY = if (isPressed) 1.1f else 1f
            }
            .background(Color.White.copy(alpha = 0.45f))
            .border(1.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    isPressed = true
                    onButtonPressed(button, true)
                    tryAwaitRelease()
                    onButtonPressed(button, false)
                    isPressed = false
                })
            },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White.copy(alpha = 0.95f),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
    }
}

// ── Mini button (Select / Start) ──────────────────────────────────────────────

@Composable
fun MiniButton(
    label: String,
    button: VirtualButton,
    width: Dp = 44.dp,
    height: Dp = 22.dp,
    onButtonPressed: (VirtualButton, Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(height / 2))
            .graphicsLayer {
                alpha = if (isPressed) 0.8f else 0.45f
                scaleX = if (isPressed) 1.1f else 1f
                scaleY = if (isPressed) 1.1f else 1f
            }
            .background(Color.White.copy(alpha = 0.45f))
            .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(height / 2))
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    isPressed = true
                    onButtonPressed(button, true)
                    tryAwaitRelease()
                    onButtonPressed(button, false)
                    isPressed = false
                })
            },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White.copy(alpha = 0.90f),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold))
    }
}

// ── D-Pad ─────────────────────────────────────────────────────────────────────

@Composable
fun VirtualDPad(
    modifier: Modifier = Modifier,
    arrowSize: Dp = 38.dp,
    centerSize: Dp = 32.dp,
    onButtonPressed: (VirtualButton, Boolean) -> Unit
) {
    val arrowColor = Color.White.copy(alpha = 0.95f)
    val iconSize = (arrowSize * 0.68f).clamp(22.dp, 34.dp)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        DPadArrow(Modifier.align(Alignment.TopCenter), VirtualButton.UP, arrowSize, onButtonPressed) {
            Icon(Icons.Rounded.KeyboardArrowUp, "Up", tint = arrowColor, modifier = Modifier.size(iconSize))
        }
        DPadArrow(Modifier.align(Alignment.BottomCenter), VirtualButton.DOWN, arrowSize, onButtonPressed) {
            Icon(Icons.Rounded.KeyboardArrowDown, "Down", tint = arrowColor, modifier = Modifier.size(iconSize))
        }
        DPadArrow(Modifier.align(Alignment.CenterStart), VirtualButton.LEFT, arrowSize, onButtonPressed) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Left", tint = arrowColor, modifier = Modifier.size(iconSize))
        }
        DPadArrow(Modifier.align(Alignment.CenterEnd), VirtualButton.RIGHT, arrowSize, onButtonPressed) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Right", tint = arrowColor, modifier = Modifier.size(iconSize))
        }
        Box(
            Modifier
                .size(centerSize)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.20f))
                .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
        )
    }
}

@Composable
private fun DPadArrow(
    modifier: Modifier,
    button: VirtualButton,
    size: Dp,
    onButtonPressed: (VirtualButton, Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .graphicsLayer {
                alpha = if (isPressed) 0.8f else 0.45f
                scaleX = if (isPressed) 1.1f else 1f
                scaleY = if (isPressed) 1.1f else 1f
            }
            .background(Color.White.copy(alpha = 0.45f))
            .border(1.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    isPressed = true
                    onButtonPressed(button, true)
                    tryAwaitRelease()
                    onButtonPressed(button, false)
                    isPressed = false
                })
            },
        contentAlignment = Alignment.Center,
        content = { content() }
    )
}

// ── ABXY diamond ──────────────────────────────────────────────────────────────

@Composable
fun VirtualActionButtons(
    modifier: Modifier = Modifier,
    buttonSize: Dp = 42.dp,
    isArcadeLayout: Boolean = false,
    onButtonPressed: (VirtualButton, Boolean) -> Unit
) {
    val buttons = listOf(
        ActionButtonSpec("Y", VirtualButton.Y, Color(0xFF8E24AA)),
        ActionButtonSpec("X", VirtualButton.X, Color(0xFF1E88E5)),
        ActionButtonSpec("B", VirtualButton.B, Color(0xFF43A047)),
        ActionButtonSpec("A", VirtualButton.A, Color(0xFFE53935))
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (isArcadeLayout) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                buttons.forEach { button ->
                    ActionFaceButton(
                        label = button.label,
                        button = button.button,
                        color = button.color,
                        modifier = Modifier,
                        size = buttonSize,
                        onButtonPressed = onButtonPressed
                    )
                }
            }
        } else {
            ActionFaceButton("X", VirtualButton.X, Color(0xFF1E88E5),
                Modifier.align(Alignment.TopCenter), buttonSize, onButtonPressed)
            ActionFaceButton("Y", VirtualButton.Y, Color(0xFF8E24AA),
                Modifier.align(Alignment.CenterStart), buttonSize, onButtonPressed)
            ActionFaceButton("A", VirtualButton.A, Color(0xFFE53935),
                Modifier.align(Alignment.CenterEnd), buttonSize, onButtonPressed)
            ActionFaceButton("B", VirtualButton.B, Color(0xFF43A047),
                Modifier.align(Alignment.BottomCenter), buttonSize, onButtonPressed)
        }
    }
}

@Composable
private fun ActionFaceButton(
    label: String,
    button: VirtualButton,
    color: Color,
    modifier: Modifier,
    size: Dp,
    onButtonPressed: (VirtualButton, Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .graphicsLayer {
                alpha = if (isPressed) 0.8f else 0.45f
                scaleX = if (isPressed) 1.1f else 1f
                scaleY = if (isPressed) 1.1f else 1f
            }
            .background(color.copy(alpha = 0.45f))
            .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    isPressed = true
                    onButtonPressed(button, true)
                    tryAwaitRelease()
                    onButtonPressed(button, false)
                    isPressed = false
                })
            },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White.copy(alpha = 0.95f),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black))
    }
}

// ── Control action (save / load) ─────────────────────────────────────────────

@Composable
fun ControlAction(
    icon: ImageVector,
    label: String,
    containerSize: Dp = 42.dp,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.18f),
            contentColor = Color.White,
            tonalElevation = 2.dp,
            shadowElevation = 6.dp
        ) {
            Icon(icon, contentDescription = label,
                modifier = Modifier.padding((containerSize * 0.24f).clamp(8.dp, 14.dp)).size((containerSize * 0.52f).clamp(18.dp, 26.dp)),
                tint = Color.White)
        }
        Spacer(Modifier.height(2.dp))
        Text(label, color = Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.labelSmall)
    }
}

private data class EmulationLayoutMetrics(
    val isArcadeLayout: Boolean,
    val dpadSize: Dp,
    val dpadArrowSize: Dp,
    val dpadCenterSize: Dp,
    val actionClusterSize: Dp,
    val actionButtonSize: Dp,
    val shoulderWidth: Dp,
    val shoulderHeight: Dp,
    val miniButtonWidth: Dp,
    val miniButtonHeight: Dp,
    val utilityButtonSize: Dp,
    val panelHorizontalPadding: Dp,
    val panelVerticalPadding: Dp,
    val clusterSpacing: Dp,
    val centerSpacing: Dp,
    val slotHorizontalPadding: Dp,
    val slotVerticalPadding: Dp,
    val statusBottomPadding: Dp
)

private data class ActionButtonSpec(
    val label: String,
    val button: VirtualButton,
    val color: Color
)

private fun emulationLayoutMetrics(width: Dp, height: Dp, systemName: String?): EmulationLayoutMetrics {
    val shortestSide = min(width.value, height.value).dp
    val isArcadeLayout = systemName?.contains("Arcade", ignoreCase = true) == true ||
        systemName?.contains("Neo Geo", ignoreCase = true) == true

    return EmulationLayoutMetrics(
        isArcadeLayout = isArcadeLayout,
        dpadSize = (shortestSide * 0.28f).clamp(118.dp, 170.dp),
        dpadArrowSize = (shortestSide * 0.16f).clamp(64.dp, 96.dp),
        dpadCenterSize = (shortestSide * 0.075f).clamp(30.dp, 42.dp),
        actionClusterSize = (if (isArcadeLayout) width * 0.34f else shortestSide * 0.32f).clamp(148.dp, 220.dp),
        actionButtonSize = (shortestSide * if (isArcadeLayout) 0.15f else 0.16f).clamp(64.dp, 96.dp),
        shoulderWidth = (width * 0.16f).clamp(68.dp, 124.dp),
        shoulderHeight = (shortestSide * 0.07f).clamp(30.dp, 42.dp),
        miniButtonWidth = (width * 0.11f).clamp(48.dp, 76.dp),
        miniButtonHeight = (shortestSide * 0.06f).clamp(24.dp, 34.dp),
        utilityButtonSize = (shortestSide * 0.10f).clamp(42.dp, 58.dp),
        panelHorizontalPadding = (width * 0.04f).clamp(14.dp, 28.dp),
        panelVerticalPadding = (height * 0.018f).clamp(10.dp, 18.dp),
        clusterSpacing = (shortestSide * 0.03f).clamp(8.dp, 14.dp),
        centerSpacing = (shortestSide * 0.022f).clamp(6.dp, 12.dp),
        slotHorizontalPadding = (shortestSide * 0.04f).clamp(12.dp, 18.dp),
        slotVerticalPadding = (shortestSide * 0.018f).clamp(5.dp, 8.dp),
        statusBottomPadding = (height * 0.24f).clamp(132.dp, 210.dp)
    )
}

private fun Dp.clamp(min: Dp, max: Dp): Dp = when {
    this < min -> min
    this > max -> max
    else -> this
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun EmulationScreenPreview() {
    DeepMuleTheme {
        EmulationContent(
            currentGame = GameEntity(path = "sample.sfc", title = "Super Mario World", system = "snes"),
            isPaused = false, syncStatus = "Running",
            onExit = {}, onTogglePause = {}, onVirtualButtonPressed = { _, _ -> },
            onSaveState = {}, onLoadState = {},
            onSurfaceReady = {}, onSurfaceDestroyed = {},
            onGamepadKeyEvent = { _, _ -> false }
        )
    }
}

@Preview
@Composable
fun VirtualDPadPreview() {
    DeepMuleTheme {
        Surface(color = Color.Black, modifier = Modifier.padding(16.dp)) {
            VirtualDPad(Modifier.size(124.dp)) { _, _ -> }
        }
    }
}

@Preview
@Composable
fun VirtualActionButtonsPreview() {
    DeepMuleTheme {
        Surface(color = Color.Black, modifier = Modifier.padding(16.dp)) {
            VirtualActionButtons(Modifier.size(140.dp)) { _, _ -> }
        }
    }
}

@Preview
@Composable
fun EmulationComponentsPreview() {
    DeepMuleTheme {
        Column(
            modifier = Modifier.background(Color.Black).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ShoulderButton("L", VirtualButton.L, onButtonPressed = { _, _ -> })
                ShoulderButton("R", VirtualButton.R, onButtonPressed = { _, _ -> })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MiniButton("SEL", VirtualButton.SELECT) { _, _ -> }
                MiniButton("STA", VirtualButton.START) { _, _ -> }
            }
            VirtualDPad(Modifier.size(124.dp)) { _, _ -> }
            VirtualActionButtons(Modifier.size(140.dp)) { _, _ -> }
        }
    }
}
