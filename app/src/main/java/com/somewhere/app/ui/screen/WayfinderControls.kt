package com.somewhere.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.somewhere.app.data.location.*
import com.somewhere.app.data.remote.NearbyDrop
import com.somewhere.app.ui.theme.*
import com.somewhere.app.viewmodel.TripUiState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun wayfinderShape(): Shape = if (LocalAppTheme.current == AppTheme.FOLD) RectangleShape else RoundedCornerShape(12.dp)

@Composable
internal fun WayfinderModeRow(options: TripOptions, enabled: Boolean = true, onChange: (TripOptions) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TravelMode.entries.forEach { mode ->
            val icon = when (mode) { TravelMode.WALKING -> Icons.Outlined.DirectionsWalk; TravelMode.CYCLING -> Icons.Outlined.DirectionsBike; TravelMode.DRIVING -> Icons.Outlined.DirectionsCar }
            Surface(onClick = { onChange(options.copy(travelMode = mode)) }, enabled = enabled, modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                .semantics { selected = options.travelMode == mode },
                shape = wayfinderShape(), color = if (options.travelMode == mode) SomewhereColors.Card else SomewhereColors.Background,
                border = BorderStroke(.5.dp, if (options.travelMode == mode) SomewhereColors.TextSecondary else SomewhereColors.CardBorder)) {
                Row(Modifier.padding(horizontal = 8.dp, vertical = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, Modifier.size(17.dp), tint = SomewhereColors.TextPrimary)
                    Spacer(Modifier.width(6.dp)); Text(mode.label, style = MaterialTheme.typography.labelMedium, color = SomewhereColors.TextPrimary)
                }
            }
        }
    }
}

@Composable
internal fun WayfinderAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit, enabled: Boolean = true) {
    Surface(onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 48.dp), shape = wayfinderShape(), color = SomewhereColors.GlassBackground,
        border = BorderStroke(.5.dp, SomewhereColors.GlassBorder)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, null, Modifier.size(16.dp), tint = SomewhereColors.TextSecondary)
            Spacer(Modifier.width(8.dp)); Text(label, style = MaterialTheme.typography.labelMedium, color = SomewhereColors.TextPrimary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun WayfinderOptionsSheet(options: TripOptions, onChange: (TripOptions) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SomewhereColors.Surface, contentColor = SomewhereColors.TextPrimary,
        shape = wayfinderShape()) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Trip options", style = MaterialTheme.typography.headlineMedium, color = SomewhereColors.TextPrimary)
            WayfinderModeRow(options, onChange = onChange)
            OptionGroup("Exploration radius") {
                listOf(1000.0,2000.0,5000.0,10000.0).forEach { value -> Choice("${(value / 1000).toInt()} km", options.radiusMeters == value) { onChange(options.copy(radiusMeters = value)) } }
            }
            Text("Used for nearby searches. A radius written in your request is a firm limit.", style = MaterialTheme.typography.bodySmall, color = SomewhereColors.TextSecondary)
            OptionGroup("Total time") {
                listOf(null, 30, 45, 60, 90).forEach { value -> Choice(value?.let { "$it min" } ?: "Any", options.timeBudgetMinutes == value) { onChange(options.copy(timeBudgetMinutes = value)) } }
            }
            OptionGroup("Extra travel on a route") {
                listOf(null,5,10,20).forEach { value -> Choice(value?.let { "$it min" } ?: "Any", options.maxDetourMinutes == value) { onChange(options.copy(maxDetourMinutes = value)) } }
            }
            OptionGroup("Stops") { listOf(1,2,3,5,8).forEach { value -> Choice("$value", options.stopCount == value) { onChange(options.copy(stopCount = value)) } } }
            OptionGroup("Time at each stop") { listOf(0,2,5,10).forEach { value -> Choice("$value min", options.dwellMinutes == value) { onChange(options.copy(dwellMinutes = value)) } } }
            OptionToggle("Return to start", "Build a circular trip.", options.returnToStart) { onChange(options.copy(returnToStart = it)) }
            OptionToggle("Unvisited only", "Leave out Drops in your visit history.", options.unvisitedOnly) { onChange(options.copy(unvisitedOnly = it)) }
            OptionToggle("Prefer recent", "Give newer Drops more weight.", options.preferRecent) { onChange(options.copy(preferRecent = it)) }
            OptionGroup("Category") {
                Choice("All", options.category == null) { onChange(options.copy(category = null)) }
                com.somewhere.app.util.CategoryUtils.CATEGORIES.forEach { value -> Choice(value, options.category == value) { onChange(options.copy(category = value)) } }
            }
            OptionGroup("Media") { TripMedia.entries.forEach { value ->
                Choice(when (value) { TripMedia.ANY -> "All"; TripMedia.PHOTOS -> "Photos"; TripMedia.AUDIO -> "Audio"; TripMedia.TEXT -> "Text" }, options.media == value) { onChange(options.copy(media = value)) }
            } }
            HorizontalDivider(color = SomewhereColors.Divider)
            OptionToggle("List navigation", "Show stops and distances without the map or camera.", options.cameraFree) { onChange(options.copy(cameraFree = it)) }
            OptionToggle("Haptic alerts", "Feel a cue when a Drop is coming up.", options.haptics) { onChange(options.copy(haptics = it)) }
            OptionToggle("Step-free stops", "Only stops with author-confirmed access. Access along the route may be unknown.", options.stepFreeOnly) { onChange(options.copy(stepFreeOnly = it)) }
            WayfinderAction("Done", Icons.Outlined.Check, Modifier.fillMaxWidth(), onDismiss)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionGroup(label: String, content: @Composable FlowRowScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = SomewhereColors.TextSecondary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}
@Composable
private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp).semantics { this.selected = selected }, shape = wayfinderShape(),
        color = if (selected) SomewhereColors.Card else SomewhereColors.Surface,
        border = BorderStroke(.5.dp, if (selected) SomewhereColors.TextSecondary else SomewhereColors.CardBorder)) {
        Text(label, Modifier.padding(horizontal = 14.dp, vertical = 14.dp), style = MaterialTheme.typography.labelMedium, color = if (selected) SomewhereColors.TextPrimary else SomewhereColors.TextSecondary)
    }
}
@Composable
private fun OptionToggle(label: String, detail: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(role = Role.Switch, onClick = { onChange(!checked) }).semantics { toggleableState = if (checked) androidx.compose.ui.state.ToggleableState.On else androidx.compose.ui.state.ToggleableState.Off },
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 16.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = SomewhereColors.TextPrimary)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = SomewhereColors.TextSecondary)
        }
        Switch(checked = checked, onCheckedChange = null, colors = SwitchDefaults.colors(checkedTrackColor = SomewhereColors.TextPrimary,
            checkedThumbColor = SomewhereColors.Background, uncheckedTrackColor = SomewhereColors.Card, uncheckedThumbColor = SomewhereColors.TextSecondary,
            uncheckedBorderColor = SomewhereColors.CardBorder))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WayfinderLibrarySheet(state: TripUiState, onDismiss: () -> Unit, onOpen: (SavedTrip) -> Unit,
    onDelete: (String) -> Unit, onJournal: (String,String,String?) -> Unit, onLoadTrails: () -> Unit, onOpenTrail: (CuratedTrail) -> Unit,
    onAddPhotos: (String,List<android.net.Uri>) -> Unit = { _,_ -> }, onRemovePhoto: (String,String) -> Unit = { _,_ -> }) {
    var tab by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<SavedTrip?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SomewhereColors.Surface, contentColor = SomewhereColors.TextPrimary, shape = wayfinderShape()) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Your wayfinder", style = MaterialTheme.typography.headlineMedium, color = SomewhereColors.TextPrimary)
            state.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = SomewhereColors.Error) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Saved", "Journal", "Trails").forEachIndexed { index, title -> Choice(title, tab == index) { tab = index; if (index == 2) onLoadTrails() } }
            }
            if (tab < 2) {
                val trips = state.savedTrips.filter { if (tab == 0) it.isBookmark else !it.isBookmark || it.completedAtMillis != null || it.visitedStopIds.isNotEmpty() }
                if (trips.isEmpty()) Text(if (tab == 0) "Save a trip to return to it later." else "Your completed trips and visit notes will appear here.", style = MaterialTheme.typography.bodyMedium, color = SomewhereColors.TextSecondary)
                trips.forEach { trip ->
                    Surface(shape = wayfinderShape(), color = SomewhereColors.Card, border = BorderStroke(.5.dp, SomewhereColors.CardBorder)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(trip.title, style = MaterialTheme.typography.titleMedium, color = SomewhereColors.TextPrimary)
                            val date = Instant.ofEpochMilli(trip.createdAtMillis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM"))
                            Text("$date · ${trip.request.options.travelMode.label} · ${trip.visitedStopIds.size}/${trip.stopIds.size} stops visited", style = MaterialTheme.typography.bodySmall, color = SomewhereColors.TextSecondary)
                            if (trip.note.isNotBlank()) Text(trip.note, style = MaterialTheme.typography.bodyMedium, color = SomewhereColors.TextSecondary)
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { onOpen(trip); onDismiss() }) { Text(if (trip.id == state.resumableTripId) "Resume" else "Open", color = SomewhereColors.TextPrimary) }
                                TextButton(onClick = { editing = trip }) { Text("Notes", color = SomewhereColors.TextSecondary) }
                                Spacer(Modifier.weight(1f))
                                IconButton(onClick = { onDelete(trip.id) }) { Icon(Icons.Outlined.Delete, "Delete ${trip.title}", tint = SomewhereColors.TextSecondary) }
                            }
                        }
                    }
                }
            } else {
                if (state.isLoadingTrails) CircularProgressIndicator(Modifier.size(20.dp), color = SomewhereColors.TextSecondary, strokeWidth = 2.dp)
                else if (state.communityTrails.isEmpty()) Text("No trails within your exploration radius. Publish an itinerary to start one.", style = MaterialTheme.typography.bodyMedium, color = SomewhereColors.TextSecondary)
                state.communityTrails.forEach { trail ->
                    Surface(onClick = { onOpenTrail(trail); onDismiss() }, shape = wayfinderShape(), color = SomewhereColors.Card, border = BorderStroke(.5.dp, SomewhereColors.CardBorder)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(trail.title, style = MaterialTheme.typography.titleMedium, color = SomewhereColors.TextPrimary)
                            Text("${trail.stop_ids.size} ordered stops${if (trail.return_to_start) " · Return to start" else ""}", style = MaterialTheme.typography.bodySmall, color = SomewhereColors.TextSecondary)
                            if (trail.description.isNotBlank()) Text(trail.description, style = MaterialTheme.typography.bodyMedium, color = SomewhereColors.TextSecondary)
                        }
                    }
                }
            }
        }
    }
    editing?.let { original ->
        val trip = state.savedTrips.firstOrNull { it.id == original.id } ?: original
        JournalDialog(trip, state.isSavingPhotos, state.error, { editing = null },
            { uris -> onAddPhotos(trip.id,uris) }, { path -> onRemovePhoto(trip.id,path) }) { note, stopNotes ->
            onJournal(trip.id,note,null); stopNotes.forEach { (id,value) -> onJournal(trip.id,value,id) }; editing = null
        }
    }
}

@Composable
private fun JournalDialog(trip: SavedTrip, busy: Boolean, error: String?, onDismiss: () -> Unit,
    onAddPhotos: (List<android.net.Uri>) -> Unit, onRemovePhoto: (String) -> Unit, onSave: (String,Map<String,String>) -> Unit) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) { uris -> if (uris.isNotEmpty()) onAddPhotos(uris) }
    var note by remember(trip.id) { mutableStateOf(trip.note) }
    var notes by remember(trip.id) { mutableStateOf(trip.stopNotes) }
    AlertDialog(onDismissRequest = onDismiss, containerColor = SomewhereColors.Surface, shape = wayfinderShape(),
        title = { Text("Trip notes", color = SomewhereColors.TextPrimary) }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (error != null) Text(error, color = SomewhereColors.Error, style = MaterialTheme.typography.bodySmall)
                WayfinderTextField(note, { note = it.take(2000) }, "About this trip")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(trip.photoPaths) { index,path ->
                        Box(Modifier.size(96.dp)) {
                            AsyncImage(java.io.File(path), "Trip photo ${index+1}", Modifier.fillMaxSize().clip(wayfinderShape()), contentScale = ContentScale.Crop)
                            IconButton(onClick = { onRemovePhoto(path) }, modifier = Modifier.align(Alignment.TopEnd).background(SomewhereColors.GlassBackground,wayfinderShape())) {
                                Icon(Icons.Outlined.Close, "Remove photo ${index+1}", tint = SomewhereColors.TextPrimary)
                            }
                        }
                    }
                }
                WayfinderAction(if (busy) "Saving photos…" else "Add photos", Icons.Outlined.AddPhotoAlternate, Modifier.fillMaxWidth(),
                    { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, enabled = !busy && trip.photoPaths.size < 12)
                trip.stopIds.forEachIndexed { i,id -> WayfinderTextField(notes[id].orEmpty(), { notes = notes + (id to it) }, "Stop ${i+1}") }
            }
        }, confirmButton = { TextButton(onClick = { onSave(note,notes) }) { Text("Save", color = SomewhereColors.TextPrimary) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = SomewhereColors.TextSecondary) } })
}

@Composable
internal fun WayfinderSaveDialog(publish: Boolean, busy: Boolean, error: String?, onDismiss: () -> Unit, onSave: (String,String) -> Unit) {
    var title by remember { mutableStateOf("") }; var description by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, containerColor = SomewhereColors.Surface, shape = wayfinderShape(),
        title = { Text(if (publish) "Publish trail" else "Save trip", color = SomewhereColors.TextPrimary) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (error != null) Text(error, color = SomewhereColors.Error, style = MaterialTheme.typography.bodySmall)
            WayfinderTextField(title, { title = it.take(80) }, "Name")
            if (publish) {
                WayfinderTextField(description, { description = it.take(500) }, "Description")
                Text("The ordered stops and description will be public. Your private starting location and journal stay on this device.", style = MaterialTheme.typography.bodySmall, color = SomewhereColors.TextSecondary)
            }
        } }, confirmButton = { TextButton(enabled = title.isNotBlank() && !busy, onClick = { onSave(title,description) }) { Text(if (busy) "Publishing…" else if (publish) "Publish" else "Save", color = SomewhereColors.TextPrimary) } },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancel", color = SomewhereColors.TextSecondary) } })
}

@Composable
private fun WayfinderTextField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(value, onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), shape = wayfinderShape(),
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SomewhereColors.TextPrimary, unfocusedTextColor = SomewhereColors.TextPrimary,
            focusedBorderColor = SomewhereColors.TextSecondary, unfocusedBorderColor = SomewhereColors.CardBorder,
            focusedLabelColor = SomewhereColors.TextPrimary, unfocusedLabelColor = SomewhereColors.TextSecondary, cursorColor = SomewhereColors.TextPrimary))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WayfinderStopsSheet(state: TripUiState, onDismiss: () -> Unit, onVisit: (String) -> Unit, onAdd: (NearbyDrop) -> Unit,
    onRemove: (String) -> Unit, onPreview: (NearbyDrop) -> Unit, onAccess: (NearbyDrop, Boolean?) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SomewhereColors.Surface, contentColor = SomewhereColors.TextPrimary, shape = wayfinderShape()) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Stops & discoveries", style = MaterialTheme.typography.headlineMedium, color = SomewhereColors.TextPrimary)
            state.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = SomewhereColors.Error) }
            if (state.itineraryStops.isEmpty()) Text("Add discoveries to build an itinerary.", style = MaterialTheme.typography.bodyMedium, color = SomewhereColors.TextSecondary)
            val stops = (state.itineraryStops + state.filteredDrops).distinctBy { it.id }
            stops.forEach { drop ->
                val index = state.itineraryStops.indexOfFirst { it.id == drop.id }
                WayfinderStopRow(drop, state, if (index >= 0) "Stop ${index+1}" else "Discovery", onPreview)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (drop.id !in state.visitedDropIds) TextButton(onClick = { onVisit(drop.id) }) { Text("Mark visited", color = SomewhereColors.TextPrimary) }
                    if (!state.isNavigating) TextButton(onClick = { if (index >= 0) onRemove(drop.id) else onAdd(drop) }) { Text(if (index >= 0) "Remove stop" else "Add stop", color = SomewhereColors.TextSecondary) }
                }
                if (drop.authorId == state.currentUserId && state.currentUserId != null) Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Step-free access", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = SomewhereColors.TextSecondary)
                    TextButton(onClick = { onAccess(drop, true) }) { Text("Yes", color = SomewhereColors.TextPrimary) }
                    TextButton(onClick = { onAccess(drop, false) }) { Text("No", color = SomewhereColors.TextSecondary) }
                    TextButton(onClick = { onAccess(drop, null) }) { Text("Unknown", color = SomewhereColors.TextSecondary) }
                }
                HorizontalDivider(color = SomewhereColors.Divider)
            }
        }
    }
}

@Composable
internal fun WayfinderStopRow(drop: NearbyDrop, state: TripUiState, label: String, onPreview: (NearbyDrop) -> Unit) {
    Column(Modifier.fillMaxWidth().clickable { onPreview(drop) }.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("$label${if (drop.id in state.visitedDropIds) " · Visited" else ""}", style = MaterialTheme.typography.labelSmall, color = SomewhereColors.TextSecondary)
        Text(drop.text, style = MaterialTheme.typography.bodyMedium, color = SomewhereColors.TextPrimary, maxLines = 3, overflow = TextOverflow.Ellipsis)
        val reason = state.recommendations[drop.id]?.reasons?.joinToString(" · ").orEmpty()
        if (reason.isNotBlank()) Text(reason, style = MaterialTheme.typography.bodySmall, color = SomewhereColors.TextSecondary)
        state.currentUserLocation?.let { loc ->
            val distance = TripGeometry.distance(TripPoint(loc.latitude,loc.longitude),drop.point()).toLong()
            Text("${TripManager.formatDistance(distance)} away", style = MaterialTheme.typography.bodySmall, color = SomewhereColors.TextSecondary)
        }
        if (drop.stepFreeAccess == true) Text("Author-confirmed step-free stop", style = MaterialTheme.typography.bodySmall, color = SomewhereColors.TextSecondary)
    }
}

@Composable
internal fun WayfinderResultActions(state: TripUiState, onStops: () -> Unit, onSave: () -> Unit, onPublish: () -> Unit,
    onToggleView: () -> Unit, modifier: Modifier = Modifier) {
    var menu by remember { mutableStateOf(false) }
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        WayfinderAction("Stops", Icons.Outlined.Route, Modifier.weight(1f), onStops)
        WayfinderAction("Save", Icons.Outlined.BookmarkBorder, Modifier.weight(1f), onSave, enabled = !state.isLoading)
        Box(Modifier.weight(1f)) {
            WayfinderAction("More", Icons.Outlined.MoreHoriz, Modifier.fillMaxWidth(), { menu = true })
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, modifier = Modifier.background(SomewhereColors.Surface)) {
                DropdownMenuItem(text = { Text(if (state.options.cameraFree) "Map view" else "List view", color = SomewhereColors.TextPrimary) }, onClick = { menu = false; onToggleView() })
                DropdownMenuItem(enabled = state.itineraryStops.isNotEmpty() && !state.isLoading,
                    text = { Text("Publish trail", color = if (state.itineraryStops.isEmpty()) SomewhereColors.TextMuted else SomewhereColors.TextPrimary) }, onClick = { menu = false; onPublish() })
            }
        }
    }
}

@Composable
internal fun TripListPhase(uiState: TripUiState, onBack: () -> Unit, onStart: () -> Unit, onStop: () -> Unit,
    onSave: () -> Unit, onPublish: () -> Unit, onStops: () -> Unit, onToggleView: () -> Unit,
    onPreview: (NearbyDrop) -> Unit, onDismissApproaching: () -> Unit) {
    Column(Modifier.fillMaxSize().background(SomewhereColors.Background).systemBarsPadding().padding(horizontal = 24.dp).padding(bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back", tint = SomewhereColors.TextPrimary) }
            Text("Wayfinder", style = MaterialTheme.typography.headlineMedium, color = SomewhereColors.TextPrimary)
        }
        Text(listOf(uiState.options.travelMode.label,uiState.routeDuration,uiState.routeDistance).filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = SomewhereColors.TextSecondary)
        WayfinderResultActions(uiState,onStops,onSave,onPublish,onToggleView)
        if (uiState.approachingDrop != null) {
            Surface(shape = wayfinderShape(), color = SomewhereColors.Card, border = BorderStroke(.5.dp, SomewhereColors.TextSecondary), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                Column(Modifier.padding(16.dp)) {
                    Text("Drop ahead", style = MaterialTheme.typography.labelMedium, color = SomewhereColors.TextPrimary)
                    Text(uiState.approachingDrop.text, style = MaterialTheme.typography.bodyMedium, color = SomewhereColors.TextPrimary, maxLines = 2)
                    TextButton(onClick = onDismissApproaching) { Text("Dismiss", color = SomewhereColors.TextSecondary) }
                }
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (uiState.isLoading || uiState.isRecalculating) Text(if (uiState.isRecalculating) "Refreshing route…" else uiState.planningMessage, style = MaterialTheme.typography.bodyMedium, color = SomewhereColors.TextSecondary)
            val stops = (uiState.itineraryStops + uiState.filteredDrops).distinctBy { it.id }
            if (stops.isEmpty()) Text("No matching Drops. Change your request or exploration radius.", style = MaterialTheme.typography.bodyMedium, color = SomewhereColors.TextSecondary)
            stops.forEach { drop ->
                val index = uiState.itineraryStops.indexOfFirst { it.id == drop.id }
                WayfinderStopRow(drop,uiState,if (index >= 0) "Stop ${index+1}" else "Discovery",onPreview)
                HorizontalDivider(color = SomewhereColors.Divider)
            }
        }
        WayfinderAction(if (uiState.isNavigating) "Stop trip" else "Start trip", if (uiState.isNavigating) Icons.Outlined.Stop else Icons.Outlined.Navigation,
            Modifier.fillMaxWidth(), if (uiState.isNavigating) onStop else onStart)
    }
}
