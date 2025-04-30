package com.example.pokemontcg.mapper

import com.example.pokemontcg.api.response.set.SetData
import com.example.pokemontcg.local.entity.SetEntity

fun SetData.toEntity(): SetEntity {
    return SetEntity(
        id = id,
        name = attributes.name
    )
}