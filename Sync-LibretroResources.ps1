param(
    [string]$ProjectRoot = "C:\Users\robso\AndroidStudioProjects\DeepMule",
    [string]$WorkDir = "build\libretro-sync",
    [string]$DatabaseRepo = "https://github.com/libretro/libretro-database.git",
    [string]$ThumbnailsRepo = "https://github.com/libretro/libretro-thumbnails.git",
    [switch]$SkipDatabase,
    [switch]$SkipThumbnails
)

$ErrorActionPreference = "Stop"

function Ensure-Git {
    $git = Get-Command git -ErrorAction SilentlyContinue
    if (-not $git) {
        throw "git nao encontrado no PATH. Instale Git para sincronizar recursos oficiais."
    }
}

function Ensure-Repo([string]$RepoUrl, [string]$TargetDir) {
    if (Test-Path (Join-Path $TargetDir ".git")) {
        Write-Host "Atualizando repo: $TargetDir"
        Push-Location $TargetDir
        try {
            git fetch --all --prune | Out-Null
            git reset --hard origin/master | Out-Null
        }
        finally {
            Pop-Location
        }
        return
    }

    if (Test-Path $TargetDir) {
        Remove-Item -Recurse -Force $TargetDir
    }

    Write-Host "Clonando: $RepoUrl"
    git clone --depth 1 $RepoUrl $TargetDir | Out-Null
}

function Sync-Database([string]$RepoDir, [string]$RetroarchPackDir) {
    $dbSrc = Join-Path $RepoDir "metadat"
    if (-not (Test-Path $dbSrc)) {
        throw "Pasta metadat nao encontrada no repo libretro-database."
    }

    $dbDst = Join-Path $RetroarchPackDir "database"
    New-Item -ItemType Directory -Force -Path $dbDst | Out-Null

    # Copia os .dat para permitir rebuild local por ferramentas externas.
    Copy-Item -Recurse -Force (Join-Path $dbSrc "*") $dbDst

    $marker = Join-Path $dbDst "_source_libretro_database.txt"
    @(
        "source=$DatabaseRepo",
        "synced_at=$(Get-Date -Format o)",
        "note=Arquivos .dat oficiais. O sqlite pode ser gerado por pipeline local quando necessario."
    ) | Set-Content -Encoding UTF8 $marker

    Write-Host "Database sincronizada em: $dbDst"
}

function Sync-Thumbnails([string]$RepoDir, [string]$RetroarchPackDir) {
    $thumbDst = Join-Path $RetroarchPackDir "thumbnails"
    New-Item -ItemType Directory -Force -Path $thumbDst | Out-Null

    # Copia tudo para manter compatibilidade com organizacao oficial.
    Copy-Item -Recurse -Force (Join-Path $RepoDir "*") $thumbDst

    $marker = Join-Path $thumbDst "_source_libretro_thumbnails.txt"
    @(
        "source=$ThumbnailsRepo",
        "synced_at=$(Get-Date -Format o)",
        "note=Thumbnails oficiais para capas e snapshots."
    ) | Set-Content -Encoding UTF8 $marker

    Write-Host "Thumbnails sincronizadas em: $thumbDst"
}

Ensure-Git

$projectRootPath = (Resolve-Path $ProjectRoot).Path
$workRoot = Join-Path $projectRootPath $WorkDir
$retroarchPackDir = Join-Path $projectRootPath "retroarch-pack"

New-Item -ItemType Directory -Force -Path $workRoot | Out-Null
New-Item -ItemType Directory -Force -Path $retroarchPackDir | Out-Null

if (-not $SkipDatabase) {
    $dbRepoDir = Join-Path $workRoot "libretro-database"
    Ensure-Repo -RepoUrl $DatabaseRepo -TargetDir $dbRepoDir
    Sync-Database -RepoDir $dbRepoDir -RetroarchPackDir $retroarchPackDir
}

if (-not $SkipThumbnails) {
    $thumbRepoDir = Join-Path $workRoot "libretro-thumbnails"
    Ensure-Repo -RepoUrl $ThumbnailsRepo -TargetDir $thumbRepoDir
    Sync-Thumbnails -RepoDir $thumbRepoDir -RetroarchPackDir $retroarchPackDir
}

Write-Host "Sincronizacao oficial libretro concluida."

