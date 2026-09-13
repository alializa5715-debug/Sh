package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ApiConfigEntity
import com.example.data.local.entity.DiscoveredModelEntity
import com.example.data.network.providers.ModelVerificationResult
import com.example.data.network.providers.ProviderRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ModelRegistryRepository(context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val dao = db.discoveredModelDao()

    fun getModelsForProvider(providerType: String): Flow<List<DiscoveredModelEntity>> =
        dao.getModelsForProvider(providerType)

    fun getAllModels(): Flow<List<DiscoveredModelEntity>> = dao.getAllModels()

    suspend fun getDiscoveredCount(providerType: String): Int = dao.getDiscoveredCount(providerType)

    suspend fun getVerifiedCount(providerType: String): Int = dao.getVerifiedCount(providerType)

    suspend fun getModel(providerType: String, modelId: String): DiscoveredModelEntity? =
        dao.getModelByProviderAndId(providerType, modelId)

    suspend fun discoverAndRegisterModels(config: ApiConfigEntity): Result<List<DiscoveredModelEntity>> =
        withContext(Dispatchers.IO) {
            val provider = ProviderRegistry.getProvider(config.providerType)
            val result = provider.listModels(config)

            result.fold(
                onSuccess = { models ->
                    val now = System.currentTimeMillis()
                    val entities = models.map { m ->
                        val entityId = "${config.providerType}:${m.modelId}"
                        val existing = dao.getModelById(entityId)
                        DiscoveredModelEntity(
                            id = entityId,
                            providerType = config.providerType,
                            modelId = m.modelId,
                            displayName = m.displayName,
                            description = m.description,
                            version = m.version,
                            status = existing?.status ?: "DISCOVERED",
                            availability = "AVAILABLE",
                            freeTier = m.freeTier,
                            freeTierNote = m.freeTierNote,
                            inputCapabilities = m.inputCapabilities,
                            outputCapabilities = m.outputCapabilities,
                            supportsStreaming = m.supportsStreaming,
                            supportsVision = m.supportsVision,
                            supportsImageGeneration = m.supportsImageGeneration,
                            supportsAudio = m.supportsAudio,
                            supportsRealtime = m.supportsRealtime,
                            contextWindow = m.contextWindow,
                            speedCategory = m.speedCategory,
                            lastChecked = now,
                            lastVerified = existing?.lastVerified ?: 0L
                        )
                    }

                    dao.insertModels(entities)
                    Result.success(entities)
                },
                onFailure = { err ->
                    Result.failure(err)
                }
            )
        }

    suspend fun verifySingleModel(
        config: ApiConfigEntity,
        modelId: String,
        capability: String = "chat"
    ): Result<ModelVerificationResult> = withContext(Dispatchers.IO) {
        val provider = ProviderRegistry.getProvider(config.providerType)
        val res = provider.verifyModel(config, modelId, capability)

        res.fold(
            onSuccess = { vResult ->
                if (vResult.verified) {
                    dao.markModelVerified(config.providerType, modelId, System.currentTimeMillis())
                } else if (vResult.is404) {
                    dao.markModelUnavailable(config.providerType, modelId)
                }
                Result.success(vResult)
            },
            onFailure = { err ->
                Result.failure(err)
            }
        )
    }

    suspend fun markModelUnavailable(providerType: String, modelId: String) {
        dao.markModelUnavailable(providerType, modelId)
    }

    suspend fun selectBestEligibleModel(
        providerType: String,
        capability: String = "chat",
        preferFreeTier: Boolean = true
    ): DiscoveredModelEntity? {
        val models = dao.getModelsForProviderSync(providerType)
            .filter { it.availability == "AVAILABLE" }

        if (models.isEmpty()) return null

        val filtered = models.filter { model ->
            when (capability) {
                "image_gen" -> model.supportsImageGeneration
                "vision" -> model.supportsVision
                "audio" -> model.supportsAudio
                else -> !model.supportsImageGeneration // Standard text/chat
            }
        }

        val pool = if (filtered.isNotEmpty()) filtered else models

        // 1. Prefer Verified over unverified
        // 2. Prefer Free Tier if requested
        // 3. Prefer Lightning / Fast (Flash variants) for low latency
        // 4. Stable version
        return pool.sortedWith(
            compareByDescending<DiscoveredModelEntity> { it.isVerified }
                .thenByDescending { if (preferFreeTier) it.freeTier else false }
                .thenByDescending { it.speedCategory == "LIGHTNING" }
                .thenByDescending { it.speedCategory == "FAST" }
                .thenByDescending { it.modelId }
        ).firstOrNull()
    }
}
