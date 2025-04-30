package com.example.pokemontcg.api.response.card

data class CardResponse(
    val data: CardData
)

data class CardData(
    val id: Int,
    val attributes: CardAttributes
) {

}

data class CardAttributes(
    val name: String,
    val number: Int,
    val illustration: String?,
    val image: ImageRelation?,
    val type: String?,
    val rarity: String?,
    val superType: String?,
    val set: SetRelation
)

data class SetRelation(
    val data: SetData?
)

data class SetData(
    val id: Int,
    val attributes: SetAttributes
)

data class SetAttributes(
    val name: String

)

data class ImageRelation(
    val data: ImageData?
)

data class ImageData(
    val id: Int,
    val attributes: ImageAttributes
)

data class ImageAttributes(
    val url: String,
    val formats: Formats?
)

data class Formats(
    val thumbnail: FormatDetail?,
    val small: FormatDetail?,
    val medium: FormatDetail?,
    val large: FormatDetail?
)

data class FormatDetail(
    val url: String
)
