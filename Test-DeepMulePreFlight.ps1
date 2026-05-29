<#
.SYNOPSIS
    Pre-flight: valida cores, controles, BIOS e configuracao ANTES de testar no aparelho.

.DESCRIPTION
    Executa uma bateria de testes locais (sem device) sobre os artefatos do DeepMule:
      1. Cores (.so)      - header ELF, tamanho minimo, mapeamento systems->core
      2. Autoconfig       - sintaxe .cfg, campos obrigatorios por tipo de controle
      3. BIOS             - presenca dos arquivos exigidos pelos sistemas
      4. retroarch.cfg    - chaves criticas presentes
      5. Testes JVM       - roda `gradlew test` (unit tests sem Android)

.PARAMETER ProjectRoot
    Raiz do projeto (default: pasta do proprio script).

.PARAMETER SkipJvmTests
    Pula a etapa dos testes JVM do Gradle (util se o SDK Android nao estiver acessivel).

.EXAMPLE
    .\Test-DeepMulePreFlight.ps1
    .\Test-DeepMulePreFlight.ps1 -SkipJvmTests
#>

param(
    [string]$ProjectRoot = $PSScriptRoot,
    [switch]$SkipJvmTests
)

$ErrorActionPreference = "Stop"

# ── Paleta de cores / helpers ────────────────────────────────────────────────
function Pass([string]$msg)  { Write-Host "  [PASS] $msg" -ForegroundColor Green }
function Fail([string]$msg)  { Write-Host "  [FAIL] $msg" -ForegroundColor Red; $script:failures++ }
function Warn([string]$msg)  { Write-Host "  [WARN] $msg" -ForegroundColor Yellow }
function Info([string]$msg)  { Write-Host "  $msg" -ForegroundColor Cyan }
function Section([string]$t) { Write-Host ""; Write-Host "══ $t " -ForegroundColor White }

$script:failures = 0

# ── Sistemas e cores esperados (deve refletir SystemConfig.kt) ───────────────
$SYSTEM_CORE_MAP = @{
    "atari2600"  = @("stella")
    "atari7800"  = @("prosystem")
    "lynx"       = @("handy")
    "gb"         = @("gambatte")
    "gbc"        = @("gambatte")
    "gba"        = @("mgba")
    "nds"        = @("melonds","desmume")
    "3ds"        = @("citra")
    "nes"        = @("fceumm")
    "snes"       = @("snes9x")
    "n64"        = @("mupen64plus_next_gles3","mupen64plus_next_gles2")
    "sms"        = @("genesis_plus_gx")
    "gg"         = @("genesis_plus_gx")
    "genesis"    = @("genesis_plus_gx")
    "segacd"     = @("genesis_plus_gx")
    "ps1"        = @("pcsx_rearmed")
    "psp"        = @("ppsspp")
    "pce"        = @("mednafen_pce_fast")
    "ngp"        = @("mednafen_ngp")
    "ngpc"       = @("mednafen_ngp")
    "ws"         = @("mednafen_wswan")
    "wsc"        = @("mednafen_wswan")
    "arcade"     = @("fbneo")
}

# BIOS obrigatorias: sistema -> arquivo
$BIOS_REQUIRED = @{
    "lynx"    = "lynxboot.img"
    "segacd"  = "bios_CD_U.bin"
    "ps1"     = "scph1001.bin"
}

# Chaves criticas do retroarch.cfg
$RETROARCH_REQUIRED_KEYS = @(
    "video_driver",
    "audio_driver",
    "input_driver",
    "savestate_auto_save",
    "savefile_directory",
    "savestate_directory"
)

# Campos minimos que todo autoconfig de gamepad deve ter
$AUTOCONFIG_REQUIRED_KEYS = @(
    "input_device",
    "input_driver",
    "input_b_btn"
)

# ─────────────────────────────────────────────────────────────────────────────
# 1. CORES
# ─────────────────────────────────────────────────────────────────────────────
Section "1 · CORES (.so)"

$coresDir = Join-Path $ProjectRoot "cores-pack"
if (-not (Test-Path $coresDir)) {
    Fail "Diretorio cores-pack nao encontrado: $coresDir"
}
else {
    $soFiles = Get-ChildItem $coresDir -Filter "*.so" -File
    $soCount = $soFiles.Count
    Info "Encontrados $soCount arquivos .so em cores-pack"

    if ($soCount -eq 0) {
        Fail "Nenhum core .so encontrado em $coresDir"
    }

    # Helper: le primeiros bytes para verificar header ELF
    function Test-ElfHeader([System.IO.FileInfo]$file) {
        try {
            $stream = [System.IO.File]::OpenRead($file.FullName)
            $buf = New-Object byte[] 5
            $read = $stream.Read($buf, 0, 5)
            $stream.Close()
            if ($read -lt 5) { return $false }
            # ELF magic: 7F 45 4C 46
            return ($buf[0] -eq 0x7F -and $buf[1] -eq 0x45 -and $buf[2] -eq 0x4C -and $buf[3] -eq 0x46)
        } catch { return $false }
    }

    $minSizeBytes = 10KB  # qualquer .so valido tem pelo menos 10 KB
    $badElf   = @()
    $tooSmall = @()

    foreach ($so in $soFiles) {
        if (-not (Test-ElfHeader $so)) { $badElf   += $so.Name }
        if ($so.Length -lt $minSizeBytes) { $tooSmall += $so.Name }
    }

    if ($badElf.Count -eq 0) {
        Pass "Todos os $soCount cores possuem header ELF valido"
    } else {
        foreach ($n in $badElf) { Fail "Header ELF invalido / arquivo corrompido: $n" }
    }

    if ($tooSmall.Count -eq 0) {
        Pass "Todos os cores estao acima do tamanho minimo (10 KB)"
    } else {
        foreach ($n in $tooSmall) { Fail "Core suspeito (muito pequeno): $n" }
    }

    # Verifica mapeamento sistemas -> pelo menos 1 core presente
    Info ""
    Info "Verificando cobertura de sistemas:"
    foreach ($sysId in ($SYSTEM_CORE_MAP.Keys | Sort-Object)) {
        $expectedCores = $SYSTEM_CORE_MAP[$sysId]
        $found = $false
        $foundName = ""
        foreach ($coreName in $expectedCores) {
            # aceita sufixo _libretro_android ou sem sufixo
            $candidates = @(
                "${coreName}_libretro_android.so",
                "${coreName}.so"
            )
            foreach ($candidate in $candidates) {
                if (Test-Path (Join-Path $coresDir $candidate)) {
                    $found = $true
                    $foundName = $candidate
                    break
                }
            }
            if ($found) { break }
        }

        if ($found) {
            Pass "[$sysId]  -> $foundName"
        } else {
            $expectedList = $expectedCores -join " | "
            Fail "[$sysId]  core nao encontrado (esperava: $expectedList)"
        }
    }
}

# ─────────────────────────────────────────────────────────────────────────────
# 2. AUTOCONFIG (controles)
# ─────────────────────────────────────────────────────────────────────────────
Section "2 · AUTOCONFIG (controles)"

$autoconfigDir = Join-Path $ProjectRoot "retroarch-pack\autoconfig\android"
if (-not (Test-Path $autoconfigDir)) {
    Fail "Diretorio autoconfig nao encontrado: $autoconfigDir"
}
else {
    $cfgFiles = Get-ChildItem $autoconfigDir -Filter "*.cfg" -File
    $cfgCount = $cfgFiles.Count
    Info "Encontrados $cfgCount perfis de controle em autoconfig\android"

    if ($cfgCount -eq 0) {
        Fail "Nenhum perfil .cfg encontrado"
    }
    else {
        $badSyntax  = @()
        $missingKey = @()

        foreach ($cfg in $cfgFiles) {
            $lines = Get-Content $cfg.FullName -ErrorAction SilentlyContinue
            if ($null -eq $lines) {
                $badSyntax += $cfg.Name
                continue
            }

            # Verifica sintaxe basica: toda linha nao-vazia deve ser "chave = valor" ou comentario
            $syntaxOk = $true
            foreach ($line in $lines) {
                $trimmed = $line.Trim()
                if ($trimmed -eq "" -or $trimmed.StartsWith("#")) { continue }
                # Ignora linhas que nao comecam com um identificador ASCII (separadores visuais, etc.)
                if ($trimmed -notmatch '^[A-Za-z_]') { continue }
                if ($trimmed -notmatch '=') {
                    $syntaxOk = $false
                    break
                }
            }
            if (-not $syntaxOk) { $badSyntax += $cfg.Name }

            # Verifica campos obrigatorios
            $content = $lines -join "`n"
            foreach ($key in $AUTOCONFIG_REQUIRED_KEYS) {
                if ($content -notmatch "(?m)^\s*$key\s*=") {
                    $missingKey += "$($cfg.Name) [falta: $key]"
                    break   # um aviso por arquivo basta
                }
            }
        }

        if ($badSyntax.Count -eq 0) {
            Pass "Todos os $cfgCount perfis passaram na verificacao de sintaxe"
        } else {
            foreach ($n in $badSyntax) { Fail "Sintaxe invalida: $n" }
        }

        if ($missingKey.Count -eq 0) {
            Pass "Todos os perfis possuem os campos minimos (input_device, input_driver, input_b_btn)"
        } else {
            foreach ($n in $missingKey) { Warn "Campo faltando: $n" }
        }

        # Lista controles mais comuns como smoke-check nominativo
        $wellKnown = @(
            "Sony_DualShock_4_Controller.cfg",
            "DualSense Wireless Controller (Android 13).cfg",
            "Pro Controller.cfg",
            "Microsoft_XBOX_360_Controller.cfg",
            "8BitDo_SN30_Pro_for_Android.cfg"
        )
        Info ""
        Info "Controles populares:"
        foreach ($name in $wellKnown) {
            $path = Join-Path $autoconfigDir $name
            if (Test-Path $path) {
                Pass "Presente: $name"
            } else {
                Warn "Nao encontrado: $name"
            }
        }
    }
}

# ─────────────────────────────────────────────────────────────────────────────
# 3. BIOS
# ─────────────────────────────────────────────────────────────────────────────
Section "3 · BIOS obrigatorias"

$biosDir = Join-Path $ProjectRoot "bios-pack"
if (-not (Test-Path $biosDir)) {
    Fail "Diretorio bios-pack nao encontrado: $biosDir"
}
else {
    foreach ($sysId in ($BIOS_REQUIRED.Keys | Sort-Object)) {
        $biosFile = $BIOS_REQUIRED[$sysId]
        $fullPath = Join-Path $biosDir $biosFile
        if (Test-Path $fullPath) {
            $size = (Get-Item $fullPath).Length
            if ($size -gt 0) {
                Pass "[$sysId] $biosFile  ($([Math]::Round($size/1KB,1)) KB)"
            } else {
                Fail "[$sysId] $biosFile existe mas esta vazio!"
            }
        } else {
            Fail "[$sysId] BIOS ausente: $biosFile  (coloque em $biosDir)"
        }
    }
}

# ─────────────────────────────────────────────────────────────────────────────
# 4. retroarch.cfg
# ─────────────────────────────────────────────────────────────────────────────
Section "4 · retroarch.cfg (chaves criticas)"

$raCfg = Join-Path $ProjectRoot "retroarch-pack\config\retroarch.cfg"
if (-not (Test-Path $raCfg)) {
    Fail "retroarch.cfg nao encontrado: $raCfg"
}
else {
    $cfgContent = Get-Content $raCfg -Raw
    $cfgSizeKB  = [Math]::Round((Get-Item $raCfg).Length / 1KB, 1)
    Info "retroarch.cfg: $cfgSizeKB KB"

    foreach ($key in $RETROARCH_REQUIRED_KEYS) {
        if ($cfgContent -match "(?m)^\s*$key\s*=") {
            Pass "Chave presente: $key"
        } else {
            Warn "Chave ausente (pode ser default do RA): $key"
        }
    }
}

# ─────────────────────────────────────────────────────────────────────────────
# 5. TESTES JVM (Gradle unit tests – sem device)
# ─────────────────────────────────────────────────────────────────────────────
Section "5 · Testes JVM (gradlew test)"

if ($SkipJvmTests) {
    Warn "Etapa JVM pulada (flag -SkipJvmTests ativa)"
}
else {
    $gradlew = Join-Path $ProjectRoot "gradlew.bat"
    if (-not (Test-Path $gradlew)) {
        Fail "gradlew.bat nao encontrado em $ProjectRoot"
    }
    else {
        Info "Executando: gradlew :app:testDebugUnitTest --continue"
        try {
            $proc = Start-Process -FilePath $gradlew `
                -ArgumentList ":app:testDebugUnitTest", "--continue", "--no-daemon" `
                -WorkingDirectory $ProjectRoot `
                -Wait -PassThru -NoNewWindow

            if ($proc.ExitCode -eq 0) {
                Pass "Todos os testes JVM passaram (exit 0)"
            } else {
                Fail "Testes JVM falharam (exit $($proc.ExitCode)). Confira: app\build\reports\tests\testDebugUnitTest\index.html"
            }
        }
        catch {
            Fail "Erro ao executar gradlew: $_"
        }
    }
}

# ─────────────────────────────────────────────────────────────────────────────
# RESUMO FINAL
# ─────────────────────────────────────────────────────────────────────────────
Write-Host ""
Write-Host ("═" * 60) -ForegroundColor White
if ($script:failures -eq 0) {
    Write-Host "  PRE-FLIGHT OK - pronto para testar no aparelho!" -ForegroundColor Green
} else {
    Write-Host ("  PRE-FLIGHT FALHOU - {0} problema(s) encontrado(s)." -f $script:failures) -ForegroundColor Red
    Write-Host "  Corrija os itens [FAIL] acima antes de provisionar o device." -ForegroundColor Red
}
Write-Host ("═" * 60) -ForegroundColor White

exit $script:failures

