# BB TxT

> Fast local text search for source code, logs, configs, and readable text files.

[Overview](../../README.md) | [English](README.en.md) | [한국어](README.ko.md) | [中文](README.zh-CN.md) | [日本語](README.ja.md)

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
