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

# ML Kit 내부(이미 난독화돼 배포됨)를 R8이 한 번 더 줄이면 사물 인식(라벨링) 생성 중 NPE로 앱이 꺼진다
-keep class com.google.android.gms.internal.mlkit_vision_** { *; }
-keep class com.google.mlkit.** { *; }

# MediaPipe가 쓰는 Flogger는 호출 스택에서 자기 클래스 이름을 찾는다. 이름이 바뀌면 초기화 실패로 앱이 꺼진다
-keep class com.google.common.flogger.** { *; }
-dontwarn com.google.common.flogger.**
