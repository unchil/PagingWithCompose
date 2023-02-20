package com.example.pagingwithcompose

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.pagingwithcompose.db.Repo
import com.example.pagingwithcompose.ui.compose.SearchGitHubRepo
import com.example.pagingwithcompose.ui.theme.PagingWithComposeTheme
import dagger.hilt.android.AndroidEntryPoint


typealias OnExploreItemClicked = (Repo) -> Unit

fun launchDetailsActivity(context: Context, item: Repo) {
    context.startActivity(createDetailsActivityIntent(context, item))
}

@VisibleForTesting
fun createDetailsActivityIntent(context: Context, item: Repo): Intent {
    return Intent(Intent.ACTION_VIEW, Uri.parse(item.url))
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PagingWithComposeTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SearchGitHubRepo(
                        onExploreItemClicked = {
                            launchDetailsActivity(context = this, item = it)
                        }
                    )
                }
            }
        }
    }
}

