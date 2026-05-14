# BB TxT

> ソースコード、ログ、設定、読み取り可能なテキストファイルを高速に検索するローカルテキスト検索ツール。

[Overview](../../README.md) | [English](README.en.md) | [한국어](README.ko.md) | [中文](README.zh-CN.md) | [日本語](README.ja.md)

BB TxT は、ローカルのソースコード、ログ、設定、その他の読み取り可能なテキストファイルを高速かつ信頼できる形で検索したい開発者向けの Java Swing デスクトップアプリです。ファイルが scope 内で読み取り可能なら検索し、そうでない場合は何を skip したのか、なぜ読み取りに失敗したのかを報告します。

製品名はアプリ全体で `BB TxT` に固定されています。言語切り替えは localize された UI 文字列だけを変更し、brand name は変更しません。

## Key Features

- ローカルフォルダーの recursive scanning。
- デフォルトの literal search。
- case-sensitive、whole-word、regex search mode。
- 進行中に更新される result table。
- skipped/error の明確な reporting。
- hidden-file と extension filtering。
- bundled Source Han Sans font set による安定した多言語 UI rendering。

## Supported Languages

- English
- Korean
- Japanese
- Chinese

## Current Release

現在の repository version は `0.1.0` です。

最初の public release は Windows app-image ZIP として配布されます。

- `BB-TxT-app-image-0.1.0.zip`

GitHub Releases から release artifact をダウンロードしてください。

Gradle build は Windows installer-style EXE package もサポートしています。ただし現在の build machine では、EXE packaging を安定して作成するために WiX Toolset のインストールが必要です。

## Local Development

repository root からアプリを実行:

```powershell
.\gradlew.bat run
```

search verification と Swing smoke checks を実行:

```powershell
.\gradlew.bat check
```

個別にも実行できます:

```powershell
.\gradlew.bat runVerification
.\gradlew.bat runSmokeCheck
```

## Build and Packaging

packaging-ready jar を build:

```powershell
.\gradlew.bat jar
```

Windows app-image を build:

```powershell
.\gradlew.bat packageAppImage
.\gradlew.bat releaseAppImageZip
```

packaging machine に WiX Toolset がある場合に Windows installer EXE を build:

```powershell
.\gradlew.bat packageExe
.\gradlew.bat releaseExe
```

jar と release artifact をまとめて build:

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

- BB TxT は phase-1 developer text search に集中しています。PDF や Office documents は core default flow には含まれません。
- English、Korean、Japanese、Chinese UI rendering を安定させるため、Source Han Sans fonts を bundle しています。
- Gradle packaging tasks は Windows-oriented です。
- Release artifacts は source tree に commit せず、GitHub Releases に upload する前提です。

## Demo Walkthrough

demo flow では folder path と search term を入力し、readable text files 内の matching lines を探します。

1. アプリを起動します。
2. 検索する root folder を入力します。
3. `apple` などの検索語を入力します。
4. `Search` をクリックします。
5. results table で file name、line number、matched text を確認します。

最初の画面には root folder、search term、search options が表示されます。

![Java explore start](../demo-screenshots/java-explore-flow-01-open.png)

folder path と `apple` などの keyword を入力し、右上の `Search` button をクリックします。

![Folder and keyword entered](../demo-screenshots/java-explore-flow-02-input-filled.png)

検索が終わると results table に matched file path、line number、text preview が表示されます。

![Search result table](../demo-screenshots/java-explore-flow-03-search-results.png)
