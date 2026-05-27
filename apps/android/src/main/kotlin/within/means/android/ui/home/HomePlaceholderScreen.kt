package within.means.android.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import within.means.shared.domain.bus.query.QueryBus
import within.means.users.application.OptionalUserResponse
import within.means.users.application.find.FindDefaultUserQuery

@Composable
fun HomePlaceholderScreen(
    onOpenCategories: () -> Unit,
    onOpenTransactions: () -> Unit,
    onOpenStats: () -> Unit,
) {
    val queryBus: QueryBus = koinInject()
    var displayName by remember { mutableStateOf("...") }
    var baseCurrency by remember { mutableStateOf("...") }

    LaunchedEffect(Unit) {
        val response: OptionalUserResponse = queryBus.ask(FindDefaultUserQuery())
        val user = response.user
        if (user != null) {
            displayName = user.displayName
            baseCurrency = user.baseCurrency
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Hola, $displayName", style = MaterialTheme.typography.headlineLarge)
            Text("Moneda base: $baseCurrency", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(24.dp))

            Button(onClick = onOpenTransactions, modifier = Modifier.fillMaxWidth()) {
                Text("Transacciones")
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = onOpenStats, modifier = Modifier.fillMaxWidth()) {
                Text("Estadísticas")
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = onOpenCategories, modifier = Modifier.fillMaxWidth()) {
                Text("Categorías")
            }
        }
    }
}
