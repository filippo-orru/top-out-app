package com.filippoorru.topout.ui.screens


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.filippoorru.topout.database.Database
import com.filippoorru.topout.ui.Center
import com.filippoorru.topout.ui.Routes

@Composable
fun ViewRouteVisitScreen(navController: NavHostController, routeVisitId: String) {
    val routeVisit by Database.i.routeVisits().get(routeVisitId).collectAsState(initial = null)
    val visit = routeVisit

    val attempts by Database.i.attempts().getByRouteVisit(routeVisitId).collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopOutAppBar(
                navigateBack = { navController.popBackStack() },
            )
        },
    ) { padding ->
        Box(
            Modifier.padding(padding),
        ) {
            if (visit == null) {
                Center {
                    Text("Visit not found")
                }
            } else {
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    attempts.forEachIndexed { index, attempt ->
                        Card(onClick = {
                            navController.navigate(Routes.Cut.build(routeVisitId, attempt.id))
                        }) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    Modifier
                                        .aspectRatio(1.0f)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Cut")
                                }
                                Text("Attempt ${index + 1}")
                            }
                        }
                    }
                }
            }
        }
    }
}