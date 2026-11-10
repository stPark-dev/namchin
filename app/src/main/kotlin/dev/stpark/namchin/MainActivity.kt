package dev.stpark.namchin

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.stpark.namchin.ui.NamchinApp
import dev.stpark.namchin.ui.theme.NamchinTheme

class MainActivity : ComponentActivity() {
    private var sharedUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) sharedUri = intent.sharedImage()
        setContent {
            NamchinTheme {
                NamchinApp(sharedUri = sharedUri, onSharedConsumed = { sharedUri = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        sharedUri = intent.sharedImage()
    }

    /** content:// 만 받는다. file:// 은 adb 테스트용으로 디버그 빌드에서만 허용. */
    private fun Intent.sharedImage(): Uri? {
        if (action != Intent.ACTION_SEND || type?.startsWith("image/") != true) return null
        val uri = getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java) ?: return null
        val debuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        return uri.takeIf { it.scheme == "content" || (debuggable && it.scheme == "file") }
    }
}
