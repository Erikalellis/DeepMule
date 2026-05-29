param(
    [string]$ProjectRoot = $PSScriptRoot,
    [string]$DeviceSerial = "",
    [string]$PackageName = "com.deepdarknessstudios.deepmule",
    [switch]$SkipInstall,
    [switch]$SkipLaunch
)

$ErrorActionPreference = "Stop"
Set-Location $ProjectRoot

function Get-RecursiveFileCount([string]$Path) {
    if (-not (Test-Path $Path)) { return 0 }
    return (Get-ChildItem -Path $Path -Recurse -File | Measure-Object).Count
}

function Invoke-Adb([string[]]$Args) {
    $cmd = @()
    if (-not [string]::IsNullOrWhiteSpace($DeviceSerial)) {
        $cmd += @("-s", $DeviceSerial)
    }
    $cmd += $Args
    & adb @cmd
    return $LASTEXITCODE
}

$payloadMap = @{
    bios      = (Join-Path $ProjectRoot "bios-pack")
    roms      = (Join-Path $ProjectRoot "Roms")
    cores     = (Join-Path $ProjectRoot "cores-pack")
    retroarch = (Join-Path $ProjectRoot "retroarch-pack")
}

Write-Host "[1/5] Validating payload folders..." -ForegroundColor Cyan
foreach ($assetFolder in $payloadMap.Keys) {
    $sourcePath = $payloadMap[$assetFolder]
    if (-not (Test-Path $sourcePath)) {
        throw "Required payload folder not found: $sourcePath"
    }
    $count = Get-RecursiveFileCount $sourcePath
    if ($count -le 0) {
        throw "Payload folder is empty: $sourcePath"
    }
    Write-Host "assets/$assetFolder <= $count files" -ForegroundColor DarkGray
}

Write-Host "[2/5] Building release APK with full payload..." -ForegroundColor Cyan
$gradleArgs = @(
    ":app:assembleRelease",
    "-Pdeepmule.bundleAllPayload=true",
    "--console=plain",
    "--no-configuration-cache"
)
& .\gradlew.bat @gradleArgs
if ($LASTEXITCODE -ne 0) {
    throw "Gradle assembleRelease failed with exit code $LASTEXITCODE"
}

$apkCandidates = @(
    (Join-Path $ProjectRoot "app\build\outputs\apk\release\app-release.apk"),
    (Join-Path $ProjectRoot "app\build\outputs\apk\release\app-release-unsigned.apk")
)
$apkPath = $apkCandidates | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $apkPath) {
    throw "Release APK not found under app\build\outputs\apk\release"
}

$apkInfo = Get-Item $apkPath
$sizeMb = [math]::Round($apkInfo.Length / 1MB, 2)
Write-Host "[3/5] APK generated: $apkPath ($sizeMb MB)" -ForegroundColor Green

$optimizationReport = Join-Path $ProjectRoot "app\build\reports\payload-optimization-summary.txt"
if (Test-Path $optimizationReport) {
    Write-Host "Payload optimization summary:" -ForegroundColor Cyan
    Get-Content $optimizationReport | ForEach-Object { Write-Host "  $_" -ForegroundColor DarkGray }
}

if ($SkipInstall) {
    Write-Host "Skipping install/launch by request (-SkipInstall)." -ForegroundColor Yellow
    return
}

Write-Host "[4/5] Installing on connected device..." -ForegroundColor Cyan
if ((Invoke-Adb @("start-server")) -ne 0) {
    throw "Failed to start adb server"
}

$deviceLines = adb devices | Select-String "\tdevice$"
if (-not $deviceLines) {
    throw "No connected device in 'device' state"
}

if ((Invoke-Adb @("install", "-r", $apkPath)) -ne 0) {
    Write-Warning "adb install -r failed. Trying uninstall/reinstall fallback..."
    [void](Invoke-Adb @("uninstall", $PackageName))
    if ((Invoke-Adb @("install", "-r", $apkPath)) -ne 0) {
        throw "APK installation failed after uninstall/reinstall fallback"
    }
}

if ($SkipLaunch) {
    Write-Host "Skipping launch by request (-SkipLaunch)." -ForegroundColor Yellow
    return
}

Write-Host "[5/5] Launching app and running basic checks..." -ForegroundColor Cyan
$mainActivity = "$PackageName/com.example.deepmule.MainActivity"
if ((Invoke-Adb @("shell", "am", "start", "-n", $mainActivity)) -ne 0) {
    throw "APK installed but app launch failed"
}

Start-Sleep -Seconds 6
$appPid = adb shell pidof $PackageName 2>$null
if ([string]::IsNullOrWhiteSpace(($appPid | Out-String).Trim())) {
    throw "App launched command succeeded, but process not running (possible startup crash)"
}

Write-Host "Release install validation passed. App is running (pid: $($appPid | Out-String).Trim())." -ForegroundColor Green

