package com.ironforge.cleanshots.ui.permissions

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
    } else if (Build.VERSION.SDK_INT >= 33) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
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
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Free up space in three taps", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text("CleanShots finds screenshots, duplicate photos and blurry photos so you can remove them.", color = Muted)
            Spacer(Modifier.height(12.dp))
            Text("To do that it needs to read your photos.")
            Text("Everything is analysed on your device. Nothing is uploaded, and deleted photos go to Trash first so you can undo.", color = Muted)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { launcher.launch(requiredPermissions()) },
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = androidx.compose.ui.graphics.Color.Black)
            ) { Text("Allow photo access") }
        }
    } else {
        content(access) { launcher.launch(requiredPermissions()) }
    }
}
