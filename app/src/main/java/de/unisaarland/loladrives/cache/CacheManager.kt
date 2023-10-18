/*
 * Copyright (c) 2021.
 */

package de.unisaarland.loladrives.cache

import com.squareup.sqldelight.android.AndroidSqliteDriver
import de.unisaarland.caches.CacheDatabase
import de.unisaarland.loladrives.MainActivity
import de.unisaarland.pcdfanalyser.analysers.SupportedPIDsAnalyser
import de.unisaarland.pcdfanalyser.analysers.VINAnalyser
import de.unisaarland.pcdfanalyser.caches.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

class CacheManager(private val externalDir: File?, val activity: MainActivity) {
    private val cacheDatabase: CacheDatabase

    // Caches
    val noxCache: NOxAnalysisCache
    val vinCache: VINAnalysisCache
    val supportedPIDsCache: SupportedPIDsAnalysisCache

    init {
        val cacheFile = setupCacheFiles()
        val driver = AndroidSqliteDriver(CacheDatabase.Schema, activity, cacheFile.absolutePath)
        cacheDatabase = AnalysisCache.sharedDatabase(cacheFile, driver)

        noxCache =
            NOxAnalysisCache(cacheDatabase, AnalysisCacheDelegate { NOxAnalyser(it, activity) })
        vinCache = VINAnalysisCache(cacheDatabase, AnalysisCacheDelegate { VINAnalyser(it) })
        supportedPIDsCache = SupportedPIDsAnalysisCache(
            cacheDatabase,
            AnalysisCacheDelegate { SupportedPIDsAnalyser(it) })
    }

    /**
     * Sets up cache directories if not existent and initializes the cache databases with the respective files
     */
    private fun setupCacheFiles(): File {
        // Set up cache directory
        val cacheDirectory = File(externalDir, "cache")
        cacheDirectory.mkdirs()

        // Set up cache files
        val cacheFile = File(cacheDirectory, "analysis_cache")
        cacheFile.createNewFile()

        return cacheFile
    }

    /**
     * Analyses all PCDF files and adds analysis results to cache
     */
    fun addAllFilesToCache() {
        GlobalScope.launch {
            // Get PCDF file directory
            val letDirectory = File(externalDir, "pcdfdata")

            letDirectory.listFiles()?.forEach {
                if (it.isDirectory) {
                    it?.listFiles()?.forEach { pcdfFile ->
                        if (pcdfFile.name.contains("ppcdf")) {
                            try {
                                vinCache.analysisResultForFile(pcdfFile, true)
                            } catch (e: Exception) {
                            }
                            try {
                                supportedPIDsCache.analysisResultForFile(pcdfFile, true)
                            } catch (e: Exception) {
                            }
                            try {
                                noxCache.analysisResultForFile(pcdfFile, true)
                            } catch (e: Exception) {
                            }
                        }
                    }
                }
            }
        }
    }

}
