# BB TxT

> Fast local text search for source code, logs, configs, and readable text files.

[English](#english) | [한국어](#한국어) | [中文](#中文) | [日本語](#日本語)

| Area | Detail |
|---|---|
| Platform | Java Swing desktop app |
| Search mode | Literal search by default, with case, whole-word, and regex options |
| Release artifact | Windows app-image ZIP |
| UI languages | English, Korean, Japanese, Chinese |

## English

BB TxT is a Java Swing desktop app for developers who need fast, trustworthy text search across local source code, logs, configs, and other readable text files. If a file is in scope and readable, BB TxT searches it; if not, it reports what was skipped or why a read failed.

The product name is fixed as `BB TxT` across the app. Language switching changes localized UI strings, not the brand name.

## Key Features

- Recursive local folder scanning
- Literal search by default
- Case-sensitive, whole-word, and regex search modes
- Progressive result updates
- Explicit skipped/error reporting
- Hidden-file and extension filtering
- Stable multilingual UI rendering with a bundled Source Han Sans font set

## Supported Languages

- English
- Korean
- Japanese
- Chinese

## Current Release

The current repository version is `0.1.0`.

The first public release is distributed as the Windows app-image ZIP:

- `BB-TxT-app-image-0.1.0.zip`

Download the release artifact from GitHub Releases.

The Gradle build also supports a Windows installer-style EXE package. That path is supported by the build setup, but the current build machine still needs WiX Toolset installed before EXE packaging can be produced reliably.

## Local Development

Run the app from the repository root:

```powershell
.\gradlew.bat run
```

Run the search verification and Swing smoke checks:

```powershell
.\gradlew.bat check
```

You can also run the checks individually:

```powershell
.\gradlew.bat runVerification
.\gradlew.bat runSmokeCheck
```

## Build and Packaging

Build the packaging-ready jar:

```powershell
.\gradlew.bat jar
```

Build the Windows app-image:

```powershell
.\gradlew.bat packageAppImage
.\gradlew.bat releaseAppImageZip
```

Build the Windows installer EXE when WiX Toolset is available on the packaging machine:

```powershell
.\gradlew.bat packageExe
.\gradlew.bat releaseExe
```

Build the jar plus both release artifacts:

```powershell
.\gradlew.bat assembleRelease
```

Runtime Swing window icon:

- `src/main/resources/assets/icons/BTXTB.png`

Packaging icon for `jpackage`:

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

- BB TxT is focused on phase-1 developer text search; PDF and Office documents are not part of the core default flow.
- The app bundles Source Han Sans fonts to keep English, Korean, Japanese, and Chinese UI rendering stable.
- The Gradle packaging tasks are Windows-oriented.
- Release artifacts are meant to be uploaded to GitHub Releases, not committed into the source tree.
## Demo Walkthrough

The demo flow enters a folder path and a search term, then finds matching lines inside readable text files.

1. Launch the app.
2. Enter the root folder to search.
3. Enter a term such as `apple`.
4. Click `Search`.
5. Review the file name, line number, and matched text in the results table.

The first screen shows fields for the root folder, search term, and search options.

![Java explore start](docs/demo-screenshots/java-explore-flow-01-open.png)

Enter the folder path and a keyword such as `apple`, then click the `Search` button in the top-right area.

![Folder and keyword entered](docs/demo-screenshots/java-explore-flow-02-input-filled.png)

After the search finishes, the results table shows the matched file path, line number, and text preview.

![Search result table](docs/demo-screenshots/java-explore-flow-03-search-results.png)

---

## 한국어

BB TxT는 로컬 소스 코드, 로그, 설정 파일, 읽을 수 있는 텍스트 파일을 빠르게 검색하기 위한 Java Swing 데스크톱 앱입니다. 검색 가능한 파일은 결과에 포함하고, 읽을 수 없거나 제외된 파일은 이유를 알려줍니다.

제품명 `BB TxT`는 모든 언어에서 그대로 사용합니다. 언어 전환은 UI 문자열만 바꾸며 브랜드명은 바꾸지 않습니다.

### 주요 기능

- 로컬 폴더 재귀 검색
- 기본 literal 검색
- 대소문자 구분, whole-word, regex 검색 옵션
- 진행 중 결과 업데이트
- 건너뛴 파일과 오류 이유 표시
- hidden file과 확장자 필터
- Source Han Sans 번들 폰트를 통한 한중일/영문 UI 안정 렌더링

### 실행

```powershell
.\gradlew.bat run
```

### 데모 흐름

1. 앱을 실행합니다.
2. 검색할 루트 폴더를 입력합니다.
3. `apple` 같은 검색어를 입력합니다.
4. `Search` 버튼을 누릅니다.
5. 결과 표에서 파일명, 줄 번호, 매칭 문장을 확인합니다.

---

## 中文

BB TxT 是一个 Java Swing 桌面应用，用于在本地源代码、日志、配置文件和其他可读文本文件中快速可靠地搜索。可读且在范围内的文件会被搜索，无法读取或被跳过的文件会显示原因。

产品名 `BB TxT` 在所有语言中保持不变。语言切换只影响 UI 字符串。

### 主要功能

- 递归扫描本地文件夹
- 默认 literal 搜索
- 支持大小写敏感、whole-word 和 regex 选项
- 搜索过程中逐步更新结果
- 明确报告跳过文件和读取错误
- 支持 hidden file 和扩展名过滤
- 通过内置 Source Han Sans 字体稳定渲染英语、韩语、日语和中文 UI

### 运行

```powershell
.\gradlew.bat run
```

### 演示流程

1. 启动应用。
2. 输入要搜索的根文件夹。
3. 输入 `apple` 这样的关键词。
4. 点击 `Search`。
5. 在结果表中查看文件名、行号和匹配文本。

---

## 日本語

BB TxT は、ローカルのソースコード、ログ、設定ファイル、その他の読み取り可能なテキストファイルを高速に検索する Java Swing デスクトップアプリです。検索対象で読めるファイルは検索し、読めないファイルや除外されたファイルは理由を表示します。

製品名 `BB TxT` はすべての言語で固定です。言語切り替えは UI 文字列のみを変更します。

### 主な機能

- ローカルフォルダの再帰検索
- デフォルトは literal 検索
- 大文字小文字、whole-word、regex 検索オプション
- 進行中の結果更新
- スキップされたファイルや読み取りエラーの明示
- hidden file と拡張子フィルタ
- Source Han Sans 同梱による英語、韓国語、日本語、中国語 UI の安定表示

### 実行

```powershell
.\gradlew.bat run
```

### デモ手順

1. アプリを起動します。
2. 検索するルートフォルダを入力します。
3. `apple` のような検索語を入力します。
4. `Search` をクリックします。
5. 結果表でファイル名、行番号、マッチした文章を確認します。
