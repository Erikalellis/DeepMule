param(
    [string]$PackageName = "com.example.deepmule",
    [string]$AdbPath     = "F:\Android\SDK\platform-tools\adb.exe",
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

function Invoke-AdbSilent {
    # Executa adb sem propagar NativeCommandError (adb escreve progresso no stderr)
    param([string[]]$ArgList)
    $prev = $ErrorActionPreference
    $ErrorActionPreference = "SilentlyContinue"
    $null = & $AdbPath @ArgList 2>&1
    $ec = $LASTEXITCODE
    $ErrorActionPreference = $prev
    return $ec
}

function Invoke-AdbShell([string]$Cmd) {
    # Executa adb shell com comando único (permite aspas simples no Android shell)
    $prev = $ErrorActionPreference
    $ErrorActionPreference = "SilentlyContinue"
    $out = & $AdbPath shell $Cmd 2>&1 | Out-String
    $ec = $LASTEXITCODE
    $ErrorActionPreference = $prev
    return @{ ExitCode = $ec; Output = $out.Trim() }
}

# ── ROM selecionadas: 1 por sistema (menor disponível, verificada) ─────────────
$testRoms = @(
    @{ System = "atari2600"; File = "D:\Atari 2600\roms\Othello (32 in 1) (PAL).bin" },
    @{ System = "arcade";    File = "D:\MAME\ROMS\blockade.zip" },
    @{ System = "arcade";    File = "D:\Neo Geo\roms\Hello World! (NEO Clock) (PD).zip" },
    @{ System = "gba";       File = "D:\Gameboy Advance\roms\Classic NES Series - Super Mario Bros.zip" },
    @{ System = "n64";       File = "D:\Nintendo 64\roms\Dr. Mario 64 (U) [!].zip" },
    @{ System = "nes";       File = "D:\Nintendo NES\roms\Demo Boy 2 (Unl).zip" },
    @{ System = "sms";       File = "D:\Sega Master System\roms\Terebi Oekaki (SG-1000) [!].zip" },
    @{ System = "genesis";   File = "D:\Sega Mega Drive (Sega Genesis)\roms\Super Ping Pong (J).zip" },
    @{ System = "snes";      File = "D:\SNES\rom\Space Invaders (U) [!].zip" },
    @{ System = "pce";       File = "D:\TurboGrafX\roms\Text Sample 1 (PD).zip" }
)

$deviceRomBase = "/data/user/0/$PackageName/files/roms"

$pass = 0; $warn = 0; $fail = 0

function Log([string]$Status, [string]$Msg) {
    $color = switch ($Status) { "PASS" { "Green" } "WARN" { "Yellow" } "FAIL" { "Red" } }
    Write-Host ("[$Status] $Msg") -ForegroundColor $color
    switch ($Status) { "PASS" { $script:pass++ } "WARN" { $script:warn++ } "FAIL" { $script:fail++ } }
}

function Invoke-Adb {
    param([string[]]$Args)
    $result = & $AdbPath @Args 2>&1
    return @{ Output = ($result | Out-String).Trim(); ExitCode = $LASTEXITCODE }
}

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "DeepMule - Provisionar ROMs de Teste" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "PackageName : $PackageName"
Write-Host "DryRun      : $([bool]$DryRun)"
Write-Host ""

# Verifica ADB
if (-not (Test-Path $AdbPath)) {
    Write-Host "[FAIL] adb nao encontrado em: $AdbPath" -ForegroundColor Red
    exit 1
}

$devices = & $AdbPath devices | Select-String "\tdevice$"
if (-not $devices) {
    Write-Host "[FAIL] Nenhum dispositivo conectado." -ForegroundColor Red
    exit 1
}
Write-Host "[INFO] Dispositivo: $($devices[0].Line.Trim())" -ForegroundColor Cyan

foreach ($rom in $testRoms) {
    $src = $rom.File
    $sys = $rom.System
    $fileName = Split-Path $src -Leaf

    # Usa LiteralPath para evitar problema com colchetes nos nomes de arquivo
    if (-not (Test-Path -LiteralPath $src)) {
        Log "WARN" "ROM nao encontrada localmente: $fileName  (sistema: $sys)"
        continue
    }

    $sizeKB = [math]::Round((Get-Item -LiteralPath $src).Length / 1KB, 1)
    $deviceDir  = "$deviceRomBase/$sys"
    $devicePath = "$deviceDir/$fileName"

    if ($DryRun) {
        Log "PASS" "[DryRun] $sys | $fileName ($sizeKB KB) -> $devicePath"
        continue
    }

    # Cria pasta no dispositivo
    $r = Invoke-AdbShell "run-as $PackageName mkdir -p '$deviceDir'"
    if ($r.ExitCode -ne 0) {
        Log "FAIL" "Nao foi possivel criar pasta $deviceDir"
        continue
    }

    # Push para sdcard temporário
    $tmpName = "dmtmp_$([System.Guid]::NewGuid().ToString('N').Substring(0,8))$([System.IO.Path]::GetExtension($src))"
    $tmpPath = "/sdcard/$tmpName"
    $ec = Invoke-AdbSilent -ArgList @("push", $src, $tmpPath)
    if ($ec -ne 0) {
        Log "FAIL" "Push falhou para $fileName"
        continue
    }

    # Copia de sdcard para dentro do app via run-as (aspas simples para escapar colchetes no shell Android)
    $r = Invoke-AdbShell "run-as $PackageName cp '$tmpPath' '$devicePath'"
    $null = Invoke-AdbShell "rm -f '$tmpPath'"

    if ($r.ExitCode -ne 0) {
        Log "FAIL" "Copia para app falhou: $fileName ($($r.Output))"
        continue
    }

    # Confirma presença no dispositivo
    $r = Invoke-AdbShell "run-as $PackageName ls '$devicePath'"
    if ($r.ExitCode -eq 0) {
        Log "PASS" "$sys | $fileName ($sizeKB KB) -> OK"
    }
    else {
        Log "WARN" "$sys | $fileName copiado mas ls falhou: $($r.Output)"
    }
}

Write-Host ""
Write-Host "================================" -ForegroundColor Cyan
Write-Host ("PASS: $pass  WARN: $warn  FAIL: $fail") -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan

if ($fail -gt 0) { exit 1 }
