# BB TxT

> 面向源代码、日志、配置和可读文本文件的快速本地文本搜索工具。

[Overview](../../README.md) | [English](README.en.md) | [한국어](README.ko.md) | [中文](README.zh-CN.md) | [日本語](README.ja.md)

BB TxT 是一个 Java Swing 桌面应用，面向需要在本地源代码、日志、配置和其他可读文本文件中进行快速、可信搜索的开发者。文件在范围内且可读取时会被搜索；否则应用会报告跳过了什么，或读取为什么失败。

产品名称在应用中固定为 `BB TxT`。切换语言只改变本地化 UI 字符串，不改变品牌名。

## Key Features

- 递归扫描本地文件夹。
- 默认使用 literal search。
- 支持大小写敏感、whole-word 和 regex 搜索模式。
- 逐步更新搜索结果。
- 明确报告 skipped/error。
- 支持 hidden-file 和 extension filtering。
- 通过 bundled Source Han Sans font set 保持多语言 UI 稳定显示。

## Supported Languages

- English
- Korean
- Japanese
- Chinese

## Current Release

当前 repository version 是 `0.1.0`。

第一个公开 release 以 Windows app-image ZIP 分发。

- `BB-TxT-app-image-0.1.0.zip`

请从 GitHub Releases 下载 release artifact。

Gradle build 也支持 Windows installer-style EXE package。但当前 build machine 仍需要安装 WiX Toolset，才能稳定生成 EXE packaging。

## Local Development

在 repository root 运行应用：

```powershell
.\gradlew.bat run
```

运行 search verification 和 Swing smoke checks：

```powershell
.\gradlew.bat check
```

也可以单独运行：

```powershell
.\gradlew.bat runVerification
.\gradlew.bat runSmokeCheck
```

## Build and Packaging

构建 packaging-ready jar：

```powershell
.\gradlew.bat jar
```

构建 Windows app-image：

```powershell
.\gradlew.bat packageAppImage
.\gradlew.bat releaseAppImageZip
```

当 packaging machine 安装了 WiX Toolset 时，构建 Windows installer EXE：

```powershell
.\gradlew.bat packageExe
.\gradlew.bat releaseExe
```

同时构建 jar 和 release artifacts：

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

- BB TxT 专注 phase-1 developer text search；PDF 和 Office documents 不属于核心默认流程。
- 应用 bundle Source Han Sans fonts，以保持 English、Korean、Japanese 和 Chinese UI 稳定渲染。
- Gradle packaging tasks 面向 Windows。
- Release artifacts 应上传到 GitHub Releases，不应 commit 到 source tree。

## Demo Walkthrough

demo flow 输入 folder path 和 search term，然后在 readable text files 中找到 matching lines。

1. 启动应用。
2. 输入要搜索的 root folder。
3. 输入 `apple` 等关键词。
4. 点击 `Search`。
5. 在 results table 中查看 file name、line number 和 matched text。

第一屏显示 root folder、search term 和 search options。

![Java explore start](../demo-screenshots/java-explore-flow-01-open.png)

输入 folder path 和 `apple` 等 keyword，然后点击右上角的 `Search` button。

![Folder and keyword entered](../demo-screenshots/java-explore-flow-02-input-filled.png)

搜索完成后，results table 会显示 matched file path、line number 和 text preview。

![Search result table](../demo-screenshots/java-explore-flow-03-search-results.png)
