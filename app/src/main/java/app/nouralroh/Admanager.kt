package app.nouralroh

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class AdManager(private val context: Context) {

    private var appOpenAd: AppOpenAd? = null
    private var interstitialAd: InterstitialAd? = null
    private var navCount = 0
    private var lastInterShowMs = 0L

    companion object {
        private const val TAG = "AdManager"

        // IMPORTANT : mettre TEST = true sur votre propre appareil pendant les tests
        // pour éviter tout clic accidentel sur de vraies annonces (trafic invalide).
        private const val TEST = false

        // Délai minimum entre deux interstitiels : 60 secondes
        private const val MIN_INTER_INTERVAL_MS = 60_000L

        // Délai minimum entre deux App Open Ads : 4 heures
        private const val MIN_APP_OPEN_INTERVAL_MS = 4 * 60 * 60 * 1_000L
        private const val PREF_LAST_APP_OPEN_MS = "last_app_open_ms"

        val APP_OPEN_ID: String
            get() = if (TEST) "ca-app-pub-3940256099942544/9257395921"
            else       "ca-app-pub-2498267529185476/1389455513"

        val INTER_ID: String
            get() = if (TEST) "ca-app-pub-3940256099942544/1033173712"
            else       "ca-app-pub-2498267529185476/7763292177"

        // Ajoutez l'ID de votre vrai téléphone ici (trouvez-le dans Logcat :
        // "Use RequestConfiguration.Builder().setTestDeviceIds(Arrays.asList("VOTRE_ID"))")
        private val TEST_DEVICE_IDS = listOf(
            AdRequest.DEVICE_ID_EMULATOR
            // "VOTRE_ID_APPAREIL_LOGCAT"
        )
    }

    fun initAndShowAppOpen(activity: Activity) {
        Log.d(TAG, "1️⃣ init AdMob…")

        val config = RequestConfiguration.Builder()
            .setTestDeviceIds(TEST_DEVICE_IDS)
            .build()
        MobileAds.setRequestConfiguration(config)

        MobileAds.initialize(context) {
            Log.d(TAG, "2️⃣ AdMob prêt → vérification cooldown App Open")
            if (canShowAppOpen()) {
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
