package com.travelingtunes.app.feature.settings

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.RadioButtonChecked
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
import androidx.compose.material3.OutlinedButton
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
    }
}

fun getTriggersForSubmenu(
    submenu: GestureSubmenu,
    numEdgeRegions: Int
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
        GestureSubmenu.BUTTON -> mapOf(
            "Top Edge Regions" to GestureTrigger.getActiveTopTriggers(numEdgeRegions),
            "Bottom Edge Regions" to GestureTrigger.getActiveBottomTriggers(numEdgeRegions)
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
            listOf(GestureSubmenu.SWIPE, GestureSubmenu.TAP, GestureSubmenu.BUTTON)
        }
    }

    var selectedSubmenu by remember { mutableStateOf<GestureSubmenu?>(initialSubmenu) }
    var editingRadialTrigger by remember { mutableStateOf<GestureTrigger?>(null) }

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
                // Swipe, Tap, or Button Actions list
                val sections = remember(sub, numEdgeRegions) {
                    getTriggersForSubmenu(sub, numEdgeRegions)
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    sections.forEach { (sectionTitle, triggers) ->
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
                        items(triggers, key = { it.name }) { trigger ->
                            val currentBinding = gestureBindings[trigger] ?: GestureBinding(
                                trigger = trigger,
                                action = GestureAction.fromKey(trigger.defaultActionKey),
                                isContinuous = trigger.isContinuousDefault
                            )

                            GestureAssignmentItem(
                                trigger = trigger,
                                binding = currentBinding,
                                numEdgeRegions = numEdgeRegions,
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
            }
        }
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
                        text = "Pop up actions arranged in a circle around the touch spot. Drag or tap to activate.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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

                    DropdownMenu(
                        expanded = isDropdownExpanded,
                        modifier = Modifier.fillMaxWidth(0.85f).widthIn(min = 280.dp, max = 560.dp),
                        onDismissRequest = { isDropdownExpanded = false }
                    ) {
                        GestureAction.entries.filter { it != GestureAction.RADIAL_MENU }.forEach { choice ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        ActionIcon(
                                            action = choice,
                                            iconSize = 20.dp,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(choice.displayName)
                                    }
                                },
                                onClick = {
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
    onActionSelected: (com.travelingtunes.app.core.model.TouchRegionTarget, GestureAction, String?) -> Unit
) {
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var editingOptionRegion by remember { mutableStateOf<com.travelingtunes.app.core.model.TouchRegionTarget?>(null) }

    val hasArtAction = binding.artAction != GestureAction.UNASSIGNED
    val hasTitleAction = binding.titleAction != GestureAction.UNASSIGNED
    val hasBothAction = binding.action != GestureAction.UNASSIGNED

    val displayLines = remember(binding) {
        val list = mutableListOf<Triple<String, GestureAction, String?>>()
        if (hasArtAction) {
            list.add(Triple("Art", binding.artAction, binding.artOtherOptionKey))
        }
        if (hasTitleAction) {
            list.add(Triple("Title", binding.titleAction, binding.titleOtherOptionKey))
        }
        if (hasBothAction || list.isEmpty()) {
            list.add(Triple("Both", binding.action, binding.otherOptionKey))
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
            Text(text = trigger.getDisplayName(numEdgeRegions), fontWeight = FontWeight.SemiBold)
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
            ActionIcon(
                action = primaryAction,
                iconSize = 28.dp,
                tint = MaterialTheme.colorScheme.primary
            )

            DropdownMenu(
                expanded = isDropdownExpanded,
                modifier = Modifier.fillMaxWidth(0.92f).widthIn(min = 360.dp, max = 680.dp),
                onDismissRequest = { isDropdownExpanded = false }
            ) {
                GestureAction.entries.forEach { choice ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    ActionIcon(
                                        action = choice,
                                        iconSize = 20.dp,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = choice.displayName,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    val isArt = binding.artAction == choice
                                    val isTitle = binding.titleAction == choice
                                    val isBoth = binding.action == choice && !hasArtAction && !hasTitleAction

                                    FilterChip(
                                        selected = isArt,
                                        onClick = {
                                            isDropdownExpanded = false
                                            if (choice == GestureAction.OTHER_OPTION) {
                                                editingOptionRegion = com.travelingtunes.app.core.model.TouchRegionTarget.ART
                                            } else {
                                                onActionSelected(com.travelingtunes.app.core.model.TouchRegionTarget.ART, choice, null)
                                            }
                                        },
                                        label = { Text("Art", fontSize = 11.sp) }
                                    )
                                    FilterChip(
                                        selected = isTitle,
                                        onClick = {
                                            isDropdownExpanded = false
                                            if (choice == GestureAction.OTHER_OPTION) {
                                                editingOptionRegion = com.travelingtunes.app.core.model.TouchRegionTarget.TITLE
                                            } else {
                                                onActionSelected(com.travelingtunes.app.core.model.TouchRegionTarget.TITLE, choice, null)
                                            }
                                        },
                                        label = { Text("Title", fontSize = 11.sp) }
                                    )
                                    FilterChip(
                                        selected = isBoth,
                                        onClick = {
                                            isDropdownExpanded = false
                                            if (choice == GestureAction.OTHER_OPTION) {
                                                editingOptionRegion = com.travelingtunes.app.core.model.TouchRegionTarget.BOTH
                                            } else {
                                                onActionSelected(com.travelingtunes.app.core.model.TouchRegionTarget.BOTH, choice, null)
                                            }
                                        },
                                        label = { Text("Both", fontSize = 11.sp) }
                                    )
                                }
                            }
                        },
                        onClick = {}
                    )
                }
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
