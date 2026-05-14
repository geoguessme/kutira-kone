package kutira.kone.app.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kutira.kone.app.components.LineGray
import kutira.kone.app.components.MaterialDropdown
import kutira.kone.app.components.PrimaryButton
import kutira.kone.app.components.RoundedInput
import kutira.kone.app.components.SmallLabel
import kutira.kone.app.components.UploadButton
import kutira.kone.app.model.AppState

@Composable
fun UploadScreen(
    state: AppState,
    selectedLocation: Pair<Double, Double>?,
    material: String,
    onMaterialChange: (String) -> Unit,
    size: String,
    onSizeChange: (String) -> Unit,
    contactInfo: String,
    onContactInfoChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    photoUri: Uri?,
    onPhotoUriChange: (Uri?) -> Unit,
    onSelectLocation: () -> Unit,
    onUpload: (Context, Uri, String, String, String, Double, Double) -> Unit
) {
    val context = LocalContext.current
    var message by remember { mutableStateOf("") }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { onPhotoUriChange(it) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(26.dp)
    ) {
        Text("Upload Scrap", fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Text(
            "Give your leftover fabrics a second life. List them on the Kone marketplace.",
            color = Color(0xFF556057),
            fontSize = 19.sp,
            lineHeight = 28.sp,
            modifier = Modifier.padding(top = 10.dp, bottom = 34.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(330.dp)
                .clip(RoundedCornerShape(22.dp))
                .border(2.dp, Color(0xFFD2C1C1), RoundedCornerShape(22.dp))
                .clickable { photoPicker.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            photoUri?.let {
                AsyncImage(
                    model = it,
                    contentDescription = "Selected scrap photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } ?: Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, LineGray, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                Text("Add Photo", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(30.dp))
        SmallLabel("Material")
        MaterialDropdown(material, { onMaterialChange(it) })
        Spacer(Modifier.height(18.dp))
        SmallLabel("Size (meters)")
        RoundedInput(size, { onSizeChange(it) }, "e.g. 1.5 x 2.0")
        Spacer(Modifier.height(18.dp))
        SmallLabel("Contact Info")
        RoundedInput(contactInfo, { onContactInfoChange(it) }, "Phone or pickup note")
        Spacer(Modifier.height(18.dp))
        SmallLabel("Description")
        RoundedInput(description, { onDescriptionChange(it) }, "Describe this scrap...", singleLine = false)
        Spacer(Modifier.height(18.dp))
        SmallLabel("Location")
        PrimaryButton("Select Location", onClick = onSelectLocation)
        if (selectedLocation != null) {
            Text(
                "Location Selected",
                color = Color(0xFF0F7A35),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 10.dp, start = 6.dp)
            )
        }
        Spacer(Modifier.height(26.dp))
        UploadButton(
            isUploading = state.isUploading,
            onClick = {
                val uri = photoUri
                val loc = selectedLocation
                if (uri == null) message = "Photo is required"
                else if (material.isBlank() || size.isBlank() || contactInfo.isBlank()) message = "Material, size and contact info are required"
                else if (loc == null) message = "Select a location"
                else {
                    message = ""
                    onUpload(context, uri, material.lowercase(), size, contactInfo, loc.first, loc.second)
                }
            }
        )
        if (message.isNotBlank()) Text(message, color = Color.Red, modifier = Modifier.padding(top = 12.dp))
    }
}
