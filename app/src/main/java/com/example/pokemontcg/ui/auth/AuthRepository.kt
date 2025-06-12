package com.example.pokemontcg.ui.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.pokemontcg.api.StrapiAuthService
import com.example.pokemontcg.api.request.auth.LoginRequest
import com.example.pokemontcg.api.request.auth.RegisterRequest
import com.example.pokemontcg.api.request.person.PersonCreateData
import com.example.pokemontcg.api.request.person.PersonCreateRequest
import com.example.pokemontcg.local.dao.PersonDao
import com.example.pokemontcg.local.dao.UserDao
import com.example.pokemontcg.local.entity.PersonEntity
import com.example.pokemontcg.local.entity.UserEntity
import com.example.pokemontcg.mapper.PersonMapper
import com.example.pokemontcg.mapper.PersonMapper.toEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.net.SocketTimeoutException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: StrapiAuthService,
    private val userDao: UserDao,
    private val personDao: PersonDao,
    private val prefs: SharedPreferences,
    @ApplicationContext private val ctx: Context
) {
    suspend fun register(data: RegisterRequest): Result<Unit> {
        return try {
            // Llamada normal al endpoint
            val resp = authApi.register(data)
            if (!resp.isSuccessful) {
                // HTTP 4xx/5xx
                return Result.failure(Exception("HTTP ${resp.code()}"))
            }
            val body = resp.body()!!

            // Guarda token + userId
            prefs.edit()
                .putString("jwt", body.jwt)
                .putInt("userId", body.user.id)
                .apply()

            // Hash de la contraseña y guardado en Room
            val pwHash = MessageDigest
                .getInstance("SHA-256")
                .digest(data.password.toByteArray())
                .joinToString("") { "%02x".format(it) }
            userDao.insert(UserEntity(
                id = body.user.id,
                username   = body.user.username,
                email      = body.user.email,
                passwordHash = pwHash
            ))

            // Crea la Person en Strapi y en Room
            val personReq = PersonCreateRequest(
                data = PersonCreateData(
                    user      = body.user.id,
                    adminRole = false,
                    email     = data.email,
                    username  = data.username,
                    image     = null
                )
            )
            val token = prefs.getString("jwt","")!!
            val respPerson = authApi.createPerson("Bearer $token", personReq)
            if (respPerson.isSuccessful) {
                respPerson.body()?.data?.let { pd ->
                    personDao.insert(toEntity(pd))
                }
            } else {
                Log.e("AuthRepo","Error creando Person en Strapi: ${respPerson.code()}")
            }

            Result.success(Unit)

        } catch (e: SocketTimeoutException) {
            Log.w("AuthRepo","Timeout al registrar", e)
            Result.failure(Exception("El servidor no responde, comprueba tu conexión"))
        } catch (e: IOException) {
            Log.w("AuthRepo","IOException al registrar", e)
            Result.failure(Exception("No hay conexión de red"))
        }
    }

    suspend fun login(data: LoginRequest): Result<Unit> {
        return try {
            val resp = authApi.login(data)
            if (resp.isSuccessful) {
                val body = resp.body()!!
                prefs.edit()
                    .putString("jwt", body.jwt)
                    .putInt("userId", body.user.id)
                    .apply()
                // guarda/actualiza también en Room
                val pwHash = MessageDigest
                    .getInstance("SHA-256")
                    .digest(data.password.toByteArray())
                    .joinToString("") { "%02x".format(it) }
                Log.d("AuthRepo", "Login online OK; guardando usuario local: id=${body.user.id}, " +
                        "username='${data.identifier}', email='${body.user.email}'")

                userDao.insert(UserEntity(body.user.id, body.user.username, data.identifier, pwHash))
                Result.success(Unit)
            } else {
                // si offline,intenta buscar en Room
                val local = userDao.findByEmail(data.identifier)
                if (local != null && local.passwordHash == pwHash(data.password)) {
                    prefs.edit().putInt("userId", local.id).apply()
                    Result.success(Unit)
                }else {
                    return Result.failure(Exception("Credenciales inválidas"))
                }
            }
        } catch (e: IOException) {
            // Sin red: intento OFFLINE
            Log.d(
                "AuthRepo",
                "IOException during login, intentando offline con identifier='${data.identifier}'"
            )
            Log.d("AuthRepo", "Identifier raw ==>'${data.identifier}<' (longitud: ${data.identifier.length})")
            val local = userDao.findByEmail(data.identifier)
            Log.d("AuthRepo", "Resultado userDao.findByEmail('${data.identifier}') = $local")

            if (local != null && local.passwordHash == pwHash(data.password)) {
                // ¡Login offline OK! → guardo el userId en prefs
                prefs.edit()
                    .putInt("userId", local.id)
                    .putString("jwt", "")
                    .apply()
                return Result.success(Unit)
            } else {
                Log.d("AuthRepo", "No se encontró usuario local con email='${data.identifier}'")
                return Result.failure(Exception("No existe usuario local o contraseña inválida"))
            }
        }
    }

    fun logout() {
        prefs.edit().clear().apply()
    }

    /** Después de un login o registro exitoso, guarda el id en prefs */
    suspend fun getLoggedInUser(): UserEntity? {
        val userId = prefs.getInt("userId", -1)
        return if (userId != -1) userDao.findById(userId) else null
    }

    /** Carga la Person asociada al userId desde Room */
    suspend fun getPersonByUserId(userId: Int): PersonEntity? =
        personDao.findByUserId(userId)

    private fun pwHash(p: String): String = MessageDigest
        .getInstance("SHA-256")
        .digest(p.toByteArray())
        .joinToString("") { "%02x".format(it) }
}
