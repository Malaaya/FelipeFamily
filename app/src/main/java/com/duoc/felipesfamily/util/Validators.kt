package com.duoc.felipesfamily.util

import java.security.MessageDigest

/**
 * Reglas de validación, separadas de la interfaz (no usan Compose).
 * Cada función devuelve el mensaje de error, o null si el valor es válido.
 */
object Validators {
    private val aliasRegex = Regex("^[A-Za-z0-9_]{4,20}$")
    private val periodRegex = Regex("""^(\d{1,2})/(\d{1,2})\s*[-–]\s*(\d{1,2})/(\d{1,2})$""")

    fun required(value: String, message: String): String? =
        if (value.isBlank()) message else null

    fun alias(value: String): String? = when {
        value.isBlank() -> "El alias es obligatorio"
        !aliasRegex.matches(value.trim()) -> "4 a 20 caracteres: letras, números o _"
        else -> null
    }

    fun password(value: String): String? = when {
        value.isBlank() -> "La contraseña es obligatoria"
        value.length < 6 -> "Mínimo 6 caracteres"
        else -> null
    }

    fun repeat(password: String, repeat: String): String? =
        if (repeat != password) "Las contraseñas no coinciden" else null

    fun zone(value: String): String? =
        if (value.isBlank()) "Ingresa la zona referencial" else null

    fun parseSurface(value: String): Double? = value.trim().replace(',', '.').toDoubleOrNull()

    fun surface(value: String): String? {
        val n = parseSurface(value)
        return when {
            value.isBlank() -> "Ingresa la superficie"
            n == null -> "Ingresa un número válido"
            n <= 0.0 -> "Ingresa una superficie mayor a 0"
            n > 10000 -> "Máximo 10.000 ha"
            else -> null
        }
    }

    fun intInRange(value: String, min: Int, max: Int): String? {
        val n = value.trim().toIntOrNull()
        return if (n == null || n < min || n > max) "Ingresa un número entre $min y $max" else null
    }

    /** Convierte "dd/mm - dd/mm" en (inicio, fin) comparables; null si el formato es inválido. */
    fun parsePeriod(value: String): Pair<Int, Int>? {
        val m = periodRegex.matchEntire(value.trim()) ?: return null
        val g = m.groupValues.drop(1).map { it.toInt() }
        if (g[0] !in 1..31 || g[2] !in 1..31 || g[1] !in 1..12 || g[3] !in 1..12) return null
        return (g[1] * 100 + g[0]) to (g[3] * 100 + g[2])
    }

    fun period(value: String): String? {
        if (value.isBlank()) return "Ingresa la ventana de disponibilidad"
        val p = parsePeriod(value) ?: return "Formato: dd/mm - dd/mm (ej: 15/11 - 30/11)"
        return if (p.first > p.second) "La fecha final debe ser posterior a la inicial" else null
    }

    fun overlaps(a: String, b: String): Boolean {
        val x = parsePeriod(a) ?: return false
        val y = parsePeriod(b) ?: return false
        return x.first <= y.second && y.first <= x.second
    }

    /** Hash SHA-256 para no guardar la contraseña en texto plano. */
    fun hash(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
