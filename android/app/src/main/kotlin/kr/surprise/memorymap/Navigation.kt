package kr.surprise.memorymap

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kr.surprise.memorymap.core.designsystem.R as DesignR
import kr.surprise.memorymap.core.designsystem.component.FloatingIconButton
import kr.surprise.memorymap.core.designsystem.component.MemoryFab
import kr.surprise.memorymap.core.designsystem.component.MemoryIcons
import kr.surprise.memorymap.core.designsystem.component.MemoryToast
import kr.surprise.memorymap.core.designsystem.component.PlainIconButton
import kr.surprise.memorymap.core.designsystem.component.PrimaryButton
import kr.surprise.memorymap.core.designsystem.component.Segmented
import kr.surprise.memorymap.core.designsystem.theme.MemoryColors
import kr.surprise.memorymap.core.designsystem.theme.MemoryShapes
import kr.surprise.memorymap.core.designsystem.theme.MemoryStroke
import kr.surprise.memorymap.core.designsystem.theme.MemoryType
import kr.surprise.memorymap.core.designsystem.theme.PLASTIC_TRIAL
import kr.surprise.memorymap.core.designsystem.theme.PlasticColors
import kr.surprise.memorymap.core.designsystem.theme.PlasticShapes
import kr.surprise.memorymap.core.designsystem.theme.PlasticSize
import kr.surprise.memorymap.core.designsystem.theme.Pretendard
import kr.surprise.memorymap.core.model.Region
import kr.surprise.memorymap.core.model.SpaceId
import kr.surprise.memorymap.core.model.SpaceKind
import kr.surprise.memorymap.feature.calendar.CalendarEffect
import kr.surprise.memorymap.feature.calendar.say
import kr.surprise.memorymap.feature.calendar.CalendarIntent
import kr.surprise.memorymap.feature.calendar.CalendarScreen
import kr.surprise.memorymap.feature.calendar.CalendarViewModel
import kr.surprise.memorymap.feature.map.MapEffect
import kr.surprise.memorymap.feature.map.say
import kr.surprise.memorymap.feature.map.MapIntent
import kr.surprise.memorymap.feature.map.MapScreen
import kr.surprise.memorymap.feature.map.MapViewModel
import kr.surprise.memorymap.feature.space.SpaceListEffect
import kr.surprise.memorymap.feature.space.say
import kr.surprise.memorymap.feature.space.SpaceListIntent
import kr.surprise.memorymap.feature.space.SpaceListScreen
import kr.surprise.memorymap.feature.space.SpaceListViewModel
import kr.surprise.memorymap.feature.space.R as SpaceR
import kr.surprise.memorymap.feature.space.SpaceMenu
import kr.surprise.memorymap.feature.space.SpaceMenuEffect
import kr.surprise.memorymap.feature.space.SpaceMenuIntent
import kr.surprise.memorymap.feature.space.SpaceMenuViewModel
import kr.surprise.memorymap.feature.upload.PickedPhoto
import kr.surprise.memorymap.feature.upload.UploadEffect
import kr.surprise.memorymap.feature.upload.say
import kr.surprise.memorymap.feature.upload.UploadIntent
import kr.surprise.memorymap.feature.upload.UploadSheet
import kr.surprise.memorymap.feature.upload.UploadViewModel
import kotlinx.coroutines.launch

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

            // 스낵바 문구는 **여기서 미리 읽어 둡니다.** LaunchedEffect 안은 Composable 이
            // 아니라 stringResource 를 부를 수 없습니다.
            val clipLabel = stringResource(R.string.invite_clip_label)
            val copiedMessage = stringResource(R.string.invite_copied)
            val shareText = stringResource(R.string.invite_share_text)
            val shareChooser = stringResource(R.string.invite_share_chooser)
            val signInFailed = stringResource(R.string.google_sign_in_failed)

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
                            snackbar.showSnackbar(effect.message.say(context))
                        // 클립보드는 소리 없이 끝나서 "복사했어요" 를 우리가 말해 줍니다.
                        is SpaceListEffect.CopyInvite -> {
                            val board = context.getSystemService(ClipboardManager::class.java)
                            board?.setPrimaryClip(ClipData.newPlainText(clipLabel, effect.code))
                            snackbar.showSnackbar(copiedMessage)
                        }
                        is SpaceListEffect.ShareInvite -> {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText.format(effect.code))
                            }
                            context.startActivity(Intent.createChooser(send, shareChooser))
                        }
                        // 계정 고르기 창은 Activity 가 있어야 떠서 여기서 띄웁니다.
                        // 사용자가 닫으면 아무 인텐트도 보내지 않습니다 — 스스로 그만둔 것을
                        // '실패했어요' 로 알리지 않으려는 것입니다.
                        SpaceListEffect.StartGoogleSignIn -> {
                            val activity = context as? Activity
                            when (val result = activity?.let { container.googleSignIn.idToken(it) }) {
                                is GoogleSignIn.Result.Token ->
                                    vm.onIntent(SpaceListIntent.GoogleTokenReceived(result.value))
                                is GoogleSignIn.Result.Failed ->
                                    snackbar.showSnackbar(signInFailed)
                                GoogleSignIn.Result.Cancelled, null -> Unit
                            }
                        }
                    }
                }
            }

            Box(Modifier.fillMaxSize()) {
                SpaceListScreen(state = state, onIntent = vm::onIntent)
                MemoryToast(
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
    var menuOpen by remember { mutableStateOf(false) }
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

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 문구는 **미리 읽어 둡니다** — LaunchedEffect 안은 Composable 이 아니라
    // 거기서는 stringResource 를 부를 수 없습니다.
    val locationOff = stringResource(R.string.my_location_off)
    val locationNotFound = stringResource(R.string.my_location_not_found)
    val locationDenied = stringResource(R.string.my_location_denied)

    /**
     * 자리를 찾아 지도에 넘깁니다. 못 찾으면 **왜 못 찾았는지**를 말합니다 —
     * 눌렀는데 아무 일이 없으면 고장 난 것으로 보입니다.
     *
     * 권한이 없을 때는 여기서 알리지 않습니다. 부르는 쪽이 곧바로 권한 창을 띄우니까요.
     */
    suspend fun goToMyLocation() {
        when (val here = findMyLocation(context)) {
            is MyLocation.Found -> mapVm.onIntent(MapIntent.MyLocationFound(here.latitude, here.longitude))
            MyLocation.NoPermission -> Unit
            MyLocation.Off -> snackbar.showSnackbar(locationOff)
            MyLocation.NotFound -> snackbar.showSnackbar(locationNotFound)
        }
    }

    // 권한을 물은 결과. **허락받은 그 자리에서 바로** 다시 찾습니다 —
    // 허락하고 나서 버튼을 또 누르게 하면 두 번 일 시키는 것입니다.
    val askLocation = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        scope.launch {
            if (granted) goToMyLocation() else snackbar.showSnackbar(locationDenied)
        }
    }

    LaunchedEffect(mapVm) {
        mapVm.effect.collect { effect ->
            when (effect) {
                is MapEffect.OpenUpload -> {
                    uploadRegion = effect.region
                    uploading = true
                }
                is MapEffect.ShowMessage -> snackbar.showSnackbar(effect.message.say(context))

                // 이미 허락받았으면 창을 다시 띄우지 않고 바로 찾습니다.
                MapEffect.AskMyLocation ->
                    if (hasLocationPermission(context)) {
                        goToMyLocation()
                    } else {
                        askLocation.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    }
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
                is CalendarEffect.ShowMessage -> snackbar.showSnackbar(effect.message.say(context))
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(if (PLASTIC_TRIAL) PlasticColors.Body else MemoryColors.Paper)
    ) {
        // 머리말과 탭은 **지도 위에 떠 있지 않고 자리를 차지합니다.** 지도가 그 아래에서
        // 시작하니, 러시아처럼 위로 긴 나라가 검색칸 뒤로 숨을 자리 자체가 없습니다.
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            TopBar(
                spaceName = spaceName,
                onlyOnThisPhone = kind == SpaceKind.Personal,
                onBack = onBack,
                onMore = { menuOpen = true },
            )
            // 패미컴 스타일에서는 탭도 몸통 위의 고무 스위치입니다.
            if (PLASTIC_TRIAL) {
                PlasticTabs(selectedIndex = tab, onSelect = { tab = it })
            } else {
                Segmented(
                    options = stringArrayResource(R.array.space_tabs).toList(),
                    selectedIndex = tab,
                    onSelect = { tab = it },
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 10.dp),
                )
            }

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

        // 패미컴 스타일에서는 ＋ 가 떠 있지 않고 **달력 안쪽 조작부**에 앉습니다
        // (`CalendarPlastic` 의 빨간 A 버튼). 몸통 위에 버튼이 다 모여 있는데
        // 하나만 화면 위에 떠 있으면 어긋납니다.
        if (tab == 1 && !PLASTIC_TRIAL) {
            MemoryFab(
                onClick = { uploading = true },
                contentDescription = stringResource(R.string.space_add_photo),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .systemBarsPadding()
                    .padding(end = 14.dp, bottom = 18.dp),
            )
        }

        MemoryToast(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 110.dp))

        if (menuOpen) {
            val menuVm: SpaceMenuViewModel =
                viewModel(key = "menu-${spaceId.value}", factory = container.spaceMenuFactory(spaceId))
            val menuState by menuVm.state.collectAsStateWithLifecycle()

            val renameFailed = stringResource(SpaceR.string.msg_rename_failed)
            // 목록 화면에도 같은 이름이 있지만 **그건 다른 함수 안**입니다.
            // 여기서 쓰려면 여기서 읽어야 합니다.
            val clipLabel = stringResource(R.string.invite_clip_label)
            val copiedMessage = stringResource(R.string.invite_copied)
            LaunchedEffect(menuVm) {
                menuVm.onIntent(SpaceMenuIntent.Appeared)
                menuVm.effect.collect { effect ->
                    when (effect) {
                        SpaceMenuEffect.Close -> menuOpen = false
                        SpaceMenuEffect.RenameFailed -> snackbar.showSnackbar(renameFailed)
                        is SpaceMenuEffect.CopyCode -> {
                            val board = context.getSystemService(ClipboardManager::class.java)
                            board?.setPrimaryClip(ClipData.newPlainText(clipLabel, effect.code))
                            snackbar.showSnackbar(copiedMessage)
                        }
                    }
                }
            }

            SpaceMenu(state = menuState, onIntent = menuVm::onIntent, modifier = Modifier.systemBarsPadding())
            BackHandler { menuVm.onIntent(SpaceMenuIntent.Dismissed) }
        }
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

        // 지역 시트에서 열었으면 그 지역을 **고른 사진 전부에** 미리 넣습니다.
        // 사진마다 지역을 들게 되면서 한 번에 하나씩만 넣을 수 있게 됐습니다.
        LaunchedEffect(uploadVm, uploadRegion, uploadState.items.size) {
            val region = uploadRegion ?: return@LaunchedEffect
            uploadState.items.forEach { item ->
                uploadVm.onIntent(UploadIntent.RegionFieldTapped(item.uri))
                uploadVm.onIntent(UploadIntent.RegionChosen(region))
            }
        }

        // 날짜 고르기. 어느 사진의 날짜인지 들고 있다가 고르면 그 사진에만 넣습니다.
        var pickingDateOf by remember { mutableStateOf<Pair<String, LocalDate>?>(null) }
        LaunchedEffect(uploadVm) {
            uploadVm.effect.collect { effect ->
                when (effect) {
                    UploadEffect.Close -> uploading = false
                    is UploadEffect.ShowMessage -> snackbar.showSnackbar(effect.message.say(context))
                    is UploadEffect.OpenDatePicker -> pickingDateOf = effect.uri to effect.current
                }
            }
        }

        // 아래에서 올라오는 시트입니다. 다른 시트들처럼 **화면을 덮지 않는 판**으로
        // 뜹니다 — 사진 목록은 판 안에서 구릅니다.
        ModalBottomSheet(
            onDismissRequest = { uploading = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = if (PLASTIC_TRIAL) PlasticColors.Body else MemoryColors.Surface,
            shape = if (PLASTIC_TRIAL) PlasticShapes.Device else MemoryShapes.Sheet,
            dragHandle = { UploadGrip() },
        ) {
            UploadSheet(
                state = uploadState,
                onIntent = uploadVm::onIntent,
                onPickPhotos = {
                    pickPhotos.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                // **높이를 정해 주지 않습니다.** 내용만큼만 차지하고, 사진이 많아지면
                // 안쪽 목록이 대신 구릅니다 (`UploadPlastic` 의 `heightIn`).
                // 사진 한 장을 올릴 때 시트가 화면 반을 먹을 까닭이 없습니다.
                modifier = Modifier.fillMaxWidth(),
            )
        }

        pickingDateOf?.let { (uri, current) ->
            DateSheet(
                current = current,
                onDismiss = { pickingDateOf = null },
                onPick = { date ->
                    uploadVm.onIntent(UploadIntent.DateChosen(uri, date))
                    pickingDateOf = null
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
 * 올리기 시트의 손잡이. 시트 넷과 같은 규칙입니다 —
 * 패미컴 스타일에서는 몸통에 새긴 회색 홈, 기준 디자인에서는 2px 잉크 선.
 */
@Composable
private fun UploadGrip() {
    if (PLASTIC_TRIAL) {
        Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .width(PlasticSize.Grip)
                    .height(PlasticSize.Stripe)
                    .clip(PlasticShapes.Pill)
                    .background(PlasticColors.Trim)
            )
        }
        return
    }

    Box(Modifier.fillMaxWidth().height(MemoryStroke.Divider).background(MemoryColors.Ink))
}

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
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlainIconButton(MemoryIcons.Back, stringResource(R.string.space_back), onBack)

        Text(
            text = spaceName,
            style = MemoryType.Title,
            // 몸통 위의 글자는 잉크가 아니라 플라스틱에 새긴 검정입니다.
            color = if (PLASTIC_TRIAL) PlasticColors.Ink else MemoryColors.Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
        )

        if (onlyOnThisPhone) {
            // 이 딱지는 몸통 위에서 **파인 자리**로 그립니다. 흰 면에 잉크 선은
            // 플라스틱 위에서 종이를 붙인 것처럼 떠 보입니다.
            if (PLASTIC_TRIAL) {
                Text(
                    text = stringResource(DesignR.string.component_only_on_this_phone),
                    fontFamily = Pretendard,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = PlasticColors.OnPlateDim,
                    modifier = Modifier
                        .clip(PlasticShapes.Chip)
                        .background(PlasticColors.Plate)
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                )
            } else {
                Text(
                    text = stringResource(DesignR.string.component_only_on_this_phone),
                    style = MemoryType.Micro,
                    color = MemoryColors.Ink,
                    modifier = Modifier
                        .background(MemoryColors.Surface)
                        .border(MemoryStroke.Border, MemoryColors.Line)
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                )
            }
        }

        PlainIconButton(MemoryIcons.More, stringResource(R.string.space_more), onMore)
    }
}

/**
 * 탭 두 칸 — 몸통 위의 고무 스위치.
 *
 * 고른 쪽만 빨갛습니다. 컨트롤러에서 빨강은 "지금 누른 것" 이고,
 * 여기서 지금 누른 것은 보고 있는 탭입니다.
 */
@Composable
private fun PlasticTabs(selectedIndex: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 8.dp)
            .clip(PlasticShapes.Housing)
            .background(PlasticColors.Rubber)
            .padding(3.dp),
    ) {
        listOf("지도", "달력").forEachIndexed { index, label ->
            val chosen = index == selectedIndex
            Box(
                Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(PlasticShapes.Knob)
                    .background(if (chosen) PlasticColors.Red else Color.Transparent)
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontFamily = Pretendard,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (chosen) PlasticColors.OnRed else PlasticColors.OnRubber,
                )
            }
        }
    }
}

/**
 * 날짜 고르기. 안드로이드 기본 달력을 그대로 씁니다 —
 * 우리가 다시 그리면 시스템 달력과 미묘하게 달라서 오히려 낯섭니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateSheet(current: LocalDate, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    val zone = ZoneId.systemDefault()
    val picker = rememberDatePickerState(
        initialSelectedDateMillis = current.atStartOfDay(zone).toInstant().toEpochMilli()
    )

    // 높이는 주지 않습니다 — 달력이 필요한 만큼만 차지합니다. 시트 넷과 같은 규칙입니다.
    // 그릇(몸통 색·기기 모서리)은 맞추되 **달력 자체는 안드로이드 기본 그대로** 둡니다:
    // 우리가 다시 그리면 시스템 달력과 미묘하게 달라서 오히려 낯섭니다.
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (PLASTIC_TRIAL) PlasticColors.Body else MemoryColors.Surface,
        shape = if (PLASTIC_TRIAL) PlasticShapes.Device else MemoryShapes.Sheet,
        dragHandle = null,
    ) {
        DatePicker(state = picker)
        PrimaryButton(
            text = stringResource(R.string.date_picker_confirm),
            onClick = {
                val millis = picker.selectedDateMillis ?: return@PrimaryButton onDismiss()
                onPick(Instant.ofEpochMilli(millis).atZone(zone).toLocalDate())
            },
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
        )
    }
}
