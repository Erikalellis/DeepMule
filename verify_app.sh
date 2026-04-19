#!/bin/bash
# Script de Verificação do DeepMule App
# Localização: C:\Users\robso\AndroidStudioProjects\DeepMule\verify_app.sh

echo "================================"
echo "DeepMule App - Verificação Completa"
echo "================================"
echo ""

# Cores para output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 1. Verificar Build
echo -e "${YELLOW}[1] Compilando aplicação (Debug)...${NC}"
./gradlew assembleDebug > /tmp/build.log 2>&1
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Build bem-sucedido${NC}"
else
    echo -e "${RED}✗ Build falhou${NC}"
    echo "Verifique /tmp/build.log"
    exit 1
fi

# 2. Lint (Análise Estática)
echo ""
echo -e "${YELLOW}[2] Executando análise estática (Lint)...${NC}"
./gradlew lint > /tmp/lint.log 2>&1
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Lint passou${NC}"
    echo "Relatório: app/build/reports/lint-results-debug.html"
else
    echo -e "${YELLOW}⚠ Lint encontrou problemas${NC}"
fi

# 3. Testes Unitários
echo ""
echo -e "${YELLOW}[3] Executando testes unitários...${NC}"
./gradlew testDebugUnitTest > /tmp/tests.log 2>&1
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Testes passaram${NC}"
else
    echo -e "${YELLOW}⚠ Sem testes ou testes falharam${NC}"
fi

# 4. Informações do APK
echo ""
echo -e "${YELLOW}[4] Informações do APK...${NC}"
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
    echo -e "${GREEN}✓ APK encontrado${NC}"
    echo "  - Caminho: $APK_PATH"
    echo "  - Tamanho: $APK_SIZE"
else
    echo -e "${RED}✗ APK não encontrado${NC}"
fi

# 5. Verificar localizações
echo ""
echo -e "${YELLOW}[5] Verificando localizações (pt-BR)...${NC}"
if grep -q "Biblioteca" app/src/main/res/values-pt-rBR/strings.xml 2>/dev/null; then
    echo -e "${GREEN}✓ Tradução pt-BR detectada${NC}"
    TRANSLATIONS=$(grep -c '<string' app/src/main/res/values-pt-rBR/strings.xml)
    echo "  - Strings traduzidas: $TRANSLATIONS"
else
    echo -e "${YELLOW}⚠ Tradução pt-BR não encontrada${NC}"
fi

# 6. Verificar Material Design 3
echo ""
echo -e "${YELLOW}[6] Verificando Material Design 3...${NC}"
if grep -q "material3" app/build.gradle.kts; then
    echo -e "${GREEN}✓ Material 3 configurado${NC}"
else
    echo -e "${YELLOW}⚠ Material 3 não encontrado${NC}"
fi

# 7. Resumo final
echo ""
echo "================================"
echo -e "${GREEN}Verificação concluída!${NC}"
echo "================================"
echo ""
echo "Próximos passos recomendados:"
echo "1. Testar em emulador: adb install -r $APK_PATH"
echo "2. Verificar relatório Lint completo"
echo "3. Implementar testes unitários"
echo ""

