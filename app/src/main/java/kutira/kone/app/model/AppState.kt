package kutira.kone.app.model

import com.google.firebase.auth.FirebaseAuth

data class AppState(
    val loggedIn: Boolean = FirebaseAuth.getInstance().currentUser != null,
    val currentUserId: String = FirebaseAuth.getInstance().currentUser?.uid.orEmpty(),
    val currentUserEmail: String = FirebaseAuth.getInstance().currentUser?.email.orEmpty(),
    val scraps: List<Scrap> = emptyList(),
    val ideas: List<Idea> = emptyList(),
    val loading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isUploading: Boolean = false,
    val isDeleting: Boolean = false,
    val uploadSuccess: Boolean = false,
    val deleteSuccess: Boolean = false,
    val message: String = ""
)
