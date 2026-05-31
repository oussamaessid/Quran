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

    companion object {
        private const val TAG = "AdManager"

        // ── false en production ───────────────────────────────────
        private const val TEST = false

        // ── IDs pub ───────────────────────────────────────────────
        val APP_OPEN_ID: String
            get() = if (TEST) "ca-app-pub-3940256099942544/9257395921"
            else       "ca-app-pub-2498267529185476/1389455513"

        val INTER_ID: String
            get() = if (TEST) "ca-app-pub-3940256099942544/1033173712"
            else       "ca-app-pub-2498267529185476/7763292177"

        // ── ID de l'émulateur/appareil de test ────────────────────
        // Récupère cet ID dans Logcat en cherchant "Use RequestConfiguration"
        // Exemple : "Use RequestConfiguration.Builder().setTestDeviceIds(Arrays.asList("ABCDEF123456"))"
        // Colle ici l'ID que tu vois dans tes logs
        private val TEST_DEVICE_IDS = listOf(
            AdRequest.DEVICE_ID_EMULATOR   // ← ID automatique pour tous les émulateurs
        )
    }

    // ─────────────────────────────────────────────────────────────
    // Appelle depuis MainActivity.onCreate AVANT setContent
    // ─────────────────────────────────────────────────────────────
    fun initAndShowAppOpen(activity: Activity) {
        Log.d(TAG, "1️⃣ init AdMob…")

        // ── Forcer le mode test sur émulateur ─────────────────────
        val config = RequestConfiguration.Builder()
            .setTestDeviceIds(TEST_DEVICE_IDS)
            .build()
        MobileAds.setRequestConfiguration(config)

        MobileAds.initialize(context) {
            Log.d(TAG, "2️⃣ AdMob prêt → load App Open")
            loadAppOpen(activity)
        }
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
        if (navCount % 3 == 0 && ad != null) {
            Log.d(TAG, "🎬 Affichage Interstitial…")
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "✅ Interstitial affichée")
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