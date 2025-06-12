package com.example.pokemontcg.mapper

import com.example.pokemontcg.api.request.card.CardCreateRequest
import com.example.pokemontcg.api.request.card.CardUpdateRequest
import com.example.pokemontcg.dto.CardDto
import com.example.pokemontcg.local.entity.CardEntity
import com.example.pokemontcg.api.response.card.CardData

object CardMapper {
    fun fromResponseToEntity(response: CardData): CardEntity {
        val attributes = response.attributes
        return CardEntity(
            id = response.id,
            name = attributes.name,
            number = attributes.number,
            type = attributes.type,
            rarity = attributes.rarity,
            superType = attributes.superType,
            illustration = attributes.illustration,
            image = attributes.image?.data?.attributes?.url,
            setId = attributes.set.data!!.id
        )
    }

    fun fromEntityToDto(entity: CardEntity): CardDto {
        return CardDto(
            id = entity.id,
            name = entity.name,
            number = entity.number,
            type = entity.type,
            rarity = entity.rarity,
            superType = entity.superType,
            illustration = entity.illustration,
            image = entity.image,
            setId = entity.setId
        )
    }

    fun fromResponseToDto(response: CardData): CardDto = CardDto(
        id         = response.id,
        name       = response.attributes.name,
        number     = response.attributes.number,
        type       = response.attributes.type,
        rarity     = response.attributes.rarity,
        superType  = response.attributes.superType,
        illustration = response.attributes.illustration,
        image      = response.attributes.image?.data?.attributes?.url,
        setId      = response.attributes.set.data!!.id
    )

    fun fromDtoToEntity(dto: CardDto): CardEntity {
        return CardEntity(
            id           = dto.id,
            name         = dto.name,
            number       = dto.number,
            type         = dto.type,
            rarity       = dto.rarity,
            superType    = dto.superType,
            illustration = dto.illustration,
            image        = dto.image,
            setId        = dto.setId!!
        )
    }

    fun fromCreateRequestToEntity(request: CardCreateRequest): CardEntity {
        val data = request.data
        return CardEntity(
            id = 0,
            name = data.name,
            number = data.number,
            type = data.type,
            rarity = data.rarity,
            superType = data.superType,
            image = "",
            illustration = "",
            setId = data.set
        )
    }

    fun fromUpdateRequestToEntity(id: Int, request: CardUpdateRequest): CardEntity {
        val data = request.data
        return CardEntity(
            id = id,
            name = data.name,
            number = data.number,
            type = data.type,
            rarity = data.rarity,
            superType = data.superType,
            image = "",
            illustration = "",
            setId = data.set
        )
    }
}