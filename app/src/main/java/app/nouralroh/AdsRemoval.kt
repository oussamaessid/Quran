package app.nouralroh

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Achat unique « Retirer les publicités » (Google Play Billing).
 * Le produit intégré "remove_ads" (type : produit unique) doit exister dans la Play Console.
 * L'état est mis en cache localement pour couper les pubs dès le lancement, puis revérifié
 * auprès de Google Play à chaque démarrage (remboursement → les pubs reviennent).
 */
object AdsRemoval {

    private const val TAG = "AdsRemoval"
    const val PRODUCT_ID = "remove_ads"
    private const val PREFS = "ad_prefs"
    private const val KEY_REMOVED = "ads_removed"

    private val _removed = MutableStateFlow(false)
    val removed: StateFlow<Boolean> = _removed.asStateFlow()

    /** Prix localisé (ex. « 2,99 € ») ; null tant que le produit n'est pas disponible. */
    private val _price = MutableStateFlow<String?>(null)
    val price: StateFlow<String?> = _price.asStateFlow()

    private var client: BillingClient? = null
    private var details: ProductDetails? = null
    private lateinit var appContext: Context

    fun isRemoved(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_REMOVED, false)

    fun init(context: Context) {
        if (client != null) return
        appContext = context.applicationContext
        _removed.value = isRemoved(appContext)
        client = BillingClient.newBuilder(appContext)
            .setListener { result, purchases ->
                if (result.responseCode == BillingResponseCode.OK) purchases?.forEach(::handle)
            }
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()
        connect()
    }

    private fun connect() {
        client?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode != BillingResponseCode.OK) {
                    Log.e(TAG, "❌ Billing setup: ${result.debugMessage}")
                    return
                }
                refreshPurchases()
                loadProduct()
            }
            override fun onBillingServiceDisconnected() {
                Log.d(TAG, "Billing déconnecté")
            }
        })
    }

    private fun refreshPurchases() {
        val params = QueryPurchasesParams.newBuilder().setProductType(ProductType.INAPP).build()
        client?.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingResponseCode.OK) return@queryPurchasesAsync
            val owned = purchases.filter { PRODUCT_ID in it.products }
            owned.forEach(::handle)
            if (owned.none { it.purchaseState == Purchase.PurchaseState.PURCHASED }) setRemoved(false)
        }
    }

    private fun loadProduct() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUCT_ID).setProductType(ProductType.INAPP).build()
        val params = QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()
        client?.queryProductDetailsAsync(params) { result, list ->
            if (result.responseCode != BillingResponseCode.OK) return@queryProductDetailsAsync
            details = list.firstOrNull()
            _price.value = details?.oneTimePurchaseOfferDetails?.formattedPrice
        }
    }

    /** false si le produit n'est pas (encore) disponible. */
    fun purchase(activity: Activity): Boolean {
        val d = details ?: return false
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d).build()
            ))
            .build()
        return client?.launchBillingFlow(activity, params)?.responseCode == BillingResponseCode.OK
    }

    private fun handle(purchase: Purchase) {
        if (PRODUCT_ID !in purchase.products ||
            purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        setRemoved(true)
        if (!purchase.isAcknowledged) {
            val ack = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
            client?.acknowledgePurchase(ack) { r ->
                if (r.responseCode != BillingResponseCode.OK) Log.e(TAG, "❌ Ack: ${r.debugMessage}")
            }
        }
    }

    private fun setRemoved(value: Boolean) {
        _removed.value = value
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_REMOVED, value).apply()
    }
}
