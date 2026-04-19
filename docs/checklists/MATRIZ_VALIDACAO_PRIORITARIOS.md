# Matriz de validacao - sistemas completos

Objetivo: validar em aparelho fisico arm64 todos os 23 sistemas suportados.

## Criterio de aprovado por sistema

- Boot ate gameplay em no maximo 30s
- Renderizacao continua (sem tela preta persistente)
- Audio utilizavel
- Input (toque e/ou controle) responsivo
- Save/Load slot 1 funcional
- 10 minutos sem crash

## Preparacao obrigatoria

1. Instalar APK debug no aparelho
2. Copiar ROMs de teste para pasta acessivel pelo app
3. Provisionar cores em `filesDir/cores/`
4. Para PSX, provisionar BIOS em `filesDir/bios/scph1001.bin`

## Sistemas suportados (validacao pratica)

| Sistema | Core | BIOS | Boot | Video | Audio | Input | Save/Load | 10 min estavel | Status |
|--------|------|------|------|-------|-------|-------|-----------|----------------|--------|
| Atari 2600 (A26) | `stella.so` | Nao | Pendente | Pendente | Pendente | Pendente | Pendente | Pendente | Pendente |
| Atari 7800 (A78) | `prosystem.so` | Nao | Pendente | Pendente | Pendente | Pendente | Pendente | Pendente | Pendente |
| Atari Lynx | `handy.so` | `lynxboot.img` | Pendente | Pendente | Pendente | Pendente | Pendente | Pendente | Pendente |
| Nintendo (NES) | `fceumm.so` | Nao | Pendente | Pendente | Pendente | Pendente | Pendente | Pendente | Pendente |
| Super Nintendo (SNES) | `snes9x.so` | Nao | Pendente | Pendente | Pendente | Pendente | Pendente | Pendente | Pendente |
| Game Boy (GB) | `gambatte.so` | Nao | Pendente | Pendente | Pendente | Pendente | Pendente | Pendente | Pendente |
| Game Boy Color (GBC) | `gambatte.so` | Nao | Pendente | Pendente | Pendente | Pendente | Pendente | Pendente | Pendente |
| Game Boy Advance (GBA) | `mgba.so` | Nao | Pendente | Pendente | Pendente | Pendente | Pendente | Pendente | Pendente |
| Sega Genesis / Mega Drive | `genesis_plus_gx.so` | Nao | Pendente | Pendente | Pendente | Pendente | Pendente | Pendente | Pendente |
| Sega CD / Mega CD | `genesis_plus_gx.so` | `bios_CD_U.bin` | Pendente | Pendente | Pendente | Pendente | Pendente | Pendente | Pendente |
| Sega Master System (SMS) | `genesis_plus_gx.so` | Nao | Pendente | Pendente | Pendente | Pendente | Pendente | Pendente | Pendente |
| Sega Game Gear (GG) | `genesis_plus_gx.so` | Nao | Pendente | Pendente | Pendente | Pendente | Pendente | Pendente | Pendente |
| Nintendo 64 (N64) | `mupen64plus_next.so` | Nao | Pendente | Pendente | Pendente | Pendente | Pendente | Pendente | Pendente |
| PlayStation (PSX) | `pcsx_rearmed.so` | `scph1001.bin` | Pendente | Pendente | Pendente | Pendente | Pendente | Pendente | Pendente |
| PlayStation Portable (PSP) | `ppsspp.so` | Nao | Pendente | Pendente | Pendente | Pendente | Pendente | Pendente | Pendente |
| FinalBurn Neo (Arcade) | `fbneo.so` | Nao | Pendente | Pendente | Pendente | Pendente | Pendente | Pendente | Pendente |
| Nintendo DS (NDS) | `melonds.so` / `desmume.so` | Nao | Pendente | Pendente | Pendente | Pendente | Pendente | Pendente | Pendente |
| PC Engine (PCE) | `beetle_pce_fast.so` | Nao | Pendente | Pendente | Pendente | Pendente | Pendente | Pendente | Pendente |
| Neo Geo Pocket (NGP) | `mednafen_ngp.so` | Nao | Pendente | Pendente | Pendente | Pendente | Pendente | Pendente | Pendente |
| Neo Geo Pocket Color (NGPC) | `mednafen_ngp.so` | Nao | Pendente | Pendente | Pendente | Pendente | Pendente | Pendente | Pendente |
| WonderSwan (WS) | `beetle_wswan.so` / `beetle_cygne.so` | Nao | Pendente | Pendente | Pendente | Pendente | Pendente | Pendente | Pendente |
| WonderSwan Color (WSC) | `beetle_wswan.so` / `beetle_cygne.so` | Nao | Pendente | Pendente | Pendente | Pendente | Pendente | Pendente | Pendente |
| Nintendo 3DS (3DS) | `citra.so` | Nao | Pendente | Pendente | Pendente | Pendente | Pendente | Pendente | Pendente |

## Registro de execucao

- Data:
- Dispositivo:
- Android:
- Build (`git commit`):
- Observacoes:

