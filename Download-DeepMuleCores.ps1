param(
    [string]$OutputDir = "C:\Users\robso\AndroidStudioProjects\DeepMule\cores-pack",
    [ValidateSet("arm64-v8a", "armeabi-v7a", "x86", "x86_64")]
    [string]$Abi = "arm64-v8a",
    [string]$BaseUrl = "https://buildbot.libretro.com/nightly/android/latest",
    [switch]$SkipExisting,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

# Lista consolidada usada pelo DeepMule (sistemas + aliases).
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

function Get-CoreNameVariants([string]$CoreName) {
    $variants = @($CoreName, $CoreName.Replace("-", "_"), $CoreName.Replace("_", "-"))
    return $variants | Select-Object -Unique
}

function Get-DownloadCandidates([string]$CoreName, [string]$RootUrl, [string]$AbiName) {
    $candidates = New-Object System.Collections.Generic.List[string]
    foreach ($variant in (Get-CoreNameVariants -CoreName $CoreName)) {
        $candidates.Add("$RootUrl/$AbiName/${variant}_libretro_android.so.zip")
        $candidates.Add("$RootUrl/$AbiName/${variant}_libretro.so.zip")
        $candidates.Add("$RootUrl/$AbiName/${variant}.so.zip")
        $candidates.Add("$RootUrl/$AbiName/${variant}_libretro_android.so")
        $candidates.Add("$RootUrl/$AbiName/${variant}_libretro.so")
        $candidates.Add("$RootUrl/$AbiName/${variant}.so")
    }
    return $candidates | Select-Object -Unique
}

function Resolve-SoFromZip([string]$ZipPath, [string]$CoreName, [string]$TempDir) {
    $extractDir = Join-Path $TempDir ([System.IO.Path]::GetFileNameWithoutExtension([System.IO.Path]::GetFileNameWithoutExtension($ZipPath)))
    if (Test-Path $extractDir) {
        Remove-Item -Path $extractDir -Recurse -Force
    }

    Expand-Archive -Path $ZipPath -DestinationPath $extractDir -Force

    $variants = Get-CoreNameVariants -CoreName $CoreName
    $candidates = Get-ChildItem -Path $extractDir -Recurse -File -Filter "*.so" -ErrorAction SilentlyContinue

    foreach ($variant in $variants) {
        $match = $candidates | Where-Object {
            $_.Name -ieq "$variant.so" -or
            $_.Name -ieq "${variant}_libretro.so" -or
            $_.Name -ieq "${variant}_libretro_android.so"
        } | Select-Object -First 1

        if ($match) {
            return $match.FullName
        }
    }

    return ($candidates | Select-Object -First 1).FullName
}

if (-not (Test-Path $OutputDir)) {
    New-Item -Path $OutputDir -ItemType Directory -Force | Out-Null
}

$tempDir = Join-Path $env:TEMP "deepmule-core-download"
if (-not (Test-Path $tempDir)) {
    New-Item -Path $tempDir -ItemType Directory -Force | Out-Null
}

$summary = [ordered]@{
    Downloaded = New-Object System.Collections.Generic.List[string]
    Existing = New-Object System.Collections.Generic.List[string]
    Missing = New-Object System.Collections.Generic.List[string]
    Failed = New-Object System.Collections.Generic.List[string]
}

foreach ($core in $requiredCores) {
    $targetSo = Join-Path $OutputDir "$core.so"

    if ($SkipExisting -and (Test-Path $targetSo)) {
        $summary.Existing.Add($core)
        Write-Host "[SKIP] $core.so ja existe em $OutputDir"
        continue
    }

    $urls = Get-DownloadCandidates -CoreName $core -RootUrl $BaseUrl -AbiName $Abi

    if ($DryRun) {
        Write-Host "[DryRun] Tentativas para ${core}:"
        $urls | ForEach-Object { Write-Host " - $_" }
        continue
    }

    $downloaded = $false
    foreach ($url in $urls) {
        $fileName = [System.IO.Path]::GetFileName($url)
        $tmpPath = Join-Path $tempDir $fileName

        try {
            Invoke-WebRequest -Uri $url -OutFile $tmpPath -UseBasicParsing -TimeoutSec 30

            if ($tmpPath.ToLowerInvariant().EndsWith(".zip")) {
                $resolvedSo = Resolve-SoFromZip -ZipPath $tmpPath -CoreName $core -TempDir $tempDir
                if (-not [string]::IsNullOrWhiteSpace($resolvedSo) -and (Test-Path $resolvedSo)) {
                    Copy-Item -Path $resolvedSo -Destination $targetSo -Force
                    $downloaded = $true
                    break
                }
            }
            else {
                Copy-Item -Path $tmpPath -Destination $targetSo -Force
                $downloaded = $true
                break
            }
        }
        catch {
            # Tenta o proximo candidato de nome/URL.
            continue
        }
        finally {
            if (Test-Path $tmpPath) {
                Remove-Item -Path $tmpPath -Force -ErrorAction SilentlyContinue
            }
        }
    }

    if ($downloaded) {
        $summary.Downloaded.Add($core)
        Write-Host "[OK] $core.so pronto em $OutputDir"
    }
    else {
        $summary.Missing.Add($core)
        Write-Host "[MISS] Falhou download de $core"
    }
}

Write-Host ""
Write-Host "=== Resumo Download de Cores ==="
Write-Host ("Downloaded: " + ($summary.Downloaded.Count))
Write-Host ("Existing: " + ($summary.Existing.Count))
Write-Host ("Missing: " + ($summary.Missing.Count))
Write-Host ""

if ($summary.Downloaded.Count -gt 0) {
    Write-Host "Cores baixados:"
    $summary.Downloaded | ForEach-Object { Write-Host " - $_.so" }
}

if ($summary.Existing.Count -gt 0) {
    Write-Host "Cores ja existentes:"
    $summary.Existing | ForEach-Object { Write-Host " - $_.so" }
}

if ($summary.Missing.Count -gt 0) {
    Write-Host "Cores nao encontrados no endpoint configurado:"
    $summary.Missing | ForEach-Object { Write-Host " - $_" }
    throw "Download incompleto: faltam $($summary.Missing.Count) cores."
}

Write-Host "Download concluido com sucesso."
