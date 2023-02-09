package com.doublesymmetry.kotlinaudio.players.components

import android.content.Context
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.doublesymmetry.kotlinaudio.models.CacheConfig
import java.io.File

object PlayerCache {
    @Volatile
    private var instance: SimpleCache? = null

    fun getInstance(context: Context, cacheConfig: CacheConfig): SimpleCache? {
        val cacheDir = File(context.cacheDir, cacheConfig.identifier)
        val db: DatabaseProvider = StandaloneDatabaseProvider(context)

        instance ?: synchronized(this) {
            instance ?: SimpleCache(cacheDir, LeastRecentlyUsedCacheEvictor(cacheConfig.maxCacheSize ?: 0), db)
                .also { instance = it }
        }

        return instance
    }
}