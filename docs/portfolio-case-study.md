# BB TxT Portfolio Case Study

## Problem

Developers often need to search project folders, logs, generated text, config files, and exported notes quickly. IDE search is useful inside one project, but it can be heavy or awkward when the target is a loose folder, mixed file set, or local archive.

## Target Users

- Developers and power users.
- People inspecting logs, configs, source folders, and readable local text files.
- Users who want quick local search without uploading files to external services.

## Design Goal

Make local folder search a small standalone desktop utility with predictable options, explicit skipped-file reporting, and a release path for Windows users.

## Core Workflow

1. Choose a root folder.
2. Enter a search phrase or regex.
3. Select optional matching modes.
4. Run the search.
5. Review matched file paths, line numbers, and text previews.
6. Check skipped or unreadable files when needed.

## Architecture Summary

BB TxT is built with Java Swing and Gradle. The codebase includes a search engine, path/file filtering, text probing, normalization helpers, result models, Swing UI components, localization resources, bundled Source Han Sans fonts, and Windows packaging tasks.

## Safety / Privacy Decisions

- Search happens on local files.
- No cloud indexing or external search service is part of the core workflow.
- Public demos should use synthetic fixtures rather than private source code, client logs, or personal documents.
- Release artifacts belong in GitHub Releases, not the source tree.

## Technical Highlights

- Literal search by default.
- Case-sensitive, whole-word, and regex modes.
- Explicit skipped/error reporting.
- Hidden-file and extension filtering.
- Multilingual UI rendering with bundled font support.
- Windows app-image release artifact.

## Current Limitations

- Focused on phase-1 developer text search.
- PDF and Office documents are not part of the core default flow.
- Windows packaging is the primary release path.

## Next Steps

- Add a short GIF showing folder selection, search execution, and results.
- Keep release artifacts in GitHub Releases.
- Keep README copy clear about supported file types.

## Portfolio Value

BB TxT demonstrates local-first developer tooling, Java desktop structure, text search behavior, explicit error reporting, multilingual UI support, and Windows packaging discipline.
