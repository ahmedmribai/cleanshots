package com.ironforge.cleanshots.ui.permissions

import com.ironforge.cleanshots.R
import androidx.compose.ui.res.stringResource
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import com.ironforge.cleanshots.ui.components.GradientButton
import com.ironforge.cleanshots.ui.components.GradientIconBadge
import com.ironforge.cleanshots.ui.components.ProgressRing
import com.ironforge.cleanshots.ui.components.enterAnimation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ironforge.cleanshots.ui.theme.Accent
import com.ironforge.cleanshots.ui.theme.Muted

fun accessState(ctx: Context): String {
    return if (Build.VERSION.SDK_INT >= 33) {
        val full = ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        if (full) "full"
        else if (Build.VERSION.SDK_INT >= 34 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED) "partial"
        else "denied"
    } else {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) "full" else "denied"
    }
}

fun requiredPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= 34) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
    } else if (Build.VERSION.SDK_INT >= 33) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

@Composable
fun PermissionGate(content: @Composable (access: String, reselect: () -> Unit) -> Unit) {
    val ctx = LocalContext.current
    var access by remember { mutableStateOf(accessState(ctx)) }
    var showRationale by remember { mutableStateOf(access == "denied") }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        access = accessState(ctx)
        showRationale = access == "denied"
    }

    LaunchedEffect(Unit) {
        if (access == "denied") showRationale = true
    }

    if (access == "denied" && showRationale) {
        Onboarding(onAllow = { launcher.launch(requiredPermissions()) })
    } else {
        content(access) { launcher.launch(requiredPermissions()) }
    }
}

@Composable
private fun Onboarding(onAllow: () -> Unit) {
    Column(
        Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ProgressRing(1f, Modifier.size(150.dp).enterAnimation(0), strokeWidth = 10.dp) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Accent, modifier = Modifier.size(56.dp))
        }
        Spacer(Modifier.height(28.dp))
        Text(
            stringResource(R.string.onboarding_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.enterAnimation(1)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.onboarding_body),
            color = Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.enterAnimation(1)
        )
        Spacer(Modifier.height(28.dp))
        Column(Modifier.enterAnimation(2), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Promise(Icons.Rounded.PhoneAndroid, stringResource(R.string.promise_device_title), stringResource(R.string.promise_device_sub))
            Promise(Icons.Rounded.Restore, stringResource(R.string.promise_trash_title), stringResource(R.string.promise_trash_sub))
            Promise(Icons.Rounded.Shield, stringResource(R.string.promise_control_title), stringResource(R.string.promise_control_sub))
        }
        Spacer(Modifier.height(32.dp))
        GradientButton(stringResource(R.string.onboarding_allow), onClick = onAllow, modifier = Modifier.fillMaxWidth().enterAnimation(3))
    }
}

@Composable
private fun Promise(icon: ImageVector, title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        GradientIconBadge(icon)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}
