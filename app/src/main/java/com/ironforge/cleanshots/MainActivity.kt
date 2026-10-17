package com.ironforge.cleanshots

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ironforge.cleanshots.data.store.PrefsManager
import com.ironforge.cleanshots.ui.paywall.PaywallScreen
import com.ironforge.cleanshots.ui.permissions.PermissionGate
import com.ironforge.cleanshots.ui.scan.ScanViewModel
import com.ironforge.cleanshots.ui.screens.BlurryScreen
import com.ironforge.cleanshots.ui.screens.DuplicatesScreen
import com.ironforge.cleanshots.ui.screens.ScreenshotsScreen
import com.ironforge.cleanshots.ui.screens.TrashScreenInner
import com.ironforge.cleanshots.ui.theme.Accent
import com.ironforge.cleanshots.ui.theme.Background
import com.ironforge.cleanshots.ui.theme.CleanShotsTheme
import com.ironforge.cleanshots.ui.trash.TrashViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as CleanShotsApp
        app.container.billing.start()
        setContent {
            CleanShotsTheme {
                PermissionGate { access, reselect ->
                    AppShell(access)
                }
            }
        }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab("shots", "Screenshots", Icons.Default.PhotoLibrary),
    Tab("dupes", "Duplicates", Icons.Default.CopyAll),
    Tab("blurry", "Blurry", Icons.Default.BrokenImage),
    Tab("trash", "Trash", Icons.Default.Delete)
)

@Composable
private fun AppShell(access: String) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val app = ctx.applicationContext as CleanShotsApp
    val scanVm: ScanViewModel = viewModel(factory = ScanViewModel.Factory(app.container.scanRepo))
    val trashVm: TrashViewModel = viewModel(factory = TrashViewModel.Factory(app.container.db.trashDao(), app.container.prefs))
    val requester = com.ironforge.cleanshots.ui.trash.rememberMediaRequester()
    val nav = rememberNavController()
    val snack = remember { SnackbarHostState() }
    var showPaywall by remember { mutableStateOf(false) }
    val isPremium by app.container.prefs.premium.collectAsState(initial = false)
    val freeUsed by app.container.prefs.freeDeleteCount.collectAsState(initial = 0)
    val trashUi by trashVm.ui.collectAsState()

    LaunchedEffect(access) {
        trashVm.load()
        if (access != "denied") scanVm.start()
    }

    if (showPaywall) {
        PaywallScreen(
            billing = app.container.billing,
            freedBytes = trashUi.items.sumOf { it.size },
            onClose = { showPaywall = false },
            onRestored = { showPaywall = false }
        )
        return
    }

    Scaffold(
        containerColor = Background,
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF111111)) {
                val back by nav.currentBackStackEntryAsState()
                val cur = back?.destination?.route
                TABS.forEach { t ->
                    NavigationBarItem(
                        selected = cur == t.route,
                        onClick = { nav.navigate(t.route) { launchSingleTop = true } },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Accent, selectedTextColor = Accent, indicatorColor = Color(0xFF1E1E1E))
                    )
                }
            }
        }
    ) { pad ->
        NavHost(nav, startDestination = "shots", modifier = Modifier.padding(pad)) {
            composable("shots") {
                ScreenshotsScreen(scanVm, trashVm, snack,
                    onNeedPremium = { showPaywall = true }, isPremium = isPremium, freeUsed = freeUsed)
            }
            composable("dupes") {
                DuplicatesScreen(scanVm, trashVm, snack,
                    onNeedPremium = { showPaywall = true }, isPremium = isPremium, freeUsed = freeUsed)
            }
            composable("blurry") { BlurryScreen(scanVm) }
            composable("trash") {
                TrashScreenInner(
                    items = trashUi.items,
                    onRestore = {
                        val uris = trashVm.lastTrashed()
                        if (uris.isNotEmpty()) requester.untrash(uris) { ok ->
                            if (ok) { trashVm.onUntrashed(uris); scanVm.start() }
                        }
                    },
                    onDelete = { ids ->
                        val uris = trashVm.urisFor(ids)
                        if (uris.isNotEmpty()) requester.delete(uris) { ok ->
                            if (ok) trashVm.onPermanentlyDeleted(ids)
                        }
                    }
                )
            }
        }
    }
}
