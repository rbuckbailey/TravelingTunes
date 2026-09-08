package com.travelingtunes.app.feature.contacts

import android.annotation.SuppressLint
import android.content.Context
import android.provider.ContactsContract
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.travelingtunes.app.core.datastore.SettingsDataStore
import com.travelingtunes.app.core.model.AddressContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsPickerScreen(
    settingsDataStore: SettingsDataStore,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var contacts by remember { mutableStateOf<List<AddressContact>>(emptyList()) }

    LaunchedEffect(Unit) {
        contacts = loadContactsWithAddresses(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pick Address") },
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
            item {
                ListItem(
                    headlineContent = { Text("Cancel Navigation", color = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable {
                        coroutineScope.launch {
                            settingsDataStore.setAddresses(destination = "cancel")
                            onNavigateBack()
                        }
                    }
                )
                Divider()
            }

            items(contacts) { contact ->
                ListItem(
                    headlineContent = { Text(contact.name) },
                    supportingContent = { Text(contact.fullAddress) },
                    modifier = Modifier.clickable {
                        coroutineScope.launch {
                            settingsDataStore.setAddresses(
                                destination = contact.fullAddress,
                                destName = contact.name
                            )
                            onNavigateBack()
                        }
                    }
                )
            }
        }
    }
}

@SuppressLint("Range")
private suspend fun loadContactsWithAddresses(context: Context): List<AddressContact> = withContext(Dispatchers.IO) {
    val contactList = mutableListOf<AddressContact>()
    val contentResolver = context.contentResolver

    val projection = arrayOf(
        ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID,
        ContactsContract.CommonDataKinds.StructuredPostal.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.StructuredPostal.STREET,
        ContactsContract.CommonDataKinds.StructuredPostal.CITY,
        ContactsContract.CommonDataKinds.StructuredPostal.REGION
    )

    contentResolver.query(
        ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI,
        projection,
        null,
        null,
        "${ContactsContract.CommonDataKinds.StructuredPostal.DISPLAY_NAME} ASC"
    )?.use { cursor ->
        val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID)
        val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.DISPLAY_NAME)
        val streetIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.STREET)
        val cityIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.CITY)
        val stateIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.REGION)

        while (cursor.moveToNext()) {
            val id = if (idIdx >= 0) cursor.getString(idIdx) ?: "" else ""
            val name = if (nameIdx >= 0) cursor.getString(nameIdx) ?: "Unknown" else "Unknown"
            val street = if (streetIdx >= 0) cursor.getString(streetIdx) ?: "" else ""
            val city = if (cityIdx >= 0) cursor.getString(cityIdx) ?: "" else ""
            val state = if (stateIdx >= 0) cursor.getString(stateIdx) ?: "" else ""

            if (street.isNotBlank()) {
                contactList.add(AddressContact(id, name, street, city, state))
            }
        }
    }
    contactList
}
