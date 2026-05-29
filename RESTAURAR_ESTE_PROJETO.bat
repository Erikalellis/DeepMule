@echo off
title RESTAURACAO DeepMule
echo ===========================================
echo   RESTAURANDO DEPENDENCIAS: DeepMule
echo ===========================================
echo.
echo 1. Verificando PNPM...
where pnpm >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [!] PNPM nao encontrado. Instalando via NPM...
    call npm install -g pnpm
)

echo.
echo 2. Instalando bibliotecas...
call pnpm install

echo.
echo 3. Limpando Gradle...
if exist android (
    cd android && call gradlew clean && cd ..
)

echo.
echo ===========================================
echo   PROJETO PRONTO!
echo ===========================================
pause
