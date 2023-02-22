package com.example.pagingwithcompose.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.example.pagingwithcompose.api.GithubInterface
import com.example.pagingwithcompose.db.RemoteKeys
import com.example.pagingwithcompose.db.Repo
import com.example.pagingwithcompose.db.RepoDatabase


private const val GITHUB_STARTING_PAGE_INDEX = 1
private const val IN_QUALIFIER = "in:name,description"

@OptIn(ExperimentalPagingApi::class)
class GithubRemoteMediator (
    private val query: String,
   // private val service: GithubService,
    private val service: GithubInterface,
    private val repoDatabase: RepoDatabase
): RemoteMediator<Int, Repo>() {


    override suspend fun load(loadType: LoadType, state: PagingState<Int, Repo>): MediatorResult {

        val page = when(loadType) {

            LoadType.REFRESH -> {
                val remoteKeys = getRemoteKeyClosestToCurrentPosition(state)
                remoteKeys?.nextKey?.minus(1) ?: GITHUB_STARTING_PAGE_INDEX
            }

            LoadType.PREPEND -> {
                val remoteKeys = getRemoteKeyFirstItem(state)
                val prevKeys = remoteKeys?.prevKey
                if (prevKeys == null) {
                    return MediatorResult.Success(endOfPaginationReached = remoteKeys != null)
                }
                prevKeys
            }

            LoadType.APPEND -> {
                val remoteKeys = getRemoteKeyForLastItem(state)
                val nextKey = remoteKeys?.nextKey
                if (nextKey == null) {
                    return MediatorResult.Success(endOfPaginationReached = remoteKeys != null)
                }
                nextKey
            }

        }

        val apiQuery = query + IN_QUALIFIER

        try {

            val apiResponse = service.searchRepos( apiQuery, page, state.config.pageSize)
            val repos = apiResponse.items
            val endOfPaginationReached = repos.isEmpty()

            repoDatabase.withTransaction {

                if(loadType == LoadType.REFRESH){
                    repoDatabase.remoteKeysDao().clearRemoteKeys()
                    repoDatabase.reposDao().clearRepos()
                }

                val prevKey = if (page == GITHUB_STARTING_PAGE_INDEX ) null else page - 1
                val nextKey = if (endOfPaginationReached) null else page + 1
                val keys = repos.map {
                    RemoteKeys(repoId = it.id, prevKey, nextKey)
                }

                repoDatabase.remoteKeysDao().insertAll(keys)
                repoDatabase.reposDao().insertAll(repos)
            }

            return MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch(e: Exception) {
            return MediatorResult.Error(e)
        }


    }

    private suspend fun getRemoteKeyClosestToCurrentPosition(state: PagingState<Int, Repo>): RemoteKeys?{
        return state.anchorPosition?.let {
            state.closestItemToPosition(it)?.id?.let {
                repoDatabase.remoteKeysDao().remoteKeysRepoId(it)
            }
        }
    }

    private suspend fun getRemoteKeyFirstItem(state: PagingState<Int, Repo>): RemoteKeys?{
        return state.pages.firstOrNull() {
            it.data.isNotEmpty()
        }?.data?.firstOrNull()?.let {
            repoDatabase.remoteKeysDao().remoteKeysRepoId(it.id)
        }
    }

    private suspend fun getRemoteKeyForLastItem(state: PagingState<Int, Repo>): RemoteKeys?{
        return state.pages.lastOrNull {
            it.data.isNotEmpty()
        }?.data?.lastOrNull()?.let {
            repoDatabase.remoteKeysDao().remoteKeysRepoId(it.id)
        }
    }


}