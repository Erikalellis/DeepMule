# Paridade funcional (referencia externa)

Este documento define "igual na pratica" por comportamento, sem copia de codigo.

## Meta de paridade minima jogavel

- Abrir ROM compativel e iniciar emulacao real.
- Audio estavel e video em tempo real.
- Input touch e gamepad funcional.
- Save/Load state local funcional.
- Retomar sessao sem crash.

## Estado atual do DeepMule

- UI e fluxo principal: pronto.
- Scanner de ROMs: pronto.
- Lista de sistemas suportados: pronto.
- Ponte de emulacao nativa (`LibretroDroid`): mock (pendente).

## Milestones tecnicos

1. Runtime nativo
   - Substituir mock por JNI real em `app/src/main/java/com/example/deepmule/emu/LibretroDroid.kt`.
   - Implementar carregamento de core, ROM, frame step e estados.

2. ABIs e empacotamento
   - Comecar com `arm64-v8a`.
   - Garantir build e instalacao em aparelho real.

3. Pipeline de emulacao
   - Conectar render/superficie real na `EmulationScreen`.
   - Integrar audio de baixa latencia.
   - Integrar input touch + gamepad.

4. Saves e BIOS
   - Validar BIOS por sistema antes do boot.
   - Garantir `LocalSaveProvider` com arquivo real gerado pela emulacao.

5. Qualidade
   - Smoke UI instrumentation.
   - Testes unitarios de mapeamento de sistemas/extensoes.
   - Teste em aparelho por sistema alvo.

## Criterio de pronto por sistema

- Boot OK
- Audio OK
- Input OK
- Save/Load OK
- 10 minutos sem crash

