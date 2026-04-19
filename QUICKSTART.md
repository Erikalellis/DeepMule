# DeepMule Quickstart

Estado verificado em 2026-04-19.

## Snapshot atual

- Release APK (`app-release-unsigned.apk`): `2,478,687 bytes` (~`2.36 MB`)
- Lint: `BUILD SUCCESSFUL`
- Unit tests (`:app:testDebugUnitTest`): `BUILD SUCCESSFUL`
- Instrumentation test APK (`:app:assembleDebugAndroidTest`): `BUILD SUCCESSFUL`

## Padrao de idioma (obrigatorio)

- Idioma principal e prioritario: `Portugues (Brasil)` (`pt-BR`)
- Todas as telas, textos e revisoes devem considerar `pt-BR` em primeira instancia
- Demais idiomas sao secundarios e devem sempre derivar do padrao `pt-BR`

## Instalar no celular/tablet (debug)

```powershell
Set-Location "C:\Users\robso\AndroidStudioProjects\DeepMule"
& "F:\Android\SDK\platform-tools\adb.exe" devices
& "F:\Android\SDK\platform-tools\adb.exe" install -r ".\app\build\outputs\apk\debug\app-debug.apk"
```

## Comandos finais (uso diario)

```powershell
Set-Location "C:\Users\robso\AndroidStudioProjects\DeepMule"
.\gradlew.bat clean
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assembleRelease
.\gradlew.bat lint
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebugAndroidTest
```

## Rodar o smoke test de UI

Com emulador/dispositivo conectado:

```powershell
Set-Location "C:\Users\robso\AndroidStudioProjects\DeepMule"
.\gradlew.bat :app:connectedDebugAndroidTest
```

Sem dispositivo, voce ainda pode validar compilacao dos testes instrumentados:

```powershell
Set-Location "C:\Users\robso\AndroidStudioProjects\DeepMule"
.\gradlew.bat :app:assembleDebugAndroidTest
```

## Arquivos principais desta rodada

- `app/src/androidTest/java/com/example/deepmule/MainActivitySmokeTest.kt`
- `app/src/main/java/com/example/deepmule/MainActivity.kt`
- `app/build/reports/lint-results-debug.html`

