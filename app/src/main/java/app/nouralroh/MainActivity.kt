package app.nouralroh

import android.Manifest
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import app.nouralroh.data.DataInstallManager
import app.nouralroh.viewmodel.InstallViewModel
import app.nouralroh.viewmodel.KhatmViewModel
import app.nouralroh.viewmodel.SalatViewModel

enum class AppScreen {
    INSTALL, HOME,
    QURAN, QIBLA, TASBIH, ADHKAR, SALAT, AUDIO, KHATM, KHATM_READ
}

class MainActivity : ComponentActivity() {

    private lateinit var adManager: AdManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = false
        KhatmNotificationHelper.createChannel(this)

        // ── Pub : init + App Open AVANT setContent ────────────────
        adManager = AdManager(this)
        adManager.initAndShowAppOpen(this)
        // ↑ Chaîne : init → load → show → loadInter (tout automatique)

        setContent {
            val installVm: InstallViewModel = viewModel()
            val salatVm: SalatViewModel     = viewModel()
            val khatmVm: KhatmViewModel     = viewModel()

            var currentScreen by remember {
                mutableStateOf(
                    if (DataInstallManager.isInstalled(this)) AppScreen.HOME
                    else AppScreen.INSTALL
                )
            }
            var khatmStartPage by remember { mutableStateOf(1) }

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

            // ── Navigation avec inter (jamais sur retour HOME) ─────
            fun goTo(screen: AppScreen) {
                adManager.onNavigate(this@MainActivity) { currentScreen = screen }
            }

            val permLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { result ->
                permissionGranted =
                    result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                gpsEnabled = hasGps()
            }

            val notifPermLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) {}

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity, Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            val lifecycle = LocalLifecycleOwner.current.lifecycle
            DisposableEffect(lifecycle) {
                val obs = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        permissionGranted = hasPermission()
                        gpsEnabled        = hasGps()
                    }
                }
                lifecycle.addObserver(obs)
                onDispose { lifecycle.removeObserver(obs) }
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .background(QuranColors.AppBg)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(350)) },
                    label = "screen"
                ) { screen ->
                    when (screen) {

                        AppScreen.INSTALL -> InstallScreen(
                            vm = installVm,
                            onInstallComplete = { currentScreen = AppScreen.HOME }
                        )

                        AppScreen.HOME -> HomeScreen(
                            permissionGranted   = permissionGranted,
                            gpsEnabled          = gpsEnabled,
                            onRequestPermission = {
                                permLauncher.launch(arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ))
                            },
                            // ── Toutes les cartes passent par goTo() ──────────
                            onOpenQuran  = { goTo(AppScreen.QURAN)  },
                            onOpenQibla  = { goTo(AppScreen.QIBLA)  },
                            onOpenTasbih = { goTo(AppScreen.TASBIH) },
                            onOpenAdhkar = { goTo(AppScreen.ADHKAR) },
                            onOpenKhatm  = { goTo(AppScreen.KHATM)  },
                            onOpenSalat  = { goTo(AppScreen.SALAT)  },
                            onOpenAudio  = { goTo(AppScreen.AUDIO)  }
                        )

                        AppScreen.QURAN  -> QuranScreen(
                            onBack = { currentScreen = AppScreen.HOME }  // retour direct, pas de pub
                        )
                        AppScreen.QIBLA  -> QiblaScreen(
                            onBack = { currentScreen = AppScreen.HOME }
                        )
                        AppScreen.TASBIH -> TasbihScreen(
                            onBack = { currentScreen = AppScreen.HOME }
                        )
                        AppScreen.ADHKAR -> AdhkarScreen(
                            onBack = { currentScreen = AppScreen.HOME }
                        )
                        AppScreen.KHATM  -> KhatmScreen(
                            khatmVm    = khatmVm,
                            onBack     = { currentScreen = AppScreen.HOME },
                            onOpenPage = { page ->
                                khatmStartPage = page
                                currentScreen  = AppScreen.KHATM_READ   // interne, pas de pub
                            }
                        )
                        AppScreen.KHATM_READ -> KhatmReadScreen(
                            startPage = khatmStartPage,
                            vm        = khatmVm,
                            onBack    = { currentScreen = AppScreen.KHATM }
                        )
                        AppScreen.SALAT -> SalatScreen(
                            vm     = salatVm,
                            onBack = { currentScreen = AppScreen.HOME }
                        )
                        AppScreen.AUDIO -> AudioScreen(
                            onBack = { currentScreen = AppScreen.HOME }
                        )
                    }
                }
            }
        }
    }
}