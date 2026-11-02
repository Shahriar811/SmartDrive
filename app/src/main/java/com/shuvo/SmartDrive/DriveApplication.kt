package com.shuvo.SmartDrive

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.request.CachePolicy
import com.shuvo.SmartDrive.data.auth.FirebaseAuthRepository
import com.shuvo.SmartDrive.data.auth.MultiDriveOAuthManager
import com.shuvo.SmartDrive.data.remote.DriveApiService
import com.shuvo.SmartDrive.data.repository.UnifiedStorageRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Main Application class.
 * Initializes singleton dependencies and configures Coil with zero-local-storage disk cache policy.
 */
class DriveApplication : Application(), ImageLoaderFactory {

    lateinit var driveApiService: DriveApiService
        private set

    lateinit var oAuthManager: MultiDriveOAuthManager
        private set

    lateinit var authRepository: FirebaseAuthRepository
        private set

    lateinit var unifiedStorageRepository: UnifiedStorageRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        driveApiService = DriveApiService.create()
        oAuthManager = MultiDriveOAuthManager(this)
        authRepository = FirebaseAuthRepository()
        unifiedStorageRepository = UnifiedStorageRepository(
            context = this,
            driveApi = driveApiService,
            oAuthManager = oAuthManager
        )
    }

    /**
     * Configures Coil ImageLoader with:
     * 1. Dynamic Authorization Bearer injection for Google Drive photo URLs.
     * 2. DISK CACHE DISABLED: Zero local storage footprint. Only in-memory caching allowed.
     */
    override fun newImageLoader(): ImageLoader {
        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val urlString = original.url.toString()

            if (urlString.contains("googleapis.com/drive") || urlString.contains("googleusercontent.com")) {
                val emailParam = original.url.queryParameter("accountEmail")
                val token = if (!emailParam.isNullOrBlank()) {
                    runBlocking { oAuthManager.getAccessToken(emailParam).getOrNull() }
                } else null

                val requestBuilder = original.newBuilder()
                if (!token.isNullOrBlank()) {
                    requestBuilder.header("Authorization", "Bearer $token")
                }
                chain.proceed(requestBuilder.build())
            } else {
                chain.proceed(original)
            }
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.DISABLED) // CRITICAL: Zero local storage footprint
            .crossfade(true)
            .build()
    }

    companion object {
        lateinit var instance: DriveApplication
            private set
    }
}

