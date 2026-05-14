# BB TxT

> Fast local text search for source code, logs, configs, and readable text files.

[Overview](../../README.md) | [English](README.en.md) | [한국어](README.ko.md) | [中文](README.zh-CN.md) | [日本語](README.ja.md)

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
