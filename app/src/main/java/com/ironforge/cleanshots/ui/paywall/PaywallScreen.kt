package com.ironforge.cleanshots.ui.paywall

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.billingclient.api.BillingClient
import com.ironforge.cleanshots.billing.BillingManager
import com.ironforge.cleanshots.billing.Products
import com.ironforge.cleanshots.ui.scan.formatBytes
import com.ironforge.cleanshots.ui.theme.Accent
import com.ironforge.cleanshots.ui.theme.Muted

// Set to the hosted privacy policy before release; the link is hidden while blank.
private const val PRIVACY_POLICY_URL = "https://ahmedmribai.github.io/cleanshots/"

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun PaywallScreen(
    billing: BillingManager,
    freedBytes: Long,
    onClose: () -> Unit,
    onRestored: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val products by billing.products.collectAsState()
    var restoreMessage by remember { mutableStateOf<String?>(null) }

    fun price(id: String): String? {
        val p = products[id] ?: return null
        return if (p.productType == BillingClient.ProductType.SUBS) {
            p.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice
        } else {
            p.oneTimePurchaseOfferDetails?.formattedPrice
        }
    }

    fun buy(id: String) {
        val activity = ctx.findActivity() ?: return
        val offer = products[id]?.subscriptionOfferDetails?.firstOrNull()?.offerToken
        billing.launch(activity, id, offer)
    }

    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(24.dp))
        val title = if (freedBytes > 0) "You've freed ${formatBytes(freedBytes)} for free" else "Unlock unlimited cleaning"
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Unlock unlimited", color = Muted, fontSize = 16.sp)
        Spacer(Modifier.height(16.dp))
        Text("✓ Unlimited cleaning\n✓ Auto weekly scan\n✓ Trash 30 days")
        Spacer(Modifier.height(24.dp))
        val lifetimePrice = price(Products.LIFETIME)
        Button(
            onClick = { buy(Products.LIFETIME) },
            enabled = lifetimePrice != null,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.Black)
        ) { Text(if (lifetimePrice != null) "Continue $lifetimePrice Lifetime" else "Loading prices...") }
        price(Products.YEARLY)?.let { yearlyPrice ->
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { buy(Products.YEARLY) }, modifier = Modifier.fillMaxWidth()) {
                Text("Yearly $yearlyPrice")
            }
        }
        price(Products.WEEKLY)?.let { weeklyPrice ->
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { buy(Products.WEEKLY) }, modifier = Modifier.fillMaxWidth()) {
                Text("Weekly $weeklyPrice")
            }
        }
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = {
            scope.launch {
                when (billing.refreshPremium()) {
                    true -> onRestored()
                    false -> restoreMessage = "No previous purchase found"
                    null -> restoreMessage = "Couldn't reach Google Play. Try again."
                }
            }
        }) { Text("Restore Purchases", color = Accent) }
        restoreMessage?.let { Text(it, color = Muted, fontSize = 12.sp) }
        if (PRIVACY_POLICY_URL.isNotBlank()) {
            Text("Privacy policy: $PRIVACY_POLICY_URL", color = Muted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onClose) { Text("Continue with limited free", color = Muted) }
    }
}
