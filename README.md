# tuning-mod-steampatches

Winlator Tuning **Steam Game Patches** module (Modularization Context v3).

## Dual purpose

1. **Development / sync** — Kotlin implementation of same-size byte patches for Steam game installs.
2. **On-demand install** — users install `steampatches-module.zip` from GitHub Releases via **Tuning Modules**.

## Installable package (`steampatches-module.zip`)

```
module.json
runtime.jar          # classes.dex (SteamPatchTable, plugin)
```

No native libraries. The shortcut toggle and its strings stay in the product APK. The module applies the bytes when installed.

Requires the Stores module, any installed version. Uninstalling Stores also removes this module.

Package version stays **0.1.0** until the product bumps it.

## Product wiring

- Catalog id: `steampatches`
- Install path: `files/tuning-modules/steampatches/`
- APK keeps SPI (`steampatches-api`), `SteamPatchesBridge`, and the `SteamPatches` facade the stores dex already calls
