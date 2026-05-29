param(
    [string]$ProjectRoot = "C:\Users\robso\AndroidStudioProjects\DeepMule",
    [string]$DevicePackDir = "retroarch-device-pack",
    [string]$OutputDir = "retroarch-pack",
    [string]$CloverPackDir = "retroarch-clover-pack",
    [string]$AppPackageName = "com.example.deepmule",
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.IO.Compression.FileSystem

function Resolve-RootPath([string]$PathValue, [string]$BasePath) {
    if ([System.IO.Path]::IsPathRooted($PathValue)) {
        return $PathValue
    }
    return (Join-Path $BasePath $PathValue)
}

function Set-OrAddConfigValue([System.Collections.Generic.List[string]]$Lines, [string]$Key, [string]$Value) {
    $pattern = "^" + [regex]::Escape($Key) + "\s*="
    for ($i = 0; $i -lt $Lines.Count; $i++) {
        if ($Lines[$i] -match $pattern) {
            $Lines[$i] = "$Key = `"$Value`""
            return
        }
    }
    $Lines.Add("$Key = `"$Value`"")
}

function Get-ConfigValueMap([string]$ConfigPath) {
    $map = @{}
    if (-not (Test-Path $ConfigPath)) {
        return $map
    }

    foreach ($line in (Get-Content -Path $ConfigPath -Encoding UTF8)) {
        $trimmed = $line.Trim()
        if ([string]::IsNullOrWhiteSpace($trimmed) -or $trimmed.StartsWith("#")) {
            continue
        }

        if ($trimmed -match '^([a-zA-Z0-9_\.]+)\s*=\s*"(.*)"\s*$') {
            $map[$matches[1]] = $matches[2]
        }
    }

    return $map
}

function Merge-CloverControlTuning([string]$BaseConfigPath, [string]$CloverConfigPath) {
    if (-not (Test-Path $BaseConfigPath) -or -not (Test-Path $CloverConfigPath)) {
        return 0
    }

    $allowedKeys = @(
        "input_autodetect_enable",
        "input_analog_deadzone",
        "input_analog_sensitivity",
        "input_player1_analog_dpad_mode",
        "input_overlay_abxy_diagonal_sensitivity",
        "input_overlay_analog_recenter_zone",
        "input_overlay_aspect_adjust_landscape",
        "input_overlay_aspect_adjust_portrait",
        "input_overlay_auto_rotate",
        "input_overlay_auto_scale",
        "input_overlay_behind_menu",
        "input_overlay_dpad_diagonal_sensitivity",
        "input_overlay_hide_in_menu",
        "input_overlay_hide_when_gamepad_connected",
        "input_overlay_opacity",
        "input_overlay_scale_landscape",
        "input_overlay_scale_portrait",
        "input_overlay_show_inputs",
        "input_overlay_show_inputs_port",
        "input_overlay_show_mouse_cursor"
    )

    $cloverValues = Get-ConfigValueMap -ConfigPath $CloverConfigPath
    $baseLines = [System.Collections.Generic.List[string]]::new()
    $baseLines.AddRange([string[]](Get-Content -Path $BaseConfigPath -Encoding UTF8))

    $merged = 0
    foreach ($key in $allowedKeys) {
        if ($cloverValues.ContainsKey($key)) {
            Set-OrAddConfigValue -Lines $baseLines -Key $key -Value $cloverValues[$key]
            $merged++
        }
    }

    Set-Content -Path $BaseConfigPath -Value $baseLines -Encoding UTF8
    return $merged
}

function Normalize-RetroArchConfig([string]$SourceCfg, [string]$DestCfg, [string]$PackageName) {
    $appFiles = "/data/user/0/$PackageName/files"
    $appCache = "/data/user/0/$PackageName/cache"
    $retroRoot = "$appFiles/retroarch"

    $lines = [System.Collections.Generic.List[string]]::new()
    $lines.AddRange([string[]](Get-Content -Path $SourceCfg -Encoding UTF8))

    $overrides = [ordered]@{
        "assets_directory" = "$retroRoot/assets"
        "audio_filter_dir" = "$retroRoot/assets/filters/audio"
        "bundle_assets_dst_path" = "$retroRoot"
        "bundle_assets_dst_path_subdir" = ""
        "bundle_assets_extract_enable" = "false"
        "bundle_assets_src_path" = ""
        "cache_directory" = "$appCache/retroarch"
        "cheat_database_path" = "$retroRoot/cheats"
        "content_database_path" = "$retroRoot/database/rdb"
        "content_favorites_path" = "$retroRoot/playlists/builtin/content_favorites.lpl"
        "content_history_path" = "$retroRoot/playlists/builtin/content_history.lpl"
        "content_image_history_path" = "$retroRoot/playlists/builtin/content_image_history.lpl"
        "content_music_history_path" = "$retroRoot/playlists/builtin/content_music_history.lpl"
        "content_video_history_path" = "$retroRoot/playlists/builtin/content_video_history.lpl"
        "core_assets_directory" = "$retroRoot/downloads"
        "dynamic_wallpapers_directory" = "$retroRoot/assets/wallpapers"
        "input_autodetect_enable" = "true"
        "input_overlay" = "$retroRoot/assets/overlays/gamepads/neo-retropad/neo-retropad.cfg"
        "input_overlay_enable" = "true"
        "input_overlay_enable_autopreferred" = "true"
        "input_osk_overlay" = "$retroRoot/assets/overlays/keyboards/US-101/US-101.cfg"
        "input_remapping_directory" = "$retroRoot/remaps"
        "joypad_autoconfig_dir" = "$retroRoot/autoconfig"
        "libretro_directory" = "$appFiles/cores"
        "libretro_info_path" = "$retroRoot/info"
        "log_dir" = "$retroRoot/logs"
        "osk_overlay_directory" = "$retroRoot/assets/overlays/keyboards"
        "overlay_directory" = "$retroRoot/assets/overlays"
        "playlist_directory" = "$retroRoot/playlists"
        "rgui_config_directory" = "$retroRoot/config"
        "savefile_directory" = "$appFiles/saves"
        "savestate_directory" = "$appFiles/states"
        "screenshot_directory" = "$retroRoot/screenshots"
        "system_directory" = "$appFiles/bios"
        "thumbnails_directory" = "$retroRoot/thumbnails"
        "video_filter_dir" = "$retroRoot/assets/filters/video"
        "video_shader_dir" = "$retroRoot/assets/shaders"
    }

    foreach ($entry in $overrides.GetEnumerator()) {
        Set-OrAddConfigValue -Lines $lines -Key $entry.Key -Value $entry.Value
    }

    $destDir = Split-Path -Path $DestCfg -Parent
    New-Item -Path $destDir -ItemType Directory -Force | Out-Null
    Set-Content -Path $DestCfg -Value $lines -Encoding UTF8
}

function Extract-ApkPrefix([System.IO.Compression.ZipArchive]$Zip, [string]$Prefix, [string]$DestinationRoot) {
    $entries = $Zip.Entries | Where-Object {
        $_.FullName.StartsWith($Prefix) -and -not [string]::IsNullOrWhiteSpace($_.Name)
    }

    foreach ($entry in $entries) {
        $relative = $entry.FullName.Substring($Prefix.Length).Replace('/', '\\')
        $target = Join-Path $DestinationRoot $relative
        $parent = Split-Path -Path $target -Parent
        if (-not (Test-Path $parent)) {
            New-Item -Path $parent -ItemType Directory -Force | Out-Null
        }
        [System.IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $target, $true)
    }

    return ($entries | Measure-Object).Count
}

$deviceRoot = Resolve-RootPath -PathValue $DevicePackDir -BasePath $ProjectRoot
$outputRoot = Resolve-RootPath -PathValue $OutputDir -BasePath $ProjectRoot
$cloverRoot = Resolve-RootPath -PathValue $CloverPackDir -BasePath $ProjectRoot
$sourceCfg = Join-Path $deviceRoot "external-files\retroarch.cfg"
$sourceApk = Join-Path $deviceRoot "retroarch-base.apk"

if (-not (Test-Path $sourceCfg)) {
    throw "retroarch.cfg nao encontrado em: $sourceCfg"
}

if (-not (Test-Path $sourceApk)) {
    throw "APK do RetroArch nao encontrado em: $sourceApk"
}

$extractMap = [ordered]@{
    "assets/assets/" = "assets"
    "assets/shaders/" = "assets/shaders"
    "assets/overlays/" = "assets/overlays"
    "assets/autoconfig/" = "autoconfig"
    "assets/database/" = "database"
    "assets/info/" = "info"
    "assets/cheats/" = "cheats"
    "assets/thumbnails/" = "thumbnails"
}

Write-Host "Origem cfg: $sourceCfg"
Write-Host "Origem apk: $sourceApk"
Write-Host "Destino pack: $outputRoot"
Write-Host "Pack Clover (opcional): $cloverRoot"

if ($DryRun) {
    Write-Host "[DryRun] Pacotes que seriam preparados:"
    foreach ($pair in $extractMap.GetEnumerator()) {
        Write-Host (" - {0} => {1}" -f $pair.Key, $pair.Value)
    }
    Write-Host " - config => config/retroarch.cfg (normalizado para $AppPackageName)"
    return
}

New-Item -Path $outputRoot -ItemType Directory -Force | Out-Null
$destConfigPath = Join-Path $outputRoot "config\retroarch.cfg"
Normalize-RetroArchConfig -SourceCfg $sourceCfg -DestCfg $destConfigPath -PackageName $AppPackageName

$zip = [System.IO.Compression.ZipFile]::OpenRead($sourceApk)
try {
    foreach ($pair in $extractMap.GetEnumerator()) {
        $dest = Join-Path $outputRoot $pair.Value
        New-Item -Path $dest -ItemType Directory -Force | Out-Null
        $count = Extract-ApkPrefix -Zip $zip -Prefix $pair.Key -DestinationRoot $dest
        Write-Host ("Extraidos {0} arquivo(s): {1} => {2}" -f $count, $pair.Key, $dest)
    }
}
finally {
    $zip.Dispose()
}

if (Test-Path $cloverRoot) {
    Write-Host "Aplicando merge do pacote Clover..."

    $cloverOverlay = Join-Path $cloverRoot "overlay"
    if (Test-Path $cloverOverlay) {
        $destOverlay = Join-Path $outputRoot "assets\overlays\clover"
        New-Item -Path $destOverlay -ItemType Directory -Force | Out-Null
        Copy-Item -Path (Join-Path $cloverOverlay "*") -Destination $destOverlay -Recurse -Force
    }

    $cloverAutoconfig = Join-Path $cloverRoot "joypad_autoconf"
    if (Test-Path $cloverAutoconfig) {
        $destAutoconfig = Join-Path $outputRoot "autoconfig\clover"
        New-Item -Path $destAutoconfig -ItemType Directory -Force | Out-Null
        Copy-Item -Path (Join-Path $cloverAutoconfig "*") -Destination $destAutoconfig -Recurse -Force
    }

    $cloverInfo = Join-Path $cloverRoot "info"
    if (Test-Path $cloverInfo) {
        $destInfo = Join-Path $outputRoot "info"
        New-Item -Path $destInfo -ItemType Directory -Force | Out-Null
        Copy-Item -Path (Join-Path $cloverInfo "*") -Destination $destInfo -Recurse -Force
    }

    $cloverCoreOptions = Join-Path $cloverRoot "retroarch-core-options.cfg"
    if (Test-Path $cloverCoreOptions) {
        $destCoreOptions = Join-Path $outputRoot "config\retroarch-core-options.cfg"
        Copy-Item -Path $cloverCoreOptions -Destination $destCoreOptions -Force

        $lines = [System.Collections.Generic.List[string]]::new()
        $lines.AddRange([string[]](Get-Content -Path $destConfigPath -Encoding UTF8))
        Set-OrAddConfigValue -Lines $lines -Key "core_options_path" -Value "/data/user/0/$AppPackageName/files/retroarch/config/retroarch-core-options.cfg"
        Set-Content -Path $destConfigPath -Value $lines -Encoding UTF8
    }

    $cloverConfigPath = Join-Path $cloverRoot "retroarch.cfg"
    $mergedCount = Merge-CloverControlTuning -BaseConfigPath $destConfigPath -CloverConfigPath $cloverConfigPath
    Write-Host "Chaves de controle mescladas do Clover: $mergedCount"
}

Write-Host "Perfil RetroArch preparado com sucesso em: $outputRoot"


