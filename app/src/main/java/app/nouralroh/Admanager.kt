package app.nouralroh

import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

class AdManager(private val context: Context) {

    private var appOpenAd: AppOpenAd? = null
    private var interstitialAd: InterstitialAd? = null
    private var navCount = 0
    private var lastInterShowMs = 0L

    companion object {
        private const val TAG = "AdManager"
        private val TEST = BuildConfig.DEBUG

        private const val MIN_INTER_INTERVAL_MS = 3 * 60_000L
        private const val MIN_APP_OPEN_INTERVAL_MS = 4 * 60 * 60 * 1_000L
        private const val PREF_LAST_APP_OPEN_MS = "last_app_open_ms"
        private const val APP_OPEN_MAX_WAIT_MS = 4_000L

        val APP_OPEN_ID: String
            get() = if (TEST) "ca-app-pub-3940256099942544/9257395921"
            else       "ca-app-pub-2498267529185476/1389455513"

        val INTER_ID: String
            get() = if (TEST) "ca-app-pub-3940256099942544/1033173712"
            else       "ca-app-pub-2498267529185476/7763292177"

        // Bannière adaptative de l'accueil. À REMPLIR avec l'identifiant du bloc « Bannière »
        // créé dans AdMob : tant qu'il est vide, aucune bannière en release.
        private const val BANNER_ID_RELEASE = ""
        val BANNER_ID: String?
            get() = if (TEST) "ca-app-pub-3940256099942544/9214589741"
            else BANNER_ID_RELEASE.ifBlank { null }

        private val TEST_DEVICE_IDS = listOf(
            AdRequest.DEVICE_ID_EMULATOR
        )
    }
    private var launchMs = 0L
    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context)
    private var adsInitialized = false

    /** true après consentement + initialisation AdMob : la bannière de l'accueil peut se charger. */
    private val _adsReady = MutableStateFlow(false)
    val adsReady: StateFlow<Boolean> = _adsReady.asStateFlow()

    val isPrivacyOptionsRequired: Boolean
        get() = consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { e ->
            if (e != null) Log.e(TAG, "❌ Privacy options form: ${e.message}")
        }
    }

    fun initAndShowAppOpen(activity: Activity, allowAppOpen: Boolean) {
        launchMs = System.currentTimeMillis()
        val params = ConsentRequestParameters.Builder().apply {
            if (TEST) setConsentDebugSettings(
                ConsentDebugSettings.Builder(context).addTestDeviceHashedId(AdRequest.DEVICE_ID_EMULATOR).build()
            )
        }.build()
        consentInformation.requestConsentInfoUpdate(activity, params, {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { e ->
                if (e != null) Log.e(TAG, "❌ Consent form: ${e.message}")
                if (consentInformation.canRequestAds()) initAds(activity, allowAppOpen)
            }
        }, { e ->
            Log.e(TAG, "❌ Consent info update: ${e.message}")
            // Debug seulement : annonces de test même si le formulaire UMP n'est pas configuré
            // dans AdMob (« Confidentialité et messages »). La release reste stricte.
            if (TEST) initAds(activity, allowAppOpen)
        })
        if (consentInformation.canRequestAds()) initAds(activity, allowAppOpen)
    }

    private fun initAds(activity: Activity, allowAppOpen: Boolean) {
        if (adsInitialized) return
        adsInitialized = true
        Log.d(TAG, "1️⃣ init AdMob…")

        val config = RequestConfiguration.Builder()
            .setTestDeviceIds(TEST_DEVICE_IDS)
            .build()
        MobileAds.setRequestConfiguration(config)

        MobileAds.initialize(context) {
            Log.d(TAG, "2️⃣ AdMob prêt → vérification cooldown App Open")
            _adsReady.value = true
            if (allowAppOpen && canShowAppOpen()) {
                loadAppOpen(activity)
            } else {
                Log.d(TAG, "⏱️ Cooldown App Open actif → loadInter")
                loadInter()
            }
        }
    }

    private fun canShowAppOpen(): Boolean {
        val prefs = context.getSharedPreferences("ad_prefs", Context.MODE_PRIVATE)
        val last = prefs.getLong(PREF_LAST_APP_OPEN_MS, 0L)
        return System.currentTimeMillis() - last >= MIN_APP_OPEN_INTERVAL_MS
    }

    private fun recordAppOpenShown() {
        context.getSharedPreferences("ad_prefs", Context.MODE_PRIVATE)
            .edit().putLong(PREF_LAST_APP_OPEN_MS, System.currentTimeMillis()).apply()
    }

    private fun loadAppOpen(activity: Activity) {
        AppOpenAd.load(
            context,
            APP_OPEN_ID,
            AdRequest.Builder().build(),
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    if (System.currentTimeMillis() - launchMs > APP_OPEN_MAX_WAIT_MS) {
                        Log.d(TAG, "⏱️ App Open chargée trop tard → ignorée")
                        loadInter()
                        return
                    }
                    Log.d(TAG, "3️⃣ App Open chargée → show")
                    appOpenAd = ad
                    showAppOpen(activity)
                }
                override fun onAdFailedToLoad(e: LoadAdError) {
                    Log.e(TAG, "❌ App Open load failed (${e.code}): ${e.message}")
                    loadInter()
                }
            }
        )
    }

    private fun showAppOpen(activity: Activity) {
        val ad = appOpenAd ?: run { loadInter(); return }
        val resumed = (activity as? LifecycleOwner)?.lifecycle?.currentState
            ?.isAtLeast(Lifecycle.State.RESUMED) ?: true
        if (activity.isFinishing || activity.isDestroyed || !resumed) {
            appOpenAd = null
            loadInter()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "✅ App Open affichée")
                recordAppOpenShown()
            }
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "4️⃣ App Open fermée → loadInter")
                appOpenAd = null
                loadInter()
            }
            override fun onAdFailedToShowFullScreenContent(e: AdError) {
                Log.e(TAG, "❌ App Open show failed (${e.code}): ${e.message}")
                appOpenAd = null
                loadInter()
            }
        }
        ad.show(activity)
    }

    fun loadInter() {
        if (interstitialAd != null) return
        Log.d(TAG, "⬇️ Chargement Interstitial…")
        InterstitialAd.load(
            context,
            INTER_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "✅ Interstitial prête")
                    interstitialAd = ad
                }
                override fun onAdFailedToLoad(e: LoadAdError) {
                    Log.e(TAG, "❌ Interstitial load failed (${e.code}): ${e.message}")
                    interstitialAd = null
                }
            }
        )
    }

    fun onNavigate(activity: Activity, doNavigate: () -> Unit) {
        navCount++
        Log.d(TAG, "🔢 Navigation #$navCount")
        val ad = interstitialAd
        val now = System.currentTimeMillis()
        val canShow = navCount % 3 == 0
                && ad != null
                && now - lastInterShowMs >= MIN_INTER_INTERVAL_MS
        if (canShow) {
            Log.d(TAG, "🎬 Affichage Interstitial…")
            ad!!.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "✅ Interstitial affichée")
                    lastInterShowMs = System.currentTimeMillis()
                }
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "✅ Interstitial fermée → navigate")
                    interstitialAd = null
                    loadInter()
                    doNavigate()
                }
                override fun onAdFailedToShowFullScreenContent(e: AdError) {
                    Log.e(TAG, "❌ Inter show failed → navigate quand même")
                    interstitialAd = null
                    loadInter()
                    doNavigate()
                }
            }
            ad.show(activity)
        } else {
            doNavigate()
        }
    }
}
