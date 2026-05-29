param(
    [string]$ProjectRoot = "C:\Users\robso\AndroidStudioProjects\DeepMule",
    [string]$PackageName = "com.example.deepmule",
    [string]$AdbPath = "F:\Android\SDK\platform-tools\adb.exe",
    [string]$DeviceSerial = "",
    [string]$CorePackDir = "cores-pack",
    [string]$ExtrasSourceRoot = "retroarch-pack",
    [string]$RomsRoot = "",
    [switch]$SyncOfficialLibretro,
    [int]$RetryCount = 3,
    [int]$RetryDelaySeconds = 3,
    [int]$ChunkThresholdFiles = 1000,
    [int]$MaxRomsPerSystem = 5,
    [int]$TotalRoms = 0,
    [switch]$Bios,
    [switch]$Cores,
    [switch]$Extras,
    [switch]$Roms,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

function Invoke-StepWithRetry([scriptblock]$Step, [string]$Label, [int]$Retries, [int]$DelaySeconds) {
    $attempt = 0
    while ($attempt -lt $Retries) {
        $attempt++
        try {
            & $Step
            return
        }
        catch {
            if ($attempt -ge $Retries) {
                throw "${Label}: $_"
            }
            Write-Host ("[retry {0}/{1}] {2}" -f $attempt, $Retries, $Label)
            Start-Sleep -Seconds $DelaySeconds
        }
    }
}

function Invoke-ProvisionScript([string]$ScriptPath, [hashtable]$Parameters) {
    if (-not (Test-Path $ScriptPath)) {
        throw "Script nao encontrado: $ScriptPath"
    }

    # Usa splatting nativo para evitar problemas de ordenacao/conversao de argumentos.
    $forward = @{}
    foreach ($key in $Parameters.Keys) {
        $value = $Parameters[$key]
        if ($null -eq $value) {
            continue
        }
        if ($value -is [string] -and [string]::IsNullOrWhiteSpace($value)) {
            continue
        }
        $forward[$key] = $value
    }

    & $ScriptPath @forward
}

if ($RetryCount -lt 1) {
    throw "RetryCount deve ser >= 1"
}

if ($RetryDelaySeconds -lt 1) {
    throw "RetryDelaySeconds deve ser >= 1"
}

$runBios = $Bios
$runCores = $Cores
$runExtras = $Extras
$runRoms = $Roms

if (-not ($runBios -or $runCores -or $runExtras -or $runRoms)) {
    $runBios = $true
    $runCores = $true
    $runExtras = $true
    $runRoms = $true
}

$biosScript = Join-Path $ProjectRoot "Provision-DeepMuleBios.ps1"
$coresScript = Join-Path $ProjectRoot "Provision-DeepMuleCores.ps1"
$extrasScript = Join-Path $ProjectRoot "Provision-DeepMuleExtras.ps1"
$romsScript = Join-Path $ProjectRoot "Provision-DeepMuleClassicRoms.ps1"
$syncScript = Join-Path $ProjectRoot "Sync-LibretroResources.ps1"

Write-Host "=== Provision ALL DeepMule ==="
Write-Host "ProjectRoot: $ProjectRoot"
Write-Host "PackageName: $PackageName"
Write-Host ("RomsRoot: {0}" -f $(if ([string]::IsNullOrWhiteSpace($RomsRoot)) { Join-Path $ProjectRoot "Roms" } else { $RomsRoot }))
Write-Host "DryRun: $DryRun"

if ($SyncOfficialLibretro -and -not $DryRun) {
    Write-Host ""
    Write-Host "[0/3] Sincronizando fontes oficiais libretro (database/thumbnails)..."
    Invoke-StepWithRetry -Retries $RetryCount -DelaySeconds $RetryDelaySeconds -Label "Sync libretro" -Step {
        Invoke-ProvisionScript -ScriptPath $syncScript -Parameters @{
            ProjectRoot = $ProjectRoot
        }
    }
}

if ($runBios) {
    if ($DryRun) {
        Write-Host "[DryRun] Bios: script nao possui modo dry-run; etapa ignorada."
    }
    else {
        Write-Host ""
        Write-Host "[1/3] Provisionando BIOS obrigatorias..."
        Invoke-StepWithRetry -Retries $RetryCount -DelaySeconds $RetryDelaySeconds -Label "Provision BIOS" -Step {
            Invoke-ProvisionScript -ScriptPath $biosScript -Parameters @{
                ProjectRoot = $ProjectRoot
                PackageName = $PackageName
                AdbPath = $AdbPath
                DeviceSerial = $DeviceSerial
            }
        }
    }
}

if ($runCores) {
    Write-Host ""
    Write-Host "[2/3] Provisionando cores..."
    Invoke-StepWithRetry -Retries $RetryCount -DelaySeconds $RetryDelaySeconds -Label "Provision cores" -Step {
        Invoke-ProvisionScript -ScriptPath $coresScript -Parameters @{
            ProjectRoot = $ProjectRoot
            PackageName = $PackageName
            AdbPath = $AdbPath
            DeviceSerial = $DeviceSerial
            CorePackDir = $CorePackDir
            DryRun = [bool]$DryRun
        }
    }
}

if ($runExtras) {
    Write-Host ""
    Write-Host "[3/4] Provisionando pacotes extras (modo resiliente)..."

    $extrasOrder = @(
        "Assets",
        "Thumbnails",
        "Cheats",
        "Autoconfig",
        "Info",
        "Config",
        "Database"
    )

    foreach ($packageSwitch in $extrasOrder) {
        Write-Host (" - Extra: {0}" -f $packageSwitch)
        Invoke-StepWithRetry -Retries $RetryCount -DelaySeconds $RetryDelaySeconds -Label ("Provision extras $packageSwitch") -Step {
            $scriptArgs = @{
                ProjectRoot = $ProjectRoot
                PackageName = $PackageName
                AdbPath = $AdbPath
                DeviceSerial = $DeviceSerial
                SourceRoot = $ExtrasSourceRoot
                RetryCount = $RetryCount
                RetryDelaySeconds = $RetryDelaySeconds
                ChunkThresholdFiles = $ChunkThresholdFiles
                DryRun = [bool]$DryRun
            }

            $scriptArgs[$packageSwitch] = $true
            if ($packageSwitch -eq "Database") {
                $scriptArgs["ChunkedPush"] = $true
            }

            Invoke-ProvisionScript -ScriptPath $extrasScript -Parameters $scriptArgs
        }
    }
}

if ($runRoms) {
    Write-Host ""
    Write-Host "[4/4] Provisionando ROMs do projeto..."
    Invoke-StepWithRetry -Retries $RetryCount -DelaySeconds $RetryDelaySeconds -Label "Provision ROMs" -Step {
        Invoke-ProvisionScript -ScriptPath $romsScript -Parameters @{
            ProjectRoot = $ProjectRoot
            RomsRoot = $RomsRoot
            PackageName = $PackageName
            AdbPath = $AdbPath
            DeviceSerial = $DeviceSerial
            MaxPerSystem = $MaxRomsPerSystem
            TotalGames = $TotalRoms
            DryRun = [bool]$DryRun
        }
    }
}

Write-Host ""
Write-Host "Provisionamento ALL concluido."



