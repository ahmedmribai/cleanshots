package com.ironforge.cleanshots.ui.paywall

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.android.billingclient.api.BillingClient
import com.ironforge.cleanshots.billing.BillingManager
import com.ironforge.cleanshots.billing.Products
import com.ironforge.cleanshots.ui.components.GradientButton
import com.ironforge.cleanshots.ui.components.GradientIconBadge
import com.ironforge.cleanshots.ui.components.Pill
import com.ironforge.cleanshots.ui.components.RoundIconButton
import com.ironforge.cleanshots.ui.components.enterAnimation
import com.ironforge.cleanshots.ui.scan.formatBytes
import com.ironforge.cleanshots.ui.theme.Accent
import com.ironforge.cleanshots.ui.theme.AccentBlue
import com.ironforge.cleanshots.ui.theme.Background
import com.ironforge.cleanshots.ui.theme.BrandGradient
import com.ironforge.cleanshots.ui.theme.Muted
import com.ironforge.cleanshots.ui.theme.OnAccent
import com.ironforge.cleanshots.ui.theme.Stroke
import com.ironforge.cleanshots.ui.theme.SurfaceCard
import com.ironforge.cleanshots.ui.theme.SurfaceHigh
import kotlinx.coroutines.launch

private const val PRIVACY_POLICY_URL = "https://ahmedmribai.github.io/cleanshots/"

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private data class Plan(val id: String, val title: String, val period: String, val badge: String?)

private val PLANS = listOf(
    Plan(Products.LIFETIME, "Lifetime", "one-time payment", "BEST VALUE"),
    Plan(Products.YEARLY, "Yearly", "per year", null),
    Plan(Products.WEEKLY, "Weekly", "per week", null)
)

@Composable
fun PaywallScreen(
    billing: BillingManager,
    freedBytes: Long,
    onClose: () -> Unit,
    onRestored: () -> Unit
) {
    val ctx = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val products by billing.products.collectAsState()
    var restoreMessage by remember { mutableStateOf<String?>(null) }
    var chosen by remember { mutableStateOf(Products.LIFETIME) }
    BackHandler(onBack = onClose)

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

    val available = PLANS.filter { price(it.id) != null }
    val active = if (price(chosen) != null) chosen else available.firstOrNull()?.id ?: chosen
    val selectedPrice = price(active)

    Box(Modifier.fillMaxSize().background(Background)) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                RoundIconButton(Icons.Rounded.Close, "Close", onClose, size = 40.dp)
            }
            GlowingCrown(Modifier.enterAnimation(0))
            Spacer(Modifier.height(12.dp))
            Text(
                if (freedBytes > 0) "You've cleared ${formatBytes(freedBytes)}" else "CleanShots Pro",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.enterAnimation(1)
            )
            Text(
                "Keep going without limits",
                style = MaterialTheme.typography.bodyLarge,
                color = Muted,
                modifier = Modifier.enterAnimation(1)
            )
            Spacer(Modifier.height(24.dp))
            Column(Modifier.enterAnimation(2), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Benefit(Icons.Rounded.AllInclusive, "Unlimited cleaning", "No cap on how many photos you clear")
                Benefit(Icons.Rounded.AutoAwesome, "Smart Clean in one tap", "Keep the best shot, clear the copies")
                Benefit(Icons.Rounded.Swipe, "Swipe review", "Fly through screenshots in seconds")
                Benefit(Icons.Rounded.Shield, "Private by design", "Nothing ever leaves your phone")
            }
            Spacer(Modifier.height(24.dp))
            Column(Modifier.enterAnimation(3), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (available.isEmpty()) {
                    Text("Loading prices…", color = Muted, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
                available.forEach { plan ->
                    PlanCard(plan, price(plan.id).orEmpty(), selected = active == plan.id) { chosen = plan.id }
                }
            }
            Spacer(Modifier.height(20.dp))
            GradientButton(
                if (selectedPrice != null) "Continue · $selectedPrice" else "Continue",
                onClick = { buy(active) },
                enabled = selectedPrice != null,
                modifier = Modifier.fillMaxWidth().enterAnimation(4)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Subscriptions renew automatically until cancelled in Google Play.",
                color = Muted,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
            Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(
                    "Restore purchases",
                    color = Accent,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.clickable {
                        scope.launch {
                            when (billing.refreshPremium()) {
                                true -> onRestored()
                                false -> restoreMessage = "No previous purchase found"
                                null -> restoreMessage = "Couldn't reach Google Play. Try again."
                            }
                        }
                    }
                )
                Text(
                    "Privacy policy",
                    color = Muted,
                    style = MaterialTheme.typography.labelLarge.copy(textDecoration = TextDecoration.Underline),
                    modifier = Modifier.clickable { uriHandler.openUri(PRIVACY_POLICY_URL) }
                )
            }
            restoreMessage?.let { Text(it, color = Muted, style = MaterialTheme.typography.bodySmall) }
            Text(
                "Continue with free version",
                color = Muted,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.clickable(onClick = onClose).padding(12.dp)
            )
        }
    }
}

@Composable
private fun GlowingCrown(modifier: Modifier = Modifier) {
    val glow by rememberInfiniteTransition(label = "crown")
        .animateFloat(0.25f, 0.55f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "crownGlow")
    Box(modifier.size(140.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Brush.radialGradient(listOf(Accent.copy(alpha = glow), AccentBlue.copy(alpha = glow * 0.4f), Background.copy(alpha = 0f))))
        }
        Box(
            Modifier.size(76.dp).clip(RoundedCornerShape(24.dp)).background(BrandGradient),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.WorkspacePremium, contentDescription = null, tint = OnAccent, modifier = Modifier.size(42.dp))
        }
    }
}

@Composable
private fun Benefit(icon: ImageVector, title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        GradientIconBadge(icon)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PlanCard(plan: Plan, price: String, selected: Boolean, onSelect: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    val bg by animateColorAsState(if (selected) SurfaceHigh else SurfaceCard, label = "planBg")
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bg)
            .then(if (selected) Modifier.border(2.dp, BrandGradient, shape) else Modifier.border(1.dp, Stroke, shape))
            .clickable(onClick = onSelect)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(plan.title, style = MaterialTheme.typography.titleMedium)
                plan.badge?.let {
                    Spacer(Modifier.width(8.dp))
                    Pill(it, brush = BrandGradient, textColor = OnAccent)
                }
            }
            Text(plan.period, color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        Text(price, style = MaterialTheme.typography.titleLarge)
    }
}
