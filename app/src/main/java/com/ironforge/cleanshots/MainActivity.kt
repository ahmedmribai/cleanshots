package com.ironforge.cleanshots

import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ironforge.cleanshots.ui.components.CelebrationOverlay
import com.ironforge.cleanshots.ui.home.HomeScreen
import com.ironforge.cleanshots.ui.paywall.PaywallScreen
import com.ironforge.cleanshots.ui.permissions.PermissionGate
import com.ironforge.cleanshots.ui.review.SwipeReviewScreen
import com.ironforge.cleanshots.ui.scan.ScanViewModel
import com.ironforge.cleanshots.ui.screens.BlurryScreen
import com.ironforge.cleanshots.ui.screens.DuplicatesScreen
import com.ironforge.cleanshots.ui.screens.ScreenshotsScreen
import com.ironforge.cleanshots.ui.screens.TrashScreenInner
import com.ironforge.cleanshots.ui.screens.VideosScreen
import com.ironforge.cleanshots.ui.theme.Accent
import com.ironforge.cleanshots.ui.theme.Background
import com.ironforge.cleanshots.ui.theme.CleanShotsTheme
import com.ironforge.cleanshots.ui.theme.Muted
import com.ironforge.cleanshots.ui.theme.OnDark
import com.ironforge.cleanshots.ui.theme.SurfaceCard
import com.ironforge.cleanshots.ui.theme.SurfaceHigh
import com.ironforge.cleanshots.ui.trash.TrashViewModel
import com.ironforge.cleanshots.ui.trash.rememberCleaner
import com.ironforge.cleanshots.ui.trash.rememberMediaRequester

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // The UI is always dark, so keep light system-bar icons regardless of the system theme.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        val app = application as CleanShotsApp
        app.container.billing.start()
        setContent {
            CleanShotsTheme {
                PermissionGate { access, _ ->
                    AppShell(access)
                }
            }
        }
    }
}

private data class Tab(val route: String, @StringRes val label: Int, val icon: ImageVector)

private val TABS = listOf(
    Tab("home", R.string.tab_home, Icons.Rounded.Home),
    Tab("shots", R.string.tab_screenshots, Icons.Rounded.Screenshot),
    Tab("dupes", R.string.tab_duplicates, Icons.Rounded.ContentCopy),
    Tab("videos", R.string.tab_videos, Icons.Rounded.VideoLibrary),
    Tab("trash", R.string.tab_trash, Icons.Rounded.Delete)
)

private const val REVIEW_ROUTE = "review"

@Composable
private fun AppShell(access: String) {
    val app = LocalContext.current.applicationContext as CleanShotsApp
    val scanVm: ScanViewModel = viewModel(factory = ScanViewModel.Factory(app.container.scanRepo))
    val trashVm: TrashViewModel = viewModel(factory = TrashViewModel.Factory(app.container.db.trashDao(), app.container.prefs))
    val requester = rememberMediaRequester()
    val nav = rememberNavController()
    val snack = remember { SnackbarHostState() }
    var showPaywall by remember { mutableStateOf(false) }
    var celebrate by remember { mutableStateOf<Long?>(null) }
    val isPremium by app.container.prefs.premium.collectAsState(initial = false)
    val freeUsed by app.container.prefs.freeDeleteCount.collectAsState(initial = 0)
    val trashUi by trashVm.ui.collectAsState()
    val scanUi by scanVm.ui.collectAsState()
    val cleaner = rememberCleaner(
        requester, trashVm, scanVm, snack, isPremium, freeUsed,
        onNeedPremium = { showPaywall = true },
        onCleaned = { celebrate = it }
    )
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(access) {
        trashVm.load()
        if (access != "denied") {
            scanVm.start()
            if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val back by nav.currentBackStackEntryAsState()
    val route = back?.destination?.route
    fun go(target: String) = nav.navigate(target) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Background,
            snackbarHost = {
                SnackbarHost(snack) {
                    Snackbar(it, containerColor = SurfaceHigh, contentColor = OnDark, actionColor = Accent, shape = RoundedCornerShape(16.dp))
                }
            },
            bottomBar = { if (route != REVIEW_ROUTE) BottomBar(route, ::go) }
        ) { pad ->
            NavHost(
                nav,
                startDestination = "home",
                modifier = Modifier.padding(pad),
                enterTransition = { fadeIn(tween(240)) + scaleIn(tween(240), initialScale = 0.97f) },
                exitTransition = { fadeOut(tween(160)) },
                popEnterTransition = { fadeIn(tween(240)) + scaleIn(tween(240), initialScale = 0.97f) },
                popExitTransition = { fadeOut(tween(160)) }
            ) {
                composable("home") {
                    HomeScreen(scanVm, cleaner, onOpen = ::go, onReview = { nav.navigate(REVIEW_ROUTE) }, onUpgrade = { showPaywall = true })
                }
                composable("shots") { ScreenshotsScreen(scanVm, cleaner) }
                composable("dupes") { DuplicatesScreen(scanVm, cleaner) }
                composable("blurry") { BlurryScreen(scanVm, cleaner) }
                composable("videos") { VideosScreen(scanVm, cleaner) }
                composable("trash") {
                    TrashScreenInner(
                        items = trashUi.items,
                        canRestore = trashUi.lastTrashed.isNotEmpty(),
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
                composable(REVIEW_ROUTE) {
                    val candidates = remember { (scanUi.shots + scanUi.blurry).distinctBy { it.mediaId } }
                    SwipeReviewScreen(candidates, cleaner, onClose = { nav.popBackStack() })
                }
            }
        }

        AnimatedVisibility(
            visible = showPaywall,
            enter = slideInVertically(tween(380)) { it } + fadeIn(),
            exit = slideOutVertically(tween(280)) { it } + fadeOut()
        ) {
            PaywallScreen(
                billing = app.container.billing,
                freedBytes = trashUi.items.sumOf { it.size },
                onClose = { showPaywall = false },
                onRestored = { showPaywall = false }
            )
        }

        CelebrationOverlay(celebrate, onDone = { celebrate = null })
    }
}

@Composable
private fun BottomBar(route: String?, onSelect: (String) -> Unit) {
    NavigationBar(containerColor = SurfaceCard, tonalElevation = 0.dp) {
        TABS.forEach { t ->
            val label = stringResource(t.label)
            NavigationBarItem(
                selected = route == t.route,
                onClick = { onSelect(t.route) },
                icon = { Icon(t.icon, contentDescription = label) },
                label = { Text(label, maxLines = 1, overflow = TextOverflow.Clip, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Accent,
                    selectedTextColor = Accent,
                    indicatorColor = Accent.copy(alpha = 0.14f),
                    unselectedIconColor = Muted,
                    unselectedTextColor = Muted
                )
            )
        }
    }
}
