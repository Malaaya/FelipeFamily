package com.duoc.felipesfamily.ui

import android.annotation.SuppressLint
import android.view.MotionEvent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.duoc.felipesfamily.util.ZoneCoords
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.io.File

/** Punto a mostrar: la necesidad del productor (NEED) o un prestador (PROVIDER) con su cobertura. */
data class MapPoint(
    val title: String,
    val snippet: String,
    val zone: String,
    val isNeed: Boolean = false,
    val coverageKm: Int = 0
)

private const val NEED_COLOR = 0xFF2E7D32.toInt()
private const val PROVIDER_COLOR = 0xFFEF6C00.toInt()

/**
 * Mapa interactivo (OpenStreetMap vía osmdroid, sin API key): zoom, desplazamiento,
 * marcadores con info al tocar y círculo de cobertura de cada prestador.
 */
@SuppressLint("ClickableViewAccessibility")
@Composable
fun ZoneMap(points: List<MapPoint>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val located = points.mapNotNull { p -> ZoneCoords.of(p.zone)?.let { p to it } }
    if (located.isEmpty()) return

    val mapView = remember {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidBasePath = File(context.cacheDir, "osmdroid")
            osmdroidTileCache = File(context.cacheDir, "osmdroid/tiles")
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            // Evita que el scroll de la pantalla se robe los gestos del mapa.
            setOnTouchListener { v, e ->
                v.parent?.requestDisallowInterceptTouchEvent(
                    e.action != MotionEvent.ACTION_UP && e.action != MotionEvent.ACTION_CANCEL
                )
                false
            }
            onResume()
        }
    }
    DisposableEffect(Unit) { onDispose { mapView.onPause(); mapView.onDetach() } }

    Column(modifier) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(12.dp)),
            update = { map ->
                map.overlays.clear()
                located.forEach { (p, geo) ->
                    val color = if (p.isNeed) NEED_COLOR else PROVIDER_COLOR
                    if (!p.isNeed && p.coverageKm > 0) {
                        map.overlays.add(Polygon().apply {
                            this.points = Polygon.pointsAsCircle(geo, p.coverageKm * 1000.0)
                            fillPaint.color = (color and 0x00FFFFFF) or 0x22000000
                            outlinePaint.color = color
                            outlinePaint.strokeWidth = 2f
                        })
                    }
                    map.overlays.add(Marker(map).apply {
                        position = geo
                        title = p.title
                        subDescription = p.snippet
                        snippet = p.snippet
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        icon = icon?.mutate()?.also { it.setTint(color) }
                    })
                }
                val geos = located.map { it.second }.distinct()
                if (geos.size == 1) {
                    map.controller.setZoom(9.0)
                    map.controller.setCenter(geos.first())
                } else {
                    val box = BoundingBox.fromGeoPoints(geos)
                    map.post { map.zoomToBoundingBox(box.increaseByScale(1.6f), false) }
                }
                map.invalidate()
            }
        )
        Text(
            "Verde: necesidad · Naranja: prestador y su cobertura. Ubicación referencial, no exacta.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
