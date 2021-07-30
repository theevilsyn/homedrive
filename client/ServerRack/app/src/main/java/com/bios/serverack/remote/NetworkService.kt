package com.bios.serverack.remote

import com.bios.serverack.BuildConfig
import com.bios.serverack.model.User
import kotlinx.coroutines.Deferred
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import java.util.concurrent.TimeUnit


val API_POINT = BuildConfig.API_POINT

interface NetworkService {

    @Headers("Content-Type: application/json")
    @POST("login")
    fun doLogin(@Body user: User): Deferred<String>

    @Headers("Content-Type: application/json")
    @POST("login")
    fun doSignUp(@Body user: User): Deferred<String>

}

object ServiceBuilder {
    var logging = HttpLoggingInterceptor()

    var client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(logging.setLevel(HttpLoggingInterceptor.Level.BODY))
        .build()


    private val retrofit = Retrofit.Builder()
        .baseUrl(API_POINT) // change this IP for testing by your actual machine IP
        .addConverterFactory(MoshiConverterFactory.create())
        .client(client)
        .build()

    fun <T> buildService(service: Class<T>): T {
        return retrofit.create(service)
    }
}