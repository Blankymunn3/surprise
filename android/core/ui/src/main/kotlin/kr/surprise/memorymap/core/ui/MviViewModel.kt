package kr.surprise.memorymap.core.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * MVI 뼈대. 규칙은 `docs/app/MVI.md`.
 *
 * - State 는 **밖에서 못 바꿉니다** (`private set`).
 * - 화면에 남아 있어야 하는 건 State, 한 번 일어나고 사라지는 건 Effect.
 * - Reducer 는 순수 함수라 각 화면 파일에 따로 두고 여기서는 부르기만 합니다.
 */
abstract class MviViewModel<I, S, E>(initial: S) : ViewModel() {

    private val _state = MutableStateFlow(initial)
    val state: StateFlow<S> = _state.asStateFlow()

    // 화면이 잠깐 없을 때(회전 등) Effect 를 흘리지 않도록 버퍼를 둡니다
    private val _effect = Channel<E>(Channel.BUFFERED)
    val effect: Flow<E> = _effect.receiveAsFlow()

    abstract fun onIntent(intent: I)

    protected fun setState(reduce: S.() -> S) {
        _state.update(reduce)
    }

    protected fun currentState(): S = _state.value

    protected fun sendEffect(effect: E) {
        viewModelScope.launch { _effect.send(effect) }
    }
}
