package com.winlator.cmod.steampatches

import android.util.Log
import java.io.File

// Byte replacements for Steam games, applied when the files are on disk.
// A later launch finds the new bytes and leaves the file alone. Paths are
// relative to the game's install folder, so a re-download is patched again.
// The untouched file is kept next to it so the shortcut option can undo it.
internal object SteamPatchTable {
    private const val BACKUP_SUFFIX = ".steampatch-original"

    private class Replacement(
        val relativePath: String,
        val from: ByteArray,
        val to: ByteArray,
    )

    private class GamePatch(val summary: String, val replacements: List<Replacement>)

    private val byAppId: Map<Int, GamePatch> = mapOf(
        2080820 to GamePatch(
            "Lets public matches start and keeps Photon from dropping the session.",
            listOf(
                // Platform already defaults to Windows. That store becomes Architecture = x86.
                Replacement(
                    "SquadBlast_Data/Managed/PhotonQuantum.dll",
                    byteArrayOf(0x06, 0x16, 0x7d, 0xba.toByte(), 0x01, 0x00, 0x0a, 0x06, 0x2a),
                    byteArrayOf(0x06, 0x18, 0x7d, 0x20, 0x04, 0x00, 0x0a, 0x06, 0x2a),
                ),
                // Skip the memory layout walk. It passes, but it blocks the frame thread
                // long enough for Photon to drop the public match.
                Replacement(
                    "SquadBlast_Data/Managed/quantum.code.dll",
                    byteArrayOf(0x02, 0x7b, 0x4e, 0x0c, 0x00, 0x04, 0x2c, 0x7d),
                    byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x2b, 0x7d),
                ),
                Replacement(
                    "SquadBlast_Data/Managed/quantum.code.dll",
                    byteArrayOf(0x02, 0x7b, 0x4e, 0x0c, 0x00, 0x04, 0x2b, 0x7d),
                    byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x2b, 0x7d),
                ),
            ),
        ),
    )

    fun summary(appId: Int): String? = byAppId[appId]?.summary

    fun apply(gameDir: File, appId: Int) {
        val patch = byAppId[appId] ?: return
        for ((path, replacements) in patch.replacements.groupBy { it.relativePath }) {
            patchFile(File(gameDir, path), replacements)
        }
    }

    fun restore(gameDir: File, appId: Int) {
        val patch = byAppId[appId] ?: return
        for (path in patch.replacements.map { it.relativePath }.distinct()) {
            val file = File(gameDir, path)
            val backup = File(file.path + BACKUP_SUFFIX)
            if (!backup.isFile) continue
            try {
                backup.copyTo(file, overwrite = true)
                backup.delete()
            } catch (error: Exception) {
                Log.w("SteamPatches", "Could not restore ${file.name}", error)
            }
        }
    }

    private fun patchFile(file: File, replacements: List<Replacement>) {
        if (!file.isFile) return
        try {
            val original = file.readBytes()
            val patched = original.copyOf()
            var changed = false
            for (r in replacements) {
                if (r.from.size != r.to.size) continue
                val at = indexOf(patched, r.from)
                if (at < 0) continue
                r.to.copyInto(patched, at)
                changed = true
            }
            if (!changed) return
            // A Steam update brings a new unpatched file; an older backup would
            // restore the previous game version.
            val backup = File(file.path + BACKUP_SUFFIX)
            val pristine = replacements.none { indexOf(original, it.to) >= 0 }
            if (pristine || !backup.isFile) backup.writeBytes(original)
            file.writeBytes(patched)
        } catch (error: Exception) {
            Log.w("SteamPatches", "Could not patch ${file.name}", error)
        }
    }

    private fun indexOf(data: ByteArray, needle: ByteArray): Int {
        if (needle.isEmpty() || needle.size > data.size) return -1
        val last = data.size - needle.size
        var i = 0
        while (i <= last) {
            var matches = true
            for (j in needle.indices) {
                if (data[i + j] != needle[j]) {
                    matches = false
                    break
                }
            }
            if (matches) return i
            i++
        }
        return -1
    }
}
