package de.unisaarland.loladrives.cache

import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import de.unisaarland.loladrives.MainActivity
import de.unisaarland.pcdfanalyser.caches.NOxAnalysisCache
import de.unisaarland.pcdfanalyser.caches.SupportedPIDsAnalysisCache
import de.unisaarland.pcdfanalyser.caches.VINAnalysisCache
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File

class CacheManagerTest {
    private var scenario: ActivityScenario<MainActivity> = ActivityScenario.launch(MainActivity::class.java)

    @Test
    fun testCacheFilesExist(){
        scenario.onActivity {
            val cacheDirectory = it.getExternalFilesDir("cache")
            val cacheAnalysisFile = File(cacheDirectory, "cacheAnalysis.json")
            assertNotNull(cacheDirectory)
            assertNotNull(cacheAnalysisFile)

        }
    }

    @Test
    fun testNOxAnalysisCacheExist(){
        scenario.onActivity {
            val noxCache = it.analysisCacheManager.noxCache
            assertThat(noxCache, instanceOf(NOxAnalysisCache::class.java))
        }
    }

    @Test
    fun testVINAnalysisCacheExist(){
        scenario.onActivity {
            val vinCache = it.analysisCacheManager.vinCache
            assertThat(vinCache, instanceOf(VINAnalysisCache::class.java))
        }
    }

    @Test
    fun testPIDsAnalysisCacheExist(){
        scenario.onActivity {
            val noxCache = it.analysisCacheManager.supportedPIDsCache
            assertThat(noxCache, instanceOf( SupportedPIDsAnalysisCache::class.java))
        }
    }
}