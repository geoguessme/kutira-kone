package kutira.kone.app

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import kutira.kone.app.components.BottomNav
import kutira.kone.app.components.TopBar
import kutira.kone.app.model.Scrap
import kutira.kone.app.screens.DetailsScreen
import kutira.kone.app.screens.HomeScreen
import kutira.kone.app.screens.IdeasScreen
import kutira.kone.app.screens.LocationPickerScreen
import kutira.kone.app.screens.LoginScreen
import kutira.kone.app.screens.MapScreen
import kutira.kone.app.screens.ProfileScreen
import kutira.kone.app.screens.UploadScreen
import kutira.kone.app.ui.theme.KutiraKoneTheme
import kutira.kone.app.viewmodel.MainViewModel
import org.osmdroid.util.GeoPoint

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeFirebase()
        setContent {
            KutiraKoneTheme {
                KutiraKoneApp()
            }
        }
    }

    private fun initializeFirebase() {
        if (FirebaseApp.getApps(this).isNotEmpty()) return

        val options = FirebaseOptions.Builder()
            .setApiKey(BuildConfig.FIREBASE_API_KEY)
            .setApplicationId(BuildConfig.FIREBASE_APPLICATION_ID)
            .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
            .setStorageBucket(BuildConfig.FIREBASE_STORAGE_BUCKET)
            .setGcmSenderId(BuildConfig.FIREBASE_GCM_SENDER_ID)
            .build()

        FirebaseApp.initializeApp(this, options)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KutiraKoneApp(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var screen by remember { mutableStateOf("home") }
    var selectedScrap by remember { mutableStateOf<Scrap?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedUploadLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    // Hoisted upload form state — persists across navigation to location picker
    var uploadMaterial by remember { mutableStateOf("") }
    var uploadSize by remember { mutableStateOf("") }
    var uploadContactInfo by remember { mutableStateOf("") }
    var uploadDescription by remember { mutableStateOf("") }
    var uploadPhotoUri by remember { mutableStateOf<Uri?>(null) }

    // Watch for upload success → clear form and go to home
    LaunchedEffect(state.uploadSuccess) {
        if (state.uploadSuccess) {
            uploadMaterial = ""
            uploadSize = ""
            uploadContactInfo = ""
            uploadDescription = ""
            uploadPhotoUri = null
            selectedUploadLocation = null
            screen = "home"
            viewModel.consumeUploadSuccess()
        }
    }

    // Watch for delete success → navigate to home
    LaunchedEffect(state.deleteSuccess) {
        if (state.deleteSuccess) {
            selectedScrap = null
            screen = "home"
            viewModel.consumeDeleteSuccess()
        }
    }

    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data).result
            val token = account.idToken
            if (token.isNullOrBlank()) {
                Toast.makeText(context, "Missing Google token. Check Firebase web client ID.", Toast.LENGTH_LONG).show()
            } else {
                viewModel.signInWithGoogle(token, context)
            }
        } catch (e: Exception) {
            Toast.makeText(context, e.message ?: "Google sign-in failed", Toast.LENGTH_SHORT).show()
        }
    }

    if (!state.loggedIn) {
        LoginScreen(
            state = state,
            context = context,
            onLogin = viewModel::signIn,
            onSignup = viewModel::signUp,
            onGoogleClick = {
                val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(context.getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()
                googleLauncher.launch(GoogleSignIn.getClient(context, options).signInIntent)
            }
        )
        if (state.loading) LoadingOverlay()
        return
    }

    Scaffold(
        topBar = {
            when (screen) {
                "details" -> {
                    TopAppBar(
                        title = { Text("Details", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = { screen = "home" }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.White,
                            titleContentColor = Color.Black
                        )
                    )
                }
                "selectLocation" -> {
                    TopAppBar(
                        title = { Text("Select Location", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = { screen = "upload" }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.White,
                            titleContentColor = Color.Black
                        )
                    )
                }
                else -> {
                    TopBar(
                        currentScreen = screen,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onProfileClick = { screen = "profile" }
                    )
                }
            }
        },
        bottomBar = {
            if (screen != "details" && screen != "profile" && screen != "selectLocation") {
                BottomNav(screen) { screen = it }
            }
        },
        containerColor = Color.White
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (screen) {
                "home" -> HomeScreen(
                    state = state,
                    searchQuery = searchQuery,
                    onRefresh = viewModel::loadScraps,
                    onPullRefresh = viewModel::refreshScraps,
                    onOpenDetails = {
                        selectedScrap = it
                        screen = "details"
                    }
                )
                "map" -> MapScreen(state.scraps)
                "upload" -> UploadScreen(
                    state = state,
                    selectedLocation = selectedUploadLocation,
                    material = uploadMaterial,
                    onMaterialChange = { uploadMaterial = it },
                    size = uploadSize,
                    onSizeChange = { uploadSize = it },
                    contactInfo = uploadContactInfo,
                    onContactInfoChange = { uploadContactInfo = it },
                    description = uploadDescription,
                    onDescriptionChange = { uploadDescription = it },
                    photoUri = uploadPhotoUri,
                    onPhotoUriChange = { uploadPhotoUri = it },
                    onSelectLocation = { screen = "selectLocation" },
                    onUpload = { ctx, uri, mat, sz, ci, lat, lng ->
                        viewModel.uploadScrap(ctx, uri, mat, sz, ci, lat, lng)
                        // Form is cleared via LaunchedEffect when uploadSuccess becomes true
                    }
                )
                "ideas" -> IdeasScreen(
                    state = state,
                    context = context,
                    onLoadIdeas = viewModel::loadIdeas,
                    onAddIdea = viewModel::addIdea
                )
                "profile" -> ProfileScreen(
                    state = state,
                    onBack = { screen = "home" },
                    onLogout = {
                        viewModel.signOut()
                        screen = "home"
                    },
                    onOpenDetails = {
                        selectedScrap = it
                        screen = "details"
                    }
                )
                "details" -> selectedScrap?.let {
                    DetailsScreen(
                        scrap = it,
                        currentUserId = state.currentUserId,
                        context = context,
                        isDeleting = state.isDeleting,
                        onUpdate = viewModel::updateScrap,
                        onDelete = { ctx, scrap ->
                            viewModel.deleteScrap(ctx, scrap)
                            // Navigation happens via LaunchedEffect on deleteSuccess
                        }
                    )
                }
                "selectLocation" -> LocationPickerScreen(
                    initialLocation = selectedUploadLocation?.let { GeoPoint(it.first, it.second) },
                    onConfirmLocation = { latitude, longitude ->
                        selectedUploadLocation = latitude to longitude
                        screen = "upload"
                    }
                )
            }
            if (state.loading) LoadingOverlay()
            if (state.message.isNotBlank()) {
                Text(
                    state.message,
                    color = Color.Black,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 18.dp)
                )
            }
        }
    }
}

@Composable
private fun LoadingOverlay() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color.Black)
    }
}
