package com.smqpro.zetnews.model

import com.smqpro.zetnews.BuildConfig
import com.smqpro.zetnews.model.response.News
import com.smqpro.zetnews.model.response.Result
import com.smqpro.zetnews.util.Constants
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.*

interface NewsApi {
    @GET("/search")
    suspend fun getNews(
        @Query(Constants.SHOW_TAGS_PARAM)
        tags: String = "contributor",
        @Query(Constants.API_KEY_PARAM)
        API_KEY: String = BuildConfig.API_KEY,
        @Query(Constants.QUERY_PARAM)
        query: String? = null,
        @Query(Constants.SECTION_PARAM)
        section: String? = null,
        @Query(Constants.ORDER_BY_PARAM)
        order: Constants.ORDER? = Constants.ORDER.NEWEST,
        @Query(Constants.FROM_DATE_PARAM)
        fromDate: String? = "2016-07-18",
        @Query(Constants.PAGE_PARAM)
        page: Int,
        @Query(Constants.SHOW_FIELDS_PARAM)
        fields: String = "thumbnail,trailText"
    ): Response<News>
}