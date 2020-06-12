package com.smqpro.zetnews.view.home

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.*
import android.net.NetworkCapabilities.*
import android.os.Build
import android.util.Log
import androidx.lifecycle.*
import com.smqpro.zetnews.model.NewsApplication
import com.smqpro.zetnews.model.response.CurrentPage
import com.smqpro.zetnews.model.response.News
import com.smqpro.zetnews.model.response.Result
import com.smqpro.zetnews.util.Constants
import com.smqpro.zetnews.util.Resource
import com.smqpro.zetnews.util.TAG
import kotlinx.coroutines.launch
import retrofit2.Response
import java.io.IOException

class HomeViewModel(
    app: Application,
    private val repository: HomeRepository
) : AndroidViewModel(app) {
    val news = MutableLiveData<Resource<List<Result>>>()

    //    val newsAvailable = MutableLiveData<Boolean>()
    var searchPage = 1
    var query = ""
    var section: String? = null
    var pages = 1
    var filter = 0

    init {
        setCurrentPage()
    }

    val cachedNews = Transformations.map(repository.getCachedNews()) {
        if (it.isNullOrEmpty()) {
            getUpdatedNews()
            Resource.Error<List<Result>>("No cached data. Trying to fetch the data from the network...")
        } else {
            Resource.Success<List<Result>>(it)
        }
    } as LiveData<Resource<List<Result>>>

    private fun cacheNews(resultList: List<Result>, resetNews: Boolean) = viewModelScope.launch {
        if (resetNews) updateCurrentPage(2)
        else updateCurrentPage(searchPage + 1)
        resultList.forEach {
            it.cache = true
        }
        if (resetNews) {
            repository.updateCachedNews(resultList)
            Log.d(TAG, "cacheNews: upsert list size - ${resultList.size}")
        } else {
            repository.insertCachedNews(resultList)
            Log.d(TAG, "cacheNews: insert list size - ${resultList.size}")
        }
    }

    private fun updateCurrentPage(currentPage: Int) = viewModelScope.launch {
        val currentPageObj = CurrentPage(0, currentPage)
        repository.upsertCurrentPage(currentPageObj)
        searchPage = currentPage
    }

    private fun setCurrentPage() =
        viewModelScope.launch {
            searchPage = repository.getCurrentPage()?.currentPage ?: 1
            pages = searchPage
        }

    fun getUpdatedNews(resetNews: Boolean = true) = viewModelScope.launch {
        news.postValue(Resource.Loading())
        Log.d(TAG, "getUpdatedNews: getting news from the network")
        try {
            if (hasInternetConnection()) {
                val order = when (filter) {
                    0 -> Constants.ORDER.NEWEST
                    1 -> Constants.ORDER.OLDEST
                    2 -> Constants.ORDER.RELEVANCE
                    else -> Constants.ORDER.NEWEST
                }
                val response =
                    repository.getNews(
                        searchQuery = query,
                        newsPage = searchPage,
                        order = order,
                        category = section
                    )
                handleNewsResponse(response, resetNews)
            } else {
                news.postValue(Resource.Error("No internet connection"))
            }
        } catch (t: Throwable) {
            when (t) {
                is IOException -> news.postValue(Resource.Error("Failed to connect to the server"))
                else -> news.postValue(Resource.Error("Error fetching the data"))
            }
        }

    }

    private fun handleNewsResponse(
        response: Response<News>,
        resetNews: Boolean
    ): Resource<List<Result>> {
        if (response.isSuccessful) {
            response.body()?.let { resResponse ->
                cacheNews(resResponse.response.results, resetNews)
                pages = resResponse.response.pages
                Log.d(TAG, "handleNewsResponse: number of pages - $pages")
                return Resource.Success(resResponse.response.results)
            }
        }
        return Resource.Error(response.message())
    }

    private fun hasInternetConnection(): Boolean {
        val connectivityManager = getApplication<NewsApplication>().getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val networkCapabilities =
                connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            return when {
                networkCapabilities.hasTransport(TRANSPORT_WIFI) -> true
                networkCapabilities.hasTransport(TRANSPORT_CELLULAR) -> true
                networkCapabilities.hasTransport(TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            connectivityManager.activeNetworkInfo?.run {
                return when (type) {
                    TYPE_WIFI -> true
                    TYPE_MOBILE -> true
                    TYPE_ETHERNET -> true
                    else -> false
                }
            }
            false
        }
    }

}