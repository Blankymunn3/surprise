package kr.jjaguk.feature.map

/**
 * 지도의 **돌릴 수 있는 손잡이들**. 기본값은 검수로 정해진 값이고
 * (픽셀 112칸 — "라이트 96"에서 한 단계 올린 것, 밤은 19~07시),
 * 앱을 다시 내지 않고 고칠 수 있게 조립부가 Remote Config 값으로 덮습니다.
 *
 * 이 모듈은 Firebase 를 모릅니다 — 값이 어디서 오는지는 조립부(`RemoteTuning`)의
 * 일입니다. 지도가 아는 것은 "지금 값이 얼마인가"뿐입니다.
 *
 * `@Volatile`: 덮는 쪽은 RC 응답 스레드, 읽는 쪽은 타일 스레드 풀입니다.
 */
object MapTuning {
    /** 타일(512px)을 몇 칸으로 픽셀화하나. 크면 잘아지고 작으면 뭉툭해집니다. */
    @Volatile var pixelCells: Int = 112

    /** 이 시각(시)부터 밤 지도. */
    @Volatile var nightStartHour: Int = 19

    /** 이 시각(시)부터 낮 지도. */
    @Volatile var nightEndHour: Int = 7
}
