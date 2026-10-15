package app.nouralroh

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Politique de confidentialité affichée dans l'appli (exigence Google Play).
 * Doit rester cohérente avec la section « Sécurité des données » de la Play Console
 * et avec docs/privacy-policy.html.
 */
const val PRIVACY_POLICY_TEXT = """Dernière mise à jour : 15 octobre 2026

Cette application ne demande aucun compte et n'exploite aucun serveur propre. Les données ci-dessous sont traitées uniquement pour faire fonctionner l'application et afficher des publicités.

1. Localisation
Si vous l'autorisez, l'application utilise la position de votre appareil (précise ou approximative) pour calculer les horaires de prière et la direction de la Qibla. Pour les horaires de prière, la latitude et la longitude sont envoyées au service tiers AlAdhan (api.aladhan.com). La direction de la Qibla est calculée sur l'appareil. La localisation est facultative : vous pouvez refuser ou retirer la permission dans les réglages Android.

2. Publicités (Google AdMob)
L'application affiche des publicités via Google AdMob. Google peut collecter l'identifiant publicitaire de l'appareil, l'adresse IP (position approximative), des interactions avec l'application ainsi que des données de diagnostic et de plantage, à des fins de publicité, d'analyse et de prévention de la fraude. Dans l'Espace économique européen, au Royaume-Uni et en Suisse, votre consentement est demandé et peut être modifié à tout moment dans Paramètres → Choix publicitaires. Vous pouvez aussi réinitialiser ou supprimer votre identifiant publicitaire dans les réglages Android. Politique de Google : https://policies.google.com/privacy

3. Contenu téléchargé
Le texte du Coran et les récitations audio sont téléchargés depuis des serveurs publics (api.qurancdn.com, quranicaudio.com, quran.com, mp3quran.net, raw.githubusercontent.com). Ces requêtes ne contiennent aucune donnée personnelle, mais ces serveurs reçoivent votre adresse IP, comme pour toute connexion Internet.

4. Données stockées sur l'appareil
Vos favoris, votre progression de lecture (Khatm), vos réglages d'Adhan et le contenu téléchargé sont enregistrés localement sur votre appareil. Selon vos réglages Android, certaines préférences peuvent être sauvegardées par le service de sauvegarde de votre appareil et restaurées lors d'une réinstallation ou d'un changement d'appareil. Vous pouvez supprimer les données locales depuis les réglages Android ou en désinstallant l'application ; la gestion et la suppression des sauvegardes dépendent des réglages de votre compte et de votre appareil.

5. Sécurité
Toutes les communications réseau sont chiffrées (HTTPS).

6. Enfants
L'application ne s'adresse pas spécifiquement aux enfants de moins de 13 ans et ne collecte pas sciemment de données les concernant.

7. Contact
Pour toute question : barshatech123@gmail.com"""

@Composable
fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Politique de confidentialité") },
        text = {
            Text(
                PRIVACY_POLICY_TEXT,
                fontSize = 12.sp,
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } },
    )
}
