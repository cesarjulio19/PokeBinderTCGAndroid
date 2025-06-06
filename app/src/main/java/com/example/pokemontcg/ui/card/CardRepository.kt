package com.example.pokemontcg.ui.card

import android.content.Context
import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.pokemontcg.api.StrapiApiService
import com.example.pokemontcg.api.request.card.CardCreateData
import com.example.pokemontcg.api.request.card.CardCreateRequest
import com.example.pokemontcg.api.request.card.CardUpdateData
import com.example.pokemontcg.api.request.card.CardUpdateRequest
import com.example.pokemontcg.dto.CardDto
import com.example.pokemontcg.local.dao.CardDao
import com.example.pokemontcg.local.entity.CardEntity
import com.example.pokemontcg.mapper.CardMapper
import androidx.paging.map
import com.example.pokemontcg.ui.auth.ConnectivityUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import okhttp3.MultipartBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardRepository @Inject constructor(
    private val api: StrapiApiService,
    private val cardDao: CardDao,
    @ApplicationContext private val appContext: Context
) {

    fun getAllCards(): Flow<List<CardDto>> =
        cardDao.getAllCards()
            .map { list -> list.map(CardMapper::fromEntityToDto) }


    suspend fun syncCardsFromApi() {
        try {
            val resp = api.getAllCards()
            if (resp.isSuccessful) {
                val entities = resp.body()!!.data
                    .map { CardMapper.fromResponseToEntity(it) }
                cardDao.clearCards()
                cardDao.insertCards(entities)
            } else {
                Log.e("CardRepo", "Error HTTP al refrescar cards: ${resp.code()}")
            }
        } catch (e: Exception) {
            Log.w("CardRepo", "Sin red: uso Room cache para todas las cartas", e)
        }
    }

    suspend fun getCardById(id: Int): CardDto? =
        cardDao.getCardById(id)?.let(CardMapper::fromEntityToDto)

    suspend fun createCard(request: CardCreateRequest): Boolean {
        val resp = api.createCard(card = request)
        return if (resp.isSuccessful) {
            val entity = CardMapper.fromResponseToEntity(resp.body()!!.data)
            cardDao.insertCards(listOf(entity))
            true
        } else {
            Log.e("CardRepo", "Error HTTP createCard: ${resp.code()}")
            false
        }
    }

    suspend fun updateCard(id: Int, request: CardUpdateRequest): Boolean {
        val resp = api.updateCard(id = id, card = request)
        return if (resp.isSuccessful) {
            val entity = CardMapper.fromResponseToEntity(resp.body()!!.data)
            cardDao.insertCards(listOf(entity))
            true
        } else {
            Log.e("CardRepo", "Error HTTP updateCard: ${resp.code()}")
            false
        }
    }

    suspend fun deleteCard(id: Int): Boolean {
        val resp = api.deleteCard(id)
        return if (resp.isSuccessful) {
            cardDao.deleteCardById(id)
            true
        } else {
            Log.e("CardRepo", "Error HTTP deleteCard: ${resp.code()}")
            false
        }
    }

    fun getCardsBySet(setId: Int): Flow<List<CardDto>> =
        cardDao.getCardsBySet(setId)
            .map { list -> list.map(CardMapper::fromEntityToDto) }

   /* suspend fun syncCardsBySet(setId: Int) {
        try {
            val response = api.getCardsBySetId(setId = setId)
            if (response.isSuccessful) {
                val entities = response.body()!!.data.map { CardMapper.fromResponseToEntity(it) }
                cardDao.deleteCardsBySetId(setId)
                cardDao.insertCards(entities)
            } else {
                Log.e("CardRepo", "Error HTTP al syncCards: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.w("CardRepo", "No hay internet, uso cartas de Room para set $setId", e)
        }
    }*/

    suspend fun syncCardsBySet(setId: Int) {
        val conteoAntes = cardDao.getCardsBySetOnce(setId).size
        Log.i("CardRepo", "=== FILAS EN ROOM ANTES DE SYNC: $conteoAntes para set $setId")
        try {
            // 1) Baja de Strapi sólo las cartas de este set
            val resp = api.getCardsBySetId(setId = setId)
            if (!resp.isSuccessful) {
                Log.e("CardRepo", "Error HTTP al syncCards: ${resp.code()}")
                return
            }
            val remoteEntities = resp.body()!!.data
                .map { CardMapper.fromResponseToEntity(it) }
                .also { Log.i("CardRepo", "Remote cards: ${it.map { c -> c.id }}") }
                .filter { it.setId == setId } // ya viene filtrado, por si acaso

            // 2) Carga de Room (lista “snapshot”)
            val localEntities = cardDao.getCardsBySetOnce(setId)
                .also { Log.i("CardRepo", "Local cards: ${it.map { c -> c.id }}") }

            // 3) Determina diffs
            val remoteIds = remoteEntities.map { it.id }.toSet()
            val localIds  = localEntities .map { it.id }.toSet()

            val toInsert = remoteEntities.filter { it.id !in localIds }
            val toDelete = localEntities .filter { it.id !in remoteIds }
            // Para actualizar, compara entidad a entidad
            val toMaybeUpdate = remoteEntities.filter { it.id in localIds }
            val toUpdate = toMaybeUpdate.filter { remote ->
                val local = localEntities.first { it.id == remote.id }
                remote != local
            }

            Log.i("CardRepo", "Insert: ${toInsert.map { it.id }}")
            Log.i("CardRepo", "Update: ${toUpdate.map { it.id }}")
            Log.i("CardRepo", "Delete: ${toDelete.map { it.id }}")

            // 4) Aplica cambios en Room
            /*if (toDelete.isNotEmpty()) {
                cardDao.deleteCards(toDelete)
            }*/
            if (toInsert.isNotEmpty() || toUpdate.isNotEmpty()) {
                cardDao.insertCards(toInsert + toUpdate)
            }
            val conteoDespues = cardDao.getCardsBySetOnce(setId).size
            Log.i("CardRepo", "=== FILAS EN ROOM DESPUÉS DE SYNC: $conteoDespues para set $setId")
        } catch (e: Exception) {
            Log.w("CardRepo", "Sin red, uso caché Room para set $setId", e)
            val conteoDespues = cardDao.getCardsBySetOnce(setId).size
            Log.i("CardRepo", "=== FILAS EN ROOM DESPUÉS DE SYNC: $conteoDespues para set $setId")
        }
    }

    fun getPagedCardsBySetApi(
        setId: Int,
        pageSize: Int = 25
    ): Flow<PagingData<CardDto>> {
        return Pager(
            config = PagingConfig(
                pageSize = pageSize,
                prefetchDistance = 5,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                CardPagingSource(api,cardDao, setId, pageSize)
            }
        )
            .flow
            .map { pagingData ->
                pagingData
            }
    }

     fun getPagedCardsBySetRoom(setId: Int, pageSize: Int = 25): Flow<PagingData<CardDto>> {
        Log.i("CardRepo", "Número de cartas en Room para set ")
        return Pager(
            config = PagingConfig(
                pageSize = pageSize,
                prefetchDistance = 5,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { cardDao.pagingSourceBySet(setId) }
        )
            .flow
            .map { pagingData ->
                // Convertimos cada CardEntity a CardDto
                pagingData.map { entity -> CardMapper.fromEntityToDto(entity) }
            }
    }

    fun getPagedCardsBySet(setId: Int, pageSize: Int = 25): Flow<PagingData<CardDto>> {
        return if (ConnectivityUtils.isOnline(appContext)) {
            // Si hay red, uso la fuente API .
            getPagedCardsBySetApi(setId, pageSize)
        } else {
            // Si no hay red, directamente devuelvo el paginado de Room.
            getPagedCardsBySetRoom(setId, pageSize)
        }
    }


    /**
     * Sube la imagen y luego crea la carta en Strapi y Room.
     * @param requestBuilder construye un CardCreateRequest sin 'image' aún.
     * @param imagePart   el MultipartBody.Part con la imagen (o null si no hay).
     */
    suspend fun createCardWithImage(
        request: CardCreateRequest,
        imagePart: MultipartBody.Part?
    ): Boolean {
        // 1) sube imagen primero, si existe, y recupera su id
        val imageId: Int? = imagePart?.let { part ->
            val resp = api.uploadImage(part)
            if (!resp.isSuccessful || resp.body().isNullOrEmpty()) return false
            resp.body()!![0].id
        }

        // 2) crea un nuevo CardCreateRequest con el imageId inyectado
        val newData = request.data.copy(image = imageId)
        val newReq = CardCreateRequest(newData)

        // 3) llama al endpoint POST /cards
        val resp = api.createCard(card = newReq)
        return if (resp.isSuccessful) {
            // 4) si OK, guarda en Room
            val entity = CardMapper.fromResponseToEntity(resp.body()!!.data)
            cardDao.insertCards(listOf(entity))
            true
        } else {
            Log.e("CardRepo","Error creando carta ${resp.code()}")
            false
        }
    }

    suspend fun updateCardWithImage(
        id: Int,
        request: CardUpdateRequest,
        imagePart: MultipartBody.Part?
    ): Boolean {
        // 1) sube imagen si viene nueva y saca su id
        val imageId: Int? = imagePart?.let { part ->
            api.uploadImage(part).let { resp ->
                if (!resp.isSuccessful || resp.body().isNullOrEmpty()) return false
                resp.body()!![0].id
            }
        }

        // 2) crea un nuevo CardUpdateRequest con el imageId inyectado
        val updatedData = request.data.copy(image = imageId)
        val newReq = CardUpdateRequest(updatedData)

        // 3) llama a Strapi
        val resp = api.updateCard(id = id, card = newReq)
        return if (resp.isSuccessful) {
            // 4) si OK, guarda en Room
            val entity = CardMapper.fromResponseToEntity(resp.body()!!.data)
            cardDao.insertCards(listOf(entity))
            true
        } else {
            Log.e("CardRepo","Error actualizando carta ${resp.code()}")
            false
        }
    }
}