package com.example.pokemontcg.mapper

import com.example.pokemontcg.api.response.person.PersonData
import com.example.pokemontcg.api.response.person.PersonResponse
import com.example.pokemontcg.dto.PersonDto
import com.example.pokemontcg.local.entity.PersonEntity

object PersonMapper {


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

    fun PersonData.toDomain(): PersonDto {
        return PersonDto(
            id        = this.id,
            username  = this.attributes.username,
            imageUrl  = this.attributes.image?.data?.attributes?.url
        )
    }

    fun PersonEntity.toDomain() = PersonDto(
        id        = this.id,
        username  = this.username,
        imageUrl  = this.imageUrl
    )

    fun PersonDto.toEntity() = PersonEntity(
        id        = this.id,
        username  = this.username,
        adminRole = false,
        email = "",
        userId = 0,
        imageUrl  = this.imageUrl,
    )
}