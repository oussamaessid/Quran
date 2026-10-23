package app.nouralroh

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Demande POST_NOTIFICATIONS au moment où l'utilisateur active une fonction qui en a
 * besoin (adhan, rappel de khatm) plutôt qu'au lancement : le contexte rend
 * l'acceptation beaucoup plus probable.
 */
@Composable
fun rememberNotificationPermissionRequest(): () -> Unit {
    val context  = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    return remember(launcher) {
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
