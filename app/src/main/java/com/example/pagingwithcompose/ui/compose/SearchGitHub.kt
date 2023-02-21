package com.example.pagingwithcompose.ui.compose

import android.annotation.SuppressLint
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.pagingwithcompose.OnExploreItemClicked
import com.example.pagingwithcompose.R
import com.example.pagingwithcompose.db.Repo
import com.example.pagingwithcompose.ui.theme.PagingWithComposeTheme
import com.example.pagingwithcompose.vmodel.DEFAULT_QUERY
import com.example.pagingwithcompose.vmodel.SearchGitHubViewModel
import com.example.pagingwithcompose.vmodel.UiAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "CoroutineCreationDuringComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchGitHubRepo(
    onExploreItemClicked: OnExploreItemClicked,
    viewModel: SearchGitHubViewModel = viewModel()
){
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyGridState()

    var isVisibleFooter by rememberSaveable { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    val repoList = viewModel.pagingDataFlow.collectAsLazyPagingItems()

    PagingWithComposeTheme {
        Surface {

            Column(modifier = Modifier.padding( 10.dp)) {

                SearchTextField(  actionHandler = viewModel.actionHandler )

                Scaffold (
                    bottomBar = { GridProgressIndicator(isVisibility = isVisibleFooter) },
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    floatingActionButton = { UpButton(listState = listState, coroutineScope = coroutineScope) }
                ) { innerPadding ->

                    Column(modifier = Modifier.padding(innerPadding)) {

                         repoList.apply {

                             when (loadState.refresh) {
                                 is LoadState.NotLoading -> {

                                     if (repoList.itemCount > 0) {

                                         LazyVerticalGrid(
                                             columns = GridCells.Fixed(1),
                                             state = listState,
                                             verticalArrangement = Arrangement.spacedBy(8.dp)
                                         ) {
                                             items(repoList.itemCount) {
                                                 repoList[it]?.let {
                                                     ItemCard(
                                                         onExploreItemClicked = onExploreItemClicked,
                                                         item = it
                                                     )
                                                 }
                                             }
                                         }
                                     } else {
                                         Box(modifier = Modifier.fillMaxSize()) {
                                             Text(
                                                 modifier = Modifier.align(Alignment.Center),
                                                 text = stringResource(id = R.string.query_result_empty_msg),
                                                 style = MaterialTheme.typography.headlineMedium,
                                                 color = Color.Black

                                             )
                                         }
                                     }



                                 }
                                 // Show loading spinner during initial load or refresh.
                                 is LoadState.Loading -> {
                                     Box(modifier = Modifier.fillMaxSize()) {
                                         CircularProgressIndicator(
                                             color = Color.Red,
                                             modifier = Modifier.align(Alignment.Center)
                                         )
                                     }
                                 }
                                 // Show the retry state if initial load or refresh fails.
                                 is LoadState.Error -> {
                                     coroutineScope.launch {
                                         val result = snackbarHostState.showSnackbar(
                                             message =  "Data Loading Error",
                                             actionLabel = "Retry",
                                             duration = SnackbarDuration.Long
                                         )
                                         when(result){
                                             SnackbarResult.ActionPerformed -> {
                                                 viewModel.searchQueryFlow.collectLatest {
                                                     viewModel.actionHandler( UiAction.Search( searchQuery = it.searchQuery ) )
                                                 }
                                             }
                                             else -> { }
                                         }
                                     }
                                 }
                             }

                             when(loadState.append){
                                 is LoadState.NotLoading -> {
                                     isVisibleFooter = false
                                 }
                                 is LoadState.Loading -> {
                                     isVisibleFooter = true
                                 }
                                 is LoadState.Error -> {
                                     isVisibleFooter = false
                                     coroutineScope.launch {
                                         snackbarHostState.showSnackbar(
                                             message = "Scroll Loading Error"
                                             ,duration = SnackbarDuration.Short
                                         )
                                     }
                                 }
                             }

                        }
                    }
                }
            }

        }
    }
}


@Composable
fun GridProgressIndicator(isVisibility:Boolean){
    if(isVisibility) {
        CenterAlignedTopAppBar(title = {
            CircularProgressIndicator(color = Color.LightGray)
        })
    }
}

@Composable
fun UpButton(listState: LazyGridState,  coroutineScope: CoroutineScope){

    val showButton by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0
        }
    }

    if( showButton) {

            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        listState.animateScrollToItem(0)

                    }
                }
            ) { Text(stringResource(id = R.string.list_fab_title)) }

    }

}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SearchTextField(actionHandler: (UiAction) -> Unit) {

    var title by remember { mutableStateOf(DEFAULT_QUERY) }

    val keyboardController = LocalSoftwareKeyboardController.current

    OutlinedTextField(
        value = title
        ,onValueChange = { title = it}
        ,leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = "search")}
        ,label = { Text("Search GitHub Repository") }
        ,singleLine = true
        ,keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
        ,keyboardActions = KeyboardActions(
            onSearch = {
                title.trim().let {
                    if (it.isNotEmpty()) {
                        actionHandler(UiAction.Search(searchQuery = it))
                    }
                }
                keyboardController?.hide()
            }
        )
        ,modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    )


}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemCard(
    onExploreItemClicked: OnExploreItemClicked,
    item:Repo){

    Card(
        onClick = { onExploreItemClicked(item)} ,
        modifier = Modifier,
        colors = CardDefaults.cardColors(),
        elevation = CardDefaults.cardElevation()
    ) {

        Column(modifier = Modifier.padding(10.dp)) {

            Text(
                text = item.fullName
                ,style = MaterialTheme.typography.titleMedium
                ,color = Color.Blue
            )

            item.description?.let {
                Text(
                    text = it
                    ,style = MaterialTheme.typography.bodySmall
                    ,color = Color.Black
                    , modifier = Modifier.padding(vertical = 6.dp)
                )
            }

            Row {

                item.language?.let {
                    Text(
                        text = "Language:${it}",
                        style = MaterialTheme.typography.bodySmall ,
                        modifier = Modifier
                            .weight(10f)
                            .wrapContentWidth(Alignment.Start)
                    )
                }

                Icon(
                    imageVector = Icons.Filled.Star
                    ,contentDescription = "start"
                    , tint = Color.Black
                    ,modifier = Modifier
                        .weight(10f)
                        .wrapContentWidth(Alignment.End)
                        .height(16.dp)
                )

                Text(
                    text = "${item.stars}"
                    ,style = MaterialTheme.typography.bodySmall
                )

                Icon(
                    painter = painterResource(R.drawable.ic_git_branch)
                    ,contentDescription = "forks"
                    , tint = Color.Black
                    ,modifier = Modifier.height(16.dp)
                )

                Text(
                    text = "${item.forks}"
                    ,style = MaterialTheme.typography.bodySmall
                )

            }
        }
    }
}


