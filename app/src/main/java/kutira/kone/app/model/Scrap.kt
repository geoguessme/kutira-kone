package kutira.kone.app.model

import com.google.firebase.firestore.PropertyName

data class Scrap(
    var id: String = "",

    @get:PropertyName("image_url")
    @set:PropertyName("image_url")
    var imageUrl: String = "",

    @get:PropertyName("image_path")
    @set:PropertyName("image_path")
    var imagePath: String = "",

    @get:PropertyName("material_type")
    @set:PropertyName("material_type")
    var materialType: String = "",

    var size: String = "",

    @get:PropertyName("contact_info")
    @set:PropertyName("contact_info")
    var contactInfo: String = "",

    @get:PropertyName("user_id")
    @set:PropertyName("user_id")
    var userId: String = "",

    var latitude: Double = 0.0,
    var longitude: Double = 0.0
)
