package com.evaluation.fetch.api

import com.evaluation.fetch.model.ListableItem
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

private const val BASE_URL = "https://fetch-hiring.s3.amazonaws.com/"

interface ListApiService {

    @GET("hiring.json")
    suspend fun getListableItems(): Response<List<ListableItem>>

    companion object {

        var listApiService: ListApiService? = null

        fun getInstance() : ListApiService {

            if (listApiService == null) {

                val retrofit = Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                listApiService = retrofit.create(ListApiService::class.java)
            }

            return listApiService!!
        }
    }
}