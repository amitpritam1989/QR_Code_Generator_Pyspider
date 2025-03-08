package com.example.qrcodegeneratorpyspider

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.qrcodegeneratorpyspider.ui.theme.QRCodeGeneratorPyspiderTheme
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.Bitmap as AndroidBitmap
import androidx.core.content.edit
import androidx.lifecycle.compose.LocalLifecycleOwner

// Enum representing the two screens.
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
    // Retrieve SharedPreferences for persisting the QR text.
    val sharedPref = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    var qrText by remember { mutableStateOf(sharedPref.getString("qrText", null)) }
    // Show the input screen if no QR text is saved; otherwise show the QR screen.
    var currentScreen by remember { mutableStateOf(if (qrText == null) Screen.Input else Screen.QR) }

    when (currentScreen) {
        Screen.Input -> {
            InputScreen(
                initialText = qrText ?: "",
                onSave = { newText ->
                    qrText = newText
                    sharedPref.edit() { putString("qrText", newText) }
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
    // State to force refresh when the app resumes.
    var refreshTrigger by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTrigger = System.currentTimeMillis()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    // Drawer state for the hamburger menu.
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showAboutDialog by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    // Format current date.
    val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    // Regenerate the QR code (the recomposition is forced by refreshTrigger state change).
    val qrBitmap = generateQRCode(qrText)

    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet {
                NavigationDrawerItem(
                    label = { Text("Change QR Text") },
                    selected = false,
                    onClick = { onChangeText() },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("About") },
                    selected = false,
                    onClick = { showAboutDialog = true },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        },
        drawerState = drawerState
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("QRC Generator PySpider") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { innerPadding ->
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
                    Spacer(modifier = Modifier.height(16.dp))
                    // Display the current date below the QR code.
                    Text(text = "Date: $currentDate", style = MaterialTheme.typography.bodyLarge)
                } else {
                    Text("Error generating QR Code")
                }
            }
        }
    }

    // About dialog showing clickable text.
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About") },
            text = {
                Column {
                    Text("Creator: Amit Pritam")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Instagram - @amit.pritam",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            uriHandler.openUri("https://www.instagram.com/amit.pritam/")
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Github - amitpritam1989",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            uriHandler.openUri("https://github.com/amitpritam1989")
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

fun generateQRCode(userText: String): ImageBitmap? {
    return try {
        // Format the current date as yyyy-MM-dd.
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        // Build the QR content string.
        val qrContent = "$userText/$currentDate/student"
        val barcodeEncoder = BarcodeEncoder()
        // Generate a 1080x1080 QR code bitmap.
        val bitmap: AndroidBitmap = barcodeEncoder.encodeBitmap(qrContent, BarcodeFormat.QR_CODE, 1080, 1080)
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
        QRScreen(qrText = "1224195", onChangeText = {})
    }
}
