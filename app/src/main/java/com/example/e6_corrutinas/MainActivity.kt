package com.example.e6_corrutinas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.e6_corrutinas.ui.theme.E6_corrutinasTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            E6_corrutinasTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PantallaContadores(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

/**
 * Subrutina anidada: recorre segundo a segundo hasta [segundos].
 * Vive dentro de una función suspend, así que delay() es el punto de
 * cancelación cooperativa: si el Job se cancela, delay() lanza
 * CancellationException y la cadena de llamadas se detiene aquí mismo.
 */
private suspend fun contarSegundos(segundos: Int, actualizar: (Int) -> Unit) {
    for (segundoActual in 1..segundos) {
        delay(1000L)
        actualizar(segundoActual)
    }
}

/** Contador 1: reinicia en 0 y delega el conteo a la subrutina anidada. */
private suspend fun contador1(segundos: Int, actualizar: (Int) -> Unit) {
    actualizar(0)
    contarSegundos(segundos, actualizar)
}

/** Contador 2: misma lógica que el contador 1, pero se lanza después. */
private suspend fun contador2(segundos: Int, actualizar: (Int) -> Unit) {
    actualizar(0)
    contarSegundos(segundos, actualizar)
}

/**
 * Tarea síncrona: contador2() está después de contador1() dentro del
 * mismo flujo suspend, así que el segundo arranca automáticamente
 * apenas termina el primero, sin hilos ni callbacks extra.
 */
private suspend fun ejecutarContadoresSincronizados(
    actualizarContador1: (Int) -> Unit,
    actualizarContador2: (Int) -> Unit,
    actualizarEstado: (String) -> Unit
) {
    actualizarEstado("Ejecutando contador 1...")
    contador1(10, actualizarContador1)

    actualizarEstado("Ejecutando contador 2...")
    contador2(10, actualizarContador2)

    actualizarEstado("Finalizado")
}

@Composable
fun PantallaContadores(modifier: Modifier = Modifier) {
    var valorContador1 by remember { mutableStateOf(0) }
    var valorContador2 by remember { mutableStateOf(0) }
    var estado by remember { mutableStateOf("Esperando para iniciar") }
    var jobActual by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Contador 1: $valorContador1 s", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Contador 2: $valorContador2 s", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Estado: $estado")
        Spacer(modifier = Modifier.height(24.dp))

        Row {
            Button(onClick = {
                jobActual?.cancel()
                valorContador1 = 0
                valorContador2 = 0
                jobActual = scope.launch {
                    ejecutarContadoresSincronizados(
                        actualizarContador1 = { valorContador1 = it },
                        actualizarContador2 = { valorContador2 = it },
                        actualizarEstado = { estado = it }
                    )
                }
            }) {
                Text("Iniciar")
            }

            Spacer(modifier = Modifier.width(12.dp))

            Button(onClick = {
                jobActual?.cancel()
                estado = "Cancelado"
            }) {
                Text("Cancelar")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PantallaContadoresPreview() {
    E6_corrutinasTheme {
        PantallaContadores()
    }
}
