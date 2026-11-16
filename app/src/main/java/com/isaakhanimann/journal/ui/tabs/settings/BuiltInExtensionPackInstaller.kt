package com.isaakhanimann.journal.ui.tabs.settings

import android.content.Context
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.util.Log
import com.isaakhanimann.journal.data.substances.repositories.SubstanceEvents
import com.isaakhanimann.journal.localization.I18n
import java.io.File
import java.util.zip.ZipFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Installs bundled extension packs on launch.
 *
 * Release builds only contain the production pack. Debug builds may add another asset under
 * the same directory for UI or model acceptance tests.
 */
object BuiltInExtensionPackInstaller {
    private const val TAG = "BuiltInExtensionPack"
    private const val ASSET_DIR = "builtin_ext_packs"
    private const val DEBUG_DEMO_REGISTER_NAME = "extended_release_demo"

    suspend fun ensureInstalled(context: Context) = withContext(Dispatchers.IO) {
        removeDebugOnlyPackFromReleaseBuild(context)
        val assetNames = runCatching {
            context.assets.list(ASSET_DIR)?.toList().orEmpty()
        }.getOrElse {
            Log.e(TAG, "Failed to list bundled extension packs", it)
            emptyList()
        }
        assetNames
            .filter { it.endsWith(".zip", ignoreCase = true) }
            .sorted()
            .forEach { assetName ->
                installBundledPack(context, assetName)
            }
    }

    private fun removeDebugOnlyPackFromReleaseBuild(context: Context) {
        val isDebuggable =
            context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (isDebuggable) return
        val demoDir = File(context.filesDir, "ext_packs/$DEBUG_DEMO_REGISTER_NAME")
        if (demoDir.exists() && demoDir.deleteRecursively()) {
            SubstanceEvents.notifySubstanceReload()
            I18n.markDirty()
        }
    }

    private fun installBundledPack(context: Context, assetName: String) {
        val tempZip = File(context.cacheDir, "builtin_$assetName")
        try {
            context.assets.open("$ASSET_DIR/$assetName").use { input ->
                tempZip.outputStream().use { output -> input.copyTo(output) }
            }
            val pack = ZipFile(tempZip).use { zip ->
                val manifestEntry = zip.getEntry("manifest.json")
                    ?: throw IllegalStateException("manifest.json missing")
                val manifestText = zip.getInputStream(manifestEntry)
                    .bufferedReader()
                    .use { it.readText() }
                ExtensionPackLoader.parseManifest(manifestText, context.cacheDir)
                    ?: throw IllegalStateException("manifest.json parse failed")
            }
            if (!isValidRegisterName(pack.registerName)) {
                throw IllegalStateException("invalid registerName")
            }

            val packDir = File(context.filesDir, "ext_packs/${pack.registerName}")
            val installedVersion = File(packDir, "manifest.json")
                .takeIf { it.isFile }
                ?.let { manifestFile ->
                    runCatching {
                        ExtensionPackLoader.parseManifest(manifestFile.readText(), packDir)
                            ?.versionCode
                    }.getOrNull()
                }
                ?: 0
            if (installedVersion >= pack.versionCode) return

            val result = ExtensionPackImporter.import(context, Uri.fromFile(tempZip))
            if (!result.succeeded) {
                Log.w(TAG, "${pack.registerName}: ${result.message}")
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to install bundled pack $assetName", exception)
        } finally {
            tempZip.delete()
        }
    }
}
