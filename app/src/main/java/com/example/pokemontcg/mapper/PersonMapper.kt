package com.example.pokemontcg.mapper

import com.example.pokemontcg.api.response.person.PersonData
import com.example.pokemontcg.api.response.person.PersonResponse
import com.example.pokemontcg.local.entity.PersonEntity

object PersonMapper {
    /* fun fromResponse(item: PersonData): PersonEntity {
        val url = item.attributes.image?.data?.attributes?.url
        return PersonEntity(
            id        = item.id,
            userId    = item.attributes.user.data.id,
            username  = item.attributes.username,
            email     = item.attributes.email,
            adminRole = item.attributes.adminRole,
            imageUrl  = url
        )
    } */

    /* fun PersonData.toEntity() = PersonEntity(
        id        = this.id,
        userId    = attributes.user.data.id,
        username  = attributes.username,
        email     = attributes.email,
        adminRole = attributes.adminRole,
        imageUrl  = attributes.image?.data?.attributes?.url
    ) */

    fun toEntity(response: PersonData): PersonEntity {
        // Obtén los atributos anidados
        val attrs = response.attributes

        val userData = attrs.user?.data
        val userId = userData?.id
            ?: throw IllegalStateException("PersonResponse no contiene user.data; revisa que creaste con ?populate=user")


        return PersonEntity(
            id        = response.id,
            userId    = userId,
            username  = attrs.username,
            email     = attrs.email,
            adminRole = attrs.adminRole,
            imageUrl     = attrs.image?.data?.attributes?.url
        )
    }
}