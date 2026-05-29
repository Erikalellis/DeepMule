param(
    [string]$AdbPath = "F:\Android\SDK\platform-tools\adb.exe",
    [string]$DeviceSerial = "",
    [string]$SharedRoot = "/sdcard/ROMS_SHARED",
    [int]$TotalGames = 60,
    [switch]$MirrorToPackageData,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

function Invoke-AdbSilent {
    param([string[]]$ArgList)
    $prev = $ErrorActionPreference
    $ErrorActionPreference = "SilentlyContinue"
    $null = & $AdbPath @script:adbBase @ArgList 2>&1
    $code = $LASTEXITCODE
    $ErrorActionPreference = $prev
    return $code
}

function Invoke-AdbShell {
    param([string]$Command)
    $prev = $ErrorActionPreference
    $ErrorActionPreference = "SilentlyContinue"
    $out = & $AdbPath @script:adbBase shell $Command 2>&1 | Out-String
    $code = $LASTEXITCODE
    $ErrorActionPreference = $prev
    return [pscustomobject]@{ ExitCode = $code; Output = $out.Trim() }
}

function Convert-ToSafeFileName([string]$Name) {
    $safe = $Name -replace '[^A-Za-z0-9\.\-_ ]', '_'
    $safe = $safe -replace '\s+', '_'
    $safe = $safe.Trim()
    if ([string]::IsNullOrWhiteSpace($safe)) { return "rom.bin" }
    return $safe
}

function Resolve-SourcePath([string[]]$Candidates) {
    foreach ($c in $Candidates) {
        if (Test-Path -LiteralPath $c) { return $c }
    }
    return ""
}

function Get-CandidateRoms {
    param(
        [string]$SourcePath,
        [string[]]$Extensions,
        [string[]]$Keywords,
        [int]$Take
    )

    if ([string]::IsNullOrWhiteSpace($SourcePath) -or -not (Test-Path -LiteralPath $SourcePath)) {
        return @()
    }

    $files = Get-ChildItem -LiteralPath $SourcePath -Recurse -File -ErrorAction SilentlyContinue |
        Where-Object {
            $Extensions -contains $_.Extension.ToLowerInvariant() -and
            $_.Length -ge 1024 -and
            $_.Name -notmatch '\.srm|delete|\[x\]|\d{3}\.zip$'
        }

    $scored = foreach ($file in $files) {
        $name = $file.Name.ToLowerInvariant()
        $score = 0
        foreach ($kw in $Keywords) {
            if ($name -match $kw) { $score += 10 }
        }

        [pscustomobject]@{
            FullName = $file.FullName
            Name = $file.Name
            Length = $file.Length
            Score = $score
        }
    }

    return $scored |
        Sort-Object @{ Expression = "Score"; Descending = $true }, @{ Expression = "Length"; Descending = $false }, @{ Expression = "Name"; Descending = $false } |
        Select-Object -First $Take
}

if (-not (Test-Path -LiteralPath $AdbPath)) {
    throw "adb not found at: $AdbPath"
}

$script:adbBase = @()
if (-not [string]::IsNullOrWhiteSpace($DeviceSerial)) {
    $script:adbBase = @("-s", $DeviceSerial)
}

$devices = & $AdbPath @script:adbBase devices | Select-String "\tdevice$"
if (-not $devices) {
    throw "No adb device connected"
}

$targetPackages = @(
    "com.fulldive.extension.fullroid",
    "creek.itgame.retroandroid",
    "com.retro.games.emulator.nostalgia"
)

$systems = @(
    @{ Id = "arcade"; SourceCandidates = @("D:\MAME"); Ext = @(".zip", ".7z"); Keys = @("pac", "galaga", "donkey kong", "street fighter", "1942", "tetris") },
    @{ Id = "psx"; SourceCandidates = @("D:\PlayStation", "D:\Sony PlayStation", "D:\PSX"); Ext = @(".cue", ".chd", ".pbp", ".iso", ".zip"); Keys = @("resident evil", "final fantasy", "metal gear", "crash", "tekken") },
    @{ Id = "psp"; SourceCandidates = @("D:\PlayStation Portable", "D:\PSP"); Ext = @(".iso", ".cso", ".pbp", ".zip"); Keys = @("god of war", "persona", "gran turismo", "tekken", "monster hunter") }
)

if ($TotalGames -lt $systems.Count) {
    throw "TotalGames ($TotalGames) must be >= number of systems ($($systems.Count))"
}

$base = [math]::Floor($TotalGames / $systems.Count)
$remainder = $TotalGames % $systems.Count

$results = New-Object System.Collections.Generic.List[object]
$totalCopied = 0
$totalWarn = 0
$totalFail = 0

Write-Host ""
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "DeepMule - Shared ROM Provision for Emulators" -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "SharedRoot   : $SharedRoot"
Write-Host "TotalGames   : $TotalGames"
Write-Host "DryRun       : $([bool]$DryRun)"
Write-Host "MirrorApps   : $([bool]$MirrorToPackageData)"
Write-Host ""

if (-not $DryRun) {
    $mk = Invoke-AdbShell -Command "mkdir -p '$SharedRoot'"
    if ($mk.ExitCode -ne 0) {
        throw "Cannot create shared root: $SharedRoot"
    }
}

for ($i = 0; $i -lt $systems.Count; $i++) {
    $sys = $systems[$i]
    $take = $base + $(if ($i -lt $remainder) { 1 } else { 0 })
    $source = Resolve-SourcePath -Candidates $sys.SourceCandidates

    if ([string]::IsNullOrWhiteSpace($source)) {
        Write-Host ("[WARN] {0}: source path not found ({1})" -f $sys.Id, ($sys.SourceCandidates -join ", ")) -ForegroundColor Yellow
        $totalWarn++
        continue
    }

    $picked = Get-CandidateRoms -SourcePath $source -Extensions $sys.Ext -Keywords $sys.Keys -Take $take
    if (-not $picked -or $picked.Count -eq 0) {
        Write-Host ("[WARN] {0}: no ROMs found in {1}" -f $sys.Id, $source) -ForegroundColor Yellow
        $totalWarn++
        continue
    }

    $sharedSystemDir = "$SharedRoot/$($sys.Id)"
    if (-not $DryRun) {
        $mkSys = Invoke-AdbShell -Command "mkdir -p '$sharedSystemDir'"
        if ($mkSys.ExitCode -ne 0) {
            Write-Host ("[FAIL] {0}: cannot create shared dir" -f $sys.Id) -ForegroundColor Red
            $totalFail++
            continue
        }
    }

    Write-Host ""
    Write-Host ("[{0}] {1}/{2} from {3}" -f $sys.Id, $picked.Count, $take, $source) -ForegroundColor Cyan

    foreach ($rom in $picked) {
        $safeName = Convert-ToSafeFileName $rom.Name
        $target = "$sharedSystemDir/$safeName"

        if ($DryRun) {
            Write-Host ("[DRY] {0} -> {1}" -f $rom.Name, $target)
            $results.Add([pscustomobject]@{ System = $sys.Id; SourceName = $rom.Name; TargetPath = $target; Status = "DRY" })
            continue
        }

        $push = Invoke-AdbSilent -ArgList @("push", $rom.FullName, $target)
        if ($push -ne 0) {
            Write-Host ("[FAIL] {0}: push failed" -f $rom.Name) -ForegroundColor Red
            $totalFail++
            $results.Add([pscustomobject]@{ System = $sys.Id; SourceName = $rom.Name; TargetPath = $target; Status = "FAIL_PUSH" })
            continue
        }

        if ($MirrorToPackageData) {
            foreach ($pkg in $targetPackages) {
                $pkgDir = "/sdcard/Android/data/$pkg/files/roms/$($sys.Id)"
                $null = Invoke-AdbShell -Command "mkdir -p '$pkgDir'"
                $null = Invoke-AdbShell -Command "cp -f '$target' '$pkgDir/$safeName'"
            }
        }

        Write-Host ("[PASS] {0} -> {1}" -f $rom.Name, $target) -ForegroundColor Green
        $totalCopied++
        $results.Add([pscustomobject]@{ System = $sys.Id; SourceName = $rom.Name; TargetPath = $target; Status = "PASS" })
    }
}

Write-Host ""
Write-Host "================================" -ForegroundColor Cyan
Write-Host ("Copied: {0} | Warn: {1} | Fail: {2}" -f $totalCopied, $totalWarn, $totalFail) -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan

$reportDir = "C:\Users\robso\AndroidStudioProjects\DeepMule\build\reports\deepmule"
New-Item -Path $reportDir -ItemType Directory -Force | Out-Null
$reportPath = Join-Path $reportDir "shared-roms-report.json"
$results | ConvertTo-Json -Depth 4 | Set-Content -Path $reportPath -Encoding UTF8
Write-Host ("Report: {0}" -f $reportPath)

if ($totalFail -gt 0) {
    exit 1
}

