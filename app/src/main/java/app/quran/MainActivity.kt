package app.quran

import android.Manifest
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import app.quran.viewmodel.QuranViewModel

enum class AppScreen { HOME, QURAN, QIBLA, TASBIH, ADHKAR }

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = false

        setContent {
            val quranVm: QuranViewModel = viewModel()
            var currentScreen by remember { mutableStateOf(AppScreen.HOME) }

            fun hasPermission() = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            fun hasGps(): Boolean {
                val lm = getSystemService(LOCATION_SERVICE) as LocationManager
                return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                        lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            }

            var permissionGranted by remember { mutableStateOf(hasPermission()) }
            var gpsEnabled        by remember { mutableStateOf(hasGps()) }

            val permLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { result ->
                permissionGranted =
                    result[Manifest.permission.ACCESS_FINE_LOCATION]  == true ||
                            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                gpsEnabled = hasGps()
            }

            val lifecycle = LocalLifecycleOwner.current.lifecycle
            DisposableEffect(lifecycle) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        permissionGranted = hasPermission()
                        gpsEnabled        = hasGps()
                    }
                }
                lifecycle.addObserver(observer)
                onDispose { lifecycle.removeObserver(observer) }
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .background(QuranColors.AppBg)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                AnimatedContent(
                    targetState   = currentScreen,
                    transitionSpec = { fadeIn() togetherWith fadeOut() }
                ) { screen ->
                    when (screen) {
                        AppScreen.HOME -> HomeScreen(
                            permissionGranted   = permissionGranted,
                            gpsEnabled          = gpsEnabled,
                            onRequestPermission = {
                                permLauncher.launch(arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ))
                            },
                            onOpenQuran  = { currentScreen = AppScreen.QURAN },
                            onOpenQibla  = { currentScreen = AppScreen.QIBLA },
                            onOpenTasbih = { currentScreen = AppScreen.TASBIH },
                            onOpenAdhkar = { currentScreen = AppScreen.ADHKAR }
                        )
                        AppScreen.QURAN -> QuranScreen(
                            vm     = quranVm,
                            onBack = { currentScreen = AppScreen.HOME }
                        )
                        AppScreen.QIBLA -> QiblaScreen(
                            onBack = { currentScreen = AppScreen.HOME }
                        )
                        AppScreen.TASBIH -> TasbihScreen(
                            onBack = { currentScreen = AppScreen.HOME }
                        )
                        AppScreen.ADHKAR -> AdhkarScreen(
                            onBack = { currentScreen = AppScreen.HOME }
                        )
                    }
                }
            }
        }
    }
}