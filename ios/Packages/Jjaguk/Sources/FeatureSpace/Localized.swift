import Foundation

/**
 이 모듈의 번역 파일에서 글을 읽습니다.

 **모듈마다 하나씩 있어야 합니다.** `Bundle.module` 은 타깃마다 다른 것을 가리켜서,
 다른 모듈의 이 함수를 가져다 쓰면 엉뚱한 번들을 봅니다.

 `Text("키")` 처럼 SwiftUI 에 키를 그대로 넘기지 않고 **여기서 풀어서 글자로** 넘기는
 이유: `PrimaryButton` 같은 부품은 DesignSystem 에 있어서, 키를 넘기면 그쪽 번들에서
 찾다가 못 찾고 키를 그대로 화면에 뿌립니다.

 안드로이드의 `stringResource(id, args…)` 와 같은 자리입니다.
 */
func localized(_ key: String.LocalizationValue, _ args: CVarArg...) -> String {
    let format = String(localized: key, bundle: .module)
    return args.isEmpty ? format : String(format: format, arguments: args)
}
