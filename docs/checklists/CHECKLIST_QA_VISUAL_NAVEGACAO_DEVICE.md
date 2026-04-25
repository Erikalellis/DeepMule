# Checklist QA Visual + Navegacao (Device)

Objetivo: validar a nova UX das telas `Home`, `Library`, `Details` e `Player` em aparelho real.

## 1) Preflight rapido

- [ ] Device conectado no ADB e reconhecido
- [ ] APK debug mais recente instalado
- [ ] App abre sem crash na primeira execucao

```powershell
adb devices
adb shell pm list packages | findstr /I deepmule
```

## 2) Validacao visual - Home

- [ ] Fundo com gradiente dark/roxo ocupando a tela inteira
- [ ] Header com titulo `DeepMule`, contador de jogos e icone de busca
- [ ] Linha de `Jogados recentemente` renderiza quando houver historico
- [ ] Filtros de console aparecem em linha horizontal e respondem ao toque
- [ ] Grid de capas com cards arredondados, elevacao e sem sobrepor bottom bar
- [ ] Toque em card abre `Details`

## 3) Validacao visual - Library

- [ ] Mesmo padrao visual da Home (gradiente dark e contraste legivel)
- [ ] Header com titulo `Biblioteca` e toggle de favoritos
- [ ] Campo de busca filtra por titulo em tempo real
- [ ] Filtros por sistema funcionam combinados com busca
- [ ] Grid de jogos respeita espacamentos e recorte da barra inferior
- [ ] Toque em card abre `Details`

## 4) Validacao de navegacao

- [ ] Bottom nav troca entre `Inicio`, `Biblioteca` e `Configuracoes`
- [ ] Rota `Details/{gameId}` abre jogo correto
- [ ] Botao voltar em `Details` retorna para a tela anterior
- [ ] Botao `Jogar` em `Details` abre `Player/{gameId}`
- [ ] Navegar para Home/Library e voltar preserva estado basico da tela

## 5) Validacao visual - Player (Console Mode)

- [ ] Tela full-screen com gradiente escuro (sem look branco/padrao)
- [ ] Titulo do jogo e sistema aparecem centralizados
- [ ] Painel/capa do jogo com estilo de console (card escuro)
- [ ] Caminho da ROM aparece em texto auxiliar
- [ ] Botao principal `INICIAR EMULACAO` chama o fluxo de emulacao

## 6) Smoke de fluxo completo

- [ ] Home -> Details -> Player -> iniciar emulacao
- [ ] Library -> busca -> Details -> voltar
- [ ] Favoritar/desfavoritar em Details reflete no estado ao voltar

## 7) Captura de evidencias (recomendado)

```powershell
adb shell screencap -p /sdcard/deepmule_home.png
adb shell screencap -p /sdcard/deepmule_library.png
adb shell screencap -p /sdcard/deepmule_details.png
adb shell screencap -p /sdcard/deepmule_player.png
adb pull /sdcard/deepmule_home.png .
adb pull /sdcard/deepmule_library.png .
adb pull /sdcard/deepmule_details.png .
adb pull /sdcard/deepmule_player.png .
```

## 8) Diagnostico rapido

- Home/Library sem gradiente: verificar se versao instalada e a mais recente.
- Card/capa quebrada: validar URL/path de capa no banco/repositorio.
- Navegacao nao abre Details/Player: conferir argumentos `gameId` e encoding.
- Botao de emulacao sem efeito: validar permissao/estado do `EmulationActivity`.
- UI cortada em devices pequenos: revisar paddings verticais e altura da bottom bar.

