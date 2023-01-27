package com.evaluation.fetch.model

import com.google.gson.annotations.SerializedName

/**
 * Model class of data fetched and de-serialized off Fetch web server.
 */
data class ListableItem(
    @SerializedName("id") val mainId : Int,
    val listId : Int,
    val name : String?)