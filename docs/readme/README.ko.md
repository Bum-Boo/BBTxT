# BB TxT

> 소스 코드, 로그, 설정, 읽을 수 있는 텍스트 파일을 빠르게 검색하는 로컬 텍스트 검색 도구.

[Overview](../../README.md) | [English](README.en.md) | [한국어](README.ko.md) | [中文](README.zh-CN.md) | [日本語](README.ja.md)

BB TxT는 개발자가 로컬 소스 코드, 로그, 설정, 기타 읽을 수 있는 텍스트 파일을 빠르고 신뢰할 수 있게 검색하기 위한 Java Swing 데스크톱 앱입니다. 파일이 범위 안에 있고 읽을 수 있으면 검색하고, 그렇지 않으면 무엇을 건너뛰었는지 또는 왜 읽기에 실패했는지 표시합니다.

제품명은 앱 전체에서 `BB TxT`로 고정됩니다. 언어 전환은 UI 문자열만 바꾸며 브랜드명은 바꾸지 않습니다.

## Key Features

- 로컬 폴더 recursive scanning.
- 기본 literal search.
- 대소문자 구분, whole-word, regex 검색 모드.
- 진행 중에도 갱신되는 result table.
- 건너뛴 파일과 오류를 명확히 보고.
- hidden-file 및 extension filtering.
- bundled Source Han Sans font set을 통한 안정적인 다국어 UI rendering.

## Supported Languages

- English
- Korean
- Japanese
- Chinese

## Current Release

현재 repository version은 `0.1.0`입니다.

첫 공개 release는 Windows app-image ZIP으로 배포됩니다.

- `BB-TxT-app-image-0.1.0.zip`

GitHub Releases에서 release artifact를 다운로드하세요.

Gradle build는 Windows installer-style EXE package도 지원합니다. 다만 현재 build machine에는 EXE packaging을 안정적으로 만들기 위해 WiX Toolset 설치가 필요합니다.

## Local Development

repository root에서 앱 실행:

```powershell
.\gradlew.bat run
```

search verification과 Swing smoke check 실행:

```powershell
.\gradlew.bat check
```

개별 check 실행:

```powershell
.\gradlew.bat runVerification
.\gradlew.bat runSmokeCheck
```

## Build and Packaging

packaging-ready jar build:

```powershell
.\gradlew.bat jar
```

Windows app-image build:

```powershell
.\gradlew.bat packageAppImage
.\gradlew.bat releaseAppImageZip
```

packaging machine에 WiX Toolset이 있을 때 Windows installer EXE build:

```powershell
.\gradlew.bat packageExe
.\gradlew.bat releaseExe
```

jar와 release artifact를 함께 build:

```powershell
.\gradlew.bat assembleRelease
```

Runtime Swing window icon:

- `src/main/resources/assets/icons/BTXTB.png`

`jpackage` packaging icon:

- `assets/icons/BTXTB.ico`

## Project Layout

```text
archive/legacy                 Archived legacy source
assets/icons                   Windows packaging icon
dist                           Local jpackage output
packaging                      Windows packaging notes
release                        Local release artifacts
src/main/java                  Application source
src/main/resources/assets/icons Runtime icon assets
src/main/resources/fonts       Bundled Source Han Sans fonts and license
src/main/resources/i18n        Localized UI strings
src/test/java                  Verification and smoke checks
sample-data                    Manual search fixtures
```

## Notes / Known Limitations

- BB TxT는 phase-1 developer text search에 집중합니다. PDF와 Office document는 기본 core flow에 포함되지 않습니다.
- 앱은 English, Korean, Japanese, Chinese UI rendering을 안정적으로 유지하기 위해 Source Han Sans font를 bundle합니다.
- Gradle packaging task는 Windows-oriented입니다.
- Release artifact는 source tree에 commit하지 않고 GitHub Releases에 upload하는 것을 전제로 합니다.

## Demo Walkthrough

demo flow는 folder path와 search term을 입력한 뒤 readable text file 내부의 matching line을 찾습니다.

1. 앱을 실행합니다.
2. 검색할 root folder를 입력합니다.
3. `apple` 같은 검색어를 입력합니다.
4. `Search`를 클릭합니다.
5. results table에서 file name, line number, matched text를 확인합니다.

첫 화면에는 root folder, search term, search option field가 표시됩니다.

![Java explore start](../demo-screenshots/java-explore-flow-01-open.png)

folder path와 `apple` 같은 keyword를 입력한 뒤 오른쪽 위의 `Search` button을 클릭합니다.

![Folder and keyword entered](../demo-screenshots/java-explore-flow-02-input-filled.png)

검색이 끝나면 results table에 matched file path, line number, text preview가 표시됩니다.

![Search result table](../demo-screenshots/java-explore-flow-03-search-results.png)
