package com.example.pokemontcg.api.request.person

data class PersonUpdateRequest(val data: PersonUpdateData)
data class PersonUpdateData(val username: String,
                             val image: Int?)
