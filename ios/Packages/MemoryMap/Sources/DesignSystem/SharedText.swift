import Foundation

/**
 **여러 모듈이 함께 쓰는 글.**

 iOS 에서 `Bundle.module` 은 **타깃마다 다릅니다** — `FeatureSpace` 에서
 `Text("component_only_on_this_phone", bundle: .module)` 라고 쓰면 DesignSystem 의
 번역 파일이 아니라 FeatureSpace 의 것을 봅니다. 그래서 같은 말을 두 모듈이 쓰면
 각자 복사해 두게 되고, 한쪽만 고쳐져 어긋납니다.

 여기서 **DesignSystem 의 번들에서 읽어 풀린 글자로** 건네줍니다.
 안드로이드에서 `app` 이 `designsystem` 의 `R` 을 가져다 쓰는 것과 같은 뜻입니다.
 */
public enum SharedText {
    /// 사진이 이 기기 안에만 있다는 딱지. 짜국 카드와 짜국 머리말이 같이 씁니다.
    public static var onlyOnThisPhone: String {
        String(localized: "component_only_on_this_phone", bundle: .module)
    }

    /// 짜국 머리말의 뒤로 버튼을 읽어 주는 이름.
    public static var back: String {
        String(localized: "space_back", bundle: .module)
    }

    /// 짜국 머리말의 ⋯ 버튼을 읽어 주는 이름.
    public static var more: String {
        String(localized: "space_more", bundle: .module)
    }

    /// 지도 | 달력 탭. **순서가 뜻을 가집니다** — 0 이 지도입니다.
    public static var spaceTabs: [String] {
        [
            String(localized: "space_tab_map", bundle: .module),
            String(localized: "space_tab_calendar", bundle: .module),
        ]
    }
}
