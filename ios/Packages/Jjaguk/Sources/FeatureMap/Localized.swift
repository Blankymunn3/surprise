import Foundation

/**
 이 모듈의 번역 파일에서 글을 읽습니다.

 **모듈마다 하나씩 있어야 합니다** — `Bundle.module` 은 타깃마다 다른 것을 가리켜서,
 다른 모듈의 이 함수를 가져다 쓰면 엉뚱한 번들을 봅니다.

 안드로이드의 `stringResource(id, args…)` 와 같은 자리입니다.
 */
func localized(_ key: String.LocalizationValue, _ args: CVarArg...) -> String {
    let format = String(localized: key, bundle: .module)
    return args.isEmpty ? format : String(format: format, arguments: args)
}
