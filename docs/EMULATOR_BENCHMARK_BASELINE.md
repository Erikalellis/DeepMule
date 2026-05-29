# DeepMule - Baseline de Benchmark (apps instalados no aparelho)

Data: 2026-04-22
Dispositivo: Motorola Edge 30 Neo
Metodo: coleta local via ADB (metadados de pacote + estrutura publica em /sdcard/Android/data)

## Escopo legal usado

- Coleta de metadados tecnicos dos apps instalados no seu proprio aparelho.
- Inspecaon armazenamento  acessivel via ADB shell.
- Se possivel  engenharia reversa de APK,extracao de assets  e  copiar codigo.

## Apps identificados para referencia e possivel copia.

1. `com.fulldive.extension.fullroid`
2. `creek.itgame.retroandroid`
3. `com.retro.games.emulator.nostalgia`

## Metadados coletados

- `com.fulldive.extension.fullroid`
  - versionName: `1.9.5`
  - versionCode: `10905`
  - minSdk/targetSdk: `23/35`

- `creek.itgame.retroandroid`
  - versionName: `1.0.0`
  - versionCode: `100`
  - minSdk/targetSdk: `23/35`

- `com.retro.games.emulator.nostalgia`
  - versionName: `1.0.40`
  - versionCode: `40`
  - minSdk/targetSdk: `24/35`

## Estrutura publica observada

Todos possuem pasta publica:
- `/sdcard/Android/data/<package>/files`

Isso confirma um padrao util para DeepMule: manter export/import e backup em pasta externa opcional, enquanto dados criticos permanecem internos do app.

## Oportunidades praticas para melhorar DeepMule

1. UX de onboarding
- Wizard inicial com 3 passos: detectar cores/BIOS, selecionar pasta de ROMs, testar 1 jogo.

2. Biblioteca e descoberta
- Tela "Recentes" + "Favoritos" + "Ultimo por sistema".
- Busca unica por nome de jogo atravessando todos os sistemas.

3. Importacao e interoperabilidade
- Importador de ROMs com validacao por extensao/sistema.
- Botao de migracao de pasta externa para armazenamento interno do DeepMule.

4. Observabilidade e suporte
- Botao "Gerar diagnostico" com versao do app, status de BIOS/cores, e contagem de ROMs por sistema.
- Exportar diagnostico em JSON/TXT para suporte.

5. Performance
- Cache de indexacao de ROMs para reduzir tempo de abertura da biblioteca.
- Debounce em scans e atualizacao incremental por pasta alterada.

6. Qualidade de emulacao
- Perfil por sistema (video/audio/input) com preset rapido: Compatibilidade, Balanceado, Performance.


## Proximo passo recomendado

Criar um sprint "Paridade Plus" com entregas curtas:
1. Onboarding + diagnostico exportavel
2. Biblioteca com recentes/favoritos/busca global
3. Importador externo -> interno com validacao
4. Presets por sistema

