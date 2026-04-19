# Checklist de execucao (hoje)

## Bloco A - Higiene do projeto

- [x] Corrigir `QUICKSTART.md` (idioma e comandos de instalacao)
- [ ] Consolidar politica pt-BR no projeto completo, projeto 100% portugues brasil 
- [x] Registrar baseline clean-room (`docs/LEGAL_SAFE_BASELINE.md`)

## Bloco B - Build e validacao

- [x] `:app:assembleDebug`
- [ ] `lint`
- [x] `:app:testDebugUnitTest`
- [x] `:app:assembleDebugAndroidTest`

## Bloco C - Aparelho real

- [ ] `adb devices` com device `device` (nao `offline`)
- [ ] Instalar `app-debug.apk`
- [ ] Executar `:app:connectedDebugAndroidTest`
- [ ] Validar fluxo: abrir pasta -> listar ROM -> abrir tela de emulacao

## Bloco D - Paridade tecnica (proxima fase)

- [x] Substituir mock `LibretroDroid` por JNI real
- [x] Definir ABI inicial (`arm64-v8a`)
- [x] Integrar save state real local
- [x] Integrar pipeline de inicializacao `loadCore -> loadGame`
- [x] Integrar superficie real (`SurfaceView`) na tela de emulacao
- [ ] Rodar matriz de validacao por sistema

