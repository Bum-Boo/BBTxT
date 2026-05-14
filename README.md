# BB TxT

> Fast local text search for source code, logs, configs, and readable files.

[Overview](README.md) | [English](docs/readme/README.en.md) | [Korean](docs/readme/README.ko.md) | [Chinese](docs/readme/README.zh-CN.md) | [Japanese](docs/readme/README.ja.md)

| Area | Detail |
|---|---|
| Platform | Java Swing desktop app |
| Search mode | Literal search by default, with case, whole-word, and regex options |
| Release artifact | Windows app-image ZIP |
| Privacy stance | Searches local files; no cloud indexing or external search service |

## Download / Release

Latest release: [BB TxT v0.1.0](https://github.com/Bum-Boo/BBTxT/releases/tag/v0.1.0)

Windows app-image ZIP:

- [BB-TxT-app-image-0.1.0.zip](https://github.com/Bum-Boo/BBTxT/releases/download/v0.1.0/BB-TxT-app-image-0.1.0.zip)

Release artifacts are published through GitHub Releases and should not be committed into the source tree.

## Preview

The search UI is built around a folder path, a query, search options, and a result table with matched lines.

![BB TxT search start](docs/demo-screenshots/java-explore-flow-01-open.png)

<details>
<summary>View demo walkthrough</summary>

1. Launch the app.
2. Enter the root folder to search.
3. Enter a term such as `apple`.
4. Click `Search`.
5. Review matched file paths, line numbers, and text previews.
6. Check skipped or unreadable files when needed.

![Folder and keyword entered](docs/demo-screenshots/java-explore-flow-02-input-filled.png)

![Search result table](docs/demo-screenshots/java-explore-flow-03-search-results.png)

</details>

## Quick Start

```powershell
.\gradlew.bat run
```

Run checks:

```powershell
.\gradlew.bat check
```

## Documentation

- [English README](docs/readme/README.en.md)
- [Korean README](docs/readme/README.ko.md)
- [Chinese README](docs/readme/README.zh-CN.md)
- [Japanese README](docs/readme/README.ja.md)
- [Portfolio case study](docs/portfolio-case-study.md)
- [GitHub metadata note](docs/github-metadata.md)

## Status

BB TxT is focused on local developer text search. PDF and Office document parsing are not part of the core default flow.
