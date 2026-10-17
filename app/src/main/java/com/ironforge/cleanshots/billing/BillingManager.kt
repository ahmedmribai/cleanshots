package com.ironforge.cleanshots.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.ironforge.cleanshots.data.store.PrefsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Product IDs are fixed in Play Console and cannot be renamed; the price suffixes are historical.
// Displayed prices always come from Play via ProductDetails.
object Products {
    const val WEEKLY = "cleanshots_weekly_4_99"
    const val YEARLY = "cleanshots_yearly_19_99"
    const val LIFETIME = "cleanshots_lifetime_9_99"
    val ALL = setOf(WEEKLY, YEARLY, LIFETIME)
}

class BillingManager(
    context: Context,
    private val prefs: PrefsManager
) : PurchasesUpdatedListener {
    private val appCtx = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _products = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val products: StateFlow<Map<String, ProductDetails>> = _products

    private val client = BillingClient.newBuilder(appCtx)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    fun start() {
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(r: BillingResult) {
                if (r.responseCode == BillingClient.BillingResponseCode.OK) {
                    scope.launch { queryProducts(); refreshPremium() }
                }
            }
            override fun onBillingServiceDisconnected() {}
        })
    }

    private fun product(id: String, type: String) =
        QueryProductDetailsParams.Product.newBuilder().setProductId(id).setProductType(type).build()

    private suspend fun queryProducts() {
        val subs = client.queryProductDetails(
            QueryProductDetailsParams.newBuilder()
                .setProductList(listOf(
                    product(Products.WEEKLY, BillingClient.ProductType.SUBS),
                    product(Products.YEARLY, BillingClient.ProductType.SUBS)
                )).build()
        )
        val inapps = client.queryProductDetails(
            QueryProductDetailsParams.newBuilder()
                .setProductList(listOf(product(Products.LIFETIME, BillingClient.ProductType.INAPP)))
                .build()
        )
        val found = (subs.productDetailsList.orEmpty() + inapps.productDetailsList.orEmpty())
            .associateBy { it.productId }
        if (found.isNotEmpty()) _products.value = found
    }

    private fun Purchase.isActiveEntitlement() =
        purchaseState == Purchase.PurchaseState.PURCHASED && products.any { it in Products.ALL }

    private fun acknowledgeIfNeeded(p: Purchase) {
        if (p.isActiveEntitlement() && !p.isAcknowledged) {
            client.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()
            ) { }
        }
    }

    /**
     * Re-syncs the premium flag from Play. Returns the new value, or null when Play could not be
     * reached; in that case the stored flag is left unchanged so a paying user is never locked out
     * by a transient billing error.
     */
    suspend fun refreshPremium(): Boolean? {
        return try {
            val subs = client.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
            )
            val inapps = client.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
            )
            val ok = BillingClient.BillingResponseCode.OK
            if (subs.billingResult.responseCode != ok || inapps.billingResult.responseCode != ok) return null
            val all = subs.purchasesList + inapps.purchasesList
            all.forEach(::acknowledgeIfNeeded)
            val active = all.any { it.isActiveEntitlement() }
            prefs.setPremium(active)
            active
        } catch (e: Exception) {
            null
        }
    }

    fun launch(activity: Activity, productId: String, offerToken: String? = null) {
        val details = _products.value[productId] ?: return
        val paramsList = if (details.productType == BillingClient.ProductType.SUBS && offerToken != null) {
            listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).setOfferToken(offerToken).build())
        } else {
            listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build())
        }
        val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(paramsList).build()
        client.launchBillingFlow(activity, params)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode != BillingClient.BillingResponseCode.OK || purchases == null) return
        scope.launch {
            purchases.forEach(::acknowledgeIfNeeded)
            if (purchases.any { it.isActiveEntitlement() }) prefs.setPremium(true)
        }
    }
}
