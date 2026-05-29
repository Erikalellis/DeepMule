# DeepMule - Checklist de Teste Fisico Final

Data: 2026-04-22
Dispositivo: Motorola Edge 30 Neo
Pacote: com.example.deepmule

## Pre-check tecnico (concluido)

- [x] Provision BIOS + cores + extras no dispositivo
- [x] ROMs classicas provisionadas (5 por sistema)
- [x] Auditoria automatica: PASS 39 / WARN 1 / FAIL 0
- [x] Estrutura `files/retroarch` presente (assets, autoconfig, cheats, config, database, info, thumbnails)

## ROMs por sistema (conferido)

- [x] arcade = 5
- [x] atari2600 = 5
- [x] gba = 5
- [x] genesis = 5
- [x] n64 = 5
- [x] nes = 5
- [x] pce = 5
- [x] sms = 5
- [x] snes = 5

## Execucao funcional por sistema

Para cada sistema abaixo, validar na ordem:
1) Abrir ROM
2) Confirmar boot sem erro de core/BIOS
3) Testar direcional + A/B + Start/Select
4) Abrir menu RetroArch e voltar ao jogo
5) Save state e load state
6) Sair e reabrir a mesma ROM

### arcade
- [ ] Boot
- [ ] Input
- [ ] Save/Load
- [ ] Reopen

### atari2600
- [ ] Boot
- [ ] Input
- [ ] Save/Load
- [ ] Reopen

### gba
- [ ] Boot
- [ ] Input
- [ ] Save/Load
- [ ] Reopen

### genesis
- [ ] Boot
- [ ] Input
- [ ] Save/Load
- [ ] Reopen

### n64
- [ ] Boot
- [ ] Input
- [ ] Save/Load
- [ ] Reopen

### nes
- [ ] Boot
- [ ] Input
- [ ] Save/Load
- [ ] Reopen

### pce
- [ ] Boot
- [ ] Input
- [ ] Save/Load
- [ ] Reopen

### sms
- [ ] Boot
- [ ] Input
- [ ] Save/Load
- [ ] Reopen

### snes
- [ ] Boot
- [ ] Input
- [ ] Save/Load
- [ ] Reopen

## Criterio de aprovacao

- Aprovado: nenhum bloqueio de boot/core/BIOS e input funcional em todos os 9 sistemas.
- Aceitavel: 1 WARN de perfil parcial de controle (ja conhecido na auditoria automatica).
- Reprovar: qualquer FAIL de boot, perda total de input, ou save/load quebrado.

