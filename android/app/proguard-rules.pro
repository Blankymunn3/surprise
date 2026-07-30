# kotlinx.serialization 이 만든 serializer 는 리플렉션으로 찾습니다
-keepclassmembers class ** {
    *** Companion;
}
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}
