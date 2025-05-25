package com.example.pokemontcg.mapper

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
}