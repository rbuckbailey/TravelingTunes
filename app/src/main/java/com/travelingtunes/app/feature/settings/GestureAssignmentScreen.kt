package com.travelingtunes.app.feature.settings

import androidx.activity.compose.BackHandler
import com.travelingtunes.app.core.model.ArtLayoutOption
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RadioButtonChecked
import com.travelingtunes.app.core.model.RadialMenuStyle
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.focusable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import com.travelingtunes.app.feature.player.keyCodeToKeyboardTrigger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelingtunes.app.core.datastore.SettingsDataStore
import com.travelingtunes.app.core.model.ConfigOption
import com.travelingtunes.app.core.model.DisplaySettings
import com.travelingtunes.app.core.model.GestureAction
import com.travelingtunes.app.core.model.GestureBinding
import com.travelingtunes.app.core.model.GestureCategory
import com.travelingtunes.app.core.model.GestureTrigger
import com.travelingtunes.app.feature.player.ActionIcon
import com.travelingtunes.app.feature.player.ConfigOptionIcon
import kotlinx.coroutines.launch

enum class GestureSubmenu(
    val title: String,
    val description: String,
    val icon: ImageVector
) {
    SWIPE(
        title = "Swipe Actions",
        description = "Configure 1, 2, and 3-finger swipe gestures",
        icon = Icons.Default.Swipe
    ),
    TAP(
        title = "Tap Actions",
        description = "Configure 1, 2, and 3-finger taps and long presses",
        icon = Icons.Default.TouchApp
    ),
    BUTTON(
        title = "Button Actions",
        description = "Configure top and bottom edge region screen buttons",
        icon = Icons.Default.RadioButtonChecked
    ),
    KEYBOARD(
        title = "Keyboard Controls",
        description = "Configure physical keyboard and media button shortcuts",
        icon = Icons.Default.Keyboard
    ),
    RADIAL_MENU(
        title = "Radial Menu Actions",
        description = "Configure action items (up to 12) for assigned Radial Menus",
        icon = Icons.Default.DonutLarge
    )
}

fun GestureTrigger.getSubmenu(): GestureSubmenu {
    return when (category) {
        GestureCategory.ONE_FINGER_SWIPE,
        GestureCategory.TWO_FINGER_SWIPE,
        GestureCategory.THREE_FINGER_SWIPE -> GestureSubmenu.SWIPE

        GestureCategory.ONE_FINGER_TAP,
        GestureCategory.TWO_FINGER_TAP,
        GestureCategory.THREE_FINGER_TAP,
        GestureCategory.LONG_PRESS -> GestureSubmenu.TAP

        GestureCategory.SCREEN_REGION -> GestureSubmenu.BUTTON

        GestureCategory.KEYBOARD -> GestureSubmenu.KEYBOARD
    }
}

fun getTriggersForSubmenu(
    submenu: GestureSubmenu,
    numEdgeRegions: Int,
    numArtEdgeRegions: Int = 3,
    isSeparateTouchZones: Boolean = false
): Map<String, List<GestureTrigger>> {
    return when (submenu) {
        GestureSubmenu.SWIPE -> mapOf(
            "1-Finger Swipes" to listOf(
                GestureTrigger.SWIPE_1_LEFT,
                GestureTrigger.SWIPE_1_RIGHT,
                GestureTrigger.SWIPE_1_UP,
                GestureTrigger.SWIPE_1_DOWN
            ),
            "2-Finger Swipes" to listOf(
                GestureTrigger.SWIPE_2_LEFT,
                GestureTrigger.SWIPE_2_RIGHT,
                GestureTrigger.SWIPE_2_UP,
                GestureTrigger.SWIPE_2_DOWN
            ),
            "3-Finger Swipes" to listOf(
                GestureTrigger.SWIPE_3_LEFT,
                GestureTrigger.SWIPE_3_RIGHT,
                GestureTrigger.SWIPE_3_UP,
                GestureTrigger.SWIPE_3_DOWN
            )
        )
        GestureSubmenu.TAP -> mapOf(
            "1-Finger Taps" to listOf(
                GestureTrigger.TAP_1_1,
                GestureTrigger.TAP_1_2,
                GestureTrigger.TAP_1_3
            ),
            "2-Finger Taps" to listOf(
                GestureTrigger.TAP_2_1,
                GestureTrigger.TAP_2_2,
                GestureTrigger.TAP_2_3
            ),
            "3-Finger Taps" to listOf(
                GestureTrigger.TAP_3_1,
                GestureTrigger.TAP_3_2,
                GestureTrigger.TAP_3_3
            ),
            "Long Presses" to listOf(
                GestureTrigger.LONG_PRESS_1,
                GestureTrigger.LONG_PRESS_2,
                GestureTrigger.LONG_PRESS_3
            )
        )
        GestureSubmenu.BUTTON -> {
            if (isSeparateTouchZones) {
                mapOf(
                    "Top Title Edge Regions" to GestureTrigger.getActiveTopTriggers(numEdgeRegions),
                    "Bottom Title Edge Regions" to GestureTrigger.getActiveBottomTriggers(numEdgeRegions),
                    "Top Art Edge Regions" to GestureTrigger.getActiveTopTriggers(numArtEdgeRegions),
                    "Bottom Art Edge Regions" to GestureTrigger.getActiveBottomTriggers(numArtEdgeRegions)
                )
            } else {
                mapOf(
                    "Top Edge Regions" to GestureTrigger.getActiveTopTriggers(numEdgeRegions),
                    "Bottom Edge Regions" to GestureTrigger.getActiveBottomTriggers(numEdgeRegions)
                )
            }
        }
        GestureSubmenu.KEYBOARD -> mapOf(
            "Navigation & Control Keys" to listOf(
                GestureTrigger.KEY_SPACE,
                GestureTrigger.KEY_F,
                GestureTrigger.KEY_LEFT,
                GestureTrigger.KEY_RIGHT,
                GestureTrigger.KEY_UP,
                GestureTrigger.KEY_DOWN,
                GestureTrigger.KEY_ESC,
                GestureTrigger.KEY_TAB,
                GestureTrigger.KEY_Q,
                GestureTrigger.KEY_QUESTION
            ),
            "F1-F12 Edge Buttons" to listOf(
                GestureTrigger.KEY_F1,
                GestureTrigger.KEY_F2,
                GestureTrigger.KEY_F3,
                GestureTrigger.KEY_F4,
                GestureTrigger.KEY_F5,
                GestureTrigger.KEY_F6,
                GestureTrigger.KEY_F7,
                GestureTrigger.KEY_F8,
                GestureTrigger.KEY_F9,
                GestureTrigger.KEY_F10,
                GestureTrigger.KEY_F11,
                GestureTrigger.KEY_F12
            ),
            "UI & Hardware Media Buttons" to listOf(
                GestureTrigger.KEY_MEDIA_PLAY_PAUSE,
                GestureTrigger.KEY_MEDIA_NEXT,
                GestureTrigger.KEY_MEDIA_PREVIOUS,
                GestureTrigger.KEY_MEDIA_FAST_FORWARD,
                GestureTrigger.KEY_MEDIA_REWIND,
                GestureTrigger.KEY_MEDIA_STOP
            )
        )
        GestureSubmenu.RADIAL_MENU -> emptyMap()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureAssignmentScreen(
    settingsDataStore: SettingsDataStore,
    gestureBindings: Map<GestureTrigger, GestureBinding>,
    onNavigateBack: () -> Unit,
    initialSubmenu: GestureSubmenu? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val displaySettings by settingsDataStore.displaySettingsFlow.collectAsState(initial = DisplaySettings())
    val numEdgeRegions = displaySettings.numEdgeRegions

    val radialMenuTriggers = remember(gestureBindings) {
        gestureBindings.filterValues { it.action == GestureAction.RADIAL_MENU }.keys.toList()
    }
    val hasRadialMenuAssigned = radialMenuTriggers.isNotEmpty()

    val availableSubmenus = remember(hasRadialMenuAssigned) {
        if (hasRadialMenuAssigned) {
            GestureSubmenu.entries
        } else {
            listOf(GestureSubmenu.SWIPE, GestureSubmenu.TAP, GestureSubmenu.BUTTON, GestureSubmenu.KEYBOARD)
        }
    }

    var selectedSubmenu by remember { mutableStateOf<GestureSubmenu?>(initialSubmenu) }
    var editingRadialTrigger by remember { mutableStateOf<GestureTrigger?>(null) }
    var showAddKeyDialog by remember { mutableStateOf(false) }

    val unassignedKeyboardTriggers = remember(gestureBindings) {
        GestureTrigger.entries
            .filter { it.category == GestureCategory.KEYBOARD }
            .filter { trigger ->
                val binding = gestureBindings[trigger] ?: GestureBinding(
                    trigger = trigger,
                    action = GestureAction.fromKey(trigger.defaultActionKey),
                    isContinuous = trigger.isContinuousDefault
                )
                binding.action == GestureAction.UNASSIGNED
            }
    }

    BackHandler(enabled = selectedSubmenu != null || editingRadialTrigger != null) {
        if (editingRadialTrigger != null) {
            editingRadialTrigger = null
        } else {
            selectedSubmenu = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val radialTrig = editingRadialTrigger
                    if (radialTrig != null) {
                        Text("${radialTrig.getDisplayName(numEdgeRegions)} Radial Menu")
                    } else {
                        Text(selectedSubmenu?.title ?: "Gesture Assignments")
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (editingRadialTrigger != null) {
                                editingRadialTrigger = null
                            } else if (selectedSubmenu != null) {
                                selectedSubmenu = null
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selectedSubmenu == GestureSubmenu.KEYBOARD && editingRadialTrigger == null) {
                        IconButton(onClick = { showAddKeyDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Keyboard Button")
                        }
                    }
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Exit to Play Screen")
                    }
                }
            )
        }
    ) { paddingValues ->
        val currentSubmenu = selectedSubmenu
        val currentRadialTrigger = editingRadialTrigger

        AnimatedContent(
            targetState = Pair(currentSubmenu, currentRadialTrigger),
            transitionSpec = {
                if (targetState.first != null || targetState.second != null) {
                    (slideInHorizontally { fullWidth -> fullWidth } + fadeIn())
                        .togetherWith(slideOutHorizontally { fullWidth -> -fullWidth } + fadeOut())
                } else {
                    (slideInHorizontally { fullWidth -> -fullWidth } + fadeIn())
                        .togetherWith(slideOutHorizontally { fullWidth -> fullWidth } + fadeOut())
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            label = "SubmenuTransition"
        ) { (sub, radialTrigger) ->
            if (radialTrigger != null) {
                // Radial Menu Slots Editor
                val radialActionsFlow = remember(radialTrigger) {
                    settingsDataStore.getRadialMenuActionsFlow(radialTrigger.key)
                }
                val currentRadialActions by radialActionsFlow.collectAsState(initial = SettingsDataStore.DEFAULT_RADIAL_ACTIONS)

                RadialMenuConfigurator(
                    trigger = radialTrigger,
                    numEdgeRegions = numEdgeRegions,
                    actions = currentRadialActions,
                    settingsDataStore = settingsDataStore,
                    onUpdateActions = { newActions ->
                        coroutineScope.launch {
                            settingsDataStore.updateRadialMenuActions(radialTrigger.key, newActions)
                        }
                    },
                    onResetDefaults = {
                        coroutineScope.launch {
                            settingsDataStore.resetRadialMenuActions(radialTrigger.key)
                        }
                    }
                )
            } else if (sub == null) {
                // Submenus Selection List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(availableSubmenus) { submenu ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable { selectedSubmenu = submenu }
                        ) {
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = submenu.title,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        text = submenu.description,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
                                leadingContent = {
                                    Icon(
                                        imageVector = submenu.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingContent = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Open ${submenu.title}"
                                    )
                                }
                            )
                        }
                    }
                }
            } else if (sub == GestureSubmenu.RADIAL_MENU) {
                // List of triggers that have Radial Menu assigned
                if (radialMenuTriggers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No gesture is currently assigned to 'Radial Menu'. Assign 'Radial Menu' to any swipe, tap, or button gesture to configure its actions.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Text(
                                text = "Configured Radial Menus",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }
                        items(radialMenuTriggers) { trigger ->
                            ListItem(
                                headlineContent = {
                                    Text(trigger.getDisplayName(numEdgeRegions), fontWeight = FontWeight.SemiBold)
                                },
                                supportingContent = {
                                    Text("Tap to edit action slots (up to 12)")
                                },
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.Default.DonutLarge,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingContent = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null
                                    )
                                },
                                modifier = Modifier.clickable {
                                    editingRadialTrigger = trigger
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            } else {
                // Swipe, Tap, Button, or Keyboard Actions list
                val isDocked = displaySettings.artDisplayLayout == ArtLayoutOption.DOCKED
                val isSeparate = isDocked && displaySettings.separateTouchZones && !displaySettings.adaptiveDockedArt
                val numArtEdgeRegions = displaySettings.numArtEdgeRegions
                val visibleSections = remember(sub, numEdgeRegions, numArtEdgeRegions, isSeparate, gestureBindings) {
                    val rawSections = getTriggersForSubmenu(sub, numEdgeRegions, numArtEdgeRegions, isSeparate)
                    if (sub == GestureSubmenu.KEYBOARD) {
                        rawSections.mapValues { (_, triggers) ->
                            triggers.filter { trigger ->
                                val currentBinding = gestureBindings[trigger] ?: GestureBinding(
                                    trigger = trigger,
                                    action = GestureAction.fromKey(trigger.defaultActionKey),
                                    isContinuous = trigger.isContinuousDefault
                                )
                                currentBinding.action != GestureAction.UNASSIGNED
                            }
                        }.filterValues { it.isNotEmpty() }
                    } else {
                        rawSections
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (sub == GestureSubmenu.KEYBOARD && visibleSections.isEmpty()) {
                        item(key = "empty_keyboard_message") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No keyboard buttons are currently assigned. Tap '+' to add keyboard buttons.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        visibleSections.forEach { (sectionTitle, triggers) ->
                            item(key = "header_$sectionTitle") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Text(
                                        text = sectionTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            items(triggers, key = { "${sectionTitle}_${it.name}" }) { trigger ->
                                val currentBinding = gestureBindings[trigger] ?: GestureBinding(
                                    trigger = trigger,
                                    action = GestureAction.fromKey(trigger.defaultActionKey),
                                    isContinuous = trigger.isContinuousDefault
                                )

                                val isArtSection = sectionTitle.contains("Art")
                                val isTitleSection = sectionTitle.contains("Title")
                                val numRegions = if (isArtSection) numArtEdgeRegions else numEdgeRegions

                                GestureAssignmentItem(
                                    trigger = trigger,
                                    binding = currentBinding,
                                    numEdgeRegions = numRegions,
                                    isArtSection = isArtSection,
                                    isTitleSection = isTitleSection,
                                    isSeparateTouchZones = isSeparate,
                                    onActionSelected = { regionTarget, newAction, otherKey ->
                                        coroutineScope.launch {
                                            when (regionTarget) {
                                                com.travelingtunes.app.core.model.TouchRegionTarget.BOTH -> {
                                                    settingsDataStore.updateGestureBinding(
                                                        trigger = trigger,
                                                        action = newAction,
                                                        isContinuous = currentBinding.isContinuous,
                                                        otherOptionKey = otherKey,
                                                        artAction = GestureAction.UNASSIGNED,
                                                        artOtherOptionKey = null,
                                                        titleAction = GestureAction.UNASSIGNED,
                                                        titleOtherOptionKey = null
                                                    )
                                                }
                                                com.travelingtunes.app.core.model.TouchRegionTarget.ART -> {
                                                    settingsDataStore.updateGestureBinding(
                                                        trigger = trigger,
                                                        action = GestureAction.UNASSIGNED,
                                                        isContinuous = currentBinding.isContinuous,
                                                        otherOptionKey = null,
                                                        artAction = newAction,
                                                        artOtherOptionKey = otherKey,
                                                        titleAction = currentBinding.titleAction,
                                                        titleOtherOptionKey = currentBinding.titleOtherOptionKey
                                                    )
                                                }
                                                com.travelingtunes.app.core.model.TouchRegionTarget.TITLE -> {
                                                    settingsDataStore.updateGestureBinding(
                                                        trigger = trigger,
                                                        action = GestureAction.UNASSIGNED,
                                                        isContinuous = currentBinding.isContinuous,
                                                        otherOptionKey = null,
                                                        artAction = currentBinding.artAction,
                                                        artOtherOptionKey = currentBinding.artOtherOptionKey,
                                                        titleAction = newAction,
                                                        titleOtherOptionKey = otherKey
                                                    )
                                                }
                                            }
                                        }
                                    }
                                )
                                HorizontalDivider()
                            }
                        }
                    }

                    if (sub == GestureSubmenu.KEYBOARD && unassignedKeyboardTriggers.isNotEmpty()) {
                        item(key = "add_keyboard_button_item") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Button(
                                    onClick = { showAddKeyDialog = true }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Add Keyboard Button")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddKeyDialog) {
        AddKeyboardControlDialog(
            unassignedTriggers = unassignedKeyboardTriggers,
            onAddKeyBinding = { trigger, action, otherKey ->
                coroutineScope.launch {
                    settingsDataStore.updateGestureBinding(
                        trigger = trigger,
                        action = action,
                        otherOptionKey = otherKey
                    )
                }
                showAddKeyDialog = false
            },
            onDismissRequest = { showAddKeyDialog = false }
        )
    }
}

@Composable
private fun RadialMenuConfigurator(
    trigger: GestureTrigger,
    numEdgeRegions: Int,
    actions: List<GestureAction>,
    settingsDataStore: SettingsDataStore,
    onUpdateActions: (List<GestureAction>) -> Unit,
    onResetDefaults: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var editingSlotIndex by remember { mutableStateOf<Int?>(null) }
    val menuStyle by settingsDataStore.getRadialMenuStyleFlow(trigger.key).collectAsState(initial = RadialMenuStyle.FAN)
    val isEdgeTrigger = trigger.category == GestureCategory.SCREEN_REGION ||
            trigger.key.startsWith("KeyF", ignoreCase = true)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "${trigger.getDisplayName(numEdgeRegions)} Actions (${actions.size}/12)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Pop up actions arranged on screen for quick access. Drag or tap to activate.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Menu Layout Style",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isEdgeTrigger) "Edge button menu format" else "Radial menu layout format",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadialMenuStyle.entries.forEach { styleOption ->
                            FilterChip(
                                selected = menuStyle == styleOption,
                                onClick = {
                                    coroutineScope.launch {
                                        settingsDataStore.updateRadialMenuStyle(trigger.key, styleOption)
                                    }
                                },
                                label = {
                                    Text(
                                        text = styleOption.displayName,
                                        fontWeight = if (menuStyle == styleOption) FontWeight.Bold else FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        itemsIndexed(actions) { index, action ->
            var isDropdownExpanded by remember { mutableStateOf(false) }
            val radialOptionKey by settingsDataStore.getRadialOtherOptionFlow(trigger.key, index).collectAsState(initial = null)
            val assignedOption = remember(radialOptionKey) {
                ConfigOption.findByKey(radialOptionKey) ?: ConfigOption.ALL_OPTIONS.first()
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "Slot ${index + 1}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(60.dp)
                )

                Box(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isDropdownExpanded = true }
                            .padding(vertical = 8.dp)
                    ) {
                        ActionIcon(
                            action = action,
                            optionKey = if (action == GestureAction.OTHER_OPTION) assignedOption.key else null,
                            iconSize = 24.dp,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = action.displayName,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (action == GestureAction.OTHER_OPTION) {
                                Text(
                                    text = assignedOption.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.clickable { editingSlotIndex = index }
                                )
                            }
                        }
                    }

                    if (isDropdownExpanded) {
                        ActionSelectionDialog(
                            title = "Select Action for Slot ${index + 1}",
                            excludeRadialMenu = true,
                            excludeUnassigned = true,
                            currentOptionKey = assignedOption.key,
                            onDismissRequest = { isDropdownExpanded = false },
                            onActionSelected = { choice ->
                                val updated = actions.toMutableList()
                                updated[index] = choice
                                onUpdateActions(updated)
                                isDropdownExpanded = false
                                if (choice == GestureAction.OTHER_OPTION) {
                                    editingSlotIndex = index
                                }
                            }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (index > 0) {
                        IconButton(
                            onClick = {
                                val updated = actions.toMutableList()
                                val temp = updated[index]
                                updated[index] = updated[index - 1]
                                updated[index - 1] = temp
                                onUpdateActions(updated)
                                coroutineScope.launch {
                                    settingsDataStore.swapRadialOtherOptions(trigger.key, index, index - 1)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Move Up",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (index < actions.size - 1) {
                        IconButton(
                            onClick = {
                                val updated = actions.toMutableList()
                                val temp = updated[index]
                                updated[index] = updated[index + 1]
                                updated[index + 1] = temp
                                onUpdateActions(updated)
                                coroutineScope.launch {
                                    settingsDataStore.swapRadialOtherOptions(trigger.key, index, index + 1)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Move Down",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (actions.size > 1) {
                        IconButton(
                            onClick = {
                                val updated = actions.toMutableList()
                                updated.removeAt(index)
                                onUpdateActions(updated)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove Slot",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            HorizontalDivider()

            val activeEditingIndex = editingSlotIndex
            if (activeEditingIndex == index) {
                ConfigOptionPickerDialog(
                    initialKey = radialOptionKey ?: assignedOption.key,
                    onOptionSelected = { selectedOption ->
                        editingSlotIndex = null
                        coroutineScope.launch {
                            settingsDataStore.updateRadialOtherOption(trigger.key, index, selectedOption.key)
                        }
                    },
                    onDismissRequest = { editingSlotIndex = null }
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (actions.size < 12) {
                    Button(
                        onClick = {
                            val updated = actions.toMutableList()
                            updated.add(GestureAction.PLAY_PAUSE)
                            onUpdateActions(updated)
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Action")
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                OutlinedButton(
                    onClick = onResetDefaults
                ) {
                    Icon(Icons.Default.Replay, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset Defaults")
                }
            }
        }
    }
}

@Composable
private fun GestureAssignmentItem(
    trigger: GestureTrigger,
    binding: GestureBinding,
    numEdgeRegions: Int,
    isArtSection: Boolean = false,
    isTitleSection: Boolean = false,
    isSeparateTouchZones: Boolean = false,
    onActionSelected: (com.travelingtunes.app.core.model.TouchRegionTarget, GestureAction, String?) -> Unit
) {
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var editingOptionRegion by remember { mutableStateOf<com.travelingtunes.app.core.model.TouchRegionTarget?>(null) }

    val isButton = trigger.category == GestureCategory.SCREEN_REGION

    val hasArtAction = binding.artAction != GestureAction.UNASSIGNED
    val hasTitleAction = binding.titleAction != GestureAction.UNASSIGNED
    val hasBothAction = binding.action != GestureAction.UNASSIGNED

    val displayLines = remember(binding, isButton) {
        val list = mutableListOf<Triple<String, GestureAction, String?>>()
        if (hasArtAction) {
            list.add(Triple("Art", binding.artAction, binding.artOtherOptionKey))
        }
        if (hasTitleAction) {
            list.add(Triple("Title", binding.titleAction, binding.titleOtherOptionKey))
        }
        if (!isButton && (hasBothAction || list.isEmpty())) {
            list.add(Triple("Both", binding.action, binding.otherOptionKey))
        } else if (isButton && list.isEmpty()) {
            if (hasBothAction) {
                list.add(Triple("Title", binding.action, binding.otherOptionKey))
            } else {
                list.add(Triple("Title", GestureAction.UNASSIGNED, null))
            }
        }
        list
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isDropdownExpanded = true }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = trigger.getDisplayName(numEdgeRegions, isArt = isArtSection, isTitle = isTitleSection), fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            displayLines.forEach { (regionName, action, otherKey) ->
                val optionTitle = remember(otherKey) {
                    if (action == GestureAction.OTHER_OPTION) {
                        ConfigOption.findByKey(otherKey)?.title ?: ConfigOption.ALL_OPTIONS.first().title
                    } else null
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 1.dp)
                ) {
                    ActionIcon(
                        action = action,
                        optionKey = if (action == GestureAction.OTHER_OPTION) otherKey else null,
                        iconSize = 18.dp,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$regionName: ${action.displayName}${if (optionTitle != null) " ($optionTitle)" else ""}",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Box {
            val primaryAction = displayLines.firstOrNull()?.second ?: binding.action
            val primaryOtherKey = displayLines.firstOrNull()?.third ?: binding.otherOptionKey
            ActionIcon(
                action = primaryAction,
                optionKey = if (primaryAction == GestureAction.OTHER_OPTION) primaryOtherKey else null,
                iconSize = 28.dp,
                tint = MaterialTheme.colorScheme.primary
            )

            if (isDropdownExpanded) {
                ActionSelectionDialog(
                    title = "Select Action for ${trigger.getDisplayName(numEdgeRegions, isArt = isArtSection, isTitle = isTitleSection)}",
                    excludeRadialMenu = false,
                    excludeUnassigned = false,
                    currentOptionKey = (displayLines.firstOrNull()?.third ?: binding.otherOptionKey),
                    isSeparateTouchZones = isSeparateTouchZones,
                    isButton = isButton,
                    binding = binding,
                    displayLines = displayLines,
                    onDismissRequest = { isDropdownExpanded = false },
                    onActionSelected = { choice ->
                        isDropdownExpanded = false
                        val targetTarget = if (isButton) com.travelingtunes.app.core.model.TouchRegionTarget.TITLE else com.travelingtunes.app.core.model.TouchRegionTarget.BOTH
                        if (choice == GestureAction.OTHER_OPTION) {
                            editingOptionRegion = targetTarget
                        } else {
                            onActionSelected(targetTarget, choice, null)
                        }
                    },
                    onRegionActionSelected = { target, choice, key ->
                        isDropdownExpanded = false
                        onActionSelected(target, choice, key)
                    },
                    onOpenOtherOptionPicker = { regionTarget ->
                        isDropdownExpanded = false
                        editingOptionRegion = regionTarget
                    }
                )
            }
        }
    }

    val activeEditingRegion = editingOptionRegion
    if (activeEditingRegion != null) {
        val currentKey = when (activeEditingRegion) {
            com.travelingtunes.app.core.model.TouchRegionTarget.ART -> binding.artOtherOptionKey
            com.travelingtunes.app.core.model.TouchRegionTarget.TITLE -> binding.titleOtherOptionKey
            com.travelingtunes.app.core.model.TouchRegionTarget.BOTH -> binding.otherOptionKey
        }
        val initialOpt = ConfigOption.findByKey(currentKey) ?: ConfigOption.ALL_OPTIONS.first()
        ConfigOptionPickerDialog(
            initialKey = currentKey ?: initialOpt.key,
            onOptionSelected = { selectedOption ->
                editingOptionRegion = null
                onActionSelected(activeEditingRegion, GestureAction.OTHER_OPTION, selectedOption.key)
            },
            onDismissRequest = { editingOptionRegion = null }
        )
    }
}

private fun ConfigOption.getSubmenu(): SettingsSubmenu {
    return when {
        key.startsWith("PROFILE_") -> SettingsSubmenu.PROFILES
        key.startsWith("LIBRARY_") -> SettingsSubmenu.LIBRARY
        key.startsWith("ALIGN_") || key.startsWith("DISPLAY_artist") ||
        key.startsWith("DISPLAY_song") || key.startsWith("DISPLAY_album") ||
        key.startsWith("DISPLAY_title") -> SettingsSubmenu.TITLES
        key.startsWith("ART_") || key == "DISPLAY_showAlbumArt" ||
        key == "DISPLAY_albumArtColors" -> SettingsSubmenu.ART
        key.startsWith("HUD_TYPE_") || key.startsWith("SCRUB_HUD_TYPE_") ||
        key == "DISPLAY_volumeAlwaysOn" || key == "DISPLAY_showStatusBar" ||
        key == "DISPLAY_showActions" || key == "DISPLAY_keepScreenOn" ||
        key == "DISPLAY_immersiveMode" -> SettingsSubmenu.HUD
        key.startsWith("THEME_") -> SettingsSubmenu.THEMES
        else -> SettingsSubmenu.THEMES
    }
}

private fun ConfigOption.getSectionName(): String {
    return when {
        key.startsWith("PROFILE_") -> "Active Profile"
        key.startsWith("ALIGN_ARTIST_") -> "Artist Alignment"
        key.startsWith("ALIGN_SONG_") -> "Song Alignment"
        key.startsWith("ALIGN_ALBUM_") -> "Album Alignment"
        key.startsWith("DISPLAY_artist") || key.startsWith("DISPLAY_song") || key.startsWith("DISPLAY_album") -> "Text Styling"
        key.startsWith("DISPLAY_title") -> "Title Behavior"
        key == "DISPLAY_showAlbumArt" || key == "DISPLAY_albumArtColors" -> "Visibility & Dynamic Colors"
        key.startsWith("ART_SCALE_") || key.startsWith("ART_LAYOUT_") -> "Scale & Layout"
        key.startsWith("ART_ALIGN_PORT_") -> "Portrait Alignment"
        key.startsWith("ART_ALIGN_LAND_") -> "Landscape Alignment"
        key.startsWith("HUD_TYPE_") -> "Volume HUD Type"
        key.startsWith("SCRUB_HUD_TYPE_") -> "Scrub HUD Type"
        key == "DISPLAY_volumeAlwaysOn" || key == "DISPLAY_showStatusBar" ||
        key == "DISPLAY_showActions" || key == "DISPLAY_keepScreenOn" ||
        key == "DISPLAY_immersiveMode" -> "Display Toggles"
        key.startsWith("THEME_") && !key.startsWith("THEME_dim") && !key.startsWith("THEME_invert") && !key.startsWith("THEME_is") -> "Theme Presets"
        key.startsWith("THEME_") -> "Theme Effects"
        key.startsWith("LIBRARY_") -> "Library & Audio Preferences"
        else -> "General Options"
    }
}

@Composable
fun ConfigOptionPickerDialog(
    initialKey: String?,
    onOptionSelected: (ConfigOption) -> Unit,
    onDismissRequest: () -> Unit
) {
    var selectedSubmenu by remember { mutableStateOf<SettingsSubmenu?>(null) }
    val initialOption = remember(initialKey) { ConfigOption.findByKey(initialKey) }
    val initialSubmenu = remember(initialOption) { initialOption?.getSubmenu() }

    BackHandler(enabled = selectedSubmenu != null) {
        selectedSubmenu = null
    }

    val optionSubmenus = remember {
        listOf(
            SettingsSubmenu.PROFILES,
            SettingsSubmenu.LIBRARY,
            SettingsSubmenu.TITLES,
            SettingsSubmenu.ART,
            SettingsSubmenu.HUD,
            SettingsSubmenu.THEMES
        )
    }

    val standaloneSubmenus = remember { optionSubmenus.filter { it.categoryGroup == null } }
    val groupedSubmenus = remember { optionSubmenus.filter { it.categoryGroup != null }.groupBy { it.categoryGroup!! } }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.fillMaxWidth(0.95f).widthIn(max = 680.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (selectedSubmenu != null) {
                    IconButton(
                        onClick = { selectedSubmenu = null },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Menu Overview"
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Column {
                    Text(
                        text = selectedSubmenu?.title ?: "Select Menu Option",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (selectedSubmenu != null) {
                            selectedSubmenu!!.description
                        } else {
                            "Navigate settings submenus to choose an option"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
            ) {
                AnimatedContent(
                    targetState = selectedSubmenu,
                    transitionSpec = {
                        if (targetState != null) {
                            (slideInHorizontally { fullWidth -> fullWidth } + fadeIn())
                                .togetherWith(slideOutHorizontally { fullWidth -> -fullWidth } + fadeOut())
                        } else {
                            (slideInHorizontally { fullWidth -> -fullWidth } + fadeIn())
                                .togetherWith(slideOutHorizontally { fullWidth -> fullWidth } + fadeOut())
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    label = "ConfigOptionMenuTransition"
                ) { targetSubmenu ->
                    if (targetSubmenu == null) {
                        // Level 1: Simulated Main Settings Menu
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (initialOption != null && initialSubmenu != null) {
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp)
                                        .clickable { selectedSubmenu = initialSubmenu }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Currently Assigned Option",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${initialSubmenu.title} › ${initialOption.title}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = "Open ${initialSubmenu.title}",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            // Standalone items (Music Library)
                            standaloneSubmenus.forEach { submenu ->
                                SubmenuCategoryCard(
                                    submenu = submenu,
                                    isActiveSubmenu = initialSubmenu == submenu,
                                    activeOptionTitle = if (initialSubmenu == submenu) initialOption?.title else null,
                                    onClick = { selectedSubmenu = submenu }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // Grouped items (Appearance: Titles, Art, HUD, Colors)
                            groupedSubmenus.forEach { (groupName, submenus) ->
                                Text(
                                    text = groupName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                                )
                                submenus.forEach { submenu ->
                                    SubmenuCategoryCard(
                                        submenu = submenu,
                                        isActiveSubmenu = initialSubmenu == submenu,
                                        activeOptionTitle = if (initialSubmenu == submenu) initialOption?.title else null,
                                        onClick = { selectedSubmenu = submenu }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    } else {
                        // Level 2: Submenu Options List
                        val submenuOptions = remember(targetSubmenu) {
                            ConfigOption.ALL_OPTIONS.filter { it.getSubmenu() == targetSubmenu }
                        }
                        val sections = remember(submenuOptions) {
                            submenuOptions.groupBy { it.getSectionName() }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            sections.forEach { (sectionName, options) ->
                                Text(
                                    text = sectionName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp, start = 4.dp)
                                )
                                options.forEach { option ->
                                    val isSelected = option.key == initialKey
                                    Card(
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected)
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                            .clickable { onOptionSelected(option) }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                        ) {
                                            ConfigOptionIcon(
                                                optionKey = option.key,
                                                iconSize = 24.dp,
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = option.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SubmenuCategoryCard(
    submenu: SettingsSubmenu,
    isActiveSubmenu: Boolean,
    activeOptionTitle: String?,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActiveSubmenu)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActiveSubmenu) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = submenu.icon,
                    contentDescription = null,
                    tint = if (isActiveSubmenu) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = submenu.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = submenu.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (activeOptionTitle != null) {
                    Text(
                        text = "Active: $activeOptionTitle",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatKeyEventName(nativeEvent: android.view.KeyEvent): Pair<String, GestureTrigger?> {
    val keyCode = nativeEvent.keyCode
    val isShift = nativeEvent.isShiftPressed
    val isCtrl = nativeEvent.isCtrlPressed
    val isAlt = nativeEvent.isAltPressed
    val isMeta = nativeEvent.isMetaPressed

    val unicodeChar = nativeEvent.getUnicodeChar(nativeEvent.metaState)
    val trigger = keyCodeToKeyboardTrigger(keyCode, isShift, unicodeChar)

    val baseName = when (keyCode) {
        android.view.KeyEvent.KEYCODE_SPACE -> "Space Bar"
        android.view.KeyEvent.KEYCODE_DPAD_LEFT -> "Left Arrow"
        android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> "Right Arrow"
        android.view.KeyEvent.KEYCODE_DPAD_UP -> "Up Arrow"
        android.view.KeyEvent.KEYCODE_DPAD_DOWN -> "Down Arrow"
        android.view.KeyEvent.KEYCODE_ESCAPE -> "Escape"
        android.view.KeyEvent.KEYCODE_TAB -> "Tab"
        android.view.KeyEvent.KEYCODE_SLASH -> "?"
        android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
        android.view.KeyEvent.KEYCODE_MEDIA_PLAY,
        android.view.KeyEvent.KEYCODE_MEDIA_PAUSE -> "Media Play/Pause"
        android.view.KeyEvent.KEYCODE_MEDIA_NEXT -> "Media Next"
        android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS -> "Media Previous"
        android.view.KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> "Media Fast Forward"
        android.view.KeyEvent.KEYCODE_MEDIA_REWIND -> "Media Rewind"
        android.view.KeyEvent.KEYCODE_MEDIA_STOP -> "Media Stop"
        in android.view.KeyEvent.KEYCODE_F1..android.view.KeyEvent.KEYCODE_F12 -> "F${keyCode - android.view.KeyEvent.KEYCODE_F1 + 1}"
        else -> {
            val charStr = if (unicodeChar > 32) unicodeChar.toChar().uppercase() else ""
            if (charStr.isNotEmpty()) charStr
            else android.view.KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_")
        }
    }

    val prefix = buildString {
        if (isCtrl && keyCode != android.view.KeyEvent.KEYCODE_CTRL_LEFT && keyCode != android.view.KeyEvent.KEYCODE_CTRL_RIGHT) append("Ctrl + ")
        if (isAlt && keyCode != android.view.KeyEvent.KEYCODE_ALT_LEFT && keyCode != android.view.KeyEvent.KEYCODE_ALT_RIGHT) append("Alt + ")
        if (isShift && keyCode != android.view.KeyEvent.KEYCODE_SHIFT_LEFT && keyCode != android.view.KeyEvent.KEYCODE_SHIFT_RIGHT) append("Shift + ")
        if (isMeta && keyCode != android.view.KeyEvent.KEYCODE_META_LEFT && keyCode != android.view.KeyEvent.KEYCODE_META_RIGHT) append("Cmd + ")
    }

    return Pair("$prefix$baseName", trigger)
}

@Composable
private fun AddKeyboardControlDialog(
    unassignedTriggers: List<GestureTrigger>,
    onAddKeyBinding: (GestureTrigger, GestureAction, String?) -> Unit,
    onDismissRequest: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    var currentHoldingKeyName by remember { mutableStateOf<String?>(null) }
    var currentHoldingTrigger by remember { mutableStateOf<GestureTrigger?>(null) }
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var statusMessage by remember { mutableStateOf("Press and hold any key or combination for 0.5 seconds") }

    var capturedKeyName by remember { mutableStateOf<String?>(null) }
    var capturedTrigger by remember { mutableStateOf<GestureTrigger?>(null) }

    var selectedAction by remember { mutableStateOf(GestureAction.PLAY_PAUSE) }
    var editingOtherOption by remember { mutableStateOf(false) }
    var selectedOtherOptionKey by remember { mutableStateOf<String?>(null) }

    var isActionDropdownExpanded by remember { mutableStateOf(false) }
    var holdJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    if (unassignedTriggers.isEmpty() && capturedTrigger == null) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text("Add Keyboard Button") },
            text = { Text("All keyboard buttons are currently assigned.") },
            confirmButton = {
                TextButton(onClick = onDismissRequest) {
                    Text("OK")
                }
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = {
            holdJob?.cancel()
            onDismissRequest()
        },
        title = {
            Text(
                text = if (capturedKeyName == null) "Define Keyboard Shortcut" else "Assign Action",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .focusable()
                    .onPreviewKeyEvent { keyEvent ->
                        if (capturedKeyName != null) return@onPreviewKeyEvent false

                        val nativeEvent = keyEvent.nativeKeyEvent
                        val (keyName, trigger) = formatKeyEventName(nativeEvent)

                        if (keyEvent.type == KeyEventType.KeyDown) {
                            if (currentHoldingKeyName != keyName) {
                                holdJob?.cancel()
                                currentHoldingKeyName = keyName
                                currentHoldingTrigger = trigger
                                holdProgress = 0f
                                statusMessage = "Holding '$keyName'..."

                                holdJob = coroutineScope.launch {
                                    val startTime = System.currentTimeMillis()
                                    while (isActive) {
                                        val elapsed = System.currentTimeMillis() - startTime
                                        val p = (elapsed.toFloat() / 500f).coerceIn(0f, 1f)
                                        holdProgress = p
                                        if (p >= 1f) {
                                            capturedKeyName = keyName
                                            val finalTrigger = trigger
                                                ?: unassignedTriggers.firstOrNull()
                                                ?: GestureTrigger.KEY_SPACE
                                            capturedTrigger = finalTrigger
                                            val defaultAct = GestureAction.fromKey(finalTrigger.defaultActionKey)
                                            selectedAction = if (defaultAct != GestureAction.UNASSIGNED) defaultAct else GestureAction.PLAY_PAUSE
                                            currentHoldingKeyName = null
                                            holdProgress = 0f
                                            statusMessage = "Key captured!"
                                            break
                                        }
                                        delay(16)
                                    }
                                }
                            }
                            return@onPreviewKeyEvent true
                        } else if (keyEvent.type == KeyEventType.KeyUp) {
                            if (currentHoldingKeyName != null) {
                                holdJob?.cancel()
                                holdJob = null
                                currentHoldingKeyName = null
                                holdProgress = 0f
                                statusMessage = "Key released too early. Hold for 0.5s to register."
                            }
                            return@onPreviewKeyEvent true
                        }
                        false
                    }
            ) {
                if (capturedKeyName == null) {
                    // Stage 1: Key Press & Hold 0.5s Ring
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Text(
                            text = statusMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(160.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { holdProgress },
                                modifier = Modifier.fillMaxSize(),
                                strokeWidth = 10.dp,
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                val activeName = currentHoldingKeyName
                                if (activeName != null) {
                                    Text(
                                        text = activeName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${(holdProgress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Keyboard,
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Press Key",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Stage 2: Select Action for Captured Key
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Shortcut Captured",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = capturedKeyName!!,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Assigned Action",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { isActionDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        ActionIcon(
                                            action = selectedAction,
                                            optionKey = if (selectedAction == GestureAction.OTHER_OPTION) selectedOtherOptionKey else null,
                                            iconSize = 20.dp,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        val displayTitle = if (selectedAction == GestureAction.OTHER_OPTION) {
                                            ConfigOption.findByKey(selectedOtherOptionKey)?.title ?: "Select Option..."
                                        } else {
                                            selectedAction.displayName
                                        }
                                        Text(
                                            text = displayTitle,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null
                                    )
                                }
                            }

                            if (isActionDropdownExpanded) {
                                ActionSelectionDialog(
                                    title = "Select Shortcut Action",
                                    excludeRadialMenu = false,
                                    excludeUnassigned = true,
                                    currentOptionKey = selectedOtherOptionKey,
                                    onDismissRequest = { isActionDropdownExpanded = false },
                                    onActionSelected = { choice ->
                                        selectedAction = choice
                                        isActionDropdownExpanded = false
                                        if (choice == GestureAction.OTHER_OPTION) {
                                            editingOtherOption = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (capturedKeyName != null) {
                Button(
                    onClick = {
                        val targetTrig = capturedTrigger
                        if (targetTrig != null) {
                            onAddKeyBinding(
                                targetTrig,
                                selectedAction,
                                if (selectedAction == GestureAction.OTHER_OPTION) selectedOtherOptionKey else null
                            )
                        }
                    }
                ) {
                    Text("Add Shortcut")
                }
            }
        },
        dismissButton = {
            if (capturedKeyName != null) {
                OutlinedButton(
                    onClick = {
                        capturedKeyName = null
                        capturedTrigger = null
                        statusMessage = "Press and hold any key or combination for 0.5 seconds"
                    }
                ) {
                    Text("Re-record")
                }
            } else {
                TextButton(
                    onClick = {
                        holdJob?.cancel()
                        onDismissRequest()
                    }
                ) {
                    Text("Cancel")
                }
            }
        }
    )

    if (editingOtherOption) {
        ConfigOptionPickerDialog(
            initialKey = selectedOtherOptionKey,
            onOptionSelected = { selectedOpt ->
                selectedOtherOptionKey = selectedOpt.key
                editingOtherOption = false
            },
            onDismissRequest = { editingOtherOption = false }
        )
    }
}

/**
 * =========================================================================================
 * CRUCIAL ARCHITECTURAL FEATURE:
 * ActionSelectionDialog presents GestureActions organized into collapsible categories that are
 * COLLAPSED BY DEFAULT.
 *
 * It is strictly constrained to at most 70% of the screen height, ensuring it NEVER expands
 * off the bottom edge even when all sections are fully opened.
 *
 * GestureAction.OTHER_OPTION ("Other Option") is ALWAYS placed at the top level at the end
 * of the dialog, fully visible without expanding any category.
 * =========================================================================================
 */
@Composable
fun ActionSelectionDialog(
    title: String = "Select Action",
    excludeRadialMenu: Boolean = false,
    excludeUnassigned: Boolean = false,
    currentOptionKey: String? = null,
    isSeparateTouchZones: Boolean = false,
    isButton: Boolean = false,
    binding: GestureBinding? = null,
    displayLines: List<Triple<String, GestureAction, String?>> = emptyList(),
    onDismissRequest: () -> Unit,
    onActionSelected: (GestureAction) -> Unit,
    onRegionActionSelected: ((com.travelingtunes.app.core.model.TouchRegionTarget, GestureAction, String?) -> Unit)? = null,
    onOpenOtherOptionPicker: ((com.travelingtunes.app.core.model.TouchRegionTarget?) -> Unit)? = null
) {
    val categoryGroups = remember(excludeRadialMenu, excludeUnassigned) {
        GestureAction.getGroupedCategories(excludeRadialMenu, excludeUnassigned)
    }

    val expandedCategories = remember { mutableStateMapOf<com.travelingtunes.app.core.model.ActionCategory, Boolean>() }
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val maxDialogHeightDp = (configuration.screenHeightDp * 0.70f).dp.coerceAtLeast(280.dp)

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .heightIn(max = maxDialogHeightDp),
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                for (group in categoryGroups) {
                    val cat = group.category
                    if (cat == com.travelingtunes.app.core.model.ActionCategory.CUSTOM) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        for (choice in group.actions) {
                            ActionDialogItemRow(
                                choice = choice,
                                currentOptionKey = currentOptionKey,
                                isSeparateTouchZones = isSeparateTouchZones,
                                isButton = isButton,
                                binding = binding,
                                displayLines = displayLines,
                                onActionSelected = onActionSelected,
                                onRegionActionSelected = onRegionActionSelected,
                                onOpenOtherOptionPicker = onOpenOtherOptionPicker,
                                onDismissRequest = onDismissRequest
                            )
                        }
                    } else {
                        val isExpanded = expandedCategories[cat] ?: false
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { expandedCategories[cat] = !isExpanded }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = cat.displayName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                if (isExpanded) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                    Column(modifier = Modifier.padding(vertical = 2.dp)) {
                                        for (choice in group.actions) {
                                            ActionDialogItemRow(
                                                choice = choice,
                                                currentOptionKey = currentOptionKey,
                                                isSeparateTouchZones = isSeparateTouchZones,
                                                isButton = isButton,
                                                binding = binding,
                                                displayLines = displayLines,
                                                onActionSelected = onActionSelected,
                                                onRegionActionSelected = onRegionActionSelected,
                                                onOpenOtherOptionPicker = onOpenOtherOptionPicker,
                                                onDismissRequest = onDismissRequest
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun ActionDialogItemRow(
    choice: GestureAction,
    currentOptionKey: String?,
    isSeparateTouchZones: Boolean,
    isButton: Boolean,
    binding: GestureBinding?,
    displayLines: List<Triple<String, GestureAction, String?>>,
    onActionSelected: (GestureAction) -> Unit,
    onRegionActionSelected: ((com.travelingtunes.app.core.model.TouchRegionTarget, GestureAction, String?) -> Unit)?,
    onOpenOtherOptionPicker: ((com.travelingtunes.app.core.model.TouchRegionTarget?) -> Unit)?,
    onDismissRequest: () -> Unit
) {
    val isOtherOption = choice == GestureAction.OTHER_OPTION
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (onRegionActionSelected == null) {
                    onActionSelected(choice)
                    onDismissRequest()
                }
            }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            ActionIcon(
                action = choice,
                optionKey = if (isOtherOption) currentOptionKey else null,
                iconSize = 20.dp,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = choice.displayName,
                fontWeight = if (isOtherOption) FontWeight.ExtraBold else FontWeight.Medium,
                fontSize = 13.sp,
                color = if (isOtherOption) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }

        if (onRegionActionSelected != null && binding != null) {
            val hasArtAction = binding.artAction != GestureAction.UNASSIGNED
            val hasTitleAction = binding.titleAction != GestureAction.UNASSIGNED

            if (isSeparateTouchZones) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val isArt = binding.artAction == choice
                    val isTitle = binding.titleAction == choice
                    val isBoth = binding.action == choice && !hasArtAction && !hasTitleAction

                    FilterChip(
                        selected = isArt,
                        onClick = {
                            onDismissRequest()
                            if (isOtherOption && onOpenOtherOptionPicker != null) {
                                onOpenOtherOptionPicker(com.travelingtunes.app.core.model.TouchRegionTarget.ART)
                            } else {
                                onRegionActionSelected(com.travelingtunes.app.core.model.TouchRegionTarget.ART, choice, null)
                            }
                        },
                        label = { Text("Art", fontSize = 10.sp) }
                    )
                    FilterChip(
                        selected = isTitle,
                        onClick = {
                            onDismissRequest()
                            if (isOtherOption && onOpenOtherOptionPicker != null) {
                                onOpenOtherOptionPicker(com.travelingtunes.app.core.model.TouchRegionTarget.TITLE)
                            } else {
                                onRegionActionSelected(com.travelingtunes.app.core.model.TouchRegionTarget.TITLE, choice, null)
                            }
                        },
                        label = { Text("Title", fontSize = 10.sp) }
                    )
                    if (!isButton) {
                        FilterChip(
                            selected = isBoth,
                            onClick = {
                                onDismissRequest()
                                if (isOtherOption && onOpenOtherOptionPicker != null) {
                                    onOpenOtherOptionPicker(com.travelingtunes.app.core.model.TouchRegionTarget.BOTH)
                                } else {
                                    onRegionActionSelected(com.travelingtunes.app.core.model.TouchRegionTarget.BOTH, choice, null)
                                }
                            },
                            label = { Text("Both", fontSize = 10.sp) }
                        )
                    }
                }
            } else {
                val isSelected = binding.action == choice
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        onDismissRequest()
                        val targetTarget = if (isButton) com.travelingtunes.app.core.model.TouchRegionTarget.TITLE else com.travelingtunes.app.core.model.TouchRegionTarget.BOTH
                        if (isOtherOption && onOpenOtherOptionPicker != null) {
                            onOpenOtherOptionPicker(targetTarget)
                        } else {
                            onRegionActionSelected(targetTarget, choice, null)
                        }
                    },
                    label = { Text("Select", fontSize = 10.sp) }
                )
            }
        }
    }
}
