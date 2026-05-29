param(
    [string]$ProjectRoot = $PSScriptRoot,
    [switch]$Install,
    [string]$DeviceSerial = ""
)

$ErrorActionPreference = "Stop"

Set-Location $ProjectRoot

function Get-RecursiveFileCount([string]$Path) {
    if (-not (Test-Path $Path)) { return 0 }
    return (Get-ChildItem -Path $Path -Recurse -File | Measure-Object).Count
}

$payloadMap = @{
    bios = (Join-Path $ProjectRoot "bios-pack")
    roms = (Join-Path $ProjectRoot "Roms")
    cores = (Join-Path $ProjectRoot "cores-pack")
    retroarch = (Join-Path $ProjectRoot "retroarch-pack")
}

Write-Host "Validating payload folders before build..." -ForegroundColor Cyan
foreach ($assetFolder in $payloadMap.Keys) {
    $sourcePath = $payloadMap[$assetFolder]
    if (-not (Test-Path $sourcePath)) {
        throw "Required payload folder not found: $sourcePath"
    }
    $sourceCount = Get-RecursiveFileCount $sourcePath
    if ($sourceCount -le 0) {
        throw "Payload folder is empty: $sourcePath"
    }
    Write-Host "assets/$assetFolder <= $sourceCount files from $sourcePath" -ForegroundColor DarkGray
}

Write-Host "Building full payload APK (ROMs/BIOS/cores in assets)..." -ForegroundColor Cyan
& .\gradlew.bat ":app:assembleDebug" "-Pdeepmule.bundleAllPayload=true" "--console=plain" "--no-configuration-cache"
if ($LASTEXITCODE -ne 0) {
    throw "Gradle assembleDebug failed with exit code $LASTEXITCODE"
}

$apkPath = Join-Path $ProjectRoot "app\\build\\outputs\\apk\\debug\\app-debug.apk"
if (-not (Test-Path $apkPath)) {
    throw "APK not found at $apkPath"
}

$apk = Get-Item $apkPath
$sizeMb = [math]::Round($apk.Length / 1MB, 2)
Write-Host "APK generated: $($apk.FullName)" -ForegroundColor Green
Write-Host "APK size: $sizeMb MB" -ForegroundColor Yellow

$optimizationReport = Join-Path $ProjectRoot "app\build\reports\payload-optimization-summary.txt"
if (Test-Path $optimizationReport) {
    Write-Host "Payload optimization summary:" -ForegroundColor Cyan
    Get-Content $optimizationReport | ForEach-Object { Write-Host "  $_" -ForegroundColor DarkGray }
}

Write-Host "Verifying payload entries inside APK..." -ForegroundColor Cyan
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($apk.FullName)
try {
    foreach ($assetFolder in $payloadMap.Keys) {
        $prefix = "assets/$assetFolder/"
        $entryCount = ($zip.Entries | Where-Object { $_.FullName.StartsWith($prefix) -and -not $_.FullName.EndsWith("/") } | Measure-Object).Count
        if ($entryCount -le 0) {
            throw "APK is missing the optimized payload folder $prefix."
        }
        Write-Host "$prefix => $entryCount entries" -ForegroundColor DarkGray
    }

    $requiredEntries = @(
        "assets/cores/stella_libretro_android.so",
        "assets/cores/gambatte_libretro_android.so",
        "assets/cores/snes9x_libretro_android.so",
        "assets/cores/mgba_libretro_android.so",
        "assets/cores/fceumm_libretro_android.so",
        "assets/cores/genesis_plus_gx_libretro_android.so",
        "assets/cores/fbneo_libretro_android.so",
        "assets/cores/mupen64plus_next_libretro_android.so",
        "assets/retroarch/config/retroarch.cfg",
        "assets/retroarch/config/retroarch-core-options.cfg"
    )

    foreach ($entryName in $requiredEntries) {
        if (-not ($zip.Entries | Where-Object { $_.FullName -eq $entryName })) {
            throw "Required optimized payload entry not found in APK: $entryName"
        }
    }
} finally {
    $zip.Dispose()
}

if ($Install) {
    $adbArgs = @()
    if (-not [string]::IsNullOrWhiteSpace($DeviceSerial)) {
        $adbArgs += @("-s", $DeviceSerial)
    }

    Write-Host "Installing APK on device..." -ForegroundColor Cyan
    & adb @adbArgs install -r "$($apk.FullName)"
}
