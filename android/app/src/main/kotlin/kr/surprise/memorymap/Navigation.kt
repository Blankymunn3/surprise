package kr.surprise.memorymap

import androidx.compose.foundation.background
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kr.surprise.memorymap.core.designsystem.component.GlassIconButton
import kr.surprise.memorymap.core.designsystem.component.MemoryFab
import kr.surprise.memorymap.core.designsystem.component.MemoryIcons
import kr.surprise.memorymap.core.designsystem.component.PlainIconButton
import kr.surprise.memorymap.core.designsystem.component.Segmented
import kr.surprise.memorymap.core.designsystem.theme.MemoryColors
import kr.surprise.memorymap.core.model.SpaceId
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
import kr.surprise.memorymap.feature.upload.UploadIntent
import kr.surprise.memorymap.feature.upload.UploadSheet
import kr.surprise.memorymap.feature.upload.UploadViewModel

private const val ROUTE_SPACES = "spaces"
private const val ROUTE_SPACE = "space/{spaceId}"

@Composable
fun MemoryMapNavHost(container: AppContainer) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = ROUTE_SPACES) {
        composable(ROUTE_SPACES) {
            val vm: SpaceListViewModel = viewModel(factory = container.spaceListFactory())
            val state by vm.state.collectAsStateWithLifecycle()
            val snackbar = remember { SnackbarHostState() }

            LaunchedEffect(Unit) { vm.onIntent(SpaceListIntent.Appeared) }

            // 실패를 조용히 삼키면 사용자에게는 "눌러도 아무 일이 없다" 로 보입니다.
            // Effect 를 하나도 빠뜨리지 않도록 when 으로 받습니다.
            LaunchedEffect(vm) {
                vm.effect.collect { effect ->
                    when (effect) {
                        is SpaceListEffect.OpenSpace ->
                            navController.navigate("space/${effect.id.value}")
                        is SpaceListEffect.ShowMessage ->
                            snackbar.showSnackbar(effect.text)
                        is SpaceListEffect.ShareInvite ->
                            snackbar.showSnackbar("초대 코드: ${effect.code}")
                    }
                }
            }

            Box(Modifier.fillMaxSize()) {
                SpaceListScreen(state = state, myInitial = "나", onIntent = vm::onIntent)
                SnackbarHost(
                    snackbar,
                    Modifier.align(Alignment.BottomCenter).systemBarsPadding().padding(bottom = 16.dp),
                )
            }
        }

        composable(
            route = ROUTE_SPACE,
            arguments = listOf(navArgument("spaceId") { type = NavType.StringType }),
        ) { entry ->
            val spaceId = SpaceId(entry.arguments?.getString("spaceId").orEmpty())
            SpaceTabs(container = container, spaceId = spaceId, onBack = { navController.popBackStack() })
        }
    }
}

/**
 * 공간 안의 화면. **지도 | 달력** 두 탭은 같은 사진을 '어디' 와 '언제' 로 보는 것뿐이라
 * 탭을 옮겨도 사진을 다시 받지 않습니다 — 같은 흐름을 두 Store 가 구독합니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpaceTabs(container: AppContainer, spaceId: SpaceId, onBack: () -> Unit) {
    var tab by remember { mutableStateOf(0) }
    var uploading by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    val mapVm: MapViewModel = viewModel(key = "map-${spaceId.value}", factory = container.mapFactory(spaceId))
    val calendarVm: CalendarViewModel =
        viewModel(key = "cal-${spaceId.value}", factory = container.calendarFactory(spaceId))

    val mapState by mapVm.state.collectAsStateWithLifecycle()
    val calendarState by calendarVm.state.collectAsStateWithLifecycle()

    LaunchedEffect(spaceId) { container.refreshPhotos(spaceId) }

    LaunchedEffect(mapVm) {
        mapVm.effect.collect { effect ->
            when (effect) {
                MapEffect.OpenUpload -> uploading = true
                is MapEffect.ShowMessage -> snackbar.showSnackbar(effect.text)
                MapEffect.AskMyLocation -> snackbar.showSnackbar("내 위치는 다음 단계에서 붙일게요.")
            }
        }
    }
    LaunchedEffect(calendarVm) {
        calendarVm.effect.collect { effect ->
            when (effect) {
                CalendarEffect.OpenUpload -> uploading = true
                is CalendarEffect.ShowMessage -> snackbar.showSnackbar(effect.text)
            }
        }
    }

    Box(Modifier.fillMaxSize().background(MemoryColors.Paper)) {
        if (tab == 0) {
            MapScreen(state = mapState, onIntent = mapVm::onIntent, topBarHeight = 96.dp)
        } else {
            Column(Modifier.fillMaxSize().systemBarsPadding()) {
                Box(Modifier.fillMaxWidth().padding(top = 44.dp))
                CalendarScreen(state = calendarState, onIntent = calendarVm::onIntent)
            }
        }

        // 지도 위에서는 유리, 달력(종이 위)에서는 그냥 놓입니다 — 같은 부품, 다른 층
        TopBar(
            floating = tab == 0,
            selected = tab,
            onSelect = { tab = it },
            onBack = onBack,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        if (tab == 1) {
            MemoryFab(
                onClick = { uploading = true },
                contentDescription = "사진 올리기",
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 40.dp),
            )
        }

        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 110.dp))
    }

    if (uploading) {
        val uploadVm: UploadViewModel =
            viewModel(key = "up-${spaceId.value}", factory = container.uploadFactory(spaceId))
        val uploadState by uploadVm.state.collectAsStateWithLifecycle()

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
                onPickPhotos = { },
            )
        }
    }
}

@Composable
private fun TopBar(
    floating: Boolean,
    selected: Int,
    onSelect: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .systemBarsPadding()
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (floating) {
            GlassIconButton(MemoryIcons.Back, "뒤로", onBack)
        } else {
            PlainIconButton(MemoryIcons.Back, "뒤로", onBack)
        }

        Segmented(
            options = listOf("지도", "달력"),
            selectedIndex = selected,
            onSelect = onSelect,
            floating = floating,
        )

        if (floating) {
            GlassIconButton(MemoryIcons.More, "더 보기", { })
        } else {
            PlainIconButton(MemoryIcons.More, "더 보기", { })
        }
    }
}
