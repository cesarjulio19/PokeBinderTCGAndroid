package com.example.pokemontcg.ui.card

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.pokemontcg.api.StrapiApiService
import com.example.pokemontcg.dto.CardDto
import com.example.pokemontcg.mapper.CardMapper

//Carga cada página desde la api
class CardPagingSource(
    private val api: StrapiApiService,
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
            if (resp.isSuccessful) {
                val body = resp.body()!!.data
                val dtos = body.map { CardMapper.fromResponseToDto(it) }
                val nextKey = if (body.size < pageSize) null else page + 1
                LoadResult.Page(
                    data = dtos,
                    prevKey = if (page == 1) null else page - 1,
                    nextKey = nextKey
                )
            } else {
                LoadResult.Error(Throwable("HTTP ${resp.code()}"))
            }
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