import Foundation

/// 지도의 **돌릴 수 있는 손잡이들**. 기본값은 검수로 정해진 값이고
/// (타일당 48칸 — "라이트 96", 밤은 19~07시), 앱을 다시 내지 않고 고칠 수 있게
/// 조립부가 Remote Config 값으로 덮습니다.
///
/// 이 모듈은 Firebase 를 모릅니다 — 값이 어디서 오는지는 조립부(`RemoteTuning`)의
/// 일입니다. 지도가 아는 것은 "지금 값이 얼마인가"뿐입니다.
/// 안드로이드 `MapTuning` 과 같은 자리 — 칸수만 다릅니다(타일 규격이 달라서,
/// 256규격 48칸 = 512규격 96칸).
public enum MapTuning {
    /// 타일(256px)을 몇 칸으로 픽셀화하나. 크면 잘아지고 작으면 뭉툭해집니다.
    nonisolated(unsafe) public static var pixelCells: Int = 48

    /// 이 시각(시)부터 밤 지도.
    nonisolated(unsafe) public static var nightStartHour: Int = 19

    /// 이 시각(시)부터 낮 지도.
    nonisolated(unsafe) public static var nightEndHour: Int = 7
}
