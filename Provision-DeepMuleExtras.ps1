param(
    [string]$ProjectRoot = "C:\Users\robso\AndroidStudioProjects\DeepMule",
    [string]$PackageName = "com.example.deepmule",
    [string]$AdbPath = "F:\Android\SDK\platform-tools\adb.exe",
    [string]$SourceRoot = "retroarch-pack",
    [string]$DeviceSerial = "",
    [switch]$Assets,
    [switch]$Thumbnails,
    [switch]$Database,
    [switch]$Cheats,
    [switch]$Autoconfig,
    [switch]$Info,
    [switch]$Config,
    [int]$RetryCount = 3,
    [int]$RetryDelaySeconds = 3,
    [int]$ChunkThresholdFiles = 1000,
    [switch]$ChunkedPush,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

function Resolve-RootPath([string]$PathValue, [string]$BasePath) {
    if ([System.IO.Path]::IsPathRooted($PathValue)) {
        return $PathValue
    }
    return (Join-Path $BasePath $PathValue)
}

function Invoke-OperationWithRetry([scriptblock]$Operation, [string]$Label, [int]$Retries, [int]$DelaySeconds) {
    $attempt = 0
    while ($attempt -lt $Retries) {
        $attempt++
        try {
            $prev = $ErrorActionPreference
            $ErrorActionPreference = "SilentlyContinue"
            & $Operation
            $ErrorActionPreference = $prev
            if ($LASTEXITCODE -ne 0) {
                throw "falhou com codigo de saida $LASTEXITCODE"
            }
            return
        }
        catch {
            $ErrorActionPreference = $prev
            if ($attempt -ge $Retries) {
                throw "${Label}: $_"
            }
            Write-Host ("[retry {0}/{1}] {2}" -f $attempt, $Retries, $Label)
            Start-Sleep -Seconds $DelaySeconds
        }
    }
}

function Get-RelativeUnixPath([string]$RootPath, [string]$FilePath) {
    $relative = $FilePath.Substring($RootPath.Length) -replace '^[\\\/]+', ''
    return $relative.Replace('\', '/')
}

function Push-DirectoryFileByFile(
    [string]$SourcePath,
    [string]$TmpPath,
    [string]$AdbBinary,
    [string[]]$AdbBaseArgs,
    [int]$Retries,
    [int]$DelaySeconds
) {
    $files = Get-ChildItem -Path $SourcePath -Recurse -File -ErrorAction SilentlyContinue
    $relativePaths = New-Object System.Collections.Generic.List[string]

    foreach ($file in $files) {
        $relative = Get-RelativeUnixPath -RootPath $SourcePath -FilePath $file.FullName
        $relativePaths.Add($relative)

        $remoteDir = [System.IO.Path]::GetDirectoryName($relative)
        if (-not [string]::IsNullOrWhiteSpace($remoteDir)) {
            $remoteDir = $remoteDir.Replace('\', '/')
            Invoke-OperationWithRetry -Retries $Retries -DelaySeconds $DelaySeconds -Label ("mkdir tmp/{0}" -f $remoteDir) -Operation {
                & $AdbBinary @AdbBaseArgs shell "mkdir -p '$TmpPath/$remoteDir'" | Out-Null
            }
        }

        Invoke-OperationWithRetry -Retries $Retries -DelaySeconds $DelaySeconds -Label ("adb push {0}" -f $relative) -Operation {
            & $AdbBinary @AdbBaseArgs push $file.FullName "$TmpPath/$relative" | Out-Null
        }
    }

    return $relativePaths
}

function Copy-ChunkedToApp(
    [string]$TmpPath,
    [string]$AppDest,
    [string]$PkgName,
    [string[]]$RelativePaths,
    [string]$AdbBinary,
    [string[]]$AdbBaseArgs,
    [int]$Retries,
    [int]$DelaySeconds
) {
    $dirs = New-Object System.Collections.Generic.HashSet[string]
    $null = $dirs.Add($AppDest)

    foreach ($relative in $RelativePaths) {
        $parent = [System.IO.Path]::GetDirectoryName($relative)
        if (-not [string]::IsNullOrWhiteSpace($parent)) {
            $parent = $parent.Replace('\', '/')
            $null = $dirs.Add("$AppDest/$parent")
        }
    }

    foreach ($dir in ($dirs | Sort-Object)) {
        Invoke-OperationWithRetry -Retries $Retries -DelaySeconds $DelaySeconds -Label ("run-as mkdir {0}" -f $dir) -Operation {
            & $AdbBinary @AdbBaseArgs shell "run-as $PkgName mkdir -p '$dir'" | Out-Null
        }
    }

    foreach ($relative in $RelativePaths) {
        Invoke-OperationWithRetry -Retries $Retries -DelaySeconds $DelaySeconds -Label ("run-as cp {0}" -f $relative) -Operation {
            & $AdbBinary @AdbBaseArgs shell "run-as $PkgName cp '$TmpPath/$relative' '$AppDest/$relative'" | Out-Null
        }
    }
}

function Get-SelectedPackages {
    $selected = New-Object System.Collections.Generic.List[string]

    if ($Assets) { $selected.Add("assets") }
    if ($Thumbnails) { $selected.Add("thumbnails") }
    if ($Database) { $selected.Add("database") }
    if ($Cheats) { $selected.Add("cheats") }
    if ($Autoconfig) { $selected.Add("autoconfig") }
    if ($Info) { $selected.Add("info") }
    if ($Config) { $selected.Add("config") }

    # Sem filtro explicito, provisiona todos os opcionais.
    if ($selected.Count -eq 0) {
        $selected.Add("assets")
        $selected.Add("thumbnails")
        $selected.Add("database")
        $selected.Add("cheats")
        $selected.Add("autoconfig")
        $selected.Add("info")
        $selected.Add("config")
    }

    return $selected
}

$sourceRootPath = Resolve-RootPath -PathValue $SourceRoot -BasePath $ProjectRoot
if (-not (Test-Path $sourceRootPath)) {
    throw "Pasta de origem nao encontrada: $sourceRootPath"
}

if ($RetryCount -lt 1) {
    throw "RetryCount deve ser >= 1"
}

if ($RetryDelaySeconds -lt 1) {
    throw "RetryDelaySeconds deve ser >= 1"
}

$selectedPackages = Get-SelectedPackages
$available = @{}

foreach ($pkg in $selectedPackages) {
    $pkgPath = Join-Path $sourceRootPath $pkg
    if (Test-Path $pkgPath) {
        $fileCount = (Get-ChildItem -Path $pkgPath -Recurse -File -ErrorAction SilentlyContinue | Measure-Object).Count
        $available[$pkg] = [ordered]@{
            Source = $pkgPath
            FileCount = $fileCount
        }
    }
}

if ($available.Count -eq 0) {
    Write-Host "Nenhum pacote opcional encontrado na origem informada."
    Write-Host "Esperado em: $sourceRootPath"
    Write-Host "Subpastas: assets, thumbnails, database, cheats, autoconfig, info, config"
    return
}

Write-Host "Origem detectada: $sourceRootPath"
Write-Host "Pacotes opcionais detectados:"
foreach ($pkg in ($available.Keys | Sort-Object)) {
    Write-Host (" - {0}: {1} arquivo(s)" -f $pkg, $available[$pkg].FileCount)
}

if ($DryRun) {
    Write-Host ""
    Write-Host "[DryRun] Destinos no app:"
    foreach ($pkg in ($available.Keys | Sort-Object)) {
        Write-Host (" - files/retroarch/{0}" -f $pkg)
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

# Build adb base args with optional -s serial
$adbBase = @()
if (-not [string]::IsNullOrWhiteSpace($DeviceSerial)) {
    $adbBase = @("-s", $DeviceSerial)
    Write-Host "Usando dispositivo: $DeviceSerial"
}

foreach ($pkg in ($available.Keys | Sort-Object)) {
    $sourcePath = $available[$pkg].Source
    $tmpPath = "/data/local/tmp/deepmule_$pkg"
    $appDest = "files/retroarch/$pkg"

    Write-Host ""
    Write-Host ("Provisionando pacote: {0}" -f $pkg)
    Invoke-OperationWithRetry -Retries $RetryCount -DelaySeconds $RetryDelaySeconds -Label ("limpar temporario $pkg") -Operation {
        & $AdbPath @adbBase shell "rm -rf $tmpPath" | Out-Null
    }
    Invoke-OperationWithRetry -Retries $RetryCount -DelaySeconds $RetryDelaySeconds -Label ("criar temporario $pkg") -Operation {
        & $AdbPath @adbBase shell "mkdir -p $tmpPath" | Out-Null
    }

    $fileCount = $available[$pkg].FileCount
    $useChunked = $ChunkedPush -or ($fileCount -ge $ChunkThresholdFiles)
    Write-Host ("Enviando pacote ({0} arquivo(s), chunked={1})" -f $fileCount, $useChunked)

    if ($useChunked) {
        $relativePaths = Push-DirectoryFileByFile -SourcePath $sourcePath -TmpPath $tmpPath -AdbBinary $AdbPath -AdbBaseArgs $adbBase -Retries $RetryCount -DelaySeconds $RetryDelaySeconds
        Write-Host ("Copiando para o app em modo resiliente: {0}" -f $appDest)
        Copy-ChunkedToApp -TmpPath $tmpPath -AppDest $appDest -PkgName $PackageName -RelativePaths $relativePaths -AdbBinary $AdbPath -AdbBaseArgs $adbBase -Retries $RetryCount -DelaySeconds $RetryDelaySeconds
    }
    else {
        Invoke-OperationWithRetry -Retries $RetryCount -DelaySeconds $RetryDelaySeconds -Label ("adb push pacote $pkg") -Operation {
            & $AdbPath @adbBase push "$sourcePath\." "$tmpPath" | Out-Null
        }
        Invoke-OperationWithRetry -Retries $RetryCount -DelaySeconds $RetryDelaySeconds -Label ("run-as mkdir $pkg") -Operation {
            & $AdbPath @adbBase shell "run-as $PackageName mkdir -p '$appDest'" | Out-Null
        }
        Invoke-OperationWithRetry -Retries $RetryCount -DelaySeconds $RetryDelaySeconds -Label ("run-as cp pacote $pkg") -Operation {
            & $AdbPath @adbBase shell "run-as $PackageName cp -R '$tmpPath/.' '$appDest/'" | Out-Null
        }
    }

    Invoke-OperationWithRetry -Retries $RetryCount -DelaySeconds $RetryDelaySeconds -Label ("limpar temporario final $pkg") -Operation {
        & $AdbPath @adbBase shell "rm -rf $tmpPath" | Out-Null
    }
    Invoke-OperationWithRetry -Retries $RetryCount -DelaySeconds $RetryDelaySeconds -Label ("listar destino $pkg") -Operation {
        & $AdbPath @adbBase shell "run-as $PackageName ls -la '$appDest'" | Out-Null
    }
}

Write-Host ""
Write-Host "Provisionamento de pacotes opcionais concluido com sucesso."

