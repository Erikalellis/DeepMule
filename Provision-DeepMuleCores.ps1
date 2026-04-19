param(
    [string]$ProjectRoot = "C:\Users\robso\AndroidStudioProjects\DeepMule",
    [string]$PackageName = "com.example.deepmule",
    [string]$AdbPath = "F:\Android\SDK\platform-tools\adb.exe",
    [string]$CorePackDir = "cores-pack",
    [string]$RetroArchDir = "",
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

function Resolve-RootPath([string]$PathValue, [string]$BasePath) {
    if ([string]::IsNullOrWhiteSpace($PathValue)) {
        return ""
    }
    if ([System.IO.Path]::IsPathRooted($PathValue)) {
        return $PathValue
    }
    return (Join-Path $BasePath $PathValue)
}

function Find-CoreRoot([string]$ProjectRootValue, [string]$CorePackDirValue, [string]$RetroArchDirValue) {
    $retroRoot = Resolve-RootPath -PathValue $RetroArchDirValue -BasePath $ProjectRootValue
    if (-not [string]::IsNullOrWhiteSpace($retroRoot)) {
        if (-not (Test-Path $retroRoot)) {
            throw "Pasta RetroArch nao encontrada em: $retroRoot"
        }

        $directCores = Join-Path $retroRoot "cores"
        if (Test-Path $directCores) {
            return $directCores
        }

        $nestedCores = Get-ChildItem -Path $retroRoot -Recurse -Directory -Force -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -ieq "cores" } |
            Select-Object -First 1

        if ($nestedCores) {
            return $nestedCores.FullName
        }

        $anySo = Get-ChildItem -Path $retroRoot -Recurse -File -Filter "*.so" -ErrorAction SilentlyContinue |
            Select-Object -First 1
        if ($anySo) {
            return $retroRoot
        }

        throw "Pasta 'cores' nao encontrada (e nenhum .so detectado) dentro de RetroArch: $retroRoot"
    }

    return Resolve-RootPath -PathValue $CorePackDirValue -BasePath $ProjectRootValue
}

function Get-CoreNameVariants([string]$CoreName) {
    $variants = @($CoreName, $CoreName.Replace("-", "_"), $CoreName.Replace("_", "-"))
    return $variants | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -Unique
}

function Get-CoreCandidateFileNames([string]$CoreName) {
    $result = @()
    foreach ($variant in (Get-CoreNameVariants -CoreName $CoreName)) {
        $result += "$variant.so"
        $result += "${variant}_libretro_android.so"
        $result += "${variant}_libretro.so"
    }
    return $result | Select-Object -Unique
}

function Build-CoreFileIndex([string]$CoreRootPath) {
    $index = @{}
    $allSoFiles = Get-ChildItem -Path $CoreRootPath -Recurse -File -Filter "*.so" -ErrorAction SilentlyContinue
    foreach ($file in $allSoFiles) {
        $key = $file.Name.ToLowerInvariant()
        if (-not $index.ContainsKey($key)) {
            $index[$key] = New-Object System.Collections.Generic.List[System.IO.FileInfo]
        }
        $index[$key].Add($file)
    }
    return $index
}

function Resolve-CoreSourceFile([hashtable]$FileIndex, [string]$CoreName) {
    foreach ($candidateFile in (Get-CoreCandidateFileNames -CoreName $CoreName)) {
        $key = $candidateFile.ToLowerInvariant()
        if ($FileIndex.ContainsKey($key)) {
            return $FileIndex[$key] | Sort-Object FullName | Select-Object -First 1
        }
    }
    return $null
}

$coreRoot = Find-CoreRoot -ProjectRootValue $ProjectRoot -CorePackDirValue $CorePackDir -RetroArchDirValue $RetroArchDir

# Lista consolidada para cobrir os 23 sistemas e aliases mais comuns.
$requiredCores = @(
    "stella",
    "prosystem",
    "handy",
    "gambatte",
    "mgba",
    "melonds",
    "desmume",
    "citra",
    "fceumm",
    "snes9x",
    "mupen64plus_next",
    "mupen64plus",
    "genesis_plus_gx",
    "pcsx_rearmed",
    "pcsx-rearmed",
    "ppsspp",
    "fbneo",
    "beetle_pce_fast",
    "mednafen_ngp",
    "beetle_wswan",
    "beetle_cygne"
)

if (-not (Test-Path $coreRoot)) {
    throw "Pasta de cores nao encontrada em: $coreRoot"
}

Write-Host "Origem de cores detectada: $coreRoot"
$fileIndex = Build-CoreFileIndex -CoreRootPath $coreRoot
if ($fileIndex.Count -eq 0) {
    throw "Nenhum arquivo .so encontrado em: $coreRoot"
}

$coreMap = @{}
foreach ($core in $requiredCores) {
    $file = Resolve-CoreSourceFile -FileIndex $fileIndex -CoreName $core
    if ($file) {
        $coreMap[$core] = $file.FullName
    }
}

if ($coreMap.Count -eq 0) {
    Write-Host "Cores esperados (qualquer um destes formatos):"
    foreach ($core in $requiredCores) {
        Write-Host (" - {0}.so | {0}_libretro_android.so | {0}_libretro.so" -f $core)
    }
    throw "Nenhum core necessario foi encontrado em $coreRoot"
}

$missing = $requiredCores | Where-Object { -not $coreMap.ContainsKey($_) }
if ($missing.Count -gt 0) {
    Write-Host "Aviso: faltando alguns cores/aliases (pode ser normal se usar apenas um alias):"
    $missing | ForEach-Object { Write-Host " - $_.so" }
}

if ($DryRun) {
    Write-Host "[DryRun] Cores que seriam provisionados para files/cores:"
    foreach ($core in ($coreMap.Keys | Sort-Object)) {
        Write-Host (" - {0}.so <= {1}" -f $core, $coreMap[$core])
    }
    return
}

if (-not (Test-Path $AdbPath)) {
    throw "adb nao encontrado em: $AdbPath"
}

$deviceList = & $AdbPath devices | Select-String "\tdevice$"
if (-not $deviceList) {
    throw "Nenhum dispositivo em estado 'device' encontrado no adb devices."
}

Write-Host "Dispositivo(s) conectado(s):"
$deviceList | ForEach-Object { Write-Host " - $($_.Line)" }

Write-Host "Criando pasta de cores interna do app..."
& $AdbPath shell "run-as $PackageName mkdir -p files/cores"

foreach ($core in ($coreMap.Keys | Sort-Object)) {
    $sourceFile = $coreMap[$core]
    $tmpPath = "/data/local/tmp/$core.so"

    Write-Host "Enviando $core.so para temporario..."
    & $AdbPath push "$sourceFile" "$tmpPath" | Out-Null

    Write-Host "Copiando $core.so para files/cores via run-as..."
    & $AdbPath shell "run-as $PackageName cp $tmpPath files/cores/$core.so"
    & $AdbPath shell "run-as $PackageName chmod 600 files/cores/$core.so"
    & $AdbPath shell "rm -f $tmpPath"
}

Write-Host "Verificando cores provisionados no app..."
& $AdbPath shell "run-as $PackageName ls -l files/cores"

Write-Host "Provisionamento de cores concluido com sucesso."

