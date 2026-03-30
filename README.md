# BB TxT

BB TxT is a Windows-friendly Java Swing desktop utility for developers who need reliable local text search across source code, logs, configs, and other technical text files inside a selected folder.

The app keeps a fixed brand name, `BB TxT`, while language switching only changes localized UI strings. The current UI theme, multilingual rendering, and search behavior are preserved.

## Highlights

- Recursive directory search with progressive results
- Literal search by default, plus case-sensitive, whole-word, and regex modes
- All matches are reported with file path, line, column, and snippet preview
- Explicit skipped/error reporting instead of silent omission
- BOM-aware decoding with UTF-8, UTF-16 LE/BE, MS949, and EUC-KR fallback support
- Unified Source Han Sans UI font strategy for English, Korean, Japanese, and Chinese rendering
- Top-bar language switching using resource bundles for localized labels, buttons, tabs, dialogs, and status text
- Fixed product branding: `BB TxT`
- Hidden file and extension filtering controls

## Project Layout

```text
assets/icons
  BTXTB.ico
  BTXTB.png

dist
  jpackage outputs are written here during packaging

packaging
  Windows packaging notes and helper documentation

release
  Release-ready zip/exe outputs are written here

src/main/java/app
  AppLauncher.java
  AppBrand.java
  /i18n
  /io
  /model
  /search
  /ui
  /util

src/main/resources/i18n
  messages*.properties

src/test/java/app
  /search
  /ui

sample-data/phase1-fixture
  Manual test files for local verification
```

## Running

Use the Gradle wrapper from the repository root:

```powershell
.\gradlew.bat run
```

Build the jar and run the lightweight checks:

```powershell
.\gradlew.bat build
```

Build only the packaging-ready jar:

```powershell
.\gradlew.bat jar
```

## Windows Packaging

Packaging uses `jpackage` and expects the Windows icon at:

```text
assets/icons/BTXTB.ico
```

If that file is missing, the packaging task fails clearly.

### Portable app-image

This produces a Windows app-image under `dist/app-image` and a release ZIP in `release`:

```powershell
.\gradlew.bat packageAppImage
.\gradlew.bat releaseAppImageZip
```

Recommended release asset name:

```text
BB-TxT-app-image-0.1.0.zip
```

### Installer-style exe

This produces a Windows installer under `dist/exe` and copies it into `release` with the recommended release name:

```powershell
.\gradlew.bat packageExe
.\gradlew.bat releaseExe
```

Recommended release asset name:

```text
BB-TxT-Setup-0.1.0.exe
```

### Build everything for release

```powershell
.\gradlew.bat assembleRelease
```

## Verification

Run the verification harnesses directly:

```powershell
java -cp out app.search.SearchEngineVerification
java -cp out app.ui.MainFrameSmokeCheck
```

`app.search.SearchEngineVerification` covers the default literal fast path, larger multi-file literal scans, extension filtering, explicit skip reporting, UTF-16 decoding, and Korean UTF-8 / UTF-8 BOM / normalization-safe matching.

## Search Scope

The default extension set targets developer-facing text and code files:

`.txt, .md, .log, .json, .yaml, .yml, .xml, .properties, .java, .kt, .kts, .py, .c, .cpp, .h, .hpp, .cs, .js, .ts, .jsx, .tsx, .html, .css, .sql, .csv, .ini, .bat, .cmd, .gradle`

You can override the extension list in the UI with a comma, semicolon, space, or newline separated list.

## Manual Verification Fixture

Search the `sample-data/phase1-fixture` folder for `needle` to exercise:

- Multiple matches in one file
- Multiple matches across files
- UTF-8, UTF-16, and MS949 text handling
- Hidden-file exclusion and inclusion
- Extension filtering
- Binary-file skip reporting

Search the same fixture for `\uAC80\uC0C9` to verify:

- Plain UTF-8 Korean search
- UTF-8 BOM Korean search
- Normalization-safe Hangul matching

Use the language selector in the top toolbar to verify UI switching between English, Korean, Japanese, and Chinese.

## Windows UI Verification Matrix

Run `.\gradlew.bat run` and verify the following on a Windows machine with the relevant IMEs installed:

- English UI + English input: labels, buttons, tables, tabs, status text, and dialogs render cleanly with a stable Windows UI font; Enter in the search field starts search normally.
- Korean UI + Korean readability: switch to Korean and confirm labels, buttons, tabs, combo boxes, status text, tables, and the search field all use stable Hangul metrics with no undersized or uneven-looking glyph fallback.
- Korean UI + Korean input: type Korean in the search field and confirm the composed Hangul stays visible while the normal Swing insert caret is suppressed during active composition, then returns immediately after commit.
- Japanese UI + Japanese rendering: switch to Japanese and confirm labels, tabs, combo boxes, dialog buttons, table headers, and table cells show proper Japanese glyphs with no broken fallback boxes or unstable metrics.
- Chinese UI + Chinese rendering: switch to Chinese and confirm labels, tabs, combo boxes, dialog buttons, table headers, and table cells show proper Chinese glyphs with no broken fallback boxes or unstable metrics.
- Korean IME search field behavior: compose Hangul syllables, keep the composition open for multiple keystrokes, commit text, move the caret with arrow keys, and run a search without the misleading left-edge caret appearing during composition.
- Japanese IME search field behavior if available: compose kana or kanji in the same search field and confirm the IME preedit text remains stable, the regular caret stays hidden during composition, and the caret returns after commit.
- Chinese IME search field behavior if available: compose Simplified or Traditional Chinese in the same search field and confirm the IME preedit text remains stable, the regular caret stays hidden during composition, and the caret returns after commit.
- Language switching stability: enter English text, Korean text, and any available Japanese or Chinese text in the search field, switch the top-bar UI language between English, Korean, Japanese, and Chinese, and confirm the query text, resolved fonts, and IME behavior remain stable.

## Phase 1 Limitations

- No PDF, Word, Excel, or PowerPoint search
- No file indexing or background daemon
- No editor integration or file-opening actions
- Regex mode searches decoded file text in-memory so very large regex targets can be slower than literal search
