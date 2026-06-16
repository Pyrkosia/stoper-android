package com.example.stoperimpulsow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket

data class WynikImpulsu(val numer: Int, val czas: String, val czyBlad: Boolean)

class UdpOdbiornik {
    private var socket: DatagramSocket? = null
    suspend fun nasluchujPaczek(onDaneOdebrane: (List<WynikImpulsu>) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                socket = DatagramSocket(8888)
                val bufor = ByteArray(1024)
                while (true) {
                    val pakiet = DatagramPacket(bufor, bufor.size)
                    socket?.receive(pakiet)
                    val odebranyTekst = String(pakiet.data, 0, pakiet.length).trim()
                    if (odebranyTekst != "BRAK_IMPULSOW" && odebranyTekst.isNotEmpty()) {
                        val listaWynikow = przetworzDane(odebranyTekst)
                        withContext(Dispatchers.Main) { onDaneOdebrane(listaWynikow) }
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
    private fun przetworzDane(tekst: String): List<WynikImpulSu> {
        return tekst.split(";").mapIndexed { index, element ->
            val czesci = element.split(",")
            WynikImpulsu(index + 1, "${czesci.getOrNull(0) ?: "0.000"} s", czesci.getOrNull(1) == "1")
        }
    }
}

class MainActivity : ComponentActivity() {
    private val odbiornik = UdpOdbiornik()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState: Bundle?)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    EkranPomiarowy(odbiornik)
                }
            }
        }
    }
}

@Composable
fun EkranPomiarowy(odbiornik: UdpOdbiornik) {
    var listaImpulsow by remember { mutableStateOf<List<WynikImpulsu>>(emptyList()) }
    LaunchedEffect(Unit) {
        odbiornik.nasluchujPaczek { listaImpulsow = it }
    }
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Wyniki Pomiaru", fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 24.dp))
        if (listaImpulsow.isEmpty()) {
            Text("Oczekiwanie na sygnał z ESP32...", color = Color.Gray, fontSize = 16.sp)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(listaImpulsow) { impuls ->
                    val kolorTekstu = if (impuls.czyBlad) Color.Red else Color(0xFF1B5E20)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Impuls ${impuls.numer}:", fontSize = 20.sp)
                        Text(impuls.czas + if (impuls.czyBlad) " (SPÓŹNIONY)" else "", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = kolorTekstu)
                    }
                }
            }
        }
    }
}
