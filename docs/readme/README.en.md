# BB TxT

> Fast local text search for source code, logs, configs, and readable text files.

[Overview](../../README.md) | [English](README.en.md) | [한국어](README.ko.md) | [中文](README.zh-CN.md) | [日本語](README.ja.md)

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

![Java explore start](../demo-screenshots/java-explore-flow-01-open.png)

Enter the folder path and a keyword such as `apple`, then click the `Search` button in the top-right area.

![Folder and keyword entered](../demo-screenshots/java-explore-flow-02-input-filled.png)

After the search finishes, the results table shows the matched file path, line number, and text preview.

![Search result table](../demo-screenshots/java-explore-flow-03-search-results.png)
