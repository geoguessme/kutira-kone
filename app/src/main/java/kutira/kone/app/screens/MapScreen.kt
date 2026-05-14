package kutira.kone.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kutira.kone.app.model.Scrap
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

@Composable
fun MapScreen(scraps: List<Scrap>) {
    val context = LocalContext.current
    var mapView by remember { mutableStateOf<MapView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            mapView?.onPause()
            mapView?.onDetach()
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                Configuration.getInstance().userAgentValue = ctx.packageName
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    minZoomLevel = 3.0
                    maxZoomLevel = 19.0
                    controller.setZoom(12.0)
                    controller.setCenter(defaultCenter(scraps))
                    onResume()
                    mapView = this
                }
            },
            update = { map ->
                map.overlays.clear()
                scraps
                    .filter { it.latitude != 0.0 || it.longitude != 0.0 }
                    .forEach { scrap ->
                        map.overlays.add(
                            Marker(map).apply {
                                position = GeoPoint(scrap.latitude, scrap.longitude)
                                title = scrap.materialType.ifBlank { "Fabric scrap" }
                                subDescription = scrap.size
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                setOnMarkerClickListener { clicked, mapView ->
                                    clicked.showInfoWindow()
                                    mapView.controller.animateTo(clicked.position)
                                    true
                                }
                            }
                        )
                    }
                map.invalidate()
            }
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(18.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White)
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Text("Available Scraps")
            Text("${scraps.size} scraps found", color = Color(0xFF777777))
        }
    }
}

@Composable
fun LocationPickerScreen(
    initialLocation: GeoPoint?,
    onConfirmLocation: (Double, Double) -> Unit
) {
    var selectedPoint by remember { mutableStateOf(initialLocation) }
    var mapView by remember { mutableStateOf<MapView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            mapView?.onPause()
            mapView?.onDetach()
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                Configuration.getInstance().userAgentValue = ctx.packageName
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    minZoomLevel = 3.0
                    maxZoomLevel = 19.0
                    controller.setZoom(12.0)
                    controller.setCenter(initialLocation ?: GeoPoint(19.0760, 72.8777))
                    overlays.add(
                        MapEventsOverlay(
                            object : MapEventsReceiver {
                                override fun singleTapConfirmedHelper(point: GeoPoint): Boolean {
                                    selectedPoint = point
                                    controller.animateTo(point)
                                    return true
                                }

                                override fun longPressHelper(point: GeoPoint): Boolean = false
                            }
                        )
                    )
                    onResume()
                    mapView = this
                }
            },
            update = { map ->
                map.overlays.removeAll { it is Marker }
                selectedPoint?.let { point ->
                    map.overlays.add(
                        Marker(map).apply {
                            position = point
                            title = "Selected Location"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        }
                    )
                }
                map.invalidate()
            }
        )

        Text(
            "Tap on map to select location",
            color = Color.Black,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(18.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White)
                .padding(horizontal = 18.dp, vertical = 12.dp)
        )

        Button(
            onClick = {
                selectedPoint?.let { onConfirmLocation(it.latitude, it.longitude) }
            },
            enabled = selectedPoint != null,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 48.dp)
        ) {
            Text("Confirm Location")
        }
    }
}

private fun defaultCenter(scraps: List<Scrap>): GeoPoint {
    val firstScrap = scraps.firstOrNull { it.latitude != 0.0 || it.longitude != 0.0 }
    return firstScrap?.let { GeoPoint(it.latitude, it.longitude) } ?: GeoPoint(19.0760, 72.8777)
}
