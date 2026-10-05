package app.quranaudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.quranaudio.ads.AdsManager
import app.quranaudio.ui.AppRoot
import app.quranaudio.ui.AppViewModel
import app.quranaudio.ui.theme.QuranAudioTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var ads: AdsManager
    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Keep the splash until preferences are read, so theme and onboarding state never flash.
        splash.setKeepOnScreenCondition { appViewModel.settings.value == null }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        ads.gatherConsent(this)
        setContent {
            val settings by appViewModel.settings.collectAsStateWithLifecycle()
            val s = settings ?: return@setContent
            // Decide the start destination once; finishing onboarding then navigates normally.
            val showOnboarding = remember { !s.onboardingDone }
            QuranAudioTheme(themeMode = s.themeMode) {
                AppRoot(appViewModel, ads, showOnboarding = showOnboarding)
            }
        }
    }
}
