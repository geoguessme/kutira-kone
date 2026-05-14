package kutira.kone.app.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kutira.kone.app.BuildConfig
import kutira.kone.app.model.AppState
import kutira.kone.app.model.Idea
import kutira.kone.app.model.Scrap
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class MainViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    init {
        if (auth.currentUser != null) {
            loadScraps()
            loadIdeas()
        }
    }

    fun signIn(email: String, password: String, context: Context) = viewModelScope.launch {
        runAuth(context) { auth.signInWithEmailAndPassword(email, password).await() }
    }

    fun signUp(email: String, password: String, context: Context) = viewModelScope.launch {
        runAuth(context) { auth.createUserWithEmailAndPassword(email, password).await() }
    }

    fun signInWithGoogle(idToken: String, context: Context) = viewModelScope.launch {
        runAuth(context) {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential).await()
        }
    }

    fun signOut() {
        auth.signOut()
        _state.value = AppState(loggedIn = false)
    }

    private suspend fun runAuth(context: Context, block: suspend () -> Unit) {
        _state.update { it.copy(loading = true, message = "") }
        try {
            block()
            _state.update {
                it.copy(
                    loggedIn = true,
                    currentUserId = auth.currentUser?.uid.orEmpty(),
                    currentUserEmail = auth.currentUser?.email.orEmpty(),
                    loading = false
                )
            }
            loadScraps()
            loadIdeas()
        } catch (e: Exception) {
            showError(context, e)
        }
    }

    fun loadScraps() = viewModelScope.launch {
        // Only show the global loading overlay on the very first load (empty list).
        // Subsequent refreshes happen silently to avoid blocking the UI.
        val isFirstLoad = _state.value.scraps.isEmpty()
        if (isFirstLoad) {
            _state.update { it.copy(loading = true, message = "") }
        }
        try {
            Log.d(TAG, "Loading scraps from Firestore (firstLoad=$isFirstLoad)...")
            val docs = firestore.collection("scraps").get().await()
            val items = docs.documents.map { doc ->
                Scrap(
                    id = doc.id,
                    imageUrl = doc.getString("image_url").orEmpty(),
                    imagePath = doc.getString("image_path").orEmpty(),
                    materialType = doc.getString("material_type").orEmpty(),
                    size = doc.getString("size").orEmpty(),
                    contactInfo = doc.getString("contact_info").orEmpty(),
                    userId = doc.getString("user_id").orEmpty(),
                    latitude = doc.getDouble("latitude") ?: 0.0,
                    longitude = doc.getDouble("longitude") ?: 0.0
                )
            }
            Log.d(TAG, "Loaded ${items.size} scraps successfully")
            _state.update {
                it.copy(
                    scraps = items,
                    currentUserId = auth.currentUser?.uid.orEmpty(),
                    currentUserEmail = auth.currentUser?.email.orEmpty(),
                    loading = false,
                    isRefreshing = false
                )
            }
        } catch (e: Exception) {
            _state.update { it.copy(loading = false, isRefreshing = false, message = e.message.orEmpty()) }
            Log.e(TAG, "Load scraps failed", e)
        }
    }

    /** Pull-to-refresh variant — sets isRefreshing flag for the indicator */
    fun refreshScraps() = viewModelScope.launch {
        _state.update { it.copy(isRefreshing = true, message = "") }
        try {
            Log.d(TAG, "Pull-to-refresh: reloading scraps...")
            val docs = firestore.collection("scraps").get().await()
            val items = docs.documents.map { doc ->
                Scrap(
                    id = doc.id,
                    imageUrl = doc.getString("image_url").orEmpty(),
                    imagePath = doc.getString("image_path").orEmpty(),
                    materialType = doc.getString("material_type").orEmpty(),
                    size = doc.getString("size").orEmpty(),
                    contactInfo = doc.getString("contact_info").orEmpty(),
                    userId = doc.getString("user_id").orEmpty(),
                    latitude = doc.getDouble("latitude") ?: 0.0,
                    longitude = doc.getDouble("longitude") ?: 0.0
                )
            }
            Log.d(TAG, "Refresh loaded ${items.size} scraps")
            _state.update {
                it.copy(
                    scraps = items,
                    currentUserId = auth.currentUser?.uid.orEmpty(),
                    currentUserEmail = auth.currentUser?.email.orEmpty(),
                    loading = false,
                    isRefreshing = false
                )
            }
        } catch (e: Exception) {
            _state.update { it.copy(loading = false, isRefreshing = false, message = e.message.orEmpty()) }
            Log.e(TAG, "Refresh scraps failed", e)
        }
    }

    fun loadIdeas() = viewModelScope.launch {
        try {
            val docs = firestore.collection("ideas").get().await()
            val items = docs.documents.map { doc ->
                Idea(
                    id = doc.id,
                    title = doc.getString("title").orEmpty(),
                    text = doc.getString("text").orEmpty(),
                    userId = doc.getString("user_id").orEmpty(),
                    timestamp = doc.getTimestamp("timestamp")?.toDate()?.time ?: 0L
                )
            }.sortedByDescending { it.timestamp }
            _state.update { it.copy(ideas = items) }
        } catch (e: Exception) {
            Log.e(TAG, "Ideas load failed", e)
        }
    }

    fun addIdea(context: Context, title: String, text: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, message = "") }
            try {
                firestore.collection("ideas").add(
                    mapOf(
                        "title" to title.trim(),
                        "text" to text.trim(),
                        "user_id" to userId,
                        "timestamp" to FieldValue.serverTimestamp()
                    )
                ).await()
                _state.update { it.copy(loading = false, message = "Idea posted") }
                loadIdeas()
            } catch (e: Exception) {
                showError(context, e)
            }
        }
    }

    fun uploadScrap(
        context: Context,
        photoUri: Uri,
        material: String,
        size: String,
        contactInfo: String,
        latitude: Double,
        longitude: Double
    ) {
        // Guard: prevent double-clicks from launching multiple uploads
        if (_state.value.isUploading) {
            Log.w(TAG, "Upload already in progress — ignoring duplicate request")
            return
        }

        val userId = auth.currentUser?.uid ?: run {
            Log.e(TAG, "Upload failed: user not authenticated")
            Toast.makeText(context, "You must be signed in to upload", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isUploading = true, message = "", uploadSuccess = false) }
            try {
                // Step 1: Upload image to Supabase
                Log.d(TAG, "Step 1: Uploading image to Supabase...")
                Log.d(TAG, "Supabase URL: $SUPABASE_URL, Bucket: $BUCKET")
                Log.d(TAG, "API key length: ${SUPABASE_KEY.length}, prefix: ${SUPABASE_KEY.take(10)}...")
                val upload = uploadToSupabase(context, photoUri)
                Log.d(TAG, "Step 1 complete: image uploaded → ${upload.url}")

                // Step 2: Save metadata to Firestore
                val data = mapOf(
                    "image_url" to upload.url,
                    "image_path" to upload.path,
                    "material_type" to material.trim(),
                    "size" to size.trim(),
                    "contact_info" to contactInfo.trim(),
                    "user_id" to userId,
                    "latitude" to latitude,
                    "longitude" to longitude
                )
                Log.d(TAG, "Step 2: Saving to Firestore: $data")
                val docRef = firestore.collection("scraps").add(data).await()
                Log.d(TAG, "Step 2 complete: Firestore document created → ${docRef.id}")

                // Step 3: Success — mark upload done FIRST, then refresh in background
                _state.update { it.copy(isUploading = false, uploadSuccess = true) }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Scrap listed successfully!", Toast.LENGTH_SHORT).show()
                }

                // Step 4: Refresh home data (silent — loadScraps no longer shows overlay)
                Log.d(TAG, "Step 4: Refreshing scraps list in background...")
                loadScraps()
            } catch (e: Exception) {
                Log.e(TAG, "Upload failed at some step", e)
                _state.update { it.copy(isUploading = false, uploadSuccess = false) }
                withContext(Dispatchers.Main) {
                    val msg = when {
                        e.message?.contains("401") == true || e.message?.contains("403") == true ->
                            "Upload failed: Supabase authentication error. Check API key."
                        e.message?.contains("timeout", ignoreCase = true) == true ->
                            "Upload timed out. Check your internet connection and try again."
                        else -> "Upload failed: ${e.message}"
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /** Clear uploadSuccess flag after it has been consumed by the UI */
    fun consumeUploadSuccess() {
        _state.update { it.copy(uploadSuccess = false) }
    }

    fun updateScrap(context: Context, scrap: Scrap, material: String, size: String) {
        if (!owns(scrap)) {
            Toast.makeText(context, "You can edit only your own listing", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, message = "") }
            try {
                firestore.collection("scraps").document(scrap.id)
                    .update(
                        mapOf(
                            "material_type" to material.trim(),
                            "size" to size.trim()
                        )
                    ).await()
                _state.update { it.copy(loading = false, message = "Scrap updated") }
                loadScraps()
            } catch (e: Exception) {
                showError(context, e)
            }
        }
    }

    fun deleteScrap(context: Context, scrap: Scrap) {
        if (!owns(scrap)) {
            Toast.makeText(context, "You can delete only your own listing", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true, deleteSuccess = false) }
            Log.d(TAG, "Delete start: id=${scrap.id} imagePath=${scrap.imagePath}")
            try {
                // Step 1: Delete image from Supabase (non-critical — continue even if it fails)
                val imagePath = scrap.imagePath.ifBlank { pathFromUrl(scrap.imageUrl) }
                try {
                    deleteFromSupabase(imagePath)
                    Log.d(TAG, "Delete: Supabase image removed ($imagePath)")
                } catch (e: Exception) {
                    // Image delete failure should NOT block Firestore delete
                    Log.w(TAG, "Delete: Supabase image delete failed (continuing): ${e.message}")
                }

                // Step 2: Delete metadata from Firestore (critical)
                Log.d(TAG, "Delete: removing Firestore doc ${scrap.id}")
                firestore.collection("scraps").document(scrap.id).delete().await()
                Log.d(TAG, "Delete: Firestore doc removed")

                // Step 3: Optimistically remove from local state
                _state.update { current ->
                    current.copy(
                        scraps = current.scraps.filter { it.id != scrap.id },
                        isDeleting = false,
                        deleteSuccess = true
                    )
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Listing deleted", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Delete failed for ${scrap.id}", e)
                _state.update { it.copy(isDeleting = false, deleteSuccess = false) }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Delete failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun consumeDeleteSuccess() {
        _state.update { it.copy(deleteSuccess = false) }
    }

    private fun owns(scrap: Scrap): Boolean = scrap.userId == auth.currentUser?.uid

    private suspend fun uploadToSupabase(context: Context, uri: Uri): UploadResult = withContext(Dispatchers.IO) {
        val path = "scraps/${UUID.randomUUID()}.jpg"

        // Step 1: Read image bytes and validate
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Could not read photo — please try a different image")

        val sizeMb = bytes.size / (1024.0 * 1024.0)
        Log.d(TAG, "Upload: ${bytes.size} bytes (%.2f MB) → bucket=$BUCKET path=$path".format(sizeMb))

        if (bytes.isEmpty()) error("Selected image is empty")
        if (bytes.size > 10 * 1024 * 1024) error("Image too large (%.1f MB). Max is 10 MB.".format(sizeMb))

        // Step 2: Upload with retry (max 3 attempts, exponential backoff)
        val maxAttempts = 3
        var lastException: Exception? = null

        for (attempt in 1..maxAttempts) {
            try {
                if (attempt > 1) {
                    val delayMs = 1000L * (1 shl (attempt - 2)) // 1s, 2s
                    Log.d(TAG, "Upload retry $attempt/$maxAttempts after ${delayMs}ms...")
                    kotlinx.coroutines.delay(delayMs)
                }

                val uploadUrl = "$SUPABASE_URL/storage/v1/object/$BUCKET/$path"
                val connection = URL(uploadUrl).openConnection() as HttpURLConnection
                try {
                    connection.connectTimeout = 60_000
                    connection.readTimeout = 60_000
                    connection.requestMethod = "POST"
                    connection.doOutput = true
                    connection.setFixedLengthStreamingMode(bytes.size)
                    connection.setRequestProperty("apikey", SUPABASE_KEY)
                    connection.setRequestProperty("Authorization", "Bearer $SUPABASE_KEY")
                    connection.setRequestProperty("Content-Type", "image/jpeg")
                    connection.setRequestProperty("Content-Length", bytes.size.toString())
                    connection.setRequestProperty("x-upsert", "true")

                    connection.outputStream.use { it.write(bytes) }

                    val responseCode = connection.responseCode
                    if (responseCode !in 200..299) {
                        val error = connection.errorStream?.bufferedReader()?.readText().orEmpty()
                        Log.e(TAG, "Upload HTTP $responseCode (attempt $attempt): $error")

                        // 401/403 = auth issue (bad API key) — retry won't help
                        if (responseCode == 401 || responseCode == 403) {
                            throw IllegalStateException(
                                "Supabase auth failed (HTTP $responseCode). " +
                                "The API key may be invalid or expired. " +
                                "Key prefix: ${SUPABASE_KEY.take(10)}..."
                            )
                        }

                        // 5xx = server error — worth retrying
                        if (responseCode in 500..599) {
                            throw java.io.IOException("Server error (HTTP $responseCode): $error")
                        }

                        throw IllegalStateException("Image upload failed (HTTP $responseCode): $error")
                    }

                    val publicUrl = "$SUPABASE_URL/storage/v1/object/public/$BUCKET/$path"
                    Log.d(TAG, "Upload success on attempt $attempt: $publicUrl")
                    return@withContext UploadResult(publicUrl, path)
                } finally {
                    connection.disconnect()
                }
            } catch (e: java.net.SocketTimeoutException) {
                Log.w(TAG, "Upload attempt $attempt/$maxAttempts timed out: ${e.message}")
                lastException = e
                // Continue to retry
            } catch (e: java.io.IOException) {
                Log.w(TAG, "Upload attempt $attempt/$maxAttempts network error: ${e.message}")
                lastException = e
                // Continue to retry
            } catch (e: Exception) {
                // Non-network errors (HTTP 4xx/5xx, etc.) — don't retry
                Log.e(TAG, "Upload attempt $attempt failed (non-retryable): ${e.message}")
                throw e
            }
        }

        // All retries exhausted
        Log.e(TAG, "Upload failed after $maxAttempts attempts", lastException)
        throw lastException ?: IllegalStateException("Upload failed after $maxAttempts attempts")
    }

    private suspend fun deleteFromSupabase(path: String) = withContext(Dispatchers.IO) {
        if (path.isBlank()) {
            Log.d(TAG, "Supabase delete skipped: empty path")
            return@withContext
        }

        val maxAttempts = 3
        var lastException: Exception? = null
        val deleteUrl = "$SUPABASE_URL/storage/v1/object/$BUCKET/$path"

        for (attempt in 1..maxAttempts) {
            try {
                if (attempt > 1) {
                    val delayMs = 1000L * (1 shl (attempt - 2))
                    Log.d(TAG, "Supabase delete retry $attempt/$maxAttempts after ${delayMs}ms...")
                    kotlinx.coroutines.delay(delayMs)
                }

                Log.d(TAG, "Supabase delete attempt $attempt: $deleteUrl")
                val connection = URL(deleteUrl).openConnection() as HttpURLConnection
                try {
                    connection.connectTimeout = 60_000
                    connection.readTimeout = 60_000
                    connection.requestMethod = "DELETE"
                    connection.setRequestProperty("apikey", SUPABASE_KEY)
                    connection.setRequestProperty("Authorization", "Bearer $SUPABASE_KEY")

                    val responseCode = connection.responseCode
                    if (responseCode in 200..299 || responseCode == 404) {
                        // 404 = already gone, treat as success
                        Log.d(TAG, "Supabase delete success (HTTP $responseCode) on attempt $attempt")
                        return@withContext
                    } else {
                        val error = connection.errorStream?.bufferedReader()?.readText().orEmpty()
                        Log.w(TAG, "Supabase delete HTTP $responseCode (attempt $attempt): $error")
                        throw IllegalStateException("Supabase delete failed (HTTP $responseCode)")
                    }
                } finally {
                    connection.disconnect()
                }
            } catch (e: java.net.SocketTimeoutException) {
                Log.w(TAG, "Supabase delete attempt $attempt timed out: ${e.message}")
                lastException = e
            } catch (e: java.io.IOException) {
                Log.w(TAG, "Supabase delete attempt $attempt network error: ${e.message}")
                lastException = e
            } catch (e: Exception) {
                Log.e(TAG, "Supabase delete attempt $attempt non-retryable: ${e.message}")
                throw e
            }
        }

        Log.e(TAG, "Supabase delete failed after $maxAttempts attempts", lastException)
        throw lastException ?: IllegalStateException("Supabase delete failed after $maxAttempts attempts")
    }

    private fun pathFromUrl(url: String): String {
        val marker = "/storage/v1/object/public/$BUCKET/"
        return url.substringAfter(marker, "")
    }

    private fun showError(context: Context, e: Exception) {
        val message = e.message ?: "Something went wrong"
        _state.update { it.copy(loading = false, message = message) }
        Log.e(TAG, message, e)
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private data class UploadResult(val url: String, val path: String)

    private companion object {
        const val TAG = "KutiraKone"
        val SUPABASE_URL = BuildConfig.SUPABASE_URL
        val SUPABASE_KEY = BuildConfig.SUPABASE_KEY
        val BUCKET = BuildConfig.SUPABASE_BUCKET
    }
}
