package kr.jjaguk

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kr.jjaguk.core.designsystem.theme.MemoryTheme

class MainActivity : ComponentActivity() {

    /** 답이 무엇이든 앱은 그대로 돕니다 — 알림만 안 뜰 뿐입니다. */
    private val askNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        // **밝은 화면이라고 못을 박습니다.** 인자 없이 부르면 폰의 다크 모드 설정을 따라가서,
        // 폰이 어두우면 상태바 글씨를 흰색으로 바꿉니다 — 우리 화면은 늘 밝은 종이색이라
        // 그러면 글씨가 안 보입니다. themes.xml 의 windowLightStatusBar 도 이게 덮어씁니다.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        val container = (application as JjagukApp).container

        // 새 사진 알림(서버 발송)을 받으려면 13+ 는 물어봐야 합니다.
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // 로그인돼 있으면 "알림은 이 기기로"를 새로 고칩니다. 첫 로그인 직후는
        // 다음 실행에서 잡힙니다 — 토큰 회전은 서비스(onNewToken)가 따로 잡습니다.
        lifecycleScope.launch { container.pushTokens.register() }

        setContent {
            MemoryTheme {
                JjagukNavHost(container = container)
            }
        }
    }
}
