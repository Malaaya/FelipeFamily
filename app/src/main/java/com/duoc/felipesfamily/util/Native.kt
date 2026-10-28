package com.duoc.felipesfamily.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.duoc.felipesfamily.MainActivity
import org.json.JSONArray

/** Recurso nativo 1: notificaciones locales (con permiso POST_NOTIFICATIONS en Android 13+). */
object NotificationHelper {
    private const val CHANNEL_ID = "felipe_family_matching"

    fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    @SuppressLint("MissingPermission")
    fun show(context: Context, title: String, text: String) {
        if (!canNotify(context)) return
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL_ID, "Avisos de vinculación", NotificationManager.IMPORTANCE_DEFAULT)
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
    }
}

data class CatalogItem(val name: String, val criteria: String, val radiusKm: Int)

/**
 * Recurso nativo 2: archivos. Lee un JSON elegido con el selector del sistema (sin permisos extra).
 * Formato: [{"name":"Fumigación con dron","criteria":"zona, cultivo","radiusKm":40}]
 */
object CatalogImporter {
    fun read(context: Context, uri: Uri): List<CatalogItem> {
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: error("No se pudo abrir el archivo")
        val array = JSONArray(text)
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            CatalogItem(
                name = o.getString("name").trim(),
                criteria = o.optString("criteria", "zona, cultivo, capacidad, disponibilidad"),
                radiusKm = o.optInt("radiusKm", 40)
            )
        }
    }
}
