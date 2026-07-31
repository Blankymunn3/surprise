package kr.surprise.memorymap

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kr.surprise.memorymap.core.designsystem.theme.MemoryTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // **밝은 화면이라고 못을 박습니다.** 인자 없이 부르면 폰의 다크 모드 설정을 따라가서,
        // 폰이 어두우면 상태바 글씨를 흰색으로 바꿉니다 — 우리 화면은 늘 밝은 종이색이라
        // 그러면 글씨가 안 보입니다. themes.xml 의 windowLightStatusBar 도 이게 덮어씁니다.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        val container = (application as MemoryMapApp).container

        setContent {
            MemoryTheme {
                MemoryMapNavHost(container = container)
            }
        }
    }
}
