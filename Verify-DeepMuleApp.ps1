param(
    [string]$ProjectRoot = "C:\Users\robso\AndroidStudioProjects\DeepMule",
    [string]$PackageName = "com.example.deepmule",
    [string]$AdbPath = "F:\Android\SDK\platform-tools\adb.exe",
    [switch]$IncludeDevice,
    [switch]$RunLint,
    [switch]$RunUnitTests
)

$ErrorActionPreference = "Stop"

$script:PassCount = 0
$script:WarnCount = 0
$script:FailCount = 0

function Write-Section([string]$Title) {
    Write-Host ""
    Write-Host $Title -ForegroundColor Cyan
    Write-Host ("-" * $Title.Length) -ForegroundColor Cyan
}

function Add-Result([ValidateSet("PASS", "WARN", "FAIL")] [string]$Status, [string]$Message) {
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
    return (Join-Path $ProjectRoot $RelativePath)
}

function Get-CoreNameVariants([string]$CoreName) {
    $variants = @($CoreName, $CoreName.Replace("-", "_"), $CoreName.Replace("_", "-"))
    return $variants | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -Unique
}

$coreAliases = @{
    "mupen64plus_next_gles3" = @("mupen64plus_next_gles2", "mupen64plus_next", "mupen64plus")
    "pcsx_rearmed" = @("pcsx-rearmed")
    "mednafen_pce_fast" = @("beetle_pce_fast", "mednafen_pce")
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

function Test-DevicePathExists([string]$RelativePath) {
    # Usa test -e dentro do shell para checar existencia sem gerar erro no stderr.
    & $AdbPath shell "run-as $PackageName sh -c 'test -e ''$RelativePath'''" 2>$null | Out-Null
    return ($LASTEXITCODE -eq 0)
}

function Get-DeviceFileSize([string]$RelativePath) {
    $output = & $AdbPath shell "run-as $PackageName ls -ln '$RelativePath'" 2>$null
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace(($output | Out-String))) {
        return $null
    }

    $line = (($output | Select-Object -Last 1) -as [string]).Trim()
    $parts = $line -split '\s+'
    if ($parts.Count -lt 5) {
        return $null
    }

    [long]$size = 0
    if ([long]::TryParse($parts[4], [ref]$size)) {
        return $size
    }

    return $null
}

function Get-DeviceEntryCount([string]$RelativePath) {
    $output = & $AdbPath shell "run-as $PackageName sh -c 'find ''$RelativePath'' -mindepth 1 | wc -l'" 2>$null
    if ($LASTEXITCODE -ne 0) {
        return $null
    }

    [int]$count = 0
    $text = ($output | Out-String).Trim()
    if ([int]::TryParse($text, [ref]$count)) {
        return $count
    }
    return $null
}

function Invoke-GradleTask([string]$TaskName, [switch]$Required) {
    Write-Host ("Executando Gradle: {0}" -f $TaskName) -ForegroundColor Yellow
    Push-Location $ProjectRoot
    try {
        $output = & .\gradlew.bat $TaskName 2>&1
        if ($LASTEXITCODE -eq 0) {
            Add-Result -Status "PASS" -Message ("Gradle {0} concluido com sucesso" -f $TaskName)
        }
        else {
            Add-Result -Status ($(if ($Required) { "FAIL" } else { "WARN" })) -Message ("Gradle {0} falhou (codigo {1})" -f $TaskName, $LASTEXITCODE)
            $output | Select-Object -Last 15 | ForEach-Object { Write-Host $_ }
        }
    }
    catch {
        Add-Result -Status ($(if ($Required) { "FAIL" } else { "WARN" })) -Message ("Erro ao executar Gradle {0}: {1}" -f $TaskName, $_.Exception.Message)
    }
    finally {
        Pop-Location
    }
}

$requiredBios = @(
    "lynxboot.img",
    "bios_CD_U.bin",
    "scph1001.bin"
)

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
    "mupen64plus_next_gles3",
    "genesis_plus_gx",
    "pcsx_rearmed",
    "ppsspp",
    "fbneo",
    "mednafen_pce_fast",
    "mednafen_ngp"
)

$retroarchPackages = @(
    @{ Name = "assets"; Required = $true },
    @{ Name = "autoconfig"; Required = $true },
    @{ Name = "config"; Required = $true },
    @{ Name = "database"; Required = $true },
    @{ Name = "info"; Required = $true },
    @{ Name = "cheats"; Required = $false },
    @{ Name = "thumbnails"; Required = $false }
)

Write-Host ""
Write-Host "================================" -ForegroundColor Cyan
Write-Host "DeepMule App - Sanity Check" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ("ProjectRoot: {0}" -f $ProjectRoot)
Write-Host ("PackageName: {0}" -f $PackageName)
Write-Host ("IncludeDevice: {0}" -f [bool]$IncludeDevice)

Write-Section "[1] Build e artefatos"

$gradlewPath = Resolve-ProjectPath "gradlew.bat"
if (Test-Path $gradlewPath) {
    Add-Result -Status "PASS" -Message "gradlew.bat encontrado"
    Invoke-GradleTask -TaskName ":app:assembleDebug" -Required
}
else {
    Add-Result -Status "FAIL" -Message "gradlew.bat nao encontrado no projeto"
}

if ($RunLint) {
    Invoke-GradleTask -TaskName "lint" -Required:$false
}

if ($RunUnitTests) {
    Invoke-GradleTask -TaskName ":app:testDebugUnitTest" -Required:$false
}

$apkPath = Resolve-ProjectPath "app\build\outputs\apk\debug\app-debug.apk"
if (Test-Path $apkPath) {
    $apkSize = (Get-Item $apkPath).Length
    $apkSizeMb = "{0:N2}" -f ($apkSize / 1MB)
    Add-Result -Status "PASS" -Message ("APK debug encontrado: {0} MB" -f $apkSizeMb)
}
else {
    Add-Result -Status "FAIL" -Message "APK debug nao encontrado em app\\build\\outputs\\apk\\debug\\app-debug.apk"
}

$buildGradlePath = Resolve-ProjectPath "app\build.gradle.kts"
if (Test-Path $buildGradlePath) {
    $buildGradle = Get-Content $buildGradlePath -Raw
    if ($buildGradle -match 'applicationId\s*=\s*"com\.example\.deepmule"') {
        Add-Result -Status "PASS" -Message "applicationId esperado encontrado"
    }
    else {
        Add-Result -Status "WARN" -Message "applicationId difere do valor esperado em app/build.gradle.kts"
    }

    if ($buildGradle -match "compose\.material3|material3") {
        Add-Result -Status "PASS" -Message "Dependencia de Material 3 detectada"
    }
    else {
        Add-Result -Status "WARN" -Message "Material 3 nao foi detectado em app/build.gradle.kts"
    }
}
else {
    Add-Result -Status "FAIL" -Message "app/build.gradle.kts nao encontrado"
}

$ptBRFile = Resolve-ProjectPath "app\src\main\res\values-pt-rBR\strings.xml"
if (Test-Path $ptBRFile) {
    $stringCount = (Get-Content $ptBRFile | Select-String '<string' | Measure-Object).Count
    Add-Result -Status "PASS" -Message ("Traducao pt-BR encontrada com {0} string(s)" -f $stringCount)
}
else {
    Add-Result -Status "FAIL" -Message "Arquivo pt-BR nao encontrado em app/src/main/res/values-pt-rBR/strings.xml"
}

Write-Section "[2] Pacotes locais de provisionamento"

$biosPack = Resolve-ProjectPath "bios-pack"
if (Test-Path $biosPack) {
    Add-Result -Status "PASS" -Message "Pasta bios-pack encontrada"
    foreach ($bios in $requiredBios) {
        $biosPath = Join-Path $biosPack $bios
        if (Test-Path $biosPath) {
            $size = (Get-Item $biosPath).Length
            Add-Result -Status "PASS" -Message ("BIOS obrigatoria presente: {0} ({1} bytes)" -f $bios, $size)
        }
        else {
            Add-Result -Status "FAIL" -Message ("BIOS obrigatoria ausente em bios-pack: {0}" -f $bios)
        }
    }
}
else {
    Add-Result -Status "FAIL" -Message "Pasta bios-pack nao encontrada"
}

$corePack = Resolve-ProjectPath "cores-pack"
if (Test-Path $corePack) {
    $coreIndex = Build-CoreFileIndex -CoreRootPath $corePack
    if ($coreIndex.Count -gt 0) {
        Add-Result -Status "PASS" -Message ("cores-pack encontrado com {0} arquivo(s) .so indexado(s)" -f (($coreIndex.Values | ForEach-Object { $_.Count } | Measure-Object -Sum).Sum))
        foreach ($core in $requiredCores) {
            $resolved = Resolve-CoreSourceFile -FileIndex $coreIndex -CoreName $core
            if ($resolved) {
                Add-Result -Status "PASS" -Message ("Core local resolvido: {0}.so <= {1}" -f $core, $resolved.Name)
            }
            else {
                Add-Result -Status "FAIL" -Message ("Core local ausente: {0}" -f $core)
            }
        }
    }
    else {
        Add-Result -Status "FAIL" -Message "Nenhum .so encontrado em cores-pack"
    }
}
else {
    Add-Result -Status "FAIL" -Message "Pasta cores-pack nao encontrada"
}

$retroarchPack = Resolve-ProjectPath "retroarch-pack"
if (Test-Path $retroarchPack) {
    Add-Result -Status "PASS" -Message "Pasta retroarch-pack encontrada"
    foreach ($pkg in $retroarchPackages) {
        $pkgPath = Join-Path $retroarchPack $pkg.Name
        if (Test-Path $pkgPath) {
            $fileCount = (Get-ChildItem -Path $pkgPath -Recurse -File -ErrorAction SilentlyContinue | Measure-Object).Count
            Add-Result -Status "PASS" -Message ("Pacote local {0}: {1} arquivo(s)" -f $pkg.Name, $fileCount)
        }
        else {
            Add-Result -Status ($(if ($pkg.Required) { "FAIL" } else { "WARN" })) -Message ("Pacote local ausente: retroarch-pack/{0}" -f $pkg.Name)
        }
    }

    $retroarchCfg = Join-Path $retroarchPack "config\retroarch.cfg"
    if (Test-Path $retroarchCfg) {
        Add-Result -Status "PASS" -Message "retroarch.cfg local encontrado"
    }
    else {
        Add-Result -Status "FAIL" -Message "retroarch-pack/config/retroarch.cfg ausente"
    }

    $coreOptionsCfg = Join-Path $retroarchPack "config\retroarch-core-options.cfg"
    if (Test-Path $coreOptionsCfg) {
        Add-Result -Status "PASS" -Message "retroarch-core-options.cfg local encontrado"
    }
    else {
        Add-Result -Status "WARN" -Message "retroarch-core-options.cfg local nao encontrado"
    }

    $libretroDb = Join-Path $retroarchPack "database\libretro-db.sqlite"
    if (Test-Path $libretroDb) {
        $dbSize = (Get-Item $libretroDb).Length
        Add-Result -Status "PASS" -Message ("libretro-db.sqlite local encontrado ({0} bytes)" -f $dbSize)
    }
    else {
        Add-Result -Status "WARN" -Message "libretro-db.sqlite local nao encontrado em retroarch-pack/database"
    }
}
else {
    Add-Result -Status "WARN" -Message "Pasta retroarch-pack nao encontrada; checks de extras locais foram ignorados"
}

$projectRoms = Resolve-ProjectPath "Roms"
if (Test-Path $projectRoms) {
    $romCount = (Get-ChildItem -Path $projectRoms -Recurse -File -ErrorAction SilentlyContinue | Measure-Object).Count
    Add-Result -Status "PASS" -Message ("Pasta Roms encontrada com {0} arquivo(s)" -f $romCount)
}
else {
    Add-Result -Status "WARN" -Message "Pasta Roms nao encontrada no projeto"
}

if ($IncludeDevice) {
    Write-Section "[3] Auditoria do app no dispositivo"

    if (-not (Test-Path $AdbPath)) {
        Add-Result -Status "FAIL" -Message ("adb nao encontrado em: {0}" -f $AdbPath)
    }
    else {
        Add-Result -Status "PASS" -Message ("adb encontrado em: {0}" -f $AdbPath)

        $deviceList = & $AdbPath devices | Select-String "\tdevice$"
        if (-not $deviceList) {
            Add-Result -Status "FAIL" -Message "Nenhum dispositivo em estado 'device' encontrado no adb devices"
        }
        else {
            foreach ($device in $deviceList) {
                Add-Result -Status "PASS" -Message ("Dispositivo conectado: {0}" -f $device.Line)
            }

            & $AdbPath shell "run-as $PackageName pwd" *> $null
            if ($LASTEXITCODE -eq 0) {
                Add-Result -Status "PASS" -Message "run-as do pacote respondeu corretamente"
            }
            else {
                Add-Result -Status "FAIL" -Message "run-as falhou; confirme app debug instalado e pacote correto"
            }

            if (Test-DevicePathExists "files/bios") {
                Add-Result -Status "PASS" -Message "files/bios existe no app"
            }
            else {
                Add-Result -Status "FAIL" -Message "files/bios nao existe no app"
            }

            foreach ($bios in $requiredBios) {
                if (Test-DevicePathExists "files/bios/$bios") {
                    $size = Get-DeviceFileSize "files/bios/$bios"
                    if ($null -ne $size) {
                        Add-Result -Status "PASS" -Message ("BIOS no app: {0} ({1} bytes)" -f $bios, $size)
                    }
                    else {
                        Add-Result -Status "PASS" -Message ("BIOS no app: {0}" -f $bios)
                    }
                }
                else {
                    Add-Result -Status "FAIL" -Message ("BIOS ausente no app: {0}" -f $bios)
                }
            }

            if (Test-DevicePathExists "files/cores") {
                Add-Result -Status "PASS" -Message "files/cores existe no app"
            }
            else {
                Add-Result -Status "FAIL" -Message "files/cores nao existe no app"
            }

            foreach ($core in $requiredCores) {
                $found = $false
                foreach ($candidate in (Get-CoreLookupNames -CoreName $core)) {
                    if (Test-DevicePathExists "files/cores/$candidate.so") {
                        Add-Result -Status "PASS" -Message ("Core no app: {0}.so (resolve {1})" -f $candidate, $core)
                        $found = $true
                        break
                    }
                }

                if (-not $found) {
                    Add-Result -Status "FAIL" -Message ("Core ausente no app: {0}" -f $core)
                }
            }

            if (Test-DevicePathExists "files/retroarch") {
                Add-Result -Status "PASS" -Message "files/retroarch existe no app"
            }
            else {
                Add-Result -Status "FAIL" -Message "files/retroarch nao existe no app"
            }

            if (Test-DevicePathExists "files/roms") {
                $romEntries = Get-DeviceEntryCount "files/roms"
                if ($null -ne $romEntries) {
                    Add-Result -Status "PASS" -Message ("files/roms existe no app com {0} entrada(s)" -f $romEntries)
                }
                else {
                    Add-Result -Status "PASS" -Message "files/roms existe no app"
                }
            }
            else {
                Add-Result -Status "WARN" -Message "files/roms ainda nao existe no app"
            }

            foreach ($pkg in $retroarchPackages) {
                $appPath = "files/retroarch/$($pkg.Name)"
                if (Test-DevicePathExists $appPath) {
                    Add-Result -Status "PASS" -Message ("Pacote no app presente: {0}" -f $appPath)
                }
                else {
                    Add-Result -Status ($(if ($pkg.Required) { "FAIL" } else { "WARN" })) -Message ("Pacote no app ausente: {0}" -f $appPath)
                }
            }

            if (Test-DevicePathExists "files/retroarch/config/retroarch.cfg") {
                Add-Result -Status "PASS" -Message "retroarch.cfg presente no app"
            }
            else {
                Add-Result -Status "FAIL" -Message "retroarch.cfg ausente no app"
            }

            if (Test-DevicePathExists "files/retroarch/config/retroarch-core-options.cfg") {
                Add-Result -Status "PASS" -Message "retroarch-core-options.cfg presente no app"
            }
            else {
                Add-Result -Status "WARN" -Message "retroarch-core-options.cfg ausente no app"
            }

            if (Test-DevicePathExists "files/retroarch/database/libretro-db.sqlite") {
                $dbSize = Get-DeviceFileSize "files/retroarch/database/libretro-db.sqlite"
                if ($null -ne $dbSize) {
                    Add-Result -Status "PASS" -Message ("libretro-db.sqlite presente no app ({0} bytes)" -f $dbSize)
                }
                else {
                    Add-Result -Status "PASS" -Message "libretro-db.sqlite presente no app"
                }
            }
            else {
                Add-Result -Status "WARN" -Message "libretro-db.sqlite ausente no app"
            }
        }
    }
}
else {
    Write-Section "[3] Auditoria do app no dispositivo"
    Add-Result -Status "WARN" -Message "Modo dispositivo nao solicitado; use -IncludeDevice para validar arquivos provisionados no app"
}

Write-Host ""
Write-Host "================================" -ForegroundColor Cyan
Write-Host "Resumo final" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ("PASS: {0}" -f $script:PassCount) -ForegroundColor Green
Write-Host ("WARN: {0}" -f $script:WarnCount) -ForegroundColor Yellow
Write-Host ("FAIL: {0}" -f $script:FailCount) -ForegroundColor Red
Write-Host ""

if ($script:FailCount -gt 0) {
    Write-Host "Sanity check concluido com falhas." -ForegroundColor Red
    exit 1
}

Write-Host "Sanity check concluido sem falhas bloqueantes." -ForegroundColor Green

