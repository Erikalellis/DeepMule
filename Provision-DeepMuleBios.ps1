param(
    [string]$ProjectRoot = "C:\Users\robso\AndroidStudioProjects\DeepMule",
    [string]$PackageName = "com.example.deepmule",
    [string]$AdbPath = "F:\Android\SDK\platform-tools\adb.exe"
)

$ErrorActionPreference = "Stop"

$biosPack = Join-Path $ProjectRoot "bios-pack"
$requiredBios = @(
    "lynxboot.img",
    "bios_CD_U.bin",
    "scph1001.bin"
)

if (-not (Test-Path $AdbPath)) {
    throw "adb nao encontrado em: $AdbPath"
}

if (-not (Test-Path $biosPack)) {
    throw "Pasta bios-pack nao encontrada em: $biosPack"
}

foreach ($bios in $requiredBios) {
    $sourceFile = Join-Path $biosPack $bios
    if (-not (Test-Path $sourceFile)) {
        throw "BIOS obrigatoria ausente em bios-pack: $bios"
    }
}

$deviceList = & $AdbPath devices | Select-String "\tdevice$"
if (-not $deviceList) {
    throw "Nenhum dispositivo em estado 'device' encontrado no adb devices."
}

Write-Host "Dispositivo(s) conectado(s):"
$deviceList | ForEach-Object { Write-Host " - $($_.Line)" }

Write-Host "Criando pasta de BIOS interna do app..."
& $AdbPath shell "run-as $PackageName mkdir -p files/bios"

foreach ($bios in $requiredBios) {
    $sourceFile = Join-Path $biosPack $bios
    $tmpPath = "/data/local/tmp/$bios"

    Write-Host "Enviando $bios para temporario..."
    & $AdbPath push "$sourceFile" "$tmpPath" | Out-Null

    Write-Host "Copiando $bios para files/bios via run-as..."
    & $AdbPath shell "run-as $PackageName cp $tmpPath files/bios/$bios"
    & $AdbPath shell "run-as $PackageName chmod 600 files/bios/$bios"
    & $AdbPath shell "rm -f $tmpPath"
}

Write-Host "Verificando BIOS provisionadas no app..."
& $AdbPath shell "run-as $PackageName ls -l files/bios"

Write-Host "Provisionamento concluido com sucesso."

