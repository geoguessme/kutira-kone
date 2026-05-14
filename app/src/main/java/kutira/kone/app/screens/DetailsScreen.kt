package kutira.kone.app.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import kutira.kone.app.components.MutedText
import kutira.kone.app.components.PrimaryButton
import kutira.kone.app.components.RoundedInput
import kutira.kone.app.components.SmallLabel
import kutira.kone.app.model.Scrap
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun DetailsScreen(
    scrap: Scrap,
    currentUserId: String,
    context: Context,
    isDeleting: Boolean,
    onUpdate: (Context, Scrap, String, String) -> Unit,
    onDelete: (Context, Scrap) -> Unit
) {
    var material by remember(scrap.id) { mutableStateOf(scrap.materialType) }
    var size by remember(scrap.id) { mutableStateOf(scrap.size) }
    val ownedByUser = scrap.userId == currentUserId

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(22.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(330.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFFEFEFEF))
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(scrap.imageUrl)
                    .crossfade(300)
                    .build(),
                contentDescription = scrap.materialType,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = MutedText,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                error = {
                    Box(Modifier.fillMaxSize().background(Color(0xFFEFEFEF)), contentAlignment = Alignment.Center) {
                        Text("Image not available", color = MutedText)
                    }
                }
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(scrap.materialType.ifBlank { "Fabric Scrap" }, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text("${scrap.size}  •  ${scrap.latitude}, ${scrap.longitude}", color = Color(0xFF555555), modifier = Modifier.padding(top = 8.dp))
        if (scrap.contactInfo.isNotBlank()) {
            Text("Contact: ${scrap.contactInfo}", color = Color.Black, fontSize = 18.sp, modifier = Modifier.padding(top = 10.dp))
        }
        Spacer(Modifier.height(28.dp))
        Text("THE DETAIL", letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
        Text(
            "A leftover fabric scrap listed by a local maker. Contact or swap outside the app flow as needed.",
            fontSize = 18.sp,
            lineHeight = 28.sp,
            modifier = Modifier.padding(top = 12.dp)
        )
        Spacer(Modifier.height(24.dp))
        Text("LOCATION", letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        ListingMapPreview(scrap)

        if (ownedByUser) {
            Spacer(Modifier.height(30.dp))
            SmallLabel("Edit Material")
            RoundedInput(material, { material = it }, "Material")
            Spacer(Modifier.height(16.dp))
            SmallLabel("Edit Size")
            RoundedInput(size, { size = it }, "Size")
            Spacer(Modifier.height(22.dp))
            PrimaryButton("Save Changes") {
                onUpdate(context, scrap, material, size)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { onDelete(context, scrap) },
                enabled = !isDeleting,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        color = Color.Black,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text("Delete Listing")
                }
            }
        }
    }
}

@Composable
private fun ListingMapPreview(scrap: Scrap) {
    val point = GeoPoint(scrap.latitude, scrap.longitude)
    var mapView: MapView? by remember { mutableStateOf(null) }

    DisposableEffect(Unit) {
        onDispose {
            mapView?.onPause()
            mapView?.onDetach()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFEFEFEF))
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                Configuration.getInstance().userAgentValue = ctx.packageName
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(false)
                    controller.setZoom(14.0)
                    controller.setCenter(point)
                    overlays.add(
                        Marker(this).apply {
                            position = point
                            title = scrap.materialType.ifBlank { "Fabric scrap" }
                            subDescription = scrap.size
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        }
                    )
                    onResume()
                    mapView = this
                }
            },
            update = { map ->
                map.controller.setCenter(point)
                map.invalidate()
            }
        )
    }
}
