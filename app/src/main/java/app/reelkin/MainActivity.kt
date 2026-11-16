package app.reelkin

import android.content.Intent
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reelkin.data.Settings
import app.reelkin.ui.ReelkinRoot
import app.reelkin.ui.theme.ReelkinTheme
import kotlinx.coroutines.flow.MutableStateFlow

/** Text shared to the app, a link to a film most often, waiting for the search to take it. */
object Shared { val text = MutableStateFlow<String?>(null) }

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) take(intent)
        val t = android.graphics.Color.TRANSPARENT
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(t), navigationBarStyle = SystemBarStyle.dark(t))
        setContent {
            val settings: Settings? by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            // Wait for settings so the welcome doesn't flash for someone already here.
            settings?.let { s -> ReelkinTheme { ReelkinRoot(s) } }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        take(intent)
    }

    private fun take(i: Intent?) {
        if (i?.action == Intent.ACTION_SEND) i.getStringExtra(Intent.EXTRA_TEXT)?.takeIf { it.isNotBlank() }?.let { Shared.text.value = it }
    }
}
