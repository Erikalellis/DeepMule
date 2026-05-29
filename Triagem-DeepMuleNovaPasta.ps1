param(
    [string]$SourceRoot = "C:\Users\robso\Downloads\Nova pasta",
    [string]$ProjectRoot = "C:\Users\robso\AndroidStudioProjects\DeepMule",
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

function Ensure-Dir([string]$Path) {
    if (-not (Test-Path $Path)) {
        New-Item -Path $Path -ItemType Directory -Force | Out-Null
    }
}

function Copy-Tree([string]$Source, [string]$Destination, [ref]$CopiedFiles, [switch]$Simulate) {
    if (-not (Test-Path $Source)) {
        return
    }

    if ($Simulate) {
        $count = (Get-ChildItem -Path $Source -Recurse -File -ErrorAction SilentlyContinue | Measure-Object).Count
        $CopiedFiles.Value += $count
        Write-Host ("[DryRun] {0} arquivo(s): {1} -> {2}" -f $count, $Source, $Destination)
        return
    }

    Ensure-Dir -Path $Destination
    Copy-Item -Path (Join-Path $Source "*") -Destination $Destination -Recurse -Force
    $count = (Get-ChildItem -Path $Source -Recurse -File -ErrorAction SilentlyContinue | Measure-Object).Count
    $CopiedFiles.Value += $count
    Write-Host ("[OK] {0} arquivo(s): {1} -> {2}" -f $count, $Source, $Destination)
}

function Copy-FileIfExists([string]$Source, [string]$Destination, [ref]$CopiedFiles, [switch]$Simulate) {
    if (-not (Test-Path $Source)) {
        return
    }

    if ($Simulate) {
        $CopiedFiles.Value += 1
        Write-Host ("[DryRun] 1 arquivo: {0} -> {1}" -f $Source, $Destination)
        return
    }

    Ensure-Dir -Path (Split-Path -Path $Destination -Parent)
    Copy-Item -Path $Source -Destination $Destination -Force
    $CopiedFiles.Value += 1
    Write-Host ("[OK] 1 arquivo: {0} -> {1}" -f $Source, $Destination)
}

if (-not (Test-Path $SourceRoot)) {
    throw "Pasta de origem nao encontrada: $SourceRoot"
}

$retroArchRoot = Join-Path $SourceRoot "RetroArch"
$emulatorRoot = Join-Path $SourceRoot "com.emulator.console.game.retro"

$retroPack = Join-Path $ProjectRoot "retroarch-pack"
$coresPack = Join-Path $ProjectRoot "cores-pack"

Ensure-Dir -Path $retroPack
Ensure-Dir -Path $coresPack

$copied = 0

Write-Host "=== Triagem Inteligente: Nova pasta ==="
Write-Host "Origem: $SourceRoot"
Write-Host "Destino retroarch-pack: $retroPack"
Write-Host "Destino cores-pack: $coresPack"
Write-Host ""

# 1) Pacotes RetroArch decompilados (maior valor).
Copy-Tree -Source (Join-Path $retroArchRoot "assets\assets") -Destination (Join-Path $retroPack "assets") -CopiedFiles ([ref]$copied) -Simulate:$DryRun
Copy-Tree -Source (Join-Path $retroArchRoot "assets\autoconfig") -Destination (Join-Path $retroPack "autoconfig") -CopiedFiles ([ref]$copied) -Simulate:$DryRun
Copy-Tree -Source (Join-Path $retroArchRoot "assets\overlays") -Destination (Join-Path $retroPack "assets\overlays") -CopiedFiles ([ref]$copied) -Simulate:$DryRun
Copy-Tree -Source (Join-Path $retroArchRoot "assets\shaders") -Destination (Join-Path $retroPack "assets\shaders") -CopiedFiles ([ref]$copied) -Simulate:$DryRun
Copy-Tree -Source (Join-Path $retroArchRoot "assets\database") -Destination (Join-Path $retroPack "database") -CopiedFiles ([ref]$copied) -Simulate:$DryRun
Copy-Tree -Source (Join-Path $retroArchRoot "assets\info") -Destination (Join-Path $retroPack "info") -CopiedFiles ([ref]$copied) -Simulate:$DryRun
Copy-Tree -Source (Join-Path $retroArchRoot "assets\cheats") -Destination (Join-Path $retroPack "cheats") -CopiedFiles ([ref]$copied) -Simulate:$DryRun
Copy-Tree -Source (Join-Path $retroArchRoot "assets\thumbnails") -Destination (Join-Path $retroPack "thumbnails") -CopiedFiles ([ref]$copied) -Simulate:$DryRun
Copy-FileIfExists -Source (Join-Path $retroArchRoot "assets\retroarch.cfg") -Destination (Join-Path $retroPack "config\retroarch.cfg") -CopiedFiles ([ref]$copied) -Simulate:$DryRun

# 2) DB adicional encontrada no outro APK decompilado.
Copy-FileIfExists -Source (Join-Path $emulatorRoot "assets\libretro-db.sqlite") -Destination (Join-Path $retroPack "database\libretro-db.sqlite") -CopiedFiles ([ref]$copied) -Simulate:$DryRun

# 3) Cores .so com assinatura libretro (evita copiar libs genéricas do app).
$libretroSo = Get-ChildItem -Path $SourceRoot -Recurse -File -Filter "*.so" -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -match "_libretro(_android)?\.so$" }

if ($libretroSo.Count -gt 0) {
    foreach ($so in $libretroSo) {
        $targetName = $so.Name -replace "_libretro_android\.so$", ".so" -replace "_libretro\.so$", ".so"
        $target = Join-Path $coresPack $targetName

        if ($DryRun) {
            $copied++
            Write-Host ("[DryRun] core: {0} -> {1}" -f $so.FullName, $target)
        }
        else {
            Copy-Item -Path $so.FullName -Destination $target -Force
            $copied++
            Write-Host ("[OK] core: {0} -> {1}" -f $so.FullName, $target)
        }
    }
}
else {
    Write-Host "[INFO] Nenhum core com padrao _libretro(.so/_android.so) encontrado na origem."
}

Write-Host ""
Write-Host ("Triagem concluida. Itens processados: {0}" -f $copied)
if ($DryRun) {
    Write-Host "Modo DryRun: nenhuma copia foi persistida."
}

