package com.movieroulette.app.ui.screens.rating

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.movieroulette.app.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRatingScreen(
    navController: NavController,
    movieId: String
) {
    var rating by remember { mutableStateOf(0) }
    var comment by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(com.movieroulette.app.R.string.rate_movie)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, stringResource(com.movieroulette.app.R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Movie,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = stringResource(com.movieroulette.app.R.string.what_did_you_think),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Rating Stars
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in 1..10) {
                    IconButton(
                        onClick = { rating = i }
                    ) {
                        Icon(
                            if (i <= rating) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = "$i",
                            tint = if (i <= rating) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            }
                        )
                    }
                }
            }
            
            Text(
                text = if (rating > 0) stringResource(com.movieroulette.app.R.string.rating_out_of_ten, rating) else stringResource(com.movieroulette.app.R.string.select_score),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            
            AppTextField(
                value = comment,
                onValueChange = { comment = it },
                label = stringResource(com.movieroulette.app.R.string.comment_optional),
                placeholder = stringResource(com.movieroulette.app.R.string.comment_optional_placeholder),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            PrimaryButton(
                text = stringResource(com.movieroulette.app.R.string.save_rating),
                onClick = {
                    // Save rating logic here
                    navController.navigateUp()
                },
                enabled = rating > 0,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
