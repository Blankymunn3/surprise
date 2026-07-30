package kr.surprise.memorymap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import kr.surprise.memorymap.core.designsystem.theme.MemoryTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val container = (application as MemoryMapApp).container

        setContent {
            MemoryTheme {
                MemoryMapNavHost(container = container)
            }
        }
    }
}
