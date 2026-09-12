package com.travelingtunes.app.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.collectAsState
import com.travelingtunes.app.core.datastore.SettingsDataStore
import com.travelingtunes.app.core.model.DisplaySettings
import com.travelingtunes.app.core.model.GestureAction
import com.travelingtunes.app.core.model.GestureBinding
import com.travelingtunes.app.core.model.GestureCategory
import com.travelingtunes.app.core.model.GestureTrigger
import com.travelingtunes.app.feature.player.ActionIcon
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureAssignmentScreen(
    settingsDataStore: SettingsDataStore,
    gestureBindings: Map<GestureTrigger, GestureBinding>,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val displaySettings by settingsDataStore.displaySettingsFlow.collectAsState(initial = DisplaySettings())
    val numEdgeRegions = displaySettings.numEdgeRegions

    val displayedTriggers = remember(numEdgeRegions) {
        GestureTrigger.entries.filter { trigger ->
            if (trigger.category == GestureCategory.SCREEN_REGION) {
                trigger in GestureTrigger.getActiveTopTriggers(numEdgeRegions) ||
                trigger in GestureTrigger.getActiveBottomTriggers(numEdgeRegions)
            } else {
                true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gesture Assignments") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            items(displayedTriggers) { trigger ->
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
