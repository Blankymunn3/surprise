package kr.jjaguk.domain.usecase

import kotlinx.coroutines.flow.Flow
import kr.jjaguk.core.common.Outcome
import kr.jjaguk.core.model.Invite
import kr.jjaguk.core.model.Space
import kr.jjaguk.core.model.SpaceId
import kr.jjaguk.core.model.SpaceKind
import kr.jjaguk.domain.repository.SpaceRepository

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

/** 짜국 이름 바꾸기. 빈 이름은 막습니다 — 목록에서 알아볼 수 없게 됩니다. */
class RenameSpaceUseCase(private val spaces: SpaceRepository) {
    suspend operator fun invoke(spaceId: SpaceId, name: String): Outcome<Unit> {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "공간 이름이 비었습니다" }
        return spaces.rename(spaceId, trimmed)
    }
}
