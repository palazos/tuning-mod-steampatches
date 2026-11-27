package com.winlator.cmod.steampatches.plugin

import com.winlator.cmod.steampatches.SteamPatchTable
import com.winlator.cmod.steampatches.api.PatchedGame
import com.winlator.cmod.steampatches.api.SteamPatchesPlugin
import java.io.File

class SteamPatchesPluginImpl : SteamPatchesPlugin {
    override fun summary(appId: Int): String? = SteamPatchTable.summary(appId)

    override fun patchedGames(): List<PatchedGame> = SteamPatchTable.patchedGames()

    override fun apply(gameDir: File, appId: Int) = SteamPatchTable.apply(gameDir, appId)

    override fun restore(gameDir: File, appId: Int) = SteamPatchTable.restore(gameDir, appId)
}
