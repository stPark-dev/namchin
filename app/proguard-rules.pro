# 남친렌즈 릴리스 축소(R8) 규칙.
# ML Kit·CameraX·Room·Coil은 라이브러리 안에 규칙이 들어 있다. 여기는 그 외 필요한 것만.

# MediaPipe Tasks: JNI가 이름으로 자바 클래스·필드를 찾고, 설정은 protobuf로 넘긴다
-keep class com.google.mediapipe.** { *; }
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.mediapipe.**
-dontwarn com.google.protobuf.**

# MediaPipe가 참조하지만 안드로이드에서 쓰지 않는 선택 의존성
-dontwarn javax.lang.model.**
-dontwarn com.google.auto.value.**
-dontwarn autovalue.shaded.**

# 크래시 기록에서 줄 번호를 볼 수 있게
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
