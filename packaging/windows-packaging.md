# Windows Packaging

BB TxT uses `jpackage` for both portable and installer-style Windows builds.

## Required icon

Packaging expects this file to exist:

```text
assets/icons/BTXTB.ico
```

If the icon is missing, the Gradle packaging task fails clearly.

## Build the jar

```powershell
.\gradlew.bat jar
```

## Portable app-image

```powershell
.\gradlew.bat packageAppImage
.\gradlew.bat releaseAppImageZip
```

Outputs:

```text
dist/app-image
release/BB-TxT-app-image-0.1.0.zip
```

## Installer exe

```powershell
.\gradlew.bat packageExe
.\gradlew.bat releaseExe
```

Outputs:

```text
dist/exe
release/BB-TxT-Setup-0.1.0.exe
```

## Release bundle

```powershell
.\gradlew.bat assembleRelease
```

This builds the jar plus both Windows release assets.
