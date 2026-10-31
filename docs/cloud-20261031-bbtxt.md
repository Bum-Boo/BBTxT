# BBTxT 릴리스 품질 점검 보고서 (2026-10-31, 클라우드 세션)

브랜치: `claude/bbtxt-release-quality-llku5m` (기준: `main` @ `c5b14ee`)
환경: Ubuntu 24.04 클라우드 VM, 4 vCPU, OpenJDK 17.0.20 (전체 JDK) / 21.0.12, Gradle 8.10.2 wrapper, Xvfb.
모든 테스트 데이터는 실행 시 임시 폴더에 생성하는 합성 데이터이며 외부 서비스 호출은 없습니다.

## 1. 요약

| 구분 | 건수 |
|---|---|
| 검증된 결함 (재현 → 수정, 코드 결함은 회귀 테스트 포함) | 11 |
| 정적 위험 (코드상 위험, 미수정) | 9 |
| 이미 처리됨 (테스트로 확인) | 6 |
| 문제 아님 | 4 |

README의 성능 주장은 수치 없이 "Fast"뿐이라 틀린 수치는 없었습니다. 다만 grep/ripgrep보다 느리다는 점을 측정해 README에 수치와 재현 방법을 추가했습니다.

## 2. 명령과 결과 (전/후)

| 명령 | 수정 전 (`main`) | 수정 후 (HEAD) |
|---|---|---|
| `./gradlew check` (깨끗한 clone) | `Permission denied` (gradlew 실행 권한 없음) | 성공 |
| `sh ./gradlew check` (JDK 21만 설치) | 실패: `Cannot find a Java installation ... languageVersion=17` | 동일 (JDK 17 필요, README에 문서화) |
| `sh ./gradlew check` (JDK 17, root, headless) | BUILD SUCCESSFUL. runVerification 6개 시나리오 통과, smoke check는 headless라 skip | BUILD SUCCESSFUL. runVerification 통과, runCorpusVerification **32 통과 / 0 실패 / 2 skip**(권한 시나리오, root라서) |
| `./gradlew runCorpusVerification` (새 스위트를 수정 전 코드에 적용, root) | **22 통과 / 10 실패 / 2 skip** | 32 / 0 / 2 |
| 같은 스위트, 비 root 사용자 `tester` | **24 통과 / 10 실패 / 0 skip** | **34 / 0 / 0** |
| `xvfb-run -a ./gradlew runSmokeCheck` | MainFrame smoke 통과. (새) 검색 재시작 검사 **3/3 실패**: 1500행 기대, 1705/1723/1801행(205~301행이 취소된 검색의 결과) | MainFrame smoke 통과, 재시작 검사 **5/5 통과** |
| CI와 같은 조건: 새 clone, 비 root, JDK 17, `xvfb-run -a ./gradlew check` | — | BUILD SUCCESSFUL (34/0/0, smoke·재시작 검사 통과) |
| `./gradlew packageAppImage` (Linux) | "only configured for Windows hosts"로 명확히 실패 (문서와 일치) | 동일 |

lint/type-check 도구는 저장소에 설정되어 있지 않습니다(`javac` 컴파일이 사실상 유일한 정적 검사이며 경고 없이 통과).

## 3. 검증된 결함 (수정 완료)

1. **gradlew 실행 권한 없음** — `gradlew`가 mode 100644로 커밋되어 macOS/Linux에서 `./gradlew`가 `Permission denied`. 실행 권한 부여 (`9e5ce50`).
2. **README와 실제 빌드의 차이** — Java 17 toolchain이 필요하지만 문서에 없고, toolchain 다운로드 저장소도 없어 JDK 21만 있으면 빌드 실패. Windows 명령만 있음. `README.md`, `docs/readme/README.*.md`에 JDK 17, `./gradlew`, headless 실행법(`xvfb-run`)을 추가 (`7b1ba82`). 의존성/toolchain 설정 자체는 바꾸지 않았습니다.
3. **판독 불가 파일이 UTF-16 쓰레기로 "검색됨" → 조용한 false negative** — `src/main/java/app/io/TextFileInspector.java:126`, `:147`. 후보 문자셋 끝에 항상 UTF-16LE/BE가 붙어, 짝수 바이트 파일은 거의 무조건 UTF-16으로 decode 성공. Latin-1/MS949 `.java`, 8 KiB 이후에 잘못된 바이트가 있는 UTF-8 파일, 잘린 UTF-16(BOM 있음)이 오류 없이 "검색됨, 일치 0건"이 됨. README의 "읽지 못하면 이유를 보고한다"와 모순. UTF-16은 헤더가 UTF-16처럼 보일 때만 시도하고 BOM은 확정적으로 취급 → `DECODE_FAILURE`로 보고 (`3697ef4`).
4. **대소문자 무시 검색에서 길이가 바뀌는 소문자 변환으로 전체 검색 중단** — `src/main/java/app/search/LiteralSearchPlan.java:31`, `:69`, `:102`. `String.toLowerCase`가 `İ`(U+0130)를 두 글자로 바꿔 접힌 줄의 오프셋을 원래 줄에 적용 → 열 번호 오류, 오프셋이 줄 끝을 넘으면 `PreviewBuilder`에서 `StringIndexOutOfBoundsException`이 나고 검색 전체가 실패. 길이가 같을 때는 `toLowerCase`를 그대로 쓰고, 다를 때만 글자 단위로 변환 (`4dec54e`, 성능 회귀 보정 `443fd34`).
5. **regex 모드에서 CR만 쓰는 줄바꿈 무시** — `src/main/java/app/search/SearchFileTask.java:194`. literal 모드(`readLine`)와 줄 번호가 달랐음 (CR-only 파일에서 3행 대신 1행) (`bf87d33`).
6. **regex 모드에서 NFD 한글 미일치** — `SearchFileTask.java:129`. literal은 NFC 정규화를 하는데 regex는 하지 않아 macOS식 자모 분리 한글을 찾지 못함 (`096231e`).
7. **대용량 파일 하나가 전체 검색을 중단** — `SearchFileTask.java:30`, `:72`, `:88`. regex 모드에서 2 GB 초과 파일(`readAllBytes`의 `OutOfMemoryError`)이나 literal 모드에서 heap을 넘는 한 줄이 Error를 던지고, `SearchEngine`이 "Search worker failed unexpectedly"로 검색 전체를 중단하며 진행 중 결과도 버림 (3 GB sparse 파일, 300 MB 한 줄로 재현). 2 GB 사전 검사와 파일 단위 `OutOfMemoryError` 처리 → 새 사유 `FILE_TOO_LARGE`(4개 언어 번역) (`318b827`).
8. **심볼릭 링크 루트 폴더를 고르면 아무것도 검색하지 않음** — `src/main/java/app/search/SearchFileEnumerator.java:38`, `:124`. `walkFileTree`가 링크인 시작 경로를 파일로 취급. 링크 대상을 순회하되 결과 경로는 사용자가 고른 루트 기준으로 표시 (`d30e12a`).
9. **트리 안의 링크·FIFO·소켓이 흔적 없이 누락** — `SearchFileEnumerator.java:128`. 여전히 따라가거나 열지 않지만(루프·FIFO 블로킹 방지), 숨김/확장자 필터 뒤에 `SYMBOLIC_LINK`, `NOT_REGULAR_FILE`로 보고 (`d30e12a`).
10. **검색 중 Enter로 재시작하면 이전 결과가 섞임** — `src/main/java/app/ui/MainFrame.java:585`. 취소된 SwingWorker의 대기 중 이벤트와 `done()`이 새 표에 반영되어 205~301행의 오래된 결과가 섞이고, Stop 버튼이 꺼지고 Search가 켜짐. 현재 worker의 콜백만 반영 (`cd6ef15`). 회귀 검사: `src/test/java/app/ui/SearchRestartCheck.java`.

11. **검증 태스크가 선언된 Java 버전으로 실행되지 않음** — 사용자 정의 `JavaExec` 검증 태스크가 Java 17 toolchain이 아니라 Gradle을 띄운 JVM(이 VM에서는 21)으로 실행되던 문제 (`build.gradle:39`, `34fb53d`).

## 4. 테스트 스위트 (추가)

외부 의존성 없이 기존 `main()` 하네스 방식을 따랐습니다 (의존성·lockfile 변경 없음).

- `src/test/java/app/testsupport/ScenarioRunner.java`, `Corpus.java` — 시나리오 실행기, 임시 corpus 생성(BOM, sparse 파일, symlink, 권한 제거).
- `src/test/java/app/search/EncodingScenarios.java` — UTF-8/UTF-8 BOM/UTF-16LE·BE(BOM 유무), MS949/EUC-KR, 빈 파일·BOM만 있는 파일, Shift_JIS/GBK/Big5/cp1252, NFD, 이모지, 8 KiB 이후의 잘못된 바이트.
- `ContentScenarios.java` — 바이너리 판별(NUL, PNG, 제어문자), 5 MB 한 줄, 한 줄 2만 건 일치, 3 GB sparse 파일(regex·literal), LF/CRLF/CR, 터키어 İ, 그리스어·키릴 대소문자, whole-word, regex.
- `TraversalScenarios.java` — 링크 루트, 링크·루프·깨진 링크, FIFO, 권한 없는 파일/폴더/루트, 없는 루트, 숨김 규칙, 확장자 규칙, 3000개 파일 정확한 개수, 취소.
- i18n: 모든 `SkipReason`/`SkippedPathKind` 키가 4개 언어 파일에 있는지 검사.
- `build.gradle`: `runCorpusVerification`(`check`에 포함, `-Xmx256m`으로 OOM 시나리오를 빠르고 결정적으로), `runBenchmark`(`check` 미포함).
- `.github/workflows/ci.yml`: Ubuntu(Xvfb) + Windows에서 Temurin 17로 `check` 실행.

## 5. 성능 측정

재현: `./gradlew runBenchmark` (시드 고정 corpus 20,000개 파일 / 221.8 MiB, `build/bench-corpus`). 엔진만 측정하며 Swing 표 렌더링은 포함하지 않습니다. 4 vCPU, JDK 17, 캐시 warm, 5회 중앙값:

| 검색 | `main` | HEAD |
|---|---|---|
| literal ASCII 대소문자 무시 | 632~668 ms | 629~661 ms |
| literal ASCII 대소문자 구분 | 511~520 ms | 532~562 ms |
| literal 한국어 | 551~563 ms | 567~586 ms |
| literal 일치 없음 | 425~436 ms | 429~443 ms |
| regex 대소문자 무시 | 756~783 ms | 764~807 ms |

두 번씩 번갈아 측정했고 차이는 실행 간 편차(약 5%) 안입니다. 캐시 cold 1회 검색: 엔진 약 1.5 s, GNU grep 3.11 `grep -rIic` 1.2 s, ripgrep 14.1 0.33 s (warm: grep 0.14 s, rg 0.05 s). 결론: 데스크톱 대화형 용도로는 "빠름"이 성립하지만 명령행 도구 수준은 아니므로 `docs/readme/README.en.md`의 Performance 절에 수치와 비교를 명시했습니다.

## 6. 정적 위험 (미수정, 판단 필요)

1. 워커의 `RuntimeException`은 여전히 검색 전체를 중단 — `src/main/java/app/search/SearchEngine.java:169-183`. 이번에 원인 결함은 고쳤지만 파일 단위 격리는 없음.
2. 디렉터리 순회 중 `DirectoryIteratorException`이 나면 `SimpleFileVisitor.postVisitDirectory` 기본 구현이 예외를 던져 순회 전체가 중단 (재현 방법 없음).
3. 취소 후 `awaitTermination(100 ms)`만 기다림 (`SearchEngine.java:253`). 큰 파일을 읽던 워커가 백그라운드에서 계속 실행될 수 있음.
4. 결과 표에 상한이 없음 (`src/main/java/app/ui/MatchTableModel.java:60`). 벤치마크 질의 하나가 68만 건을 반환하므로 UI에서는 메모리·반응성 문제가 될 수 있음 (GUI에서 미측정).
5. `.java` 등 UTF-8 우선 확장자(`TextFileInspector.java:25`)는 MS949 대체 decode를 하지 않음. 한국어 Windows에서 만든 MS949 소스는 이제 decode failure로 표시됨 (이전에는 짝수 길이면 조용히 누락). 대체 decode 허용 여부는 제품 판단.
6. Shift_JIS/GBK/Big5 `.txt`는 5개 중 4개가 MS949로 오류 없이 decode되어 CJK 질의가 누락됨 (ASCII 질의는 일치). README에 한계로 기록.
7. `SearchOptions`가 질의를 `trim()` (`src/main/java/app/model/SearchOptions.java:21`) → 앞뒤 공백 포함 검색 불가.
8. literal의 ASCII 빠른 경로(`LiteralSearchPlan.java:30`)는 KELVIN SIGN 같은 유니코드 대소문자 대응을 하지 않아 regex(`UNICODE_CASE`)와 결과가 다를 수 있음.
9. `jpackage`는 toolchain이 아니라 Gradle JVM의 `java.home`에서 찾음 (`build.gradle:60`). Gradle을 JDK 21로 돌리면 런타임 21로 패키징될 수 있음 (Windows 전용이라 미검증).

## 7. 이미 처리됨 / 문제 아님

- 이미 처리됨: 권한 없는 파일·폴더·루트 보고(비 root로 확인), 숨김 폴더를 한 번에 건너뜀, 확장자 대소문자 무시, 8 KiB 내 NUL·PNG 바이너리 판별, 취소 시 즉시 종료, 대량 파일에서 진행 이벤트 배치 처리.
- 문제 아님: 심볼릭 링크 루프(따라가지 않으므로 무한 루프 없음), whole-word에서 한글 조사가 붙으면 불일치(`isWordCharacter`, 의도된 동작으로 테스트에 고정), 열 번호가 UTF-16 단위·NFC 기준(이모지 뒤에서 1 차이, VS Code와 같은 방식), 8 KiB 이후 NUL은 바이너리로 보지 않음(grep류와 같은 휴리스틱, README에 기록).

## 8. 이 VM에서 검증하지 못한 것

- Windows: `.\gradlew.bat`, `jpackage` 패키징(app-image/EXE, WiX), Windows의 숨김 속성·junction·심볼릭 링크 권한, NTFS에서 3 GB 파일 생성 속도. CI의 Windows 작업은 작성만 했고 실행 결과는 확인하지 못했습니다.
- macOS: `/tmp` 같은 링크 루트, NFD 파일명, 실제 GUI.
- 실제 화면 사용: IME 입력, 폰트 렌더링, 대량 결과에서의 표 반응성(Xvfb에서 smoke·재시작 검사만 실행).
- Windows Defender 등 실시간 검사 환경의 성능, HDD/네트워크 드라이브 성능.
- GitHub Actions 실제 실행(푸시 후 결과는 확인하지 않음).

## 9. 다음 단계 제안

1. GitHub Actions에서 Windows 작업 결과를 확인하고, 실패하면 해당 시나리오(심볼릭 링크·3 GB sparse 파일)를 Windows에서 skip할지 결정.
2. 워커 예외를 파일 단위로 격리(`SearchEngine.resolveResult`)하고 `postVisitDirectory` 예외를 skip으로 보고.
3. 결과 표 상한(예: 10만 건 후 "더 있음" 표시) 도입 여부 결정.
4. `.java` 등에서 MS949 대체 decode를 허용할지, 또는 사용자가 인코딩을 고를 수 있게 할지 결정.
5. 성능 개선이 필요하면 `runBenchmark`로 비교하며 진행 (파일당 두 번 여는 비용, 줄 단위 NFC 검사, 엄격한 `CharsetDecoder` 비용이 후보).
6. 패키징 태스크가 toolchain의 `jpackage`를 쓰도록 변경 검토.
