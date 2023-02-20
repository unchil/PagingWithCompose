package com.example.pagingwithcompose.data

import android.content.Context
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData


import com.example.pagingwithcompose.api.GithubService

import com.example.pagingwithcompose.db.Repo
import com.example.pagingwithcompose.db.RepoDatabase

import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject


class GithubRepository @Inject constructor(
    @ApplicationContext private val context: Context) {


    private val database = RepoDatabase.getInstance(context)

    fun getSearchResultStream(  query: String): Flow<PagingData<Repo>> {

        val dbQuery = "%${query.replace(' ', '%')}%"
        val pagingSourceFactory = { database.reposDao().reposByName(dbQuery)}

        @OptIn(ExperimentalPagingApi::class)
        return Pager(
            config = PagingConfig(
                pageSize = NETWORK_PAGE_SIZE,
                enablePlaceholders = false
            ),
            remoteMediator = GithubRemoteMediator(
                query,
                GithubService.create(),
                database
            ),
            pagingSourceFactory = pagingSourceFactory
        ).flow
    }

    companion object {
        const val NETWORK_PAGE_SIZE = 30
    }
}
