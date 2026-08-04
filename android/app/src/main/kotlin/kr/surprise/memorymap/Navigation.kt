package kr.surprise.memorymap

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kr.surprise.memorymap.core.designsystem.component.FloatingIconButton
import kr.surprise.memorymap.core.designsystem.component.MemoryFab
import kr.surprise.memorymap.core.designsystem.component.MemoryIcons
import kr.surprise.memorymap.core.designsystem.component.PlainIconButton
import kr.surprise.memorymap.core.designsystem.component.Segmented
import kr.surprise.memorymap.core.designsystem.theme.MemoryColors
import kr.surprise.memorymap.core.designsystem.theme.MemoryStroke
import kr.surprise.memorymap.core.designsystem.theme.MemoryType
import kr.surprise.memorymap.core.model.Region
import kr.surprise.memorymap.core.model.SpaceId
import kr.surprise.memorymap.core.model.SpaceKind
import kr.surprise.memorymap.feature.calendar.CalendarEffect
import kr.surprise.memorymap.feature.calendar.CalendarIntent
import kr.surprise.memorymap.feature.calendar.CalendarScreen
import kr.surprise.memorymap.feature.calendar.CalendarViewModel
import kr.surprise.memorymap.feature.map.MapEffect
import kr.surprise.memorymap.feature.map.MapIntent
import kr.surprise.memorymap.feature.map.MapScreen
import kr.surprise.memorymap.feature.map.MapViewModel
import kr.surprise.memorymap.feature.space.SpaceListEffect
import kr.surprise.memorymap.feature.space.SpaceListIntent
import kr.surprise.memorymap.feature.space.SpaceListScreen
import kr.surprise.memorymap.feature.space.SpaceListViewModel
import kr.surprise.memorymap.feature.upload.PickedPhoto
import kr.surprise.memorymap.feature.upload.UploadIntent
import kr.surprise.memorymap.feature.upload.UploadSheet
import kr.surprise.memorymap.feature.upload.UploadViewModel

private const val ROUTE_SPACES = "spaces"

/**
 * 짜국의 **종류**도 경로에 넣습니다. 들어간 화면이 기기 안 사진을 볼지 서버 사진을 볼지
 * 정해야 하는데, 경로에 있으면 앱이 죽었다 살아나도 그대로 살아납니다.
 */
private const val ROUTE_SPACE = "space/{spaceId}/{kind}?name={name}"

/**
 * 화면이 오갈 때의 움직임.
 *
 * 들어갈 때는 오른쪽에서 밀려 들어오고, 나갈 때는 그 반대로 나갑니다.
 * 뒤에 남는 화면은 **조금만**(1/6) 따라 움직입니다 — 같은 거리로 밀면 두 장이 붙어
 * 움직이는 것처럼 보여서 어느 쪽이 위인지 알 수 없습니다.
 */
private val SLIDE = tween<IntOffset>(300, easing = FastOutSlowInEasing)
private val FADE = tween<Float>(200, easing = LinearEasing)
private const val PARALLAX = 6

@Composable
fun MemoryMapNavHost(container: AppContainer) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = ROUTE_SPACES,
        enterTransition = { slideInHorizontally(SLIDE) { it } + fadeIn(FADE) },
        exitTransition = { slideOutHorizontally(SLIDE) { -it / PARALLAX } + fadeOut(FADE) },
        popEnterTransition = { slideInHorizontally(SLIDE) { -it / PARALLAX } + fadeIn(FADE) },
        popExitTransition = { slideOutHorizontally(SLIDE) { it } + fadeOut(FADE) },
    ) {
        composable(ROUTE_SPACES) {
            val vm: SpaceListViewModel = viewModel(factory = container.spaceListFactory())
            val state by vm.state.collectAsStateWithLifecycle()
            val snackbar = remember { SnackbarHostState() }
            val context = LocalContext.current

            LaunchedEffect(Unit) {
                // 저장해 둔 로그인을 먼저 읽습니다. 안 읽으면 켤 때마다 로그아웃으로 보입니다.
                container.accounts.restore()
                vm.onIntent(SpaceListIntent.Appeared)
            }

            // 실패를 조용히 삼키면 사용자에게는 "눌러도 아무 일이 없다" 로 보입니다.
            // Effect 를 하나도 빠뜨리지 않도록 when 으로 받습니다.
            LaunchedEffect(vm) {
                vm.effect.collect { effect ->
                    when (effect) {
                        is SpaceListEffect.OpenSpace ->
                            // 이름은 사용자가 지은 것이라 `/` 나 `?` 가 들어 있을 수 있습니다.
                            // 그대로 붙이면 경로가 갈라져 화면을 못 찾습니다.
                            navController.navigate(
                                "space/${effect.id.value}/${effect.kind.name}" +
                                    "?name=${Uri.encode(effect.name)}"
                            )
                        is SpaceListEffect.ShowMessage ->
                            snackbar.showSnackbar(effect.text)
                        is SpaceListEffect.ShareInvite ->
                            snackbar.showSnackbar("초대 코드: ${effect.code}")
                        // 계정 고르기 창은 Activity 가 있어야 떠서 여기서 띄웁니다.
                        // 사용자가 닫으면 아무 인텐트도 보내지 않습니다 — 스스로 그만둔 것을
                        // '실패했어요' 로 알리지 않으려는 것입니다.
                        SpaceListEffect.StartGoogleSignIn -> {
                            val activity = context as? Activity
                            when (val result = activity?.let { container.googleSignIn.idToken(it) }) {
                                is GoogleSignIn.Result.Token ->
                                    vm.onIntent(SpaceListIntent.GoogleTokenReceived(result.value))
                                is GoogleSignIn.Result.Failed ->
                                    snackbar.showSnackbar("구글 계정을 가져오지 못했어요.")
                                GoogleSignIn.Result.Cancelled, null -> Unit
                            }
                        }
                    }
                }
            }

            Box(Modifier.fillMaxSize()) {
                SpaceListScreen(state = state, onIntent = vm::onIntent)
                SnackbarHost(
                    snackbar,
                    Modifier.align(Alignment.BottomCenter).systemBarsPadding().padding(bottom = 16.dp),
                )
            }
        }

        composable(
            route = ROUTE_SPACE,
            arguments = listOf(
                navArgument("spaceId") { type = NavType.StringType },
                navArgument("kind") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType; defaultValue = "" },
            ),
        ) { entry ->
            val spaceId = SpaceId(entry.arguments?.getString("spaceId").orEmpty())
            // 못 읽으면 서버 쪽으로 봅니다 — 지금까지의 짜국이 전부 그쪽입니다.
            val kind = runCatching { SpaceKind.valueOf(entry.arguments?.getString("kind").orEmpty()) }
                .getOrDefault(SpaceKind.Shared)
            SpaceTabs(
                container = container,
                spaceId = spaceId,
                kind = kind,
                spaceName = entry.arguments?.getString("name").orEmpty(),
                onBack = { navController.popBackStack() },
            )
        }
    }
}

/**
 * 공간 안의 화면. **지도 | 달력** 두 탭은 같은 사진을 '어디' 와 '언제' 로 보는 것뿐이라
 * 탭을 옮겨도 사진을 다시 받지 않습니다 — 같은 흐름을 두 Store 가 구독합니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpaceTabs(
    container: AppContainer,
    spaceId: SpaceId,
    kind: SpaceKind,
    spaceName: String,
    onBack: () -> Unit,
) {
    var tab by remember { mutableStateOf(0) }
    var uploading by remember { mutableStateOf(false) }
    // 지역 시트에서 열었으면 그 지역을 들고 갑니다. 이미 고른 곳을 아는데 올리기 화면에서
    // 다시 고르게 하면 안 됩니다. 아래쪽 ＋ 로 열었으면 null 이고, 그때는 사진의 EXIF 가 정합니다.
    var uploadRegion by remember { mutableStateOf<Region?>(null) }
    val snackbar = remember { SnackbarHostState() }

    val mapVm: MapViewModel =
        viewModel(key = "map-${spaceId.value}", factory = container.mapFactory(spaceId, kind))
    val calendarVm: CalendarViewModel =
        viewModel(key = "cal-${spaceId.value}", factory = container.calendarFactory(spaceId, kind))

    val mapState by mapVm.state.collectAsStateWithLifecycle()
    val calendarState by calendarVm.state.collectAsStateWithLifecycle()

    LaunchedEffect(spaceId) { container.refreshPhotos(kind, spaceId) }

    LaunchedEffect(mapVm) {
        mapVm.effect.collect { effect ->
            when (effect) {
                is MapEffect.OpenUpload -> {
                    uploadRegion = effect.region
                    uploading = true
                }
                is MapEffect.ShowMessage -> snackbar.showSnackbar(effect.text)
                MapEffect.AskMyLocation -> snackbar.showSnackbar("내 위치는 다음 단계에서 붙일게요.")
            }
        }
    }
    LaunchedEffect(calendarVm) {
        calendarVm.effect.collect { effect ->
            when (effect) {
                // 달력에서 열 때는 지역을 알 수 없습니다 — 날짜만 아는 자리라서요.
                CalendarEffect.OpenUpload -> {
                    uploadRegion = null
                    uploading = true
                }
                is CalendarEffect.ShowMessage -> snackbar.showSnackbar(effect.text)
            }
        }
    }

    Box(Modifier.fillMaxSize().background(MemoryColors.Paper)) {
        // 머리말과 탭은 **지도 위에 떠 있지 않고 자리를 차지합니다.** 지도가 그 아래에서
        // 시작하니, 러시아처럼 위로 긴 나라가 검색칸 뒤로 숨을 자리 자체가 없습니다.
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            TopBar(
                spaceName = spaceName,
                onlyOnThisPhone = kind == SpaceKind.Personal,
                onBack = onBack,
            )
            Segmented(
                options = listOf("지도", "달력"),
                selectedIndex = tab,
                onSelect = { tab = it },
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 10.dp),
            )

            // 탭도 옆으로 밀립니다. 누른 쪽으로 미끄러져야 어느 쪽으로 옮겼는지 보입니다.
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    val toRight = targetState > initialState
                    val dir = if (toRight) 1 else -1
                    (slideInHorizontally(SLIDE) { dir * it } + fadeIn(FADE)) togetherWith
                        (slideOutHorizontally(SLIDE) { -dir * it } + fadeOut(FADE))
                },
                label = "탭",
                modifier = Modifier.weight(1f),
            ) { current ->
                if (current == 0) {
                    MapScreen(state = mapState, onIntent = mapVm::onIntent)
                } else {
                    CalendarScreen(state = calendarState, onIntent = calendarVm::onIntent)
                }
            }
        }

        if (tab == 1) {
            MemoryFab(
                onClick = { uploading = true },
                contentDescription = "사진 올리기",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .systemBarsPadding()
                    .padding(end = 14.dp, bottom = 18.dp),
            )
        }

        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 110.dp))
    }

    if (uploading) {
        val uploadVm: UploadViewModel =
            viewModel(key = "up-${spaceId.value}", factory = container.uploadFactory(spaceId, kind))
        val uploadState by uploadVm.state.collectAsStateWithLifecycle()

        /**
         * 사진 고르기. **안드로이드 사진 선택기**라 저장소 권한을 안 물어봅니다 —
         * 사용자가 고른 사진만 넘어오고, 앨범 전체를 열어 주지 않습니다.
         */
        val pickPhotos = rememberLauncherForActivityResult(
            ActivityResultContracts.PickMultipleVisualMedia(MAX_PICK)
        ) { uris ->
            // 아무것도 안 고르고 닫으면 빈 목록이 옵니다. 그건 취소지 실패가 아닙니다.
            if (uris.isNotEmpty()) {
                uploadVm.onIntent(UploadIntent.PhotosPicked(uris.map { PickedPhoto(it.toString()) }))
            }
        }

        // 지역 시트에서 열었으면 그 지역을 미리 넣어 둡니다. 시트가 뜰 때 한 번만 합니다.
        LaunchedEffect(uploadVm, uploadRegion) {
            uploadRegion?.let { uploadVm.onIntent(UploadIntent.RegionChosen(it)) }
        }

        ModalBottomSheet(
            onDismissRequest = { uploading = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MemoryColors.Surface,
            dragHandle = null,
        ) {
            UploadSheet(
                state = uploadState,
                onIntent = { intent ->
                    if (intent is UploadIntent.Dismissed) uploading = false
                    uploadVm.onIntent(intent)
                },
                onPickPhotos = {
                    pickPhotos.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
            )
        }
    }
}

/**
 * 한 번에 고를 수 있는 사진 수. 선택기 자체가 요구하는 값이라 정해 둡니다 —
 * 너무 크게 두면 줄이고 올리는 데 한참 걸려 멈춘 것처럼 보입니다.
 */
private const val MAX_PICK = 20

/**
 * 짜국 안쪽의 머리말 — 뒤로 · 이름 · ⋯.
 *
 * 이름이 **가운데가 아니라 왼쪽**입니다. 가운데에 두면 이름 길이에 따라 자리가
 * 매번 달라지는데, 왼쪽에 붙이면 늘 같은 곳에서 시작합니다.
 */
@Composable
private fun TopBar(
    spaceName: String,
    onlyOnThisPhone: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlainIconButton(MemoryIcons.Back, "뒤로", onBack)

        Text(
            text = spaceName,
            style = MemoryType.Title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
        )

        if (onlyOnThisPhone) {
            Text(
                text = "이 폰에만",
                style = MemoryType.Micro,
                color = MemoryColors.Ink,
                modifier = Modifier
                    .background(MemoryColors.Surface)
                    .border(MemoryStroke.Border, MemoryColors.Line)
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            )
        }

        PlainIconButton(MemoryIcons.More, "더 보기", { })
    }
}
