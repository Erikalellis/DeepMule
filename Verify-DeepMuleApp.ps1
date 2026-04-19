# Verificacao do DeepMule App
# cd C:\Users\robso\AndroidStudioProjects\DeepMule
# & ".\Verify-DeepMuleApp.ps1"

Write-Host ""
Write-Host "================================" -ForegroundColor Cyan
Write-Host "DeepMule App - Verificacao" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""

# 1. Build
Write-Host "[1] Compilando aplicacao (Debug)..." -ForegroundColor Yellow
& .\gradlew.bat assembleDebug 2>&1 | Select-Object -Last 5
Write-Host ""

# 2. APK Size
Write-Host "[2] Tamanho do APK..." -ForegroundColor Yellow
$apkPath = "app\build\outputs\apk\debug\app-debug.apk"
if (Test-Path $apkPath) {
    $apkSize = (Get-Item $apkPath).Length
    $apkSizeMB = "{0:N2}" -f ($apkSize / 1MB)
    Write-Host "APK encontrado: $apkSizeMB MB" -ForegroundColor Green
}
else {
    Write-Host "APK nao encontrado" -ForegroundColor Red
}
Write-Host ""

# 3. Localizacoes
Write-Host "[3] Verificando localizacoes pt-BR..." -ForegroundColor Yellow
$ptBRFile = "app\src\main\res\values-pt-rBR\strings.xml"
if (Test-Path $ptBRFile) {
    $count = (Get-Content $ptBRFile | Select-String '<string' | Measure-Object).Count
    Write-Host "Traducao pt-BR: $count strings" -ForegroundColor Green
}
else {
    Write-Host "Arquivo de traducao nao encontrado" -ForegroundColor Yellow
}
Write-Host ""

# 4. Material 3
Write-Host "[4] Verificando Material Design 3..." -ForegroundColor Yellow
$buildFile = Get-Content "app\build.gradle.kts" -Raw
if ($buildFile -match "material3") {
    Write-Host "Material 3: Configurado" -ForegroundColor Green
}
Write-Host ""

# 5. Resumo
Write-Host "================================" -ForegroundColor Green
Write-Host "Verificacao Concluida!" -ForegroundColor Green
Write-Host "================================" -ForegroundColor Green
Write-Host ""
Write-Host "Proximos passos:" -ForegroundColor Cyan
Write-Host "  1. Revisar: .agent\IMPROVEMENTS_AND_VERIFICATION.md"
Write-Host "  2. Testar em emulador/dispositivo"
Write-Host "  3. Implementar testes unitarios"
Write-Host ""

