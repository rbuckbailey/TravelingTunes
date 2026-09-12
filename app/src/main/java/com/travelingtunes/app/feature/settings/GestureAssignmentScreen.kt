package com.travelingtunes.app.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.travelingtunes.app.core.datastore.SettingsDataStore
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
                                onActionSelected = { newAction ->
                                    coroutineScope.launch {
                                        settingsDataStore.updateGestureBinding(trigger, newAction, currentBinding.isContinuous)
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
    onUpdateActions: (List<GestureAction>) -> Unit,
    onResetDefaults: () -> Unit
) {
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
                        Text(
                            text = action.displayName,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    DropdownMenu(
                        expanded = isDropdownExpanded,
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
    onActionSelected: (GestureAction) -> Unit
) {
    var isDropdownExpanded by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isDropdownExpanded = true }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = trigger.getDisplayName(numEdgeRegions), fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                ActionIcon(
                    action = binding.action,
                    iconSize = 20.dp,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = binding.action.displayName,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Box {
            ActionIcon(
                action = binding.action,
                iconSize = 28.dp,
                tint = MaterialTheme.colorScheme.primary
            )

            DropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { isDropdownExpanded = false }
            ) {
                GestureAction.entries.forEach { action ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ActionIcon(
                                    action = action,
                                    iconSize = 22.dp,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(action.displayName)
                            }
                        },
                        onClick = {
                            onActionSelected(action)
                            isDropdownExpanded = false
                        }
                    )
                }
            }
        }
    }
}
