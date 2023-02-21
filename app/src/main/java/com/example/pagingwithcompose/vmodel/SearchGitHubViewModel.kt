package com.example.pagingwithcompose.vmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.pagingwithcompose.data.GithubRepository
import com.example.pagingwithcompose.db.Repo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

const val DEFAULT_QUERY = "Android"

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchGitHubViewModel @Inject constructor (
    private val githubRepository: GithubRepository
) : ViewModel() {

    val pagingDataFlow: Flow<PagingData<Repo>>
    val searchQueryFlow: Flow<UiAction.Search>
    val actionHandler: (UiAction) -> Unit

    init {

        val actionStateFlow = MutableSharedFlow<UiAction>()

        searchQueryFlow = actionStateFlow
            .filterIsInstance<UiAction.Search>()
            .distinctUntilChanged()
            .onStart {
                emit( UiAction.Search(searchQuery = DEFAULT_QUERY) )

            }

        pagingDataFlow = searchQueryFlow
            .flatMapLatest {
                searchRepo(query = it.searchQuery)
            }.cachedIn(viewModelScope)

        actionHandler = {
            viewModelScope.launch {
                actionStateFlow.emit(it)
            }
        }

    }

    private fun searchRepo(query: String): Flow<PagingData<Repo>> =
        githubRepository.getSearchResultStream(query)


}

sealed class UiAction {
    data class Search(val searchQuery: String) : UiAction()
}
