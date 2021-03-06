package com.smqpro.zetnews.view.liked

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smqpro.zetnews.model.response.Result
import kotlinx.coroutines.launch

class LikedViewModel(private val repository: LikedRepository) : ViewModel() {

    fun upsertNews(result: Result) = viewModelScope.launch {
        repository.upsertNews(result)
    }

    fun getLikedNews() = repository.getLikedNews()
}