package com.example.pokemontcg.ui.card

import android.util.Log
import com.example.pokemontcg.api.StrapiApiService
import com.example.pokemontcg.api.request.card.CardCreateRequest
import com.example.pokemontcg.api.request.card.CardUpdateRequest
import com.example.pokemontcg.dto.CardDto
import com.example.pokemontcg.local.dao.CardDao
import com.example.pokemontcg.mapper.CardMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardRepository @Inject constructor(
    private val api: StrapiApiService,
    private val cardDao: CardDao
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
        } catch (e: Exception) {
            Log.w("CardRepo", "Sin red, uso caché Room para set $setId", e)
        }
    }
}