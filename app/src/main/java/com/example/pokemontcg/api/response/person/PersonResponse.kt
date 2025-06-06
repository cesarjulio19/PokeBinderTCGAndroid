package com.example.pokemontcg.api.response.person

data class PersonResponse(
    val data: PersonData?
)

data class PersonData(
    val id: Int,
    val attributes: PersonAttributes
)

data class PersonAttributes(
    val adminRole: Boolean,
    val email: String,
    val username: String,
    val image: ImageRelation?,
    val user: UserRelation?
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


data class UserRelation(
    val data: UserData?
)

data class UserData(
    val id: Int,
    val attributes: UserAttributes
)

data class UserAttributes(
    val username: String,
    val email: String
)
