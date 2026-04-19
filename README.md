# 🎮 DeepMule

> Emulador retrô moderno para Android, 100% em Português Brasil (pt-BR)

[![Build](https://github.com/Erikalellis/DeepMule/actions/workflows/build.yml/badge.svg)](https://github.com/Erikalellis/DeepMule/actions/workflows/build.yml)
[![Release](https://img.shields.io/github/v/release/Erikalellis/DeepMule)](https://github.com/Erikalellis/DeepMule/releases)
[![Android](https://img.shields.io/badge/Android-7.0%2B-green)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.x-blue)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/Licença-MIT-yellow)](LICENSE)

---

## 📱 Sobre

**DeepMule** é um emulador de jogos retrô para Android, desenvolvido com as tecnologias mais modernas do ecossistema Android:

- 🎨 **Material Design 3** — Interface moderna e bonita
- 🇧🇷 **Português Brasil** — 100% localizado em pt-BR
- 🏗️ **Jetpack Compose** — Interface declarativa
- 🏛️ **Arquitetura MVVM** — Código organizado e testável
- ☁️ **Saves Plugáveis** — Local agora, nuvem no futuro

---

## 🎮 Sistemas Suportados

> 23 sistemas | Mesma arquitetura de cores do [Lemuroid](https://github.com/Swordfish90/Lemuroid) | 100% pt-BR

### Atari
| Sistema | Extensões | Core Libretro | BIOS |
|---------|-----------|---------------|------|
| Atari 2600 (A26) | `.a26` `.bin` `.rom` | `stella` | ❌ |
| Atari 7800 (A78) | `.a78` | `prosystem` | ❌ |
| Atari Lynx | `.lnx` `.lynx` | `handy` | ✅ `lynxboot.img` |

### Nintendo — Portátil
| Sistema | Extensões | Core Libretro | BIOS |
|---------|-----------|---------------|------|
| Game Boy (GB) | `.gb` | `gambatte` | ❌ |
| Game Boy Color (GBC) | `.gbc` | `gambatte` | ❌ |
| Game Boy Advance (GBA) | `.gba` | `mgba` | ❌ |
| Nintendo DS (NDS) | `.nds` | `melonds` | ❌ |
| Nintendo 3DS | `.3ds` `.3dsx` `.cia` | `citra` | ❌ |

### Nintendo — Console
| Sistema | Extensões | Core Libretro | BIOS |
|---------|-----------|---------------|------|
| Nintendo (NES) | `.nes` | `fceumm` | ❌ |
| Super Nintendo (SNES) | `.sfc` `.smc` | `snes9x` | ❌ |
| Nintendo 64 (N64) | `.n64` `.z64` `.v64` | `mupen64plus_next` | ❌ |

### Sega
| Sistema | Extensões | Core Libretro | BIOS |
|---------|-----------|---------------|------|
| Sega Master System (SMS) | `.sms` | `genesis_plus_gx` | ❌ |
| Sega Game Gear (GG) | `.gg` | `genesis_plus_gx` | ❌ |
| Sega Genesis / Mega Drive | `.md` `.gen` `.smd` | `genesis_plus_gx` | ❌ |
| Sega CD / Mega CD | `.cue` `.iso` `.chd` | `genesis_plus_gx` | ✅ `bios_CD_U.bin` |

### Sony
| Sistema | Extensões | Core Libretro | BIOS |
|---------|-----------|---------------|------|
| PlayStation (PSX) | `.cue` `.iso` `.pbp` `.chd` | `pcsx_rearmed` | ✅ `scph1001.bin` |
| PlayStation Portable (PSP) | `.iso` `.cso` `.pbp` | `ppsspp` | ❌ |

### NEC / SNK / Bandai / Arcade
| Sistema | Extensões | Core Libretro | BIOS |
|---------|-----------|---------------|------|
| PC Engine / TurboGrafx-16 (PCE) | `.pce` `.cue` `.chd` | `beetle_pce_fast` | ❌ |
| Neo Geo Pocket (NGP) | `.ngp` | `mednafen_ngp` | ❌ |
| Neo Geo Pocket Color (NGPC) | `.ngc` `.ngpc` | `mednafen_ngp` | ❌ |
| WonderSwan (WS) | `.ws` | `beetle_wswan` | ❌ |
| WonderSwan Color (WSC) | `.wsc` | `beetle_wswan` | ❌ |
| Arcade — FinalBurn Neo | `.zip` `.7z` | `fbneo` | ❌ |

### ⚠️ Sistemas que precisam de BIOS

Coloque os arquivos na pasta `DeepMule/bios/` do armazenamento interno:

| Sistema | Arquivo esperado |
|---------|-----------------|
| Atari Lynx | `lynxboot.img` |
| Sega CD / Mega CD | `bios_CD_U.bin` |
| PlayStation (PSX) | `scph1001.bin` |

### ✅ Status de validação por sistema

| Sistema | Scanner | UI | Teste unitário | Em aparelho |
|---------|---------|-----|----------------|-------------|
| Atari 2600 | ✅ | ✅ | ✅ | 🧪 |
| Atari 7800 | ✅ | ✅ | ✅ | 🧪 |
| Atari Lynx | ✅ | ✅ | ✅ | 🧪 |
| Game Boy | ✅ | ✅ | ✅ | 🧪 |
| Game Boy Color | ✅ | ✅ | ✅ | 🧪 |
| Game Boy Advance | ✅ | ✅ | ✅ | 🧪 |
| Nintendo DS | ✅ | ✅ | ✅ | 🧪 |
| Nintendo 3DS | ✅ | ✅ | ✅ | 🧪 |
| NES | ✅ | ✅ | ✅ | 🧪 |
| SNES | ✅ | ✅ | ✅ | 🧪 |
| Nintendo 64 | ✅ | ✅ | ✅ | 🧪 |
| Sega Master System | ✅ | ✅ | ✅ | 🧪 |
| Sega Game Gear | ✅ | ✅ | ✅ | 🧪 |
| Sega Genesis | ✅ | ✅ | ✅ | 🧪 |
| Sega CD | ✅ | ✅ | ✅ | 🧪 |
| PlayStation | ✅ | ✅ | ✅ | 🧪 |
| PSP | ✅ | ✅ | ✅ | 🧪 |
| PC Engine | ✅ | ✅ | ✅ | 🧪 |
| Neo Geo Pocket | ✅ | ✅ | ✅ | 🧪 |
| Neo Geo Pocket Color | ✅ | ✅ | ✅ | 🧪 |
| WonderSwan | ✅ | ✅ | ✅ | 🧪 |
| WonderSwan Color | ✅ | ✅ | ✅ | 🧪 |
| Arcade (FBNeo) | ✅ | ✅ | ✅ | 🧪 |

`✅` implementado e testado automaticamente · `🧪` teste prático pendente em aparelho

---

## 🚀 Instalação

### Baixar APK (mais fácil)
1. Acesse [Releases](https://github.com/Erikalellis/DeepMule/releases)
2. Baixe o arquivo `.apk` mais recente
3. Ative **"Instalar de fontes desconhecidas"** no Android
4. Instale o APK

### Compilar do fonte
```bash
git clone https://github.com/Erikalellis/DeepMule.git
cd DeepMule
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 🛠️ Tecnologias

```
Kotlin 2.x             — Linguagem principal
Jetpack Compose        — Interface de usuário
Material Design 3      — Design system
Room (SQLite)          — Banco de dados local
Retrofit + OkHttp      — Requisições HTTP
Moshi                  — Serialização JSON
DataStore              — Preferências persistentes
Coroutines + Flow      — Programação assíncrona
Coil                   — Carregamento de imagens
Libretro               — Núcleos de emulação
```

---

## 🏗️ Arquitetura

```
app/
├── analytics/          # Reporter de crashes → GitHub Issues
├── data/
│   ├── storage/        # Sistema de saves plugável
│   │   ├── SaveProvider.kt          (interface)
│   │   ├── SaveProviderFactory.kt   (factory)
│   │   ├── SaveStorageManager.kt    (gerenciador)
│   │   └── providers/
│   │       ├── LocalSaveProvider.kt
│   │       └── RestSaveProvider.kt
│   ├── GameEntity.kt
│   ├── GameDao.kt
│   ├── GameDatabase.kt
│   ├── GameRepository.kt
│   ├── GameScanner.kt
│   └── SystemConfig.kt
├── emu/
│   └── EmulationViewModel.kt
└── ui/
    ├── LibraryViewModel.kt
    ├── EmulationScreen.kt
    └── theme/
```

---

## 💾 Sistema de Saves

O DeepMule usa uma arquitetura **plugável** para saves:

- ✅ **Local** — Salvo no próprio aparelho (padrão atual)
- 🔜 **REST API** — Backend customizado (em desenvolvimento)
- 🔜 **Google Drive** — Estrutura pronta
- 🔜 **Dropbox** — Estrutura pronta

---

## 🤖 CI/CD Automático

| Workflow | Disparo | O que faz |
|---------|---------|-----------|
| `build.yml` | Push/PR | Build, testes, lint, APK Debug |
| `release.yml` | Tag `v*.*.*` | APK Release + GitHub Release |
| `analytics.yml` | Toda segunda | Relatório semanal de métricas |

---

## 📊 Análises Automáticas

O app reporta automaticamente:
- 💥 Crashes não tratados → Issues GitHub com label `crash`
- 📊 Relatório semanal → Issue automático toda segunda-feira

---

## 🧪 Testes

```bash
# Testes unitários
./gradlew testDebugUnitTest

# Testes instrumentados (precisa de emulador/device)
./gradlew connectedDebugAndroidTest

# Lint
./gradlew lint
```

Ultima validacao automatica local:
- `assembleDebug` ✅
- `testDebugUnitTest` ✅
- `lint` ✅

---

## 📋 Roadmap

- [x] Biblioteca de jogos (grid/lista)
- [x] Escaneamento de ROMs
- [x] Tela de emulação
- [x] Save States locais
- [x] 100% Português Brasil (pt-BR)
- [x] CI/CD com GitHub Actions
- [x] Reporter de crashes automático → GitHub Issues
- [x] **23 sistemas suportados** (Atari, Nintendo, Sega, Sony, NEC, SNK, Bandai, Arcade)
- [x] BIOS obrigatória sinalizada por sistema
- [x] Testes unitários para todos os 23 sistemas
- [ ] Validação de emulação em aparelho (todos os 23 sistemas)
- [ ] Tela de Configurações completa
- [ ] Seletor de BIOS na UI
- [ ] Backend REST para sync de saves na nuvem
- [ ] Box art automático (scraping por sistema)
- [ ] Suporte a controle Bluetooth / gamepad
- [ ] Conquistas por jogo

---

## 🤝 Contribuindo

1. Fork o projeto
2. Crie sua branch: `git checkout -b feature/minha-feature`
3. Commit: `git commit -m 'feat: adiciona minha feature'`
4. Push: `git push origin feature/minha-feature`
5. Abra um Pull Request

---

## 📄 Licença

MIT © 2026 Erikalellis

---

## 📞 Contato

Encontrou um bug? [Abra uma Issue](https://github.com/Erikalellis/DeepMule/issues/new)

---

*Feito com ❤️ em Português Brasil 🇧🇷*

