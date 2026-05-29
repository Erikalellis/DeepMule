# Pacotes opcionais RetroArch para DeepMule

Coloque aqui os pacotes opcionais que voce quiser provisionar no app.

Estrutura esperada:

- `retroarch-pack/assets/`
- `retroarch-pack/thumbnails/`
- `retroarch-pack/database/`
- `retroarch-pack/cheats/`
- `retroarch-pack/autoconfig/`
- `retroarch-pack/info/`
- `retroarch-pack/config/retroarch.cfg`

Destino dentro do app (via `run-as`):

- `files/retroarch/assets/`
- `files/retroarch/thumbnails/`
- `files/retroarch/database/`
- `files/retroarch/cheats/`
- `files/retroarch/autoconfig/`
- `files/retroarch/info/`
- `files/retroarch/config/retroarch.cfg`

Uso rapido:

```powershell
Set-Location "C:\Users\robso\AndroidStudioProjects\DeepMule"
.\Provision-DeepMuleExtras.ps1 -DryRun
.\Provision-DeepMuleExtras.ps1
```

Preparar automaticamente a base a partir do RetroArch importado do emulador:

```powershell
Set-Location "C:\Users\robso\AndroidStudioProjects\DeepMule"
.\Import-RetroArchFromDevice.ps1 -IncludeApk
.\Prepare-DeepMuleRetroArchProfile.ps1
.\Provision-DeepMuleExtras.ps1 -SourceRoot "retroarch-pack"
```

Se o projeto tiver `retroarch-clover-pack/`, o script `Prepare-DeepMuleRetroArchProfile.ps1`
tambem mescla automaticamente overlays/autoconfig/core-options e ajustes de controle
seguros no `retroarch.cfg` final.

Provisionar apenas um pacote:

```powershell
Set-Location "C:\Users\robso\AndroidStudioProjects\DeepMule"
.\Provision-DeepMuleExtras.ps1 -Assets
```

