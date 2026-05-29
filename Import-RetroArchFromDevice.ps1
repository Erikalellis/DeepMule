param(
    [string]$ProjectRoot = "C:\Users\robso\AndroidStudioProjects\DeepMule",
    [string]$AdbPath = "F:\Android\SDK\platform-tools\adb.exe",
    [string]$PackageName = "com.retroarch",
    [string]$OutputDir = "retroarch-device-pack",
    [switch]$IncludeApk,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

function Resolve-RootPath([string]$PathValue, [string]$BasePath) {
    if ([System.IO.Path]::IsPathRooted($PathValue)) {
        return $PathValue
    }
    return (Join-Path $BasePath $PathValue)
}

if (-not (Test-Path $AdbPath)) {
    throw "adb nao encontrado em: $AdbPath"
}

$deviceList = & $AdbPath devices | Select-String "\tdevice$"
if (-not $deviceList) {
    throw "Nenhum dispositivo em estado 'device' encontrado no adb devices."
}

$outputRoot = Resolve-RootPath -PathValue $OutputDir -BasePath $ProjectRoot
$externalDir = "/sdcard/Android/data/$PackageName/files"
$internalDir = "/data/user/0/$PackageName/files"
$apkPathLine = (& $AdbPath shell pm path $PackageName | Select-Object -First 1)

Write-Host "Dispositivo(s) conectado(s):"
$deviceList | ForEach-Object { Write-Host " - $($_.Line)" }
Write-Host "Pacote alvo: $PackageName"
Write-Host "Origem externa: $externalDir"
Write-Host "Origem interna: $internalDir"
if ($apkPathLine) {
    Write-Host "APK detectado: $apkPathLine"
}

if ($DryRun) {
    Write-Host "[DryRun] Destino local: $outputRoot"
    Write-Host "[DryRun] Pastas previstas: external-files, internal-files, apk"
    return
}

New-Item -Path $outputRoot -ItemType Directory -Force | Out-Null

Write-Host "Copiando arquivos externos do RetroArch..."
& $AdbPath pull "$externalDir" (Join-Path $outputRoot "external-files")

Write-Host "Tentando copiar arquivos internos do RetroArch..."
& $AdbPath root | Out-Null
Start-Sleep -Seconds 2
try {
    & $AdbPath pull "$internalDir" (Join-Path $outputRoot "internal-files")
}
catch {
    Write-Host "Aviso: nao foi possivel copiar a pasta interna."
}

if ($IncludeApk -and $apkPathLine) {
    $apkPath = ($apkPathLine -replace '^package:', '').Trim()
    if (-not [string]::IsNullOrWhiteSpace($apkPath)) {
        $apkOut = Join-Path $outputRoot "retroarch-base.apk"
        Write-Host "Copiando APK base do RetroArch..."
        & $AdbPath pull "$apkPath" "$apkOut"
    }
}

Write-Host "Importacao do RetroArch concluida em: $outputRoot"

