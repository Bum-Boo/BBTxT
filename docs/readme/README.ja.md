# BB TxT

> Fast local text search for source code, logs, configs, and readable text files.

[Overview](../../README.md) | [English](README.en.md) | [한국어](README.ko.md) | [中文](README.zh-CN.md) | [日本語](README.ja.md)

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
