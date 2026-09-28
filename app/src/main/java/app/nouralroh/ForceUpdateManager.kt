package app.nouralroh

import android.app.Activity
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability

/**
 * Mise à jour OBLIGATOIRE via Google Play (In-App Updates, mode IMMEDIATE).
 * Dès qu'une version plus récente existe sur le Play Store, un écran plein
 * écran bloque l'app jusqu'à la mise à jour. Si l'utilisateur annule → l'app se ferme.
 *
 * Ne fonctionne que pour une app installée depuis le Play Store
 * (tester avec Internal App Sharing / piste de test interne).
 *
 * Doit être créé pendant l'initialisation de l'activité (avant onStart).
 */
class ForceUpdateManager(private val activity: ComponentActivity) {

    private val appUpdateManager by lazy { AppUpdateManagerFactory.create(activity) }

    private val launcher: ActivityResultLauncher<IntentSenderRequest> =
        activity.registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_CANCELED) {
                Log.d(TAG, "Mise à jour refusée → fermeture de l'app")
                activity.finishAffinity()
            }
        }

    /** Vérifie une mise à jour ; [onNoUpdate] est appelé si l'app est à jour (ou hors-ligne). */
    fun check(onNoUpdate: () -> Unit) {
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                ) {
                    Log.d(TAG, "Mise à jour disponible (${info.availableVersionCode()}) → obligatoire")
                    startUpdate(info)
                } else {
                    onNoUpdate()
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Vérification impossible : ${e.message}")
                onNoUpdate()
            }
    }

    /** À appeler dans onResume : relance l'écran si l'utilisateur a quitté pendant la mise à jour. */
    fun resumeIfInProgress() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                startUpdate(info)
            }
        }
    }

    private fun startUpdate(info: AppUpdateInfo) {
        appUpdateManager.startUpdateFlowForResult(
            info,
            launcher,
            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
        )
    }

    companion object {
        private const val TAG = "ForceUpdate"
    }
}
