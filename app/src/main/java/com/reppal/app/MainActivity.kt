package com.reppal.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reppal.app.ui.theme.ReppalTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class SortOption(val labelRes: Int, val iconRes: Int) {
    TOUT(R.string.sort_all, R.drawable.ic_tout),
    A_VENIR(R.string.sort_upcoming, R.drawable.ic_avenir),
    PASSE(R.string.sort_past, R.drawable.ic_passe),
    DATE_ASC(R.string.sort_date_asc, R.drawable.ic_croissant),
    DATE_DESC(R.string.sort_date_desc, R.drawable.ic_decroissant)
}

class MainActivity : ComponentActivity() {
    private var idRappelDepuisNotif by mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = RappelDatabase.getDatabase(this)
        val dao = database.rappelDao()

        val idInitial = intent.getIntExtra("OPEN_RAPPEL_ID", -1)
        if (idInitial != -1) idRappelDepuisNotif = idInitial

        enableEdgeToEdge()
        setContent {
            ReppalTheme {
                val context = LocalContext.current
                var permissionAccordee by remember {
                    mutableStateOf(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                        } else {
                            true
                        }
                    )
                }

                val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { accordee ->
                    if (accordee) {
                        permissionAccordee = true
                    } else {
                        (context as? ComponentActivity)?.finish()
                    }
                }

                LaunchedEffect(Unit) {
                    if (!permissionAccordee) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                if (permissionAccordee) {
                    val rappelViewModel: RappelViewModel = viewModel(
                        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                @Suppress("UNCHECKED_CAST")
                                return RappelViewModel(dao) as T
                            }
                        }
                    )
                    MainScreen(viewModel = rappelViewModel, idNotif = idRappelDepuisNotif, onConsommerId = { idRappelDepuisNotif = null })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val id = intent.getIntExtra("OPEN_RAPPEL_ID", -1)
        if (id != -1) idRappelDepuisNotif = id
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: RappelViewModel, idNotif: Int?, onConsommerId: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE) }
    val coroutineScope = rememberCoroutineScope()

    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }

    var currentSort by remember {
        val savedSort = prefs.getString("sort_mode", SortOption.TOUT.name)
        mutableStateOf(SortOption.valueOf(savedSort ?: SortOption.TOUT.name))
    }

    var rappelAModifier by remember { mutableStateOf<Rappel?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var expandedSortMenu by remember { mutableStateOf(false) }

    val listeRappelsBrute by viewModel.tousLesRappels.collectAsState(initial = emptyList())

    LaunchedEffect(idNotif, listeRappelsBrute) {
        if (idNotif != null && listeRappelsBrute.isNotEmpty()) {
            listeRappelsBrute.find { it.id == idNotif }?.let {
                rappelAModifier = it
                showBottomSheet = true
                onConsommerId()
            }
        }
    }

    LaunchedEffect(currentSort) { prefs.edit { putString("sort_mode", currentSort.name) } }

    val listeRappels = remember(listeRappelsBrute, currentSort) {
        val now = System.currentTimeMillis()
        when (currentSort) {
            SortOption.TOUT -> listeRappelsBrute
            SortOption.A_VENIR -> listeRappelsBrute.filter { it.timestamp >= now }.sortedBy { it.timestamp }
            SortOption.PASSE -> listeRappelsBrute.filter { it.timestamp < now }.sortedByDescending { it.timestamp }
            SortOption.DATE_ASC -> listeRappelsBrute.sortedBy { it.timestamp }
            SortOption.DATE_DESC -> listeRappelsBrute.sortedByDescending { it.timestamp }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding() + 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(id = R.string.my_reminders),
                        style = MaterialTheme.typography.headlineLarge
                    )

                    Box {
                        IconButton(onClick = { expandedSortMenu = true }) {
                            Icon(painter = painterResource(id = currentSort.iconRes), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        DropdownMenu(expanded = expandedSortMenu, onDismissRequest = { expandedSortMenu = false }) {
                            SortOption.entries.forEach { option ->
                                val isSelected = currentSort == option
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = stringResource(id = option.labelRes),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Unspecified,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = { currentSort = option; expandedSortMenu = false },
                                    leadingIcon = { Icon(painter = painterResource(id = option.iconRes), contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(25.dp))

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0f to Color.Transparent,
                                    0.05f to Color.Black,
                                    1f to Color.Black
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        }
                ) {
                    if (listeRappels.isEmpty()) {
                        val emptyMessage = when (currentSort) {
                            SortOption.A_VENIR -> stringResource(id = R.string.empty_upcoming)
                            SortOption.PASSE -> stringResource(id = R.string.empty_past)
                            else -> stringResource(id = R.string.empty_general)
                        }

                        Column(
                            modifier = Modifier.fillMaxSize().padding(bottom = 100.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(painter = painterResource(id = currentSort.iconRes), contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = emptyMessage, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(top = 24.dp, bottom = 120.dp, start = 16.dp, end = 16.dp)
                        ) {
                            items(items = listeRappels, key = { it.id }) { rappel ->
                                CardRappel(
                                    rappel = rappel,
                                    onDelete = { viewModel.supprimerRappel(context, rappel) },
                                    onClick = {
                                        rappelAModifier = rappel
                                        showBottomSheet = true
                                    }
                                )
                            }

                            item {
                                val count = listeRappels.size
                                val total = listeRappelsBrute.size
                                val footerText = when (currentSort) {
                                    SortOption.TOUT, SortOption.DATE_ASC, SortOption.DATE_DESC -> {
                                        if (count <= 1) stringResource(R.string.footer_singular, count)
                                        else stringResource(R.string.footer_plural, count)
                                    }
                                    SortOption.PASSE -> {
                                        val labelStatus = stringResource(R.string.sort_past).lowercase()
                                        stringResource(R.string.footer_split, count, labelStatus, total)
                                    }
                                    SortOption.A_VENIR -> {
                                        val labelStatus = stringResource(R.string.sort_upcoming).lowercase()
                                        stringResource(R.string.footer_split, count, labelStatus, total)
                                    }
                                }

                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = footerText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.outline,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                rappelAModifier = null
                showBottomSheet = true
            },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 50.dp)
                .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        stiffness = Spring.StiffnessMediumLow,
                                        dampingRatio = Spring.DampingRatioLowBouncy
                                    )
                                )
                            }
                            coroutineScope.launch {
                                offsetY.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        stiffness = Spring.StiffnessMediumLow,
                                        dampingRatio = Spring.DampingRatioLowBouncy
                                    )
                                )
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                offsetX.snapTo(offsetX.value + dragAmount.x)
                                offsetY.snapTo(offsetY.value + dragAmount.y)
                            }
                        }
                    )
                }
        ) {
            Icon(painter = painterResource(id = R.drawable.ic_notif), contentDescription = null)
        }

        if (showBottomSheet) {
            ModalBottomSheet(onDismissRequest = { showBottomSheet = false; rappelAModifier = null }, sheetState = sheetState) {
                AddReminderSheet(rappelInitial = rappelAModifier, viewModel = viewModel, onDismiss = { showBottomSheet = false; rappelAModifier = null })
            }
        }
    }
}

@Composable
fun CardRappel(
    rappel: Rappel,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    val dateAffichee = remember(rappel.timestamp) {
        android.text.format.DateFormat.getMediumDateFormat(context).format(java.util.Date(rappel.timestamp))
    }
    val heureAffichee = remember(rappel.timestamp) {
        android.text.format.DateFormat.getTimeFormat(context).format(java.util.Date(rappel.timestamp))
    }
    val estPasse = remember(rappel.timestamp) { rappel.timestamp < System.currentTimeMillis() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick, // Simple clic pour modifier
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
        )
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f).padding(end = 24.dp)) {
                Text(
                    text = rappel.titre,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = rappel.description.ifBlank { stringResource(id = R.string.no_description) },
                    fontSize = 14.sp,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = if (rappel.description.isNotBlank())
                        TextStyle.Default
                    else
                        TextStyle(fontStyle = FontStyle.Italic)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(modifier = Modifier.width(180.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painterResource(id = R.drawable.ic_calendrier),
                            null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = dateAffichee, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            painterResource(id = R.drawable.ic_montre),
                            null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = heureAffichee, fontSize = 12.sp)
                    }
                    Surface(
                        modifier = Modifier.width(55.dp),
                        color = if (estPasse)
                            MaterialTheme.colorScheme.errorContainer
                        else
                            MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = if (estPasse) {
                                stringResource(id = R.string.status_passed)
                            } else {
                                stringResource(id = R.string.status_upcoming)
                            },
                            modifier = Modifier.padding(vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (estPasse)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    painterResource(id = R.drawable.ic_corbeille),
                    null,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}