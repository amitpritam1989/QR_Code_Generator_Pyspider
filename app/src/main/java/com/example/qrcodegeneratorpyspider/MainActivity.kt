package com.example.qrcodegeneratorpyspider

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.qrcodegeneratorpyspider.ui.theme.QRCodeGeneratorPyspiderTheme
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.Bitmap as AndroidBitmap

// Enum to represent which screen to show.
enum class Screen {
    Input, QR
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QRCodeGeneratorPyspiderTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    val context = LocalContext.current
    // Use SharedPreferences to persist the QR text.
    val sharedPref = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    // Initially, try to read the saved text. If none is found, it will be null.
    var qrText by remember { mutableStateOf(sharedPref.getString("qrText", null)) }
    // Determine the current screen: if no text, show the Input screen; otherwise show the QR screen.
    var currentScreen by remember { mutableStateOf(if (qrText == null) Screen.Input else Screen.QR) }

    when (currentScreen) {
        Screen.Input -> {
            InputScreen(
                initialText = qrText ?: "",
                onSave = { newText ->
                    qrText = newText
                    sharedPref.edit().putString("qrText", newText).apply()
                    currentScreen = Screen.QR
                }
            )
        }
        Screen.QR -> {
            QRScreen(
                qrText = qrText ?: "",
                onChangeText = { currentScreen = Screen.Input }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputScreen(initialText: String, onSave: (String) -> Unit) {
    var inputText by remember { mutableStateOf(initialText) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Enter QR Text") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = { Text("QR Code Text") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (inputText.isNotBlank()) {
                        onSave(inputText)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRScreen(qrText: String, onChangeText: () -> Unit) {
    // Use a drawer state for the hamburger menu.
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet {
                // Add a drawer item to allow changing the QR text.
                NavigationDrawerItem(
                    label = { Text("Change QR Text") },
                    selected = false,
                    onClick = { onChangeText() },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        },
        drawerState = drawerState
    )



    {

        Scaffold(
            topBar = {

                TopAppBar(
                    title = { Text("Amit Pritam Pati") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { innerPadding ->
            // Generate the QR code based on the stored text and current date.
            val qrBitmap = generateQRCode(qrText)
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap,
                        contentDescription = "QR Code",
                        modifier = Modifier.size(300.dp)
                    )
                } else {
                    Text("Error generating QR Code")
                }
            }
        }
    }
}

fun generateQRCode(userText: String): ImageBitmap? {
    return try {
        // Format the current date as yyyy-MM-dd.
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        // Create the text to encode in the QR code.
        val qrText = "$userText/$currentDate/student"
        val barcodeEncoder = BarcodeEncoder()
        // Generate a 1080x1080 QR code bitmap.
        val bitmap: AndroidBitmap = barcodeEncoder.encodeBitmap(qrText, BarcodeFormat.QR_CODE, 1080, 1080)
        bitmap.asImageBitmap()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    QRCodeGeneratorPyspiderTheme {
        // For preview, we simulate the QR screen with a sample text.
        QRScreen(qrText = "1224195", onChangeText = {})
    }
}
