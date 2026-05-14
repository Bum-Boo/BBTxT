# BB TxT Portfolio Case Study

BB TxT is a Java Swing desktop app for fast local text search across source code, logs, configs, and readable text files. It is designed for developers and power users who need trustworthy search results without sending local content to external services.

## Positioning

BB TxT fits the portfolio theme of local-first productivity tools and desktop utilities. It is a focused search tool rather than a general document manager: pick a local folder, enter a search term, tune a few options, and inspect the matches and skipped files.

The strongest public framing is:

- fast local text search
- explicit skipped-file reporting
- predictable search options
- Windows-friendly desktop packaging
- multilingual UI rendering

## Problem

Developers often need to search project folders, logs, generated text, config files, and exported notes quickly. General-purpose IDE search is useful inside one project, but it can be heavy or awkward when the search target is a loose folder, mixed file set, or local archive.

BB TxT addresses that practical workflow by making local folder search a small standalone utility.

## Product Shape

The app centers on a single direct workflow:

1. Choose a root folder.
2. Enter a search phrase or regex.
3. Select optional matching modes.
4. Run the search.
5. Review matched file paths, line numbers, and text previews.
6. Check skipped or unreadable files when needed.

The interface is intentionally compact because the primary user task is scanning, comparing, and deciding where to open or inspect next.

## Implementation Notes

BB TxT is built with Java Swing and Gradle. The codebase includes a search engine, path/file filtering, text probing, normalization helpers, result models, Swing UI components, localization resources, bundled Source Han Sans fonts, and packaging tasks for Windows app-image releases.

Validation commands documented in the README include:

```powershell
.\gradlew.bat check
.\gradlew.bat runVerification
.\gradlew.bat runSmokeCheck
```

## Portfolio Value

BB TxT demonstrates:

- local-first developer tooling
- Java desktop application structure
- text search behavior and file filtering
- explicit error/skipped-file reporting
- multilingual UI support
- Windows packaging and release workflow

## Safety and Privacy Notes

The repo should keep the local-first boundary visible. BB TxT searches files on the user's machine and does not need cloud sync, accounts, or external indexing services for the core workflow.

Public examples should use synthetic fixtures rather than private source code, client logs, or personal documents.

## Next Steps

- Link this case study from the README after the existing README changes are finalized.
- Keep release artifacts in GitHub Releases rather than committing generated ZIPs.
- Add a short GIF showing folder selection, search execution, and results.
- Keep the README clear about what file types are searched by default.
