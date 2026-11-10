# 남친렌즈 구현 계획

기획: [PLAN.md](PLAN.md)

## 모듈 구조

```
namchin/
├── core/   (순수 Kotlin/JVM — 안드로이드 의존성 0, TDD 대상)
│   ├── model/      Keypoint, PersonPose, FramingSpec, Reference, PersonSpec, Tag
│   ├── pose/       PoseNormalizer, PoseSimilarity (좌우반전 포함)
│   ├── framing/    FramingAnalyzer → List<GuidanceCue>
│   ├── guidance/   GuidancePrioritizer (우선순위 + 쿨다운, Clock 주입)
│   ├── shutter/    AutoShutterStateMachine (0.5초 유지)
│   ├── tagging/    TagInference (코사인 kNN + 기본 라벨 매핑)
│   ├── crop/       ScreenshotCropDetector (휘도 배열 → 사진 영역)
│   └── bestshot/   BestShotScorer
└── app/    (Android)
    ├── ml/         ML Kit/MediaPipe 어댑터 → core 모델로 변환
    ├── camera/     CameraX 바인딩, FrameAnalyzer
    ├── feedback/   TTS(음성), Haptics(진동)
    ├── data/       Room (Reference, Tag, ShotResult)
    ├── importer/   공유 인텐트 수신, 크롭, 분석 파이프라인
    └── ui/         home / import / camera / result 화면 (Compose)
```

원칙: **판단 로직은 전부 core**, app은 센서·ML·화면을 core에 연결하는 얇은 층.

## 단계

### Phase 0. 부트스트랩
- Gradle wrapper, version catalog, `:core`(JVM) + `:app` 모듈, Kover(커버리지)
- 검증: `./gradlew :core:test :app:assembleDebug` 성공, S23에 빈 화면 설치

### Phase 1. 핵심 로직 (core, TDD)
테스트 먼저 → 구현 순서:
1. ~~`PoseNormalizer`~~ — 제외: 마디 각도 비교가 이미 거리·위치 불변
2. `PoseSimilarity` — 마디 각도(이미지 비율 보정) 점수, 반전은 0.05 이상 나을 때만 전환, 현재 안 보이는 마디는 0점
3. `FramingAnalyzer` — 몸통(어깨 중점·몸통 길이) 기준 위치·크기 + 수평각 → 안내 큐, 히스테리시스
4. `GuidancePrioritizer` — 인물없음 → 수평 → 거리 → 기울기 → 좌우 → 포즈, 400ms 안정 후 발화, 쿨다운
5. `AutoShutter` — 0.5초 유지 시 발사, 150ms 끊김 허용, 쿨다운 후 포즈가 한 번 풀려야 재발사, 수동 촬영 시 쿨다운
6. `TagInference` — kNN 투표 + ML Kit 라벨 매핑, 확정 예시 0개일 때 기본 라벨만
7. `ScreenshotCropDetector` — 인스타 UI 단색 띠를 걸러 사진 사각형 검출, 실패 시 null
8. `BestShotScorer` — 일치도 + 눈 뜸 확률 + 선명도

- 검증: 테스트 전부 통과, core 커버리지 80%+

### Phase 2. 레퍼런스 가져오기
- 공유 인텐트(`ACTION_SEND image/*`) 수신 → 자동 크롭(실패 시 수동 크롭 UI) → ML Kit Pose 분석 → Room 저장
- 홈 화면: 레퍼런스 그리드 + 골격 미리보기
- 검증: 갤러리의 인스타 캡처를 공유 → 목록에 골격과 함께 표시

### Phase 3. 촬영 화면
- CameraX Preview + ImageAnalysis, 실시간 골격 추출 → core로 판정
- 오버레이: 반투명 레퍼런스(투명도 슬라이더), 목표/현재 골격, 수평계, 텍스트·화살표
- 검증: 기기에서 분석 지연 측정(목표 프레임당 100ms 이하), 안내가 실제 움직임대로 바뀌는지 확인

### Phase 4. 피드백 + 자동 셔터
- TTS(한국어), 진동, 자동 셔터 → 연속 촬영
- 일정 시간 불일치 시 가장 어긋난 부위 음성 안내
- 검증: 이어폰 착용 상태로 실제 촬영 시나리오 수행

### Phase 5. 결과
- MediaStore `DCIM/남친렌즈` 저장, 레퍼런스와 나란히 비교, 베스트 컷 표시 (Face Detection 눈 뜸 확률)
- 검증: 연속 촬영 → 비교 화면 → 갤러리에서 확인

### Phase 6. 태그 + 추천
- MediaPipe Image Embedder(에셋 포함) → 임베딩 저장, 태그 칩 수정 UI, 현장 프레임 기반 추천
- 검증: 태그 수정 후 비슷한 새 레퍼런스에 수정 태그가 제안됨

### Phase 7. 야경 처리
- 프레임 평균 휘도로 저조도 판정 → 포즈 매칭·자동셔터 OFF, 자이로 기반 흔들림 경고
- 검증: 어두운 곳에서 모드 전환 확인

### Phase 1 리뷰 후 보류 항목
- "폰 높이" 안내: 현재는 기울기(Tilt)로 대체. 실측 후 피치 센서 추가 여부 결정
- 포즈 안내에 방향(올려/내려/굽혀) 포함
- 의도적으로 기울인 레퍼런스(더치 앵글) 재현
- Phase 3 연결 시: 실시간 좌표는 레퍼런스 비율로 자른 영역 기준으로 정규화할 것, 직전 반전 선택(`preferMirrored`)과 직전 안내(`previous`)를 다음 프레임에 넘길 것

## 주요 위험과 대응

| 위험 | 대응 |
|---|---|
| Accurate 포즈 모델 지연 | 실측 후 느리면 Base 모델 또는 N프레임마다 분석, 화면은 보간 |
| 체형·거리 차이로 포즈 비교 부정확 | 좌표가 아닌 관절 각도 비교 + 정규화, 실제 사진으로 임계값 튜닝 |
| 인스타 캡처 자동 크롭 실패 | 수동 크롭 폴백을 처음부터 제공 |
| 한국어 TTS 음성 미설치 | 첫 실행 시 확인, 없으면 텍스트+진동만 |
| 자동 셔터가 잘 안 눌림 | 임계값을 상수로 분리해 실측 튜닝, 수동 셔터 상시 |
| 라이브러리 버전 | 부트스트랩 시 최신 안정판 확인 후 version catalog에 고정 |

## 테스트 전략

- **단위 테스트:** core 전체 (JUnit5, 시간은 `Clock` 주입)
- **통합 테스트:** Room DAO, ML Kit 어댑터 (계측 테스트, 샘플 이미지)
- **UI 테스트:** Compose UI 테스트로 주요 흐름. Playwright는 웹 전용이라 이 프로젝트에는 해당되지 않음
- **수동 검증:** 카메라·음성·진동·자동 셔터의 실제 체감은 기기에서만 확인 가능
