# 🇧🇷 DeepMule - Referência Rápida pt-BR

**Status:** ✅ PRONTO - 100% Português Brasil

---

## ⚡ Comando de Build/Deploy

```powershell
cd C:\Users\robso\AndroidStudioProjects\DeepMule

# Compilar Debug (pt-BR ativado automaticamente)
.\gradlew.bat assembleDebug

# Compilar Release (otimizado, R8/ProGuard ativado)
.\gradlew.bat assembleRelease

# Instalar em device/emulador
.\gradlew.bat installDebug

# Rodar todos os testes
.\gradlew.bat testDebugUnitTest

# Validação completa (recomendado antes de release)
.\gradlew.bat assembleDebug assembleRelease testDebugUnitTest lint
```

---

## 📥 Artefatos

| Tipo | Caminho | Tamanho |
|------|---------|---------|
| Debug APK | `app/build/outputs/apk/debug/app-debug.apk` | 25.38 MB |
| Release APK | `app/build/outputs/apk/release/app-release-unsigned.apk` | 2.37 MB |
| Lint Report | `app/build/reports/lint-results-debug.html` | HTML |

---

## 🗂️ Arquivos de Localização

| Arquivo | Propósito |
|---------|----------|
| `app/src/main/res/values/strings.xml` | Strings base em pt-BR (19 chaves) |
| `app/src/main/res/values-pt-rBR/strings.xml` | Redundância pt-BR |
| `app/src/main/java/.../DeepMuleApplication.kt` | Force locale pt-BR em runtime |

---

## 🎮 Testar no Aparelho

### 1. Instalar
```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### 2. Abrir
- Todos os textos em **português brasileiro**
- Mesmo que device esteja em EN/ES/FR

### 3. Validar Strings Principais
- ✅ "Biblioteca" (tela inicial)
- ✅ "Escanear" (botão FAB)
- ✅ "Emulando: [Jogo]" (tela de emulação)
- ✅ "Sair", "Pausar", "Retomar"
- ✅ "Configurações", "Criações"

---

## 📝 Política de Idioma (Atual)

1. O app e exclusivo em `pt-BR` nesta fase
2. Nao criar pastas `values-XX/` por enquanto
3. Novos textos devem seguir o padrao de escrita pt-BR
4. Build/test normalmente com foco em consistencia de linguagem

---

## 📚 Documentação

- **Guia Completo:** `.agent/LOCALIZACAO_PT-BR_COMPLETA.md`
- **Resumo Executivo:** `RESUMO_FINAL_PT-BR.md` (este arquivo)
- **Quick Start:** `QUICKSTART.md`
- **Política de Idioma:** `.agent/README.md`

---

## ✅ Checklist Pré-Release

- [ ] Build Debug passa
- [ ] Build Release passa
- [ ] Testes unitários passam
- [ ] Lint sem problemas
- [ ] Testado em device/emulador
- [ ] Todos textos em português
- [ ] Locale força pt-BR mesmo mudando idioma do device
- [ ] APK Release assinado (se publicando em Play Store)

---

## 📞 Suporte Rápido

**P: O app está em português?**  
A: Sim, 100%. Abra e veja.

**P: E se eu mudar idioma do device para inglês?**  
A: O app continua em português (força aplicada em `DeepMuleApplication`).

**P: Como adicionar novo idioma?**  
A: No escopo atual, nao adicionamos novos idiomas. O app permanece 100% pt-BR.

**P: Posso publicar na Play Store assim?**  
A: Sim, com idioma unico em pt-BR, conforme a politica atual do projeto.

---

**Última atualização:** 2026-04-19  
**Versão:** 1.0  
**Idioma Primário:** Português Brasil (pt-BR)  
**Status Localização:** ✅ 100% COMPLETO

