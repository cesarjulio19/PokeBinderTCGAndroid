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
import java.io.IOException
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

    // crea un set y lo añade a room
    suspend fun createSet(name: String): Boolean {
        return try {
            val resp = api.createSet(SetCreateRequest(SetCreateData(name)))
            if (resp.isSuccessful) {
                // inserta la respuesta de Strapi con su id real:
                setDao.insertSet(resp.body()!!.data.toEntity())
            } else {
                Log.e("SetRepo", "Error HTTP createSet: ${resp.code()}")
                // fallback local: calculo un id nuevo
                val nextId = (setDao.getMaxId() ?: 0) + 1
                setDao.insertSet(SetEntity(id = nextId, name = name))
            }
            true
        } catch (e: IOException) {
            // sin conexión, mismo fallback
            val nextId = (setDao.getMaxId() ?: 0) + 1
            setDao.insertSet(SetEntity(id = nextId, name = name))
            true
        }
    }
    // modifica el set en strapi y en room
    suspend fun updateSet(id: Int, name: String): Boolean {
        return try {
            val resp = api.updateSet(id, SetUpdateRequest(SetUpdateData(name)))
            if (resp.isSuccessful) {
                setDao.insertSet(resp.body()!!.data.toEntity())
            } else {
                Log.e("SetRepo","Error HTTP updateSet: ${resp.code()}")
                setDao.insertSet(SetEntity(id = id, name = name))
            }
            true
        } catch (e: IOException) {
            setDao.insertSet(SetEntity(id = id, name = name))
            true
        }
    }


    // elimina el set en strapi y en room
    suspend fun deleteSet(id: Int): Boolean {
        return try {
            val resp = api.deleteSet(id)
            if (resp.isSuccessful) {
                setDao.deleteSetById(id)
            } else {
                Log.e("SetRepo","Error HTTP deleteSet: ${resp.code()}")
                setDao.deleteSetById(id)
            }
            true
        } catch (e: IOException) {
            setDao.deleteSetById(id)
            true
        }
    }
}