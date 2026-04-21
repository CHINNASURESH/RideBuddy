package com.example.ridebuddy.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import dagger.hilt.android.qualifiers.ApplicationContext
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import com.example.ridebuddy.util.AnalyticsManager

@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analyticsManager: AnalyticsManager
) : PurchasesUpdatedListener {

    private val _isPro = MutableStateFlow(false)
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    init {
        startConnection()
    }

    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryPurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        })
    }

    fun queryPurchases() {
        if (!billingClient.isReady) {
            return
        }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in purchases) {
                    handlePurchase(purchase)
                }
            }
        }
    }

    fun launchBillingFlow(activity: Activity, onComplete: (Boolean) -> Unit) {
        if (!billingClient.isReady) {
            onComplete(false)
            return
        }

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("ridebuddy_pro")
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val productDetails = productDetailsList[0]

                val offerToken = productDetails.subscriptionOfferDetails?.get(0)?.offerToken

                val productDetailsParamsList = if (offerToken != null) {
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetails)
                            .setOfferToken(offerToken)
                            .build()
                    )
                } else return@queryProductDetailsAsync

                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build()

                val result = billingClient.launchBillingFlow(activity, billingFlowParams)
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    analyticsManager.logProBillingError(result.responseCode, result.debugMessage)
                    onComplete(false)
                }
            } else {
                analyticsManager.logProBillingError(billingResult.responseCode, billingResult.debugMessage)
                onComplete(false)
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) {
            analyticsManager.logProBillingError(billingResult.responseCode, billingResult.debugMessage)
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            val data = hashMapOf(
                "purchaseToken" to purchase.purchaseToken,
                "productId" to purchase.products[0]
            )

            // Call Cloud Function for server-side verification.
            // The Cloud Function will verify, acknowledge the purchase, and update the user's Firestore doc.
            Firebase.functions.getHttpsCallable("verifySubscription")
                .call(data)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        analyticsManager.logProBillingSuccess()
                        // The server will update Firestore, and our listener will pick up the 'isProActive' change.
                    } else {
                        analyticsManager.logProBillingError(-1, task.exception?.message ?: "Server verification failed")
                        // Handle error (e.g., show message to user)
                    }
                }
        }
    }
}
