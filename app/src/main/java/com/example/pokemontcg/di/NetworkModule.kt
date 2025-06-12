package com.example.pokemontcg.di

import android.content.Context
import android.content.SharedPreferences
import com.example.pokemontcg.api.StrapiApiService
import com.example.pokemontcg.api.StrapiAuthService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            // hasta 30 s de conexión
            .connectTimeout(30, TimeUnit.SECONDS)
            // hasta 30 s esperando respuesta
            .readTimeout(30, TimeUnit.SECONDS)
            // hasta 30 s para enviar el body (p.ej. imagen grande)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://pokemontcg-strapi.onrender.com/api/") // https://pokemontcg-strapi.onrender.com/api/ http://10.0.2.2:1337/api/
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideStrapiApiService(retrofit: Retrofit): StrapiApiService =
        retrofit.create(StrapiApiService::class.java)

    @Provides
    @Singleton
    fun provideStrapiAuthService(retrofit: Retrofit): StrapiAuthService =
        retrofit.create(StrapiAuthService::class.java)

    @Provides
    @Singleton
    fun provideSharedPreferences(
        @ApplicationContext ctx: Context
    ): SharedPreferences =
        ctx.getSharedPreferences("prefs_pokemontcg", Context.MODE_PRIVATE)
}