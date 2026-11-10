package dev.stpark.namchin

import android.content.Intent
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

    private fun Intent.sharedImage(): Uri? =
        if (action == Intent.ACTION_SEND && type?.startsWith("image/") == true) {
            getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            null
        }
}
