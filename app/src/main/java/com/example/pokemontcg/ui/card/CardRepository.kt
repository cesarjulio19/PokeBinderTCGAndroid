package com.example.pokemontcg.ui.card

import android.content.Context
import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.pokemontcg.api.StrapiApiService
import com.example.pokemontcg.api.request.card.CardCreateRequest
import com.example.pokemontcg.api.request.card.CardUpdateRequest
import com.example.pokemontcg.dto.CardDto
import com.example.pokemontcg.local.dao.CardDao
import com.example.pokemontcg.mapper.CardMapper
import androidx.paging.map
import com.example.pokemontcg.ui.auth.ConnectivityUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MultipartBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardRepository @Inject constructor(
    private val api: StrapiApiService,
    private val cardDao: CardDao,
    @ApplicationContext private val appContext: Context
) {
    //obtiene las cartas
    fun getAllCards(): Flow<List<CardDto>> =
        cardDao.getAllCards()
            .map { list -> list.map(CardMapper::fromEntityToDto) }

    //elimina una carta
    suspend fun deleteCard(id: Int): Boolean {
        return try {
            val resp = api.deleteCard(id)
            if (resp.isSuccessful) {
                cardDao.deleteCardById(id)
            } else {
                Log.e("CardRepo", "Error HTTP deleteCard: ${resp.code()}")
                cardDao.deleteCardById(id)    // fallback a local
            }
            true
        } catch (e: IOException) {
            // no hay red
            cardDao.deleteCardById(id)
            true
        }
    }
    //obtiene cartas por set en room
    fun getCardsBySet(setId: Int): Flow<List<CardDto>> =
        cardDao.getCardsBySet(setId)
            .map { list -> list.map(CardMapper::fromEntityToDto) }

    //sincroniza con strapi y carga en room
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
    //obtiene pagina de cartaspor set (strapi)
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
     //obtiene pagina de cartas por set (room)
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

    //decide si usar las de strapi o room para obtener pagina de carta por set
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
        return try {
            // sube imagen si hay
            val imageId = imagePart?.let { part ->
                val upload = api.uploadImage(part)
                if (!upload.isSuccessful || upload.body().isNullOrEmpty()) throw IOException()
                upload.body()!![0].id
            }
            // prepara request final
            val req2 = request.copy(data = request.data.copy(image = imageId))
            val resp = api.createCard(card = req2)
            if (resp.isSuccessful) {
                val entity = CardMapper.fromResponseToEntity(resp.body()!!.data)
                cardDao.insertCards(listOf(entity))
            } else {
                Log.e("CardRepo", "Error HTTP createCard: ${resp.code()}")
                // fallback local: construye una entidad mínima a partir del request
                val local = CardMapper.fromCreateRequestToEntity(req2)
                cardDao.insertCards(listOf(local))
            }
            true
        } catch (e: Exception) {
            // offline o subida de imagen fallida
            val local = CardMapper.fromCreateRequestToEntity(request)
            cardDao.insertCards(listOf(local))
            true
        }
    }
    //edita una carta
    suspend fun updateCardWithImage(
        id: Int,
        request: CardUpdateRequest,
        imagePart: MultipartBody.Part?
    ): Boolean {
        return try {
            val imageId = imagePart?.let { part ->
                val upload = api.uploadImage(part)
                if (!upload.isSuccessful || upload.body().isNullOrEmpty()) throw IOException()
                upload.body()!![0].id
            }
            val req2 = request.copy(data = request.data.copy(image = imageId))
            val resp = api.updateCard(id = id, card = req2)
            if (resp.isSuccessful) {
                val entity = CardMapper.fromResponseToEntity(resp.body()!!.data)
                cardDao.insertCards(listOf(entity))
            } else {
                Log.e("CardRepo", "Error HTTP updateCard: ${resp.code()}")
                // fallback local: convierte request en entidad
                val local = CardMapper.fromUpdateRequestToEntity(id, req2)
                cardDao.insertCards(listOf(local))
            }
            true
        } catch (e: Exception) {
            // offline
            val local = CardMapper.fromUpdateRequestToEntity(id, request)
            cardDao.insertCards(listOf(local))
            true
        }
    }

    // Lee de Room el listado completo
    suspend fun getCardsOnceForSet(setId: Int): List<CardDto> {
        return cardDao.getCardsBySetOnce(setId)
            .map { CardMapper.fromEntityToDto(it) }
    }

    //Cuenta cuántas cartas hay en el set
    suspend fun getCardsCountOnce(setId: Int): Int {
        return cardDao.getCardsBySetOnce(setId).size
    }
}