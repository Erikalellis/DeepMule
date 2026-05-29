param(
    [string]$ProjectRoot = $PSScriptRoot,
    [string]$RomsRoot = "",
    [string]$PackageName = "com.example.deepmule",
    [string]$AdbPath = "F:\Android\SDK\platform-tools\adb.exe",
    [string]$DeviceSerial = "",
    [int]$MaxPerSystem = 5,
    [int]$TotalGames = 0,
    [switch]$IncludeNeoGeoAsArcade,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

function Invoke-AdbSilent {
    param([string[]]$ArgList)
    $prev = $ErrorActionPreference
    $ErrorActionPreference = "SilentlyContinue"
    $null = & $AdbPath @script:adbBase @ArgList 2>&1
    $exitCode = $LASTEXITCODE
    $ErrorActionPreference = $prev
    return $exitCode
}

function Invoke-AdbShell {
    param([string]$Command)
    $prev = $ErrorActionPreference
    $ErrorActionPreference = "SilentlyContinue"
    $output = & $AdbPath @script:adbBase shell $Command 2>&1 | Out-String
    $exitCode = $LASTEXITCODE
    $ErrorActionPreference = $prev
    return [pscustomobject]@{ ExitCode = $exitCode; Output = $output.Trim() }
}

function Escape-ForDoubleQuotes([string]$Text) {
    return $Text.Replace('"', '\"')
}

function Convert-ToSafeFileName([string]$Name) {
    $safe = $Name -replace '[^A-Za-z0-9\.\-_ ]', '_'
    $safe = $safe -replace '\s+', '_'
    $safe = $safe.Trim()
    if ([string]::IsNullOrWhiteSpace($safe)) {
        $safe = "rom.bin"
    }
    return $safe
}

function Get-CandidateRoms {
    param(
        [string]$SourcePath,
        [string[]]$Extensions,
        [string[]]$Keywords,
        [int]$Take
    )

    if (-not (Test-Path -LiteralPath $SourcePath)) {
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
            if ($name -match $kw) {
                $score += 10
            }
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

function Get-WorkspaceSystems([string]$Root) {
    if ([string]::IsNullOrWhiteSpace($Root) -or -not (Test-Path -LiteralPath $Root)) {
        return @()
    }

    $specs = @(
        @{ Id = "atari2600"; Folder = "roms atari"; Ext = @(".a26", ".bin", ".rom", ".zip"); Keys = @("pitfall", "pac", "space invaders", "asteroids", "river raid", "kaboom", "frogger") },
        @{ Id = "gba"; Folder = "roms gameboy advance"; Ext = @(".gba", ".zip"); Keys = @("mario", "zelda", "metroid", "pokemon", "castlevania", "advance wars", "kirby") },
        @{ Id = "arcade"; Folder = "roms neo geo"; Ext = @(".zip", ".7z"); Keys = @("metal slug", "king of fighters", "samurai", "fatal fury", "sengoku", "neo geo") },
        @{ Id = "n64"; Folder = "roms nintendo 64"; Ext = @(".z64", ".v64", ".n64", ".zip"); Keys = @("mario", "zelda", "mario kart", "smash", "star fox", "banjo", "diddy") },
        @{ Id = "nes"; Folder = "roms nintendo nes"; Ext = @(".nes", ".zip"); Keys = @("mario", "zelda", "metroid", "megaman", "contra", "castlevania", "tetris") },
        @{ Id = "sms"; Folder = "roms Sega Master System"; Ext = @(".sms", ".zip"); Keys = @("sonic", "alex kidd", "wonder boy", "shinobi", "castle", "outrun") },
        @{ Id = "genesis"; Folder = "roms Sega Mega Drive (Sega Genesis)"; Ext = @(".md", ".gen", ".smd", ".bin", ".zip"); Keys = @("sonic", "streets of rage", "golden axe", "shinobi", "mortal kombat", "castlevania", "contra") },
        @{ Id = "snes"; Folder = "rom snes"; Ext = @(".sfc", ".smc", ".zip"); Keys = @("mario", "zelda", "donkey kong", "metroid", "chrono", "street fighter", "mortal kombat") }
    )

    $resolved = New-Object System.Collections.Generic.List[object]
    foreach ($spec in $specs) {
        $sourcePath = Join-Path $Root $spec.Folder
        if (Test-Path -LiteralPath $sourcePath) {
            $resolved.Add([pscustomobject]@{
                Id = $spec.Id
                Source = $sourcePath
                Ext = $spec.Ext
                Keys = $spec.Keys
            })
        }
    }
    return $resolved
}

$legacySystems = @(
    @{ Id = "atari2600"; Source = "D:\Atari 2600"; Ext = @(".a26", ".bin", ".zip"); Keys = @("pitfall", "pac", "space invaders", "asteroids", "river raid", "kaboom", "frogger") },
    @{ Id = "gba"; Source = "D:\Gameboy Advance"; Ext = @(".gba", ".zip"); Keys = @("mario", "zelda", "metroid", "pokemon", "castlevania", "advance wars", "kirby") },
    @{ Id = "arcade"; Source = "D:\MAME"; Ext = @(".zip", ".7z"); Keys = @("pac", "galaga", "donkey kong", "street fighter", "1942", "tetris", "bubble bobble") },
    @{ Id = "n64"; Source = "D:\Nintendo 64"; Ext = @(".z64", ".v64", ".n64", ".zip"); Keys = @("mario", "zelda", "mario kart", "smash", "star fox", "banjo", "diddy") },
    @{ Id = "nes"; Source = "D:\Nintendo NES"; Ext = @(".nes", ".zip"); Keys = @("mario", "zelda", "metroid", "megaman", "contra", "castlevania", "tetris") },
    @{ Id = "sms"; Source = "D:\Sega Master System"; Ext = @(".sms", ".zip"); Keys = @("sonic", "alex kidd", "wonder boy", "shinobi", "castle", "outrun") },
    @{ Id = "genesis"; Source = "D:\Sega Mega Drive (Sega Genesis)"; Ext = @(".md", ".bin", ".smd", ".zip"); Keys = @("sonic", "streets of rage", "golden axe", "shinobi", "mortal kombat", "castlevania", "contra") },
    @{ Id = "snes"; Source = "D:\SNES"; Ext = @(".sfc", ".smc", ".zip"); Keys = @("mario", "zelda", "donkey kong", "metroid", "chrono", "street fighter", "mortal kombat") },
    @{ Id = "pce"; Source = "D:\TurboGrafX"; Ext = @(".pce", ".zip"); Keys = @("bonk", "pc genjin", "r-type", "bomberman", "splatterhouse", "galaga", "gradius") }
)

if ([string]::IsNullOrWhiteSpace($RomsRoot)) {
    $RomsRoot = Join-Path $ProjectRoot "Roms"
}

$systems = @(Get-WorkspaceSystems -Root $RomsRoot)
if ($systems.Count -gt 0) {
    Write-Host ("Origem principal de ROMs detectada no projeto: {0}" -f $RomsRoot) -ForegroundColor Cyan
}
else {
    $systems = $legacySystems
    if ($IncludeNeoGeoAsArcade) {
        $systems += @{ Id = "arcade"; Source = "D:\Neo Geo"; Ext = @(".zip", ".7z"); Keys = @("metal slug", "king of fighters", "samurai", "fatal fury", "sengoku") }
    }
    Write-Host "Origem local Roms/ nao encontrada; usando fontes legadas configuradas." -ForegroundColor Yellow
}

# If TotalGames is informed, distribute total across systems evenly.
$targetByIndex = @{}
if ($TotalGames -gt 0) {
    if ($TotalGames -lt $systems.Count) {
        throw "TotalGames ($TotalGames) must be >= number of systems ($($systems.Count))"
    }

    $base = [math]::Floor($TotalGames / $systems.Count)
    $remainder = $TotalGames % $systems.Count
    for ($i = 0; $i -lt $systems.Count; $i++) {
        $targetByIndex[$i] = $base + $(if ($i -lt $remainder) { 1 } else { 0 })
    }
}

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "DeepMule - Provision Classic ROMs" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "PackageName  : $PackageName"
Write-Host "MaxPerSystem : $MaxPerSystem"
Write-Host "TotalGames   : $TotalGames"
Write-Host "DryRun       : $([bool]$DryRun)"
Write-Host ""

if ($MaxPerSystem -lt 1) {
    throw "MaxPerSystem must be >= 1"
}
if ($TotalGames -lt 0) {
    throw "TotalGames must be >= 0"
}

# Build adb base args with optional -s serial
$script:adbBase = @()
if (-not [string]::IsNullOrWhiteSpace($DeviceSerial)) {
    $script:adbBase = @("-s", $DeviceSerial)
    Write-Host "Usando dispositivo: $DeviceSerial" -ForegroundColor Cyan
}

$devices = & $AdbPath @script:adbBase devices | Select-String "\tdevice$"
if (-not $devices) {
    throw "No adb device connected"
}
Write-Host ("Device: {0}" -f $devices[0].Line.Trim()) -ForegroundColor Cyan

$results = New-Object System.Collections.Generic.List[object]
$totalCopied = 0
$totalWarn = 0
$totalFail = 0

for ($idx = 0; $idx -lt $systems.Count; $idx++) {
    $sys = $systems[$idx]
    $targetForSystem = if ($TotalGames -gt 0) { [int]$targetByIndex[$idx] } else { $MaxPerSystem }

    $picked = Get-CandidateRoms -SourcePath $sys.Source -Extensions $sys.Ext -Keywords $sys.Keys -Take $targetForSystem
    if (-not $picked -or $picked.Count -eq 0) {
        Write-Host ("[WARN] {0}: no candidates found in {1}" -f $sys.Id, $sys.Source) -ForegroundColor Yellow
        $totalWarn++
        continue
    }

    Write-Host ""
    Write-Host ("[{0}] selecting {1}/{2} file(s)" -f $sys.Id, $picked.Count, $targetForSystem) -ForegroundColor Cyan

    $deviceDir = "/data/user/0/$PackageName/files/roms/$($sys.Id)"
    if (-not $DryRun) {
        $mkCmd = "run-as $PackageName mkdir -p '$deviceDir'"
        $mk = Invoke-AdbShell -Command $mkCmd
        if ($mk.ExitCode -ne 0) {
            Write-Host ("[FAIL] {0}: cannot create target dir" -f $sys.Id) -ForegroundColor Red
            $totalFail++
            continue
        }
    }

    foreach ($rom in $picked) {
        $src = $rom.FullName
        $name = $rom.Name
        $safeName = Convert-ToSafeFileName $name
        $sizeKB = [math]::Round($rom.Length / 1KB, 1)
        $devicePath = "$deviceDir/$safeName"

        if ($DryRun) {
            Write-Host ("[DRY] {0} -> {1}" -f $name, $devicePath)
            $results.Add([pscustomobject]@{ System = $sys.Id; SourceName = $name; TargetName = $safeName; DevicePath = $devicePath; Status = "DRY" })
            continue
        }

        $tmpName = "dmtmp_$([System.Guid]::NewGuid().ToString('N').Substring(0,8))$([System.IO.Path]::GetExtension($src))"
        $tmpPath = "/data/local/tmp/$tmpName"

        $pushEc = Invoke-AdbSilent -ArgList @("push", $src, $tmpPath)
        if ($pushEc -ne 0) {
            Write-Host ("[FAIL] {0}: adb push failed" -f $name) -ForegroundColor Red
            $totalFail++
            $results.Add([pscustomobject]@{ System = $sys.Id; SourceName = $name; TargetName = $safeName; DevicePath = $devicePath; Status = "FAIL_PUSH" })
            continue
        }

        $cpCmd = "run-as $PackageName cp -f '$tmpPath' '$devicePath'"
        $cp = Invoke-AdbShell -Command $cpCmd
        $rmCmd = "rm -f '$tmpPath'"
        $null = Invoke-AdbShell -Command $rmCmd

        if ($cp.ExitCode -ne 0) {
            Write-Host ("[FAIL] {0}: copy to app failed" -f $name) -ForegroundColor Red
            $totalFail++
            $results.Add([pscustomobject]@{ System = $sys.Id; SourceName = $name; TargetName = $safeName; DevicePath = $devicePath; Status = "FAIL_COPY" })
            continue
        }

        $lsCmd = "run-as $PackageName ls '$devicePath'"
        $verify = Invoke-AdbShell -Command $lsCmd
        if ($verify.ExitCode -eq 0) {
            Write-Host ("[PASS] {0} -> {1} ({2} KB)" -f $name, $safeName, $sizeKB) -ForegroundColor Green
            $totalCopied++
            $results.Add([pscustomobject]@{ System = $sys.Id; SourceName = $name; TargetName = $safeName; DevicePath = $devicePath; Status = "PASS" })
        }
        else {
            Write-Host ("[WARN] {0}: copied but verify failed" -f $name) -ForegroundColor Yellow
            $totalWarn++
            $results.Add([pscustomobject]@{ System = $sys.Id; SourceName = $name; TargetName = $safeName; DevicePath = $devicePath; Status = "WARN_VERIFY" })
        }
    }
}

Write-Host ""
Write-Host "================================" -ForegroundColor Cyan
Write-Host ("Copied: {0} | Warn: {1} | Fail: {2}" -f $totalCopied, $totalWarn, $totalFail) -ForegroundColor Cyan
if ($TotalGames -gt 0) {
    Write-Host ("Target total: {0}" -f $TotalGames) -ForegroundColor Cyan
}
Write-Host "================================" -ForegroundColor Cyan

$reportDir = "C:\Users\robso\AndroidStudioProjects\DeepMule\build\reports\deepmule"
New-Item -Path $reportDir -ItemType Directory -Force | Out-Null
$reportPath = Join-Path $reportDir "classic-roms-report.json"
$results | ConvertTo-Json -Depth 4 | Set-Content -Path $reportPath -Encoding UTF8
Write-Host ("Report: {0}" -f $reportPath)

if ($totalFail -gt 0) {
    exit 1
}
