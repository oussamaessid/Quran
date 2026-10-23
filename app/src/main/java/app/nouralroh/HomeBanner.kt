package app.nouralroh

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * Bannière adaptative ancrée en bas de l'accueil (uniquement l'accueil : jamais pendant la
 * lecture, la prière ou les adhkar). Habillée aux couleurs de l'app, avec la mention « إعلان ».
 * Rien n'est affiché tant que l'annonce n'est pas chargée : pas de cadre vide hors ligne,
 * sans consentement, ou si aucune annonce n'est disponible.
 */
@Composable
fun HomeBanner(adsReady: Boolean) {
    val adUnitId = AdManager.BANNER_ID ?: return
    if (!adsReady) return

    val context = LocalContext.current
    val widthDp = LocalConfiguration.current.screenWidthDp
    val adSize = remember(widthDp) {
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
    }
    var loaded by remember { mutableStateOf(false) }

    val adView = remember(adSize) {
        AdView(context).apply {
            setAdSize(adSize)
            this.adUnitId = adUnitId
            adListener = object : AdListener() {
                override fun onAdLoaded() { loaded = true }
                override fun onAdFailedToLoad(e: LoadAdError) { loaded = false }
            }
            loadAd(AdRequest.Builder().build())
        }
    }
    DisposableEffect(adView) { onDispose { adView.destroy() } }

    // Un seul AndroidView, toujours composé (l'AdView doit exister pour se charger) ;
    // le conteneur reste à hauteur 0 jusqu'au chargement puis s'ouvre en douceur.
    Column(
        Modifier
            .fillMaxWidth()
            .animateContentSize()
            .then(if (loaded) Modifier.background(QuranColors.Panel) else Modifier.height(0.dp)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Fine ligne dorée qui sépare l'annonce du contenu de l'accueil.
        Box(
            Modifier.fillMaxWidth().height(0.5.dp).background(
                Brush.horizontalGradient(listOf(
                    Color.Transparent, QuranColors.Gold.copy(alpha = 0.6f), Color.Transparent
                ))
            )
        )
        Text(
            stringResource(R.string.ad_label),
            fontSize = 9.sp, color = QuranColors.TextMuted,
            modifier = Modifier.align(Alignment.Start).padding(horizontal = 12.dp, vertical = 2.dp)
        )
        AndroidView(
            factory = { adView },
            modifier = Modifier.fillMaxWidth().height(adSize.height.dp)
        )
    }
}
