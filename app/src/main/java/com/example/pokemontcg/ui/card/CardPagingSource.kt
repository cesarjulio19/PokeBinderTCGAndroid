package com.example.pokemontcg.ui.card

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.pokemontcg.api.StrapiApiService
import com.example.pokemontcg.dto.CardDto
import com.example.pokemontcg.local.dao.CardDao
import com.example.pokemontcg.mapper.CardMapper

//Carga cada página desde la api
class CardPagingSource(
    private val api: StrapiApiService,
    private val cardDao: CardDao,
    private val setId: Int,
    private val pageSize: Int = 25
) : PagingSource<Int, CardDto>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, CardDto> {
        val page = params.key ?: 1
        return try {

            val resp = api.getCardsBySetPaged(
                setId = setId,
                page = page,
                pageSize = pageSize,
                sort = "id:desc"
            )
            if (!resp.isSuccessful) {
                return LoadResult.Error(Throwable("HTTP ${resp.code()}"))
            }

            // Paso 1: Convertir las respuestas en DTOs
            val dtoList = resp.body()!!.data.map { CardMapper.fromResponseToDto(it) }

            // Paso 2: Persistir esos DTOs en Room
            //   Primero convierto cada CardDto a CardEntity, luego los inserto
            val entities = dtoList.map { dto ->
                CardMapper.fromDtoToEntity(dto)
            }
            cardDao.insertCards(entities)

            // Paso 3: Calcular la clave de la siguiente página
            val nextKey = if (dtoList.size < pageSize) null else page + 1

            LoadResult.Page(
                data    = dtoList,
                prevKey = if (page == 1) null else page - 1,
                nextKey = nextKey
            )

        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, CardDto>): Int? {
        return state.anchorPosition
            ?.let { anchor ->
                state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                    ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
            }
    }
}