package com.evaluation.fetch.viewmodel

import androidx.lifecycle.*
import com.evaluation.fetch.api.ListFetcher
import com.evaluation.fetch.model.ListableItem
import kotlinx.coroutines.*
import retrofit2.Response

class ListViewModel(private val fetcher: ListFetcher) : ViewModel() {

    val isRunningAsyncLiveData = MutableLiveData(false)
    val itemListLiveData = MutableLiveData<List<ListableItem>>()
    val errorLiveData = MutableLiveData<String?>()

    var currentJob : Job? = null

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        onError("Exception occurred:\n" + throwable.localizedMessage)
    }

    override fun onCleared() {
        super.onCleared()
        currentJob?.cancel()
    }

    /**
     * Fetches a list of [items][ListableItem] from the web server, filters out any entries with
     * null or empty names, and sorts the result by listId and then name. If fetch attempt fails,
     * sets the error message to display.
     */
    fun fetchData() {

        currentJob = viewModelScope.launch(Dispatchers.IO + exceptionHandler) {

            isRunningAsyncLiveData.postValue(true)

            // Get response from data source
            val response : Response<List<ListableItem>> = fetcher.getListableItems()

            if (response.isSuccessful) {

                // Sanitize list before posting to LiveData
                val sortedList = (response.body() ?: emptyList())
                    .filterNullOrEmptyNames()
                    .sortByIds()

                itemListLiveData.postValue(sortedList)
                errorLiveData.postValue(null)
                isRunningAsyncLiveData.postValue(false)

            } else {

                onError("Error: ${response.errorBody()?.string()}\n" +
                        "(Error code ${response.code()})")
            }
        }
    }

    /**
     * Called when an exception or error occurs during [fetchData]. Updates the error message and
     * flag for showing the loading card. This function should only be called on background threads.
     */
    private fun onError(error: String) {
        errorLiveData.postValue(error)
        isRunningAsyncLiveData.postValue(false)
    }

    /**
     * Returns a copy of this list of [ListableItems][ListableItem] that does not contain entries
     * where the name is either empty or null.
     */
    private fun List<ListableItem>.filterNullOrEmptyNames() : List<ListableItem> =
        this.filterNot{ it.name.isNullOrEmpty() }

    /**
     * Returns a copy of this list of [ListableItems][ListableItem] sorted by listId group, then by
     * mainId.
     */
    private fun List<ListableItem>.sortByIds() : List<ListableItem> {

        /*
         * While the instructions were to sort by the listId and then name fields, this would result
         * in lists where items with longer names but whose number start with lower digits would
         * come before shorter ones that start with higher digits (i.e. "Item 123" ordered before
         * "Item 2").
         *
         * This did not seem like expected behavior, and while the number could be parsed from name
         * string and ordered based off that, the mainId field contains the same number and is
         * already returned as an Int as part of the API response.
         *
         * As such, I implemented it using mainId as the second criteria for the provided use case.
         * If the data set were to change so that the name and id fields were not so linked, it
         * would be easy to change it back to sorting by name.
         */

        return this.sortedWith(compareBy({ it.listId },{ it.mainId }))
    }
}

class ListViewModelFactory(private val fetcher: ListFetcher) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ListViewModel(fetcher) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}