package kr.surprise.memorymap.domain.usecase

import kotlinx.coroutines.flow.Flow
import kr.surprise.memorymap.core.common.Outcome
import kr.surprise.memorymap.core.model.Invite
import kr.surprise.memorymap.core.model.Space
import kr.surprise.memorymap.core.model.SpaceId
import kr.surprise.memorymap.core.model.SpaceKind
import kr.surprise.memorymap.domain.repository.SpaceRepository

class ObserveSpacesUseCase(private val spaces: SpaceRepository) {
    operator fun invoke(): Flow<List<Space>> = spaces.observeSpaces()
}

class RefreshSpacesUseCase(private val spaces: SpaceRepository) {
    suspend operator fun invoke(): Outcome<Unit> = spaces.refresh()
}

/** 이름을 정하는 순간 초대 코드도 함께 나옵니다 — 같이 쓰는 짜국만. */
class CreateSpaceUseCase(private val spaces: SpaceRepository) {
    suspend operator fun invoke(name: String, kind: SpaceKind): Outcome<Pair<Space, Invite?>> {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "공간 이름이 비었습니다" }
        return spaces.create(trimmed, kind)
    }
}

class JoinSpaceUseCase(private val spaces: SpaceRepository) {
    suspend operator fun invoke(code: String): Outcome<Space> =
        spaces.join(code.trim().uppercase())
}

class NewInviteUseCase(private val spaces: SpaceRepository) {
    suspend operator fun invoke(spaceId: SpaceId): Outcome<Invite> = spaces.newInvite(spaceId)
}
