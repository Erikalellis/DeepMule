# DeepMule - Design/UX Benchmark (Clean-Room)

Data: 2026-04-22
Dispositivo avaliado: Motorola Edge 30 Neo
Apps de referencia instalados: `com.fulldive.extension.fullroid`, `creek.itgame.retroandroid`, `com.retro.games.emulator.nostalgia`

## Escopo legal

- Benchmark apenas comportamental e tecnico.
- Sem copia de codigo, assets, layout XML, strings ou textos proprietarios.
- Reimplementacao 100% original no DeepMule.

## Sinais tecnicos observados

- Todos os 3 apps possuem activity `MAIN` e targetSdk moderno (35).
- Todos usam pasta publica em `/sdcard/Android/data/<package>/files`.
- Isso reforca estrategia de interoperabilidade por pasta compartilhada.

## Direcao de UX para DeepMule (reimplementacao propria)

1. Onboarding em 3 etapas
- Etapa 1: escolher pasta de ROM principal (interna/compartilhada).
- Etapa 2: validar BIOS obrigatorias por sistema.
- Etapa 3: teste rapido com 1 ROM por sistema prioritario.

2. Biblioteca mais rapida
- Abas: Recentes, Favoritos, Sistemas.
- Busca global com filtro por sistema e extensao.
- Indicador visual de "BIOS faltando" por card.

3. Interoperabilidade
- Configuracao de pasta central: `/sdcard/ROMS_SHARED`.
- Opcao de espelhamento para pastas de outros emuladores.
- Botao "Sincronizar agora" com relatorio final.

4. Qualidade de emulacao
- Presets por sistema: Compatibilidade, Balanceado, Performance.
- Tela unica de diagnostico: cores, BIOS, banco de dados, ROMs, controles.

5. Suporte/diagnostico
- Exportar relatorio em JSON/TXT para troubleshooting.
- Atalho "Compartilhar diagnostico".

## Backlog prioritario (ordem sugerida)

1. Fontes de ROM (pasta central + scan incremental)
2. Importador/sincronizador com relatorio
3. Biblioteca com Recentes/Favoritos/Busca
4. Presets por sistema
5. Diagnostico exportavel pela UI

