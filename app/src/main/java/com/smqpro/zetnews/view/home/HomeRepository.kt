package com.smqpro.zetnews.view.home

import android.util.Log
import com.smqpro.zetnews.model.RetrofitInstance
import com.smqpro.zetnews.model.db.NewsDatabase
import com.smqpro.zetnews.model.response.CurrentPage
import com.smqpro.zetnews.model.response.News
import com.smqpro.zetnews.model.response.Result
import com.smqpro.zetnews.util.Constants
import com.smqpro.zetnews.util.TAG
import retrofit2.Response

class HomeRepository(
    private val db: NewsDatabase
) {

    suspend fun getNews(
        newsPage: Int,
        searchQuery: String? = null,
        order: Constants.ORDER = Constants.ORDER.NEWEST,
        category: String? = null
    ): Response<News> {
        val response = RetrofitInstance.api.getNews(
            page = newsPage,
            query = searchQuery,
            order = order,
            section = category
        )
        response.body()?.response?.results?.forEach {
            it.cache = true
        }
        Log.d(TAG, "getNews: size - ${response.body()?.response?.results?.size}")
        return response
    }

    suspend fun updateCachedNews(resultList: List<Result>) {
        db.getNewsDao().resetCachedNews(resultList)
    }

    suspend fun insertCachedNews(resultList: List<Result>) {
        db.getNewsDao().insertNewsList(resultList)
    }

    fun getCachedNewsDesc() = db.getNewsDao().selectCachedDesc()

    fun getCachedNewsAsc() = db.getNewsDao().selectCachedAsc()

    fun getCachedNews() = db.getNewsDao().selectCached()

    suspend fun getCurrentPage() = db.getNewsDao().getCurrentPage()

    suspend fun upsertCurrentPage(currentPage: CurrentPage) =
        db.getNewsDao().upsertCurrentPage(currentPage)

}