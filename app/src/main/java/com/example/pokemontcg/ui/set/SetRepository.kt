package com.example.pokemontcg.ui.set

import android.util.Log
import com.example.pokemontcg.api.StrapiApiService
import com.example.pokemontcg.api.request.set.SetCreateData
import com.example.pokemontcg.api.request.set.SetCreateRequest
import com.example.pokemontcg.api.request.set.SetUpdateData
import com.example.pokemontcg.api.request.set.SetUpdateRequest
import com.example.pokemontcg.local.dao.SetDao
import com.example.pokemontcg.local.entity.SetEntity
import com.example.pokemontcg.mapper.toEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SetRepository @Inject constructor(
    private val api: StrapiApiService,
    private val setDao: SetDao
) {
    fun getAllSets(): Flow<List<SetEntity>> = setDao.getAllSets()

    // devuelve la lista de IDs de sets en room
    suspend fun getAllSetIdsOnce(): List<Int> =
        setDao.getAllSetIdsOnce()

    suspend fun refreshSetsFromApi() {
        try {
            val response = api.getAllSets()
            if (response.isSuccessful) {
                // Limpia y actualiza Room sólo si el GET fue 2xx
                val entities = response.body()!!
                    .data
                    .map { it.toEntity() }

                setDao.deleteAllSets()
                setDao.insertAllSets(entities)
            } else {
                Log.e("SetRepo", "Error HTTP al refrescar sets: ${response.code()}")
            }
        } catch (e: Exception) {
            // Aquí atrapamos ConnectException, timeouts, etc.
            Log.w("SetRepo", "No hay internet, usando Room como caché", e)
        }
    }

    suspend fun createSet(name: String) {
        val request = SetCreateRequest(SetCreateData(name))
        val resp = api.createSet(request)
        if (resp.isSuccessful) {
            // Inserta localmente SOLO el nuevo set
            val newSet = resp.body()!!.data.toEntity()
            setDao.insertSet(newSet)
        }
    }

    suspend fun updateSet(id: Int, name: String) {
        val request = SetUpdateRequest(SetUpdateData(name))
        val resp = api.updateSet(id, request)
        if (resp.isSuccessful) {
            // Actualiza localmente el set editado
            val updatedSet = resp.body()!!.data.toEntity()
            setDao.insertSet(updatedSet)
        }
    }

    suspend fun deleteSet(id: Int) {
        val resp = api.deleteSet(id)
        if (resp.isSuccessful) {
            // Elimina localmente
            setDao.deleteSetById(id)
        }
    }
}