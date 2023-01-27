package com.evaluation.fetch.api

private const val BASE_URL = "https://fetch-hiring.s3.amazonaws.com/"

/**
 * Repository responsible for controlling access to data source.
 */
class ListFetcher(private val listApiService: ListApiService) {

    suspend fun getListableItems() = listApiService.getListableItems()
}