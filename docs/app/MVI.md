# MVI 규칙

화면 상태는 **한 곳에서만** 바뀝니다. 이 규칙을 지키면 "왜 이렇게 보이지?" 를
State 하나만 보고 답할 수 있습니다.

## 네 가지 조각

| 이름 | 무엇인가 | 규칙 |
|---|---|---|
| **State** | 화면을 그리는 데 필요한 전부 | 불변. `data class` / `struct` |
| **Intent** | 사용자가 한 일 | 과거형·사실만. `RegionTapped`, `PhotoPicked` |
| **Effect** | 딱 한 번 일어나는 일 | 안내 문구, 사진 선택창 열기, 지도 이동 |
| **Reducer** | (State, 결과) → State | **순수 함수.** 네트워크·시간·난수 금지 |

## 이름 짓는 법

Intent 는 **사용자가 한 일**이지 개발자가 시키는 일이 아닙니다.

```kotlin
// 좋음 — 무슨 일이 있었는지
data class RegionTapped(val code: RegionCode) : MapIntent
data object MyLocationTapped : MapIntent

// 나쁨 — 무엇을 하라는 명령 (그건 Reducer 가 정할 일)
data class ZoomToRegion(val code: RegionCode) : MapIntent
data object ShowLoading : MapIntent
```

## 로딩·실패를 State 로 표현하기

`isLoading`, `error` 같은 필드를 따로 두지 않습니다. 서로 모순되는 상태
(로딩 중인데 에러도 있음)를 만들 수 있기 때문입니다.

```kotlin
sealed interface RegionsUi {
    data object Loading : RegionsUi
    data class Ready(val regions: List<Region>) : RegionsUi
    data class Failed(val reason: FailureReason) : RegionsUi
}

data class MapState(
    val regions: RegionsUi = RegionsUi.Loading,
    val selected: Region? = null,
    val upload: UploadUi = UploadUi.Idle,
)
```

## 안드로이드 뼈대

```kotlin
abstract class MviViewModel<I, S, E>(initial: S) : ViewModel() {
    private val _state = MutableStateFlow(initial)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _effect = Channel<E>(Channel.BUFFERED)
    val effect: Flow<E> = _effect.receiveAsFlow()

    abstract fun onIntent(intent: I)

    protected fun setState(reduce: S.() -> S) { _state.update(reduce) }
    protected fun sendEffect(effect: E) { viewModelScope.launch { _effect.send(effect) } }
}
```

Composable 은 **State 를 받고 Intent 를 올려보내기만** 합니다. ViewModel 을 직접 받지 않습니다.
그래야 프리뷰와 테스트가 됩니다.

```kotlin
@Composable
fun MapScreen(state: MapState, onIntent: (MapIntent) -> Unit) { … }
```

## iOS 뼈대

```swift
@Observable
final class MapStore {
    private(set) var state: MapState
    private let uploadPhoto: UploadPhotoUseCase

    func send(_ intent: MapIntent) async { … }   // 여기서만 state 를 바꿉니다
}
```

`View` 는 `store.state` 를 읽고 `store.send(...)` 만 호출합니다.

## 반드시 지킬 것

1. **State 는 밖에서 못 바꿉니다.** `private set` / `private(set)`.
2. **Reducer 는 순수하게.** `Date()`, `Random`, 네트워크 호출 금지 — 필요하면 인자로 받습니다.
3. **Effect 로 상태를 대신하지 않습니다.** 화면에 남아 있어야 하는 건 State 입니다.
   안내 문구는 잠깐 뜨고 사라지므로 Effect, 선택된 지역은 남아 있으므로 State.
4. **화면 하나에 Store 하나.** 지도 화면의 검색창이 따로 Store 를 갖지 않습니다.

## 테스트

Reducer 는 순수 함수라 테스트가 제일 쉽습니다. **여기를 제일 먼저 테스트합니다.**

```kotlin
@Test
fun `사진 업로드가 실패하면 기기 저장으로 넘어가고 그 사실을 알린다`() {
    val before = MapState(selected = 중구, upload = UploadUi.Uploading)
    val after = reduce(before, UploadResult.Failed(Timeout))

    assertEquals(UploadUi.SavedLocally, after.upload)   // 사진을 잃지 않았다
}
```

UseCase 는 가짜 Repository 로 테스트합니다. ViewModel 테스트는 Turbine 으로
State 흐름을 확인합니다. **Compose UI 테스트는 꼭 필요한 것만** — 느리고 잘 깨집니다.
