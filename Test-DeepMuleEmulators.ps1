param(
    [string]$ProjectRoot = "C:\Users\robso\AndroidStudioProjects\DeepMule",
    [string]$PackageName = "com.example.deepmule",
    [string]$AdbPath = "F:\Android\SDK\platform-tools\adb.exe",
    [string]$DeviceSerial = "",
    [switch]$IncludeDevice,
    [switch]$StrictControllers,
    [string]$ReportDir = "build\reports\deepmule"
)

$ErrorActionPreference = "Stop"

# Build adb base args with optional -s serial
$script:adbBase = @()
if (-not [string]::IsNullOrWhiteSpace($DeviceSerial)) {
    $script:adbBase = @("-s", $DeviceSerial)
}

$script:PassCount = 0
$script:WarnCount = 0
$script:FailCount = 0
$script:Results = New-Object System.Collections.Generic.List[object]

function Write-Section([string]$Title) {
    Write-Host ""
    Write-Host $Title -ForegroundColor Cyan
    Write-Host ("-" * $Title.Length) -ForegroundColor Cyan
}

function Add-Result([ValidateSet("PASS", "WARN", "FAIL")][string]$Status, [string]$Message) {
    $script:Results.Add([pscustomobject]@{
            status = $Status
            message = $Message
        })

    switch ($Status) {
        "PASS" {
            $script:PassCount++
            Write-Host ("[PASS] {0}" -f $Message) -ForegroundColor Green
        }
        "WARN" {
            $script:WarnCount++
            Write-Host ("[WARN] {0}" -f $Message) -ForegroundColor Yellow
        }
        "FAIL" {
            $script:FailCount++
            Write-Host ("[FAIL] {0}" -f $Message) -ForegroundColor Red
        }
    }
}

function Resolve-ProjectPath([string]$RelativePath) {
    return Join-Path $ProjectRoot $RelativePath
}

function Get-CoreNameVariants([string]$CoreName) {
    $variants = @($CoreName, $CoreName.Replace("-", "_"), $CoreName.Replace("_", "-"))
    return $variants | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -Unique
}

$coreAliases = @{
    "mupen64plus_next" = @("mupen64plus")
    "mupen64plus_next_gles3" = @("mupen64plus_next", "mupen64plus")
    "mupen64plus_next_gles2" = @("mupen64plus_next", "mupen64plus")
    "pcsx_rearmed" = @("pcsx-rearmed")
    "beetle_pce_fast" = @("mednafen_pce_fast")
}

function Get-CoreLookupNames([string]$CoreName) {
    $names = New-Object System.Collections.Generic.List[string]
    $names.Add($CoreName)
    if ($coreAliases.ContainsKey($CoreName)) {
        foreach ($alias in $coreAliases[$CoreName]) {
            $names.Add($alias)
        }
    }

    $expanded = New-Object System.Collections.Generic.List[string]
    foreach ($name in ($names | Select-Object -Unique)) {
        foreach ($variant in (Get-CoreNameVariants -CoreName $name)) {
            $expanded.Add($variant)
        }
    }

    return $expanded | Select-Object -Unique
}

function Get-CoreCandidateFileNames([string]$CoreName) {
    $result = @()
    foreach ($variant in (Get-CoreLookupNames -CoreName $CoreName)) {
        $result += "$variant.so"
        $result += "${variant}_gles3_libretro_android.so"
        $result += "${variant}_gles2_libretro_android.so"
        $result += "${variant}_libretro_android.so"
        $result += "${variant}_libretro.so"
    }
    return $result | Select-Object -Unique
}

function Build-CoreFileIndex([string]$CoreRootPath) {
    $index = @{}
    if (-not (Test-Path $CoreRootPath)) {
        return $index
    }

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

function Get-SystemConfigBlocks([string]$Content) {
    $blocks = New-Object System.Collections.Generic.List[string]
    $searchStart = 0

    while ($true) {
        $start = $Content.IndexOf("SystemConfig(", $searchStart, [System.StringComparison]::Ordinal)
        if ($start -lt 0) {
            break
        }

        $i = $start + "SystemConfig(".Length
        $depth = 1
        while ($i -lt $Content.Length -and $depth -gt 0) {
            $ch = $Content[$i]
            if ($ch -eq '(') { $depth++ }
            elseif ($ch -eq ')') { $depth-- }
            $i++
        }

        if ($depth -eq 0) {
            $blocks.Add($Content.Substring($start, $i - $start))
            $searchStart = $i
        }
        else {
            break
        }
    }

    return $blocks
}

function Get-SystemsFromKotlin([string]$SystemConfigPath) {
    if (-not (Test-Path $SystemConfigPath)) {
        throw "Arquivo nao encontrado: $SystemConfigPath"
    }

    $content = Get-Content -Path $SystemConfigPath -Raw -Encoding UTF8
    $blocks = Get-SystemConfigBlocks -Content $content

    $systems = New-Object System.Collections.Generic.List[object]
    foreach ($block in $blocks) {
        if ($block -notmatch 'id\s*=\s*"([^"]+)"') { continue }
        $id = $matches[1]

        if ($block -notmatch 'coreName\s*=\s*"([^"]+)"') { continue }
        $coreName = $matches[1]

        $biosRequired = $false
        if ($block -match 'biosRequired\s*=\s*true') {
            $biosRequired = $true
        }

        $biosFileName = $null
        if ($block -match 'biosFileName\s*=\s*"([^"]+)"') {
            $biosFileName = $matches[1]
        }

        $alternatives = @()
        if ($block -match 'coreAlternatives\s*=\s*listOf\(([^\)]*)\)') {
            $altsRaw = $matches[1]
            $altMatches = [regex]::Matches($altsRaw, '"([^"]+)"')
            foreach ($m in $altMatches) {
                $alternatives += $m.Groups[1].Value
            }
        }

        $systems.Add([pscustomobject]@{
                id = $id
                coreName = $coreName
                coreAlternatives = $alternatives
                biosRequired = $biosRequired
                biosFileName = $biosFileName
            })
    }

    return $systems
}

function Get-CfgKeyMap([string]$CfgPath) {
    $map = @{}
    foreach ($line in (Get-Content -Path $CfgPath -Encoding UTF8)) {
        $trimmed = $line.Trim()
        if ([string]::IsNullOrWhiteSpace($trimmed) -or $trimmed.StartsWith("#")) {
            continue
        }
        if ($trimmed -match '^([A-Za-z0-9_\.]+)\s*=\s*"(.*)"\s*$') {
            $map[$matches[1]] = $matches[2]
        }
    }
    return $map
}

function Test-DevicePathExists([string]$RelativePath) {
    & $AdbPath @script:adbBase shell "run-as $PackageName ls '$RelativePath'" *> $null
    return ($LASTEXITCODE -eq 0)
}

Write-Host ""
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "DeepMule - Emulator and Controller Audit" -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host ("ProjectRoot: {0}" -f $ProjectRoot)
Write-Host ("PackageName: {0}" -f $PackageName)
Write-Host ("IncludeDevice: {0}" -f [bool]$IncludeDevice)
Write-Host ("StrictControllers: {0}" -f [bool]$StrictControllers)

Write-Section "[1] Sistemas, cores e BIOS (local)"

$systemConfigPath = Resolve-ProjectPath "app\src\main\java\com\example\deepmule\data\SystemConfig.kt"
$systems = Get-SystemsFromKotlin -SystemConfigPath $systemConfigPath
if ($systems.Count -gt 0) {
    Add-Result -Status "PASS" -Message ("SystemConfig parseado: {0} sistema(s)" -f $systems.Count)
}
else {
    Add-Result -Status "FAIL" -Message "Nao foi possivel extrair sistemas de SystemConfig.kt"
}

$coreRoot = Resolve-ProjectPath "cores-pack"
$coreIndex = Build-CoreFileIndex -CoreRootPath $coreRoot
if ($coreIndex.Count -eq 0) {
    Add-Result -Status "FAIL" -Message "Nenhum core .so encontrado em cores-pack"
}
else {
    $coreFilesCount = (($coreIndex.Values | ForEach-Object { $_.Count } | Measure-Object -Sum).Sum)
    Add-Result -Status "PASS" -Message ("cores-pack indexado com {0} arquivo(s) .so" -f $coreFilesCount)
}

$missingCoreCount = 0
foreach ($system in $systems) {
    $coreCandidates = @($system.coreName) + @($system.coreAlternatives)
    $resolved = $null
    $resolvedName = $null
    foreach ($candidateCore in ($coreCandidates | Select-Object -Unique)) {
        $resolved = Resolve-CoreSourceFile -FileIndex $coreIndex -CoreName $candidateCore
        if ($resolved) {
            $resolvedName = $candidateCore
            break
        }
    }

    if ($resolved) {
        Add-Result -Status "PASS" -Message ("Core OK [{0}] -> {1} ({2})" -f $system.id, $resolvedName, $resolved.Name)
    }
    else {
        $missingCoreCount++
        Add-Result -Status "FAIL" -Message ("Core ausente para sistema [{0}] (tentados: {1})" -f $system.id, (($coreCandidates | Select-Object -Unique) -join ", "))
    }
}

$biosRoot = Resolve-ProjectPath "bios-pack"
if (-not (Test-Path $biosRoot)) {
    Add-Result -Status "FAIL" -Message "Pasta bios-pack nao encontrada"
}
else {
    $requiredBiosSystems = $systems | Where-Object { $_.biosRequired }
    Add-Result -Status "PASS" -Message ("Sistemas com BIOS obrigatoria: {0}" -f $requiredBiosSystems.Count)

    foreach ($system in $requiredBiosSystems) {
        if ([string]::IsNullOrWhiteSpace($system.biosFileName)) {
            Add-Result -Status "FAIL" -Message ("Sistema [{0}] exige BIOS mas biosFileName esta vazio" -f $system.id)
            continue
        }

        $biosPath = Join-Path $biosRoot $system.biosFileName
        if (Test-Path $biosPath) {
            $size = (Get-Item $biosPath).Length
            Add-Result -Status "PASS" -Message ("BIOS OK [{0}] -> {1} ({2} bytes)" -f $system.id, $system.biosFileName, $size)
        }
        else {
            Add-Result -Status "FAIL" -Message ("BIOS ausente [{0}] -> {1}" -f $system.id, $system.biosFileName)
        }
    }
}

Write-Section "[2] Autoconfig de controles"

$requiredInputKeys = @(
    "input_a_btn",
    "input_b_btn",
    "input_start_btn",
    "input_select_btn",
    "input_up_btn",
    "input_down_btn",
    "input_left_btn",
    "input_right_btn"
)

$autoconfigRoots = @(
    @{ Name = "android"; Path = Resolve-ProjectPath "retroarch-pack\autoconfig\android" },
    @{ Name = "clover"; Path = Resolve-ProjectPath "retroarch-pack\autoconfig\clover" }
)

foreach ($root in $autoconfigRoots) {
    if (-not (Test-Path $root.Path)) {
        Add-Result -Status ($(if ($root.Name -eq "android") { "FAIL" } else { "WARN" })) -Message ("Autoconfig ausente: {0}" -f $root.Path)
        continue
    }

    $cfgFiles = Get-ChildItem -Path $root.Path -File -Filter "*.cfg" -ErrorAction SilentlyContinue
    if ($cfgFiles.Count -eq 0) {
        Add-Result -Status ($(if ($root.Name -eq "android") { "FAIL" } else { "WARN" })) -Message ("Nenhum .cfg encontrado em autoconfig/{0}" -f $root.Name)
        continue
    }

    Add-Result -Status "PASS" -Message ("autoconfig/{0}: {1} perfil(is)" -f $root.Name, $cfgFiles.Count)

    $complete = 0
    $partialProfiles = New-Object System.Collections.Generic.List[object]
    foreach ($cfg in $cfgFiles) {
        $map = Get-CfgKeyMap -CfgPath $cfg.FullName
        $missing = @($requiredInputKeys | Where-Object { -not $map.ContainsKey($_) })
        if ($missing.Count -eq 0) {
            $complete++
        }
        else {
            $partialProfiles.Add([pscustomobject]@{
                file    = $cfg.Name
                missing = $missing
            })
        }
    }

    $partial = $partialProfiles.Count

    if ($partial -eq 0) {
        Add-Result -Status "PASS" -Message ("autoconfig/{0}: todos os perfis com mapeamento base completo" -f $root.Name)
    }
    else {
        $partialStatus = if ($StrictControllers) { "FAIL" } else { "WARN" }
        Add-Result -Status $partialStatus -Message ("autoconfig/{0}: {1} completo(s), {2} parcial(is)" -f $root.Name, $complete, $partial)

        # Emite detalhe individual somente se StrictControllers ou se for poucos perfis
        $showDetail = $StrictControllers -or ($partial -le 10)
        if ($showDetail) {
            foreach ($p in $partialProfiles) {
                Write-Host ("         [partial] {0} -> faltam: {1}" -f $p.file, ($p.missing -join ", ")) -ForegroundColor DarkYellow
            }
        }

        # Sempre salva relatório detalhado de parciais
        $partialReportDir = Join-Path (Resolve-ProjectPath $ReportDir) "partial-controllers"
        New-Item -Path $partialReportDir -ItemType Directory -Force | Out-Null
        $partialReportPath = Join-Path $partialReportDir ("partial-{0}.txt" -f $root.Name)
        $lines = @("Perfis de controle parciais - autoconfig/{0}" -f $root.Name, "Gerado: $(Get-Date -Format 's')", "")
        foreach ($p in ($partialProfiles | Sort-Object file)) {
            $lines += ("{0}" -f $p.file)
            $lines += ("  Binds faltantes: {0}" -f ($p.missing -join ", "))
            $lines += ""
        }
        Set-Content -Path $partialReportPath -Value $lines -Encoding UTF8
        Write-Host ("         Relatorio parciais: {0}" -f $partialReportPath) -ForegroundColor DarkYellow
    }
}

$retroarchCfg = Resolve-ProjectPath "retroarch-pack\config\retroarch.cfg"
if (Test-Path $retroarchCfg) {
    $cfgMap = Get-CfgKeyMap -CfgPath $retroarchCfg

    if ($cfgMap.ContainsKey("joypad_autoconfig_dir")) {
        Add-Result -Status "PASS" -Message ("retroarch.cfg possui joypad_autoconfig_dir = {0}" -f $cfgMap["joypad_autoconfig_dir"])
    }
    else {
        Add-Result -Status "FAIL" -Message "retroarch.cfg sem joypad_autoconfig_dir"
    }

    if ($cfgMap.ContainsKey("input_autodetect_enable") -and $cfgMap["input_autodetect_enable"].ToLowerInvariant() -eq "true") {
        Add-Result -Status "PASS" -Message "retroarch.cfg com input_autodetect_enable = true"
    }
    else {
        Add-Result -Status "WARN" -Message "retroarch.cfg sem input_autodetect_enable=true"
    }
}
else {
    Add-Result -Status "FAIL" -Message "retroarch-pack/config/retroarch.cfg nao encontrado"
}

if ($IncludeDevice) {
    Write-Section "[3] Sinais de resposta no emulador/dispositivo (adb)"

    if (-not (Test-Path $AdbPath)) {
        Add-Result -Status "FAIL" -Message ("adb nao encontrado em: {0}" -f $AdbPath)
    }
    else {
        Add-Result -Status "PASS" -Message ("adb encontrado em: {0}" -f $AdbPath)

        $devices = & $AdbPath @script:adbBase devices | Select-String "\tdevice$"
        if (-not $devices) {
            Add-Result -Status "FAIL" -Message "Nenhum dispositivo em estado device no adb"
        }
        else {
            foreach ($d in $devices) {
                Add-Result -Status "PASS" -Message ("Dispositivo detectado: {0}" -f $d.Line)
            }

            $bootCompleted = (& $AdbPath @script:adbBase shell getprop sys.boot_completed 2>$null | Out-String).Trim()
            if ($bootCompleted -eq "1") {
                Add-Result -Status "PASS" -Message "sys.boot_completed = 1"
            }
            else {
                Add-Result -Status "WARN" -Message ("sys.boot_completed retornou: {0}" -f $bootCompleted)
            }

            & $AdbPath @script:adbBase shell "run-as $PackageName pwd" *> $null
            if ($LASTEXITCODE -eq 0) {
                Add-Result -Status "PASS" -Message "run-as do app respondeu"
            }
            else {
                Add-Result -Status "FAIL" -Message "run-as falhou; valide app debug instalado"
            }

            if (Test-DevicePathExists "files/cores") {
                Add-Result -Status "PASS" -Message "files/cores presente no app"
            }
            else {
                Add-Result -Status "FAIL" -Message "files/cores ausente no app"
            }
        }
    }
}
else {
    Write-Section "[3] Sinais de resposta no emulador/dispositivo (adb)"
    Add-Result -Status "WARN" -Message "Modo dispositivo nao solicitado; use -IncludeDevice para validar resposta via adb"
}

$reportRoot = Resolve-ProjectPath $ReportDir
New-Item -Path $reportRoot -ItemType Directory -Force | Out-Null

$txtReport = Join-Path $reportRoot "test-emulators-report.txt"
$jsonReport = Join-Path $reportRoot "test-emulators-report.json"

$summaryLines = @(
    "DeepMule Emulator/Controller Audit",
    "PASS: $($script:PassCount)",
    "WARN: $($script:WarnCount)",
    "FAIL: $($script:FailCount)",
    ""
)
$summaryLines += ($script:Results | ForEach-Object { "[$($_.status)] $($_.message)" })
Set-Content -Path $txtReport -Value $summaryLines -Encoding UTF8

$reportObject = [pscustomobject]@{
    generatedAt = (Get-Date).ToString("s")
    projectRoot = $ProjectRoot
    includeDevice = [bool]$IncludeDevice
    totals = [pscustomobject]@{
        pass = $script:PassCount
        warn = $script:WarnCount
        fail = $script:FailCount
    }
    results = $script:Results
}
$reportObject | ConvertTo-Json -Depth 5 | Set-Content -Path $jsonReport -Encoding UTF8

Write-Host ""
Write-Host "================================" -ForegroundColor Cyan
Write-Host "Resumo final" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ("PASS: {0}" -f $script:PassCount) -ForegroundColor Green
Write-Host ("WARN: {0}" -f $script:WarnCount) -ForegroundColor Yellow
Write-Host ("FAIL: {0}" -f $script:FailCount) -ForegroundColor Red
Write-Host ("Relatorio TXT: {0}" -f $txtReport)
Write-Host ("Relatorio JSON: {0}" -f $jsonReport)

if ($script:FailCount -gt 0) {
    exit 1
}

